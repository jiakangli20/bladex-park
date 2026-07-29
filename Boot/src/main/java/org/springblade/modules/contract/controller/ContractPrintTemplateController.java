package org.springblade.modules.contract.controller;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.contract.pojo.entity.ContractPrintTemplate;
import org.springblade.modules.contract.service.IContractPrintTemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@NonDS
@RestController
@RequiredArgsConstructor
@PreAuth(menu = "contract_print_template")
@RequestMapping("/blade-contract/print-template")
public class ContractPrintTemplateController {
	private final IContractPrintTemplateService templateService;

	@GetMapping("/list")
	public R<List<ContractPrintTemplate>> list() {
		return R.data(templateService.listTemplates());
	}

	@PostMapping("/upload")
	public R<ContractPrintTemplate> upload(@RequestParam String businessType,
											 @RequestParam String templateName,
											 @RequestParam String versionNo,
											 @RequestParam(required = false) String remark,
											 @RequestParam MultipartFile file) {
		return R.data(templateService.uploadTemplate(businessType, templateName, versionNo, remark, file));
	}

	@PostMapping("/enable/{templateId}")
	public R enable(@PathVariable Long templateId) {
		return R.status(templateService.enableTemplate(templateId));
	}

	@PostMapping("/remove/{templateId}")
	public R remove(@PathVariable Long templateId) {
		return R.status(templateService.removeTemplate(templateId));
	}
}
