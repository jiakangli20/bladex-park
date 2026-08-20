package org.springblade.modules.ai.pojo.vo;

import lombok.Data;

@Data
public class AiChatResponseVO {
	private Long conversationId;
	private AiChatMessageVO userMessage;
	private AiChatMessageVO assistantMessage;
}
