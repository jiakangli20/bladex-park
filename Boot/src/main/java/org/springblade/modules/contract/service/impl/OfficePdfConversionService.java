/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 使用 LibreOffice 将生成后的 Office 文件转换为 PDF，保证预览和打印使用同一份数据文件.
 *
 * @author BladeX
 */
@Slf4j
@Service
public class OfficePdfConversionService {

	private static final long MAX_FILE_BYTES = 30L * 1024L * 1024L;
	private static final int MAX_CONCURRENT_CONVERSIONS = 2;
	private static final Path CACHE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "bladex-office-preview-cache");
	private static final String FONTCONFIG_RESOURCE = "fontconfig/contract-preview-fonts.conf";
	private static final String PREVIEW_RENDER_VERSION = "font-v1";

	@Value("${blade.contract.preview.soffice-command:soffice}")
	private String sofficeCommand;

	@Value("${blade.contract.preview.timeout-seconds:60}")
	private long timeoutSeconds;

	private final Semaphore conversionSlots = new Semaphore(MAX_CONCURRENT_CONVERSIONS);
	private final Map<String, Object> cacheLocks = new ConcurrentHashMap<>();

	public byte[] convert(ContractNoticeFileVO document) {
		validate(document);
		String extension = extension(document.getFileName());
		String cacheKey = PREVIEW_RENDER_VERSION + "-" + sha256(document.getFileBytes());
		Path cacheFile = CACHE_ROOT.resolve(cacheKey + ".pdf");
		try {
			Files.createDirectories(CACHE_ROOT);
			if (Files.isRegularFile(cacheFile) && Files.size(cacheFile) > 0) {
				return Files.readAllBytes(cacheFile);
			}
			Object lock = cacheLocks.computeIfAbsent(cacheKey, key -> new Object());
			synchronized (lock) {
				if (Files.isRegularFile(cacheFile) && Files.size(cacheFile) > 0) {
					return Files.readAllBytes(cacheFile);
				}
				byte[] pdfBytes = convertWithLibreOffice(document.getFileBytes(), extension);
				Files.write(cacheFile, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				return pdfBytes;
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ServiceException("Office 预览转换被中断，请稍后重试");
		} catch (Exception exception) {
			log.error("Office 文件转换 PDF 失败, fileName={}", document.getFileName(), exception);
			throw new ServiceException("Office 预览转换失败，请确认服务器已安装 LibreOffice 且中文字体完整");
		} finally {
			cacheLocks.remove(cacheKey);
		}
	}

	private byte[] convertWithLibreOffice(byte[] fileBytes, String extension) throws Exception {
		conversionSlots.acquire();
		Path workDir = null;
		try {
			workDir = Files.createTempDirectory(CACHE_ROOT, "work-");
			Path sourceFile = workDir.resolve("document." + extension);
			Path outputDir = Files.createDirectories(workDir.resolve("output"));
			Path profileDir = Files.createDirectories(workDir.resolve("profile"));
			Path fontConfigFile = workDir.resolve("fonts.conf");
			Path logFile = workDir.resolve("soffice.log");
			Files.write(sourceFile, fileBytes, StandardOpenOption.CREATE_NEW);
			copyFontConfig(fontConfigFile);

			ProcessBuilder processBuilder = new ProcessBuilder(
				sofficeCommand,
				"--headless",
				"--nologo",
				"--nodefault",
				"--nofirststartwizard",
				"-env:UserInstallation=" + profileDir.toUri(),
				"--convert-to",
				"pdf",
				"--outdir",
				outputDir.toString(),
				sourceFile.toString()
			)
				.directory(workDir.toFile())
				.redirectErrorStream(true)
				.redirectOutput(logFile.toFile());
			processBuilder.environment().put("FONTCONFIG_FILE", fontConfigFile.toString());
			processBuilder.environment().put("HOME", profileDir.toString());
			processBuilder.environment().put("XDG_CACHE_HOME", profileDir.resolve("cache").toString());
			Process process = processBuilder.start();

			boolean completed = process.waitFor(Math.max(10L, timeoutSeconds), TimeUnit.SECONDS);
			if (!completed) {
				process.destroyForcibly();
				throw new IOException("LibreOffice 转换超时");
			}
			Path pdfFile = outputDir.resolve("document.pdf");
			if (process.exitValue() != 0 || !Files.isRegularFile(pdfFile) || Files.size(pdfFile) == 0) {
				String processOutput = Files.isRegularFile(logFile) ? Files.readString(logFile) : "";
				throw new IOException("LibreOffice 转换失败, exit=" + process.exitValue() + ", output=" + processOutput);
			}
			return Files.readAllBytes(pdfFile);
		} finally {
			conversionSlots.release();
			deleteDirectory(workDir);
		}
	}

	private void copyFontConfig(Path target) throws IOException {
		ClassPathResource resource = new ClassPathResource(FONTCONFIG_RESOURCE);
		try (var input = resource.getInputStream()) {
			Files.copy(input, target);
		}
	}

	private void validate(ContractNoticeFileVO document) {
		if (document == null || document.getFileBytes() == null || document.getFileBytes().length == 0) {
			throw new ServiceException("预览文件内容不能为空");
		}
		if (document.getFileBytes().length > MAX_FILE_BYTES) {
			throw new ServiceException("预览文件不能超过 30MB");
		}
		String extension = extension(document.getFileName());
		if (!("doc".equals(extension) || "docx".equals(extension) || "xls".equals(extension) || "xlsx".equals(extension))) {
			throw new ServiceException("暂不支持该文件格式预览");
		}
	}

	private String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (Exception exception) {
			throw new ServiceException("预览文件摘要计算失败");
		}
	}

	private String extension(String fileName) {
		if (StringUtil.isBlank(fileName)) {
			return "";
		}
		int index = fileName.lastIndexOf('.');
		return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
	}

	private void deleteDirectory(Path directory) {
		if (directory == null || !Files.exists(directory)) {
			return;
		}
		try (var paths = Files.walk(directory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException exception) {
					log.debug("清理 Office 预览临时文件失败: {}", path, exception);
				}
			});
		} catch (IOException exception) {
			log.debug("清理 Office 预览临时目录失败: {}", directory, exception);
		}
	}
}
