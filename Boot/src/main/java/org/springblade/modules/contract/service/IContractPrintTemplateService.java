package org.springblade.modules.contract.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.contract.pojo.entity.ContractPrintTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IContractPrintTemplateService extends IService<ContractPrintTemplate> {
	List<ContractPrintTemplate> listTemplates();
	ContractPrintTemplate uploadTemplate(String businessType, String templateName, String versionNo, String remark, MultipartFile file);
	boolean enableTemplate(Long templateId);
	boolean removeTemplate(Long templateId);
	String resolveEnabledTemplateSource(String businessType);
}
