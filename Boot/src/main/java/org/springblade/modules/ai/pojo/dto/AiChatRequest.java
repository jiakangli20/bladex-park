package org.springblade.modules.ai.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiChatRequest {

	private Long conversationId;

	@NotBlank(message = "请输入问题")
	@Size(max = 500, message = "问题不能超过500个字符")
	private String content;
}
