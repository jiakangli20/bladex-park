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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 使用 LibreOffice 将生成后的 Office 文件转换为 PDF，保证预览和打印使用同一份数据文件.
 *
 * @author BladeX
 */
@Slf4j
@Service
public class OfficePdfConversionService {

	private static final long MAX_FILE_BYTES = 30L * 1024L * 1024L;
	private static final long MAX_EXPANDED_FILE_BYTES = 120L * 1024L * 1024L;
	private static final int MAX_CONCURRENT_CONVERSIONS = 2;
	private static final Path CACHE_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "bladex-office-preview-cache");
	private static final String FONTCONFIG_RESOURCE = "fontconfig/contract-preview-fonts.conf";
	private static final String PREVIEW_RENDER_VERSION = "font-v2";
	private static final Pattern CACHE_KEY_PATTERN = Pattern.compile("^" + PREVIEW_RENDER_VERSION + "-[0-9a-f]{64}$");

	@Value("${blade.contract.preview.soffice-command:soffice}")
	private String sofficeCommand;

	@Value("${blade.contract.preview.timeout-seconds:60}")
	private long timeoutSeconds;

	@Value("${blade.contract.preview.cache-ttl-hours:24}")
	private long cacheTtlHours;

	@Value("${blade.contract.preview.cache-max-bytes:536870912}")
	private long cacheMaxBytes;

	@Value("${blade.contract.preview.cache-maintenance-interval-seconds:300}")
	private long maintenanceIntervalSeconds;

	private final Semaphore conversionSlots = new Semaphore(MAX_CONCURRENT_CONVERSIONS);
	private final Map<String, Object> cacheLocks = new ConcurrentHashMap<>();
	private final AtomicLong lastMaintenanceAt = new AtomicLong(0L);

	public byte[] convert(ContractNoticeFileVO document) {
		PreparedPreview preview = prepare(document);
		return readCachedPdf(preview.cacheKey());
	}

	/**
	 * 准备可复用的 PDF 预览文件。OOXML 文件按可见内容计算摘要，忽略 ZIP 时间戳和文档属性，
	 * 避免同一业务数据每次生成 Office 文件后都重复启动 LibreOffice。
	 */
	public PreparedPreview prepare(ContractNoticeFileVO document) {
		validate(document);
		long startedAt = System.nanoTime();
		String extension = extension(document.getFileName());
		String cacheKey = PREVIEW_RENDER_VERSION + "-" + contentSha256(document.getFileBytes(), extension);
		Path cacheFile = CACHE_ROOT.resolve(cacheKey + ".pdf");
		Object lock = cacheLocks.computeIfAbsent(cacheKey, key -> new Object());
		try {
			Files.createDirectories(CACHE_ROOT);
			maintainCache(cacheKey, false);
			if (Files.isRegularFile(cacheFile) && Files.size(cacheFile) > 0) {
				touch(cacheFile);
				long elapsedMillis = elapsedMillis(startedAt);
				long fileSize = Files.size(cacheFile);
				log.info("Office 预览缓存命中, fileName={}, cacheKey={}, pdfBytes={}, elapsedMs={}",
					document.getFileName(), cacheKey, fileSize, elapsedMillis);
				return new PreparedPreview(cacheKey, true, fileSize, elapsedMillis);
			}
			long waitingAt = System.nanoTime();
			synchronized (lock) {
				long waitMillis = elapsedMillis(waitingAt);
				if (Files.isRegularFile(cacheFile) && Files.size(cacheFile) > 0) {
					touch(cacheFile);
					long elapsedMillis = elapsedMillis(startedAt);
					long fileSize = Files.size(cacheFile);
					log.info("Office 预览等待后命中缓存, fileName={}, cacheKey={}, pdfBytes={}, waitMs={}, elapsedMs={}",
						document.getFileName(), cacheKey, fileSize, waitMillis, elapsedMillis);
					return new PreparedPreview(cacheKey, true, fileSize, elapsedMillis);
				}
				long conversionAt = System.nanoTime();
				byte[] pdfBytes = convertWithLibreOffice(document.getFileBytes(), extension);
				Files.write(cacheFile, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				maintainCache(cacheKey, true);
				long elapsedMillis = elapsedMillis(startedAt);
				log.info("Office 预览转换完成, fileName={}, cacheKey={}, pdfBytes={}, waitMs={}, convertMs={}, elapsedMs={}",
					document.getFileName(), cacheKey, pdfBytes.length, waitMillis, elapsedMillis(conversionAt), elapsedMillis);
				return new PreparedPreview(cacheKey, false, pdfBytes.length, elapsedMillis);
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new ServiceException("Office 预览转换被中断，请稍后重试");
		} catch (Exception exception) {
			log.error("Office 文件转换 PDF 失败, fileName={}", document.getFileName(), exception);
			throw new ServiceException("Office 预览转换失败，请确认服务器已安装 LibreOffice 且中文字体完整");
		} finally {
			cacheLocks.remove(cacheKey, lock);
		}
	}

	public byte[] readCachedPdf(String cacheKey) {
		if (StringUtil.isBlank(cacheKey) || !CACHE_KEY_PATTERN.matcher(cacheKey).matches()) {
			throw new ServiceException("预览地址无效，请重新打开");
		}
		Path cacheFile = CACHE_ROOT.resolve(cacheKey + ".pdf");
		try {
			if (!Files.isRegularFile(cacheFile) || Files.size(cacheFile) <= 0) {
				throw new ServiceException("预览已失效，请重新打开");
			}
			if (isExpired(cacheFile, System.currentTimeMillis())) {
				Files.deleteIfExists(cacheFile);
				throw new ServiceException("预览已失效，请重新打开");
			}
			touch(cacheFile);
			return Files.readAllBytes(cacheFile);
		} catch (ServiceException exception) {
			throw exception;
		} catch (IOException exception) {
			log.error("读取 Office 预览缓存失败, cacheKey={}", cacheKey, exception);
			throw new ServiceException("预览文件读取失败，请重新打开");
		}
	}

	private void maintainCache(String protectedCacheKey, boolean force) {
		long now = System.currentTimeMillis();
		long intervalMillis = TimeUnit.SECONDS.toMillis(Math.max(10L, maintenanceIntervalSeconds));
		long previous = lastMaintenanceAt.get();
		if (!force && now - previous < intervalMillis) {
			return;
		}
		if (!lastMaintenanceAt.compareAndSet(previous, now) && !force) {
			return;
		}
		try (var paths = Files.list(CACHE_ROOT)) {
			List<Path> cacheFiles = paths
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".pdf"))
				.filter(path -> !path.getFileName().toString().equals(protectedCacheKey + ".pdf"))
				.filter(path -> !isCacheFileLocked(path))
				.sorted(Comparator.comparingLong(this::lastModifiedMillis))
				.toList();
			for (Path path : cacheFiles) {
				if (isExpired(path, now)) {
					Files.deleteIfExists(path);
				}
			}
			long totalBytes = cacheSizeBytes();
			if (totalBytes <= Math.max(0L, cacheMaxBytes)) {
				return;
			}
			for (Path path : cacheFiles) {
				if (!Files.exists(path)) {
					continue;
				}
				long size = Files.size(path);
				Files.deleteIfExists(path);
				totalBytes -= size;
				if (totalBytes <= Math.max(0L, cacheMaxBytes)) {
					break;
				}
			}
		} catch (IOException exception) {
			log.warn("维护 Office 预览缓存失败", exception);
		}
	}

	private boolean isCacheFileLocked(Path path) {
		String fileName = path.getFileName().toString();
		String cacheKey = fileName.substring(0, fileName.length() - ".pdf".length());
		return cacheLocks.containsKey(cacheKey);
	}

	private boolean isExpired(Path path, long now) {
		long ttlMillis = TimeUnit.HOURS.toMillis(Math.max(1L, cacheTtlHours));
		return now - lastModifiedMillis(path) > ttlMillis;
	}

	private long cacheSizeBytes() throws IOException {
		try (var paths = Files.list(CACHE_ROOT)) {
			return paths.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".pdf"))
				.mapToLong(path -> {
					try {
						return Files.size(path);
					} catch (IOException ignored) {
						return 0L;
					}
				})
				.sum();
		}
	}

	private long lastModifiedMillis(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		} catch (IOException ignored) {
			return 0L;
		}
	}

	private void touch(Path path) {
		try {
			Files.setLastModifiedTime(path, FileTime.fromMillis(System.currentTimeMillis()));
		} catch (IOException exception) {
			log.debug("更新 Office 预览缓存访问时间失败: {}", path, exception);
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

			String executable = resolveSofficeExecutable();
			ProcessBuilder processBuilder = new ProcessBuilder(
				executable,
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

	private String resolveSofficeExecutable() throws IOException {
		if (StringUtil.isNotBlank(sofficeCommand) && sofficeCommand.contains("/")) {
			Path configured = Path.of(sofficeCommand).toAbsolutePath().normalize();
			if (Files.isExecutable(configured)) {
				return configured.toString();
			}
			throw new IOException("配置的 LibreOffice 命令不可执行：" + configured);
		}

		Set<Path> candidates = new LinkedHashSet<>();
		String commandName = StringUtil.isBlank(sofficeCommand) ? "soffice" : sofficeCommand.trim();
		String pathValue = System.getenv("PATH");
		if (StringUtil.isNotBlank(pathValue)) {
			for (String directory : pathValue.split(java.io.File.pathSeparator)) {
				if (StringUtil.isNotBlank(directory)) {
					candidates.add(Path.of(directory).resolve(commandName));
				}
			}
		}
		candidates.addAll(List.of(
			Path.of("/Applications/LibreOffice.app/Contents/MacOS/soffice"),
			Path.of("/usr/bin/soffice"),
			Path.of("/usr/local/bin/soffice"),
			Path.of("/opt/homebrew/bin/soffice"),
			Path.of("/opt/libreoffice/program/soffice")
		));
		String userHome = System.getProperty("user.home");
		if (StringUtil.isNotBlank(userHome)) {
			candidates.add(Path.of(userHome, ".cache", "codex-runtimes", "codex-primary-runtime", "dependencies", "bin", "override", "soffice"));
		}
		return candidates.stream()
			.map(Path::toAbsolutePath)
			.filter(Files::isExecutable)
			.map(Path::toString)
			.findFirst()
			.orElseThrow(() -> new IOException("未找到 LibreOffice 可执行文件，请配置 blade.contract.preview.soffice-command"));
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

	private String contentSha256(byte[] bytes, String extension) {
		if (!("docx".equals(extension) || "xlsx".equals(extension))) {
			return sha256(bytes);
		}
		try {
			return stableOpenXmlSha256(bytes);
		} catch (IOException exception) {
			log.warn("Office 稳定摘要计算失败，回退到原始文件摘要, extension={}", extension, exception);
			return sha256(bytes);
		}
	}

	private String stableOpenXmlSha256(byte[] bytes) throws IOException {
		List<OpenXmlEntryDigest> entries = new ArrayList<>();
		long expandedBytes = 0L;
		byte[] buffer = new byte[8192];
		try (ZipInputStream zipInput = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			ZipEntry entry;
			while ((entry = zipInput.getNextEntry()) != null) {
				String entryName = entry.getName() == null ? "" : entry.getName().replace('\\', '/');
				if (entry.isDirectory() || isIgnoredOpenXmlEntry(entryName)) {
					zipInput.closeEntry();
					continue;
				}
				MessageDigest entryDigest = messageDigest();
				int read;
				while ((read = zipInput.read(buffer)) != -1) {
					expandedBytes += read;
					if (expandedBytes > MAX_EXPANDED_FILE_BYTES) {
						throw new IOException("Office 文件解压后内容超过 120MB");
					}
					entryDigest.update(buffer, 0, read);
				}
				entries.add(new OpenXmlEntryDigest(entryName, entryDigest.digest()));
				zipInput.closeEntry();
			}
		}
		if (entries.isEmpty()) {
			throw new IOException("Office 文件中未找到可摘要内容");
		}
		entries.sort(Comparator.comparing(OpenXmlEntryDigest::name).thenComparing(entry -> HexFormat.of().formatHex(entry.digest())));
		MessageDigest digest = messageDigest();
		for (OpenXmlEntryDigest entry : entries) {
			digest.update(entry.name().getBytes(StandardCharsets.UTF_8));
			digest.update((byte) 0);
			digest.update(entry.digest());
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private boolean isIgnoredOpenXmlEntry(String entryName) {
		String normalized = entryName.toLowerCase(Locale.ROOT);
		return "docprops/core.xml".equals(normalized) || "docprops/app.xml".equals(normalized);
	}

	private MessageDigest messageDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (Exception exception) {
			throw new ServiceException("预览文件摘要计算失败");
		}
	}

	private long elapsedMillis(long startedAt) {
		return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
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

	public record PreparedPreview(String cacheKey, boolean cacheHit, long fileSize, long elapsedMillis) {
	}

	private record OpenXmlEntryDigest(String name, byte[] digest) {
	}
}
