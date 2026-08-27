package org.springblade.modules.ai.pojo.dto;

import java.util.List;

/**
 * AI 请求的可信访问范围，在请求线程捕获后传入异步处理。
 * authorizedParkIds 为 null 表示全部园区，空集合表示无园区权限。
 */
public record AiAccessContext(Long userId, String tenantId, List<Long> authorizedParkIds) {

	public AiAccessContext {
		authorizedParkIds = authorizedParkIds == null ? null : List.copyOf(authorizedParkIds);
	}
}
