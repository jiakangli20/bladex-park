package org.springblade.modules.contract.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oss.model.BladeFile;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.FileUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.mapper.ContractPrintTemplateMapper;
import org.springblade.modules.contract.pojo.entity.ContractPrintTemplate;
import org.springblade.modules.contract.service.IContractPrintTemplateService;
import org.springblade.modules.resource.builder.OssBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ContractPrintTemplateServiceImpl extends ServiceImpl<ContractPrintTemplateMapper, ContractPrintTemplate>
	implements IContractPrintTemplateService {

	private static final long MAX_TEMPLATE_BYTES = 20L * 1024 * 1024;
	private static final Set<String> ALLOWED_SUFFIXES = Set.of("doc", "docx", "xls", "xlsx");
	private final OssBuilder ossBuilder;

	@Override
	public List<ContractPrintTemplate> listTemplates() {
		return list(Wrappers.<ContractPrintTemplate>lambdaQuery()
			.eq(ContractPrintTemplate::getDelFlag, "0")
			.orderByDesc(ContractPrintTemplate::getEnabledFlag)
			.orderByDesc(ContractPrintTemplate::getCreateTime));
	}

	@Override
	@SneakyThrows
	@Transactional(rollbackFor = Exception.class)
	public ContractPrintTemplate uploadTemplate(String businessType, String templateName, String versionNo,
											 String remark, MultipartFile file) {
		if (StringUtil.isBlank(businessType) || StringUtil.isBlank(templateName) || StringUtil.isBlank(versionNo)) {
			throw new ServiceException("业务类型、模板名称和版本号不能为空");
		}
		if (file == null || file.isEmpty()) {
			throw new ServiceException("请选择模板文件");
		}
		if (file.getSize() > MAX_TEMPLATE_BYTES) {
			throw new ServiceException("模板文件不能超过20MB");
		}
		String fileName = StringUtil.isBlank(file.getOriginalFilename()) ? "template" : file.getOriginalFilename();
		String suffix = FileUtil.getFileExtension(fileName).toLowerCase(Locale.ROOT);
		if (!ALLOWED_SUFFIXES.contains(suffix)) {
			throw new ServiceException("仅支持 doc、docx、xls、xlsx 模板");
		}
		validateOfficeHeader(file, suffix);
		BladeFile stored = ossBuilder.template().putFile(fileName, file.getInputStream());
		ContractPrintTemplate entity = new ContractPrintTemplate();
		entity.setBusinessType(businessType.trim());
		entity.setTemplateName(templateName.trim());
		entity.setVersionNo(versionNo.trim());
		entity.setFileName(fileName);
		entity.setFileUrl(stored.getLink());
		entity.setFileObjectName(stored.getName());
		entity.setFileSuffix(suffix);
		entity.setFileSize(file.getSize());
		entity.setEnabledFlag("0");
		entity.setBuiltinFlag("0");
		entity.setRemark(remark);
		entity.setDelFlag("0");
		entity.setCreateBy(currentUser());
		entity.setCreateTime(DateUtil.now());
		if (!save(entity)) {
			throw new ServiceException("模板信息保存失败");
		}
		return entity;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean enableTemplate(Long templateId) {
		ContractPrintTemplate template = getById(templateId);
		if (template == null || "1".equals(template.getDelFlag())) {
			throw new ServiceException("模板不存在");
		}
		update(Wrappers.<ContractPrintTemplate>lambdaUpdate()
			.eq(ContractPrintTemplate::getBusinessType, template.getBusinessType())
			.eq(ContractPrintTemplate::getDelFlag, "0")
			.set(ContractPrintTemplate::getEnabledFlag, "0")
			.set(ContractPrintTemplate::getUpdateBy, currentUser())
			.set(ContractPrintTemplate::getUpdateTime, new Date()));
		return update(Wrappers.<ContractPrintTemplate>lambdaUpdate()
			.eq(ContractPrintTemplate::getTemplateId, templateId)
			.set(ContractPrintTemplate::getEnabledFlag, "1")
			.set(ContractPrintTemplate::getUpdateBy, currentUser())
			.set(ContractPrintTemplate::getUpdateTime, new Date()));
	}

	@Override
	public boolean removeTemplate(Long templateId) {
		return update(Wrappers.<ContractPrintTemplate>lambdaUpdate()
			.eq(ContractPrintTemplate::getTemplateId, templateId)
			.set(ContractPrintTemplate::getEnabledFlag, "0")
			.set(ContractPrintTemplate::getDelFlag, "1")
			.set(ContractPrintTemplate::getUpdateBy, currentUser())
			.set(ContractPrintTemplate::getUpdateTime, new Date()));
	}

	@Override
	public String resolveEnabledTemplateSource(String businessType) {
		ContractPrintTemplate template = getOne(Wrappers.<ContractPrintTemplate>lambdaQuery()
			.eq(ContractPrintTemplate::getBusinessType, businessType)
			.eq(ContractPrintTemplate::getEnabledFlag, "1")
			.eq(ContractPrintTemplate::getDelFlag, "0")
			.orderByDesc(ContractPrintTemplate::getUpdateTime)
			.last("limit 1"));
		if (template == null) {
			return null;
		}
		if (StringUtil.isNotBlank(template.getFileObjectName())) {
			return "oss://" + URLEncoder.encode(template.getFileObjectName(), StandardCharsets.UTF_8);
		}
		return template.getFileUrl();
	}

	private void validateOfficeHeader(MultipartFile file, String suffix) throws Exception {
		byte[] header = new byte[8];
		int read;
		try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
			read = input.read(header);
		}
		boolean ole = read >= 8 && (header[0] & 0xff) == 0xD0 && (header[1] & 0xff) == 0xCF
			&& (header[2] & 0xff) == 0x11 && (header[3] & 0xff) == 0xE0;
		boolean zip = read >= 4 && header[0] == 'P' && header[1] == 'K';
		if ((Set.of("doc", "xls").contains(suffix) && !ole)
			|| (Set.of("docx", "xlsx").contains(suffix) && !zip)) {
			throw new ServiceException("模板扩展名与真实文件格式不一致");
		}
		if (Set.of("docx", "xlsx").contains(suffix)) {
			boolean contentTypesFound = false;
			boolean officeRootFound = false;
			try (ZipInputStream zipInput = new ZipInputStream(file.getInputStream())) {
				ZipEntry entry;
				while ((entry = zipInput.getNextEntry()) != null) {
					String name = entry.getName() == null ? "" : entry.getName().replace('\\', '/');
					contentTypesFound |= "[Content_Types].xml".equals(name);
					officeRootFound |= ("docx".equals(suffix) && name.startsWith("word/"))
						|| ("xlsx".equals(suffix) && name.startsWith("xl/"));
					zipInput.closeEntry();
				}
			}
			if (!contentTypesFound || !officeRootFound) {
				throw new ServiceException("模板内部结构与扩展名不一致");
			}
		}
	}

	private String currentUser() {
		return StringUtil.isBlank(AuthUtil.getUserName()) ? "system" : AuthUtil.getUserName();
	}
}
