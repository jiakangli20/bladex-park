package org.springblade.modules.ai.service;

import org.springblade.modules.ai.pojo.entity.AiMessage;

import java.util.List;
import java.util.function.Consumer;

/**
 * 可插拔的问答领域。新增客户问答时实现该接口即可接入同一套会话、鉴权和存储机制。
 */
public interface AiDomainHandler {

	String domain();

	boolean supports(String question, List<AiMessage> recentMessages);

	String answer(String question, List<AiMessage> recentMessages);

	default void streamAnswer(String question, List<AiMessage> recentMessages, Consumer<String> chunkConsumer) {
		chunkConsumer.accept(answer(question, recentMessages));
	}
}
