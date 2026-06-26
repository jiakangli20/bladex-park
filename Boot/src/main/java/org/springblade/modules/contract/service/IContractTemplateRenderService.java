/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service;

import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;

import java.util.Map;

/**
 * 合同模板填充服务.
 *
 * @author BladeX
 */
public interface IContractTemplateRenderService {

	/**
	 * 按原始模板填充业务字段.
	 *
	 * @param noticeType           文书类型
	 * @param noticeName           文书名称
	 * @param templateRelativePath 模板相对“系统所需材料”的路径
	 * @param fileNamePrefix       输出文件名前缀
	 * @param fields               标签字段，按表格/单元格标签写入相邻值区域
	 * @param replacements         文本替换，适用于模板里已有样例值或占位符
	 * @return 文书文件
	 */
	ContractNoticeFileVO render(String noticeType,
							 String noticeName,
							 String templateRelativePath,
							 String fileNamePrefix,
							 Map<String, String> fields,
							 Map<String, String> replacements);

}
