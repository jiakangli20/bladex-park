package org.springblade.modules.ai.service;

import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiChatRequest;
import org.springblade.modules.ai.pojo.entity.AiConversation;
import org.springblade.modules.ai.pojo.vo.AiChatMessageVO;
import org.springblade.modules.ai.pojo.vo.AiChatResponseVO;

import java.util.List;
import java.util.function.Consumer;

public interface IAiChatService {
	record AiStreamEvent(String type, Object data) {}

	List<AiConversation> conversations();
	List<AiChatMessageVO> messages(Long conversationId);
	void removeConversation(Long conversationId);
	AiChatResponseVO send(AiChatRequest request, AiAccessContext accessContext);
	void sendStream(AiChatRequest request, AiAccessContext accessContext, Consumer<AiStreamEvent> eventConsumer);
}
