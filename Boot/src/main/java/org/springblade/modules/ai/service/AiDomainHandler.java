package org.springblade.modules.ai.service;

import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.entity.AiMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 可插拔的问答领域。新增客户问答时实现该接口即可接入同一套会话、鉴权和存储机制。
 */
public interface AiDomainHandler {
	record DomainAnswer(String content, Long reportId) {
		public static DomainAnswer text(String content) {
			return new DomainAnswer(content, null);
		}
	}

	String domain();

	boolean supports(String question, List<AiMessage> recentMessages);

	DomainAnswer answer(String question, List<AiMessage> recentMessages, AiAccessContext accessContext);

	default DomainAnswer streamAnswer(String question, List<AiMessage> recentMessages, AiAccessContext accessContext, Consumer<String> chunkConsumer) {
		DomainAnswer answer = answer(question, recentMessages, accessContext);
		chunkConsumer.accept(answer.content());
		return answer;
	}

	default String outOfScopeReply() {
		return "当前问题不在该助手的支持范围内。";
	}
}
