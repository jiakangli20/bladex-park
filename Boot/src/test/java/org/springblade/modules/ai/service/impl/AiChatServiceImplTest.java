package org.springblade.modules.ai.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.ai.mapper.AiConversationMapper;
import org.springblade.modules.ai.mapper.AiMessageMapper;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiChatRequest;
import org.springblade.modules.ai.pojo.entity.AiConversation;
import org.springblade.modules.ai.pojo.entity.AiMessage;
import org.springblade.modules.ai.pojo.vo.AiChatResponseVO;
import org.springblade.modules.ai.service.AiDomainHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatServiceImplTest {

	@Test
	void reportIdIsPersistedAndReturnedInMessageHistoryContract() {
		AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
		AiMessageMapper messageMapper = mock(AiMessageMapper.class);
		AiDomainHandler handler = enterpriseHandler();
		when(handler.supports(any(), any())).thenReturn(true);
		when(handler.answer(any(), any(), any())).thenReturn(new AiDomainHandler.DomainAnswer("报告已生成", 9001L));
		when(messageMapper.selectList(any())).thenReturn(List.of());
		when(conversationMapper.insert(any(AiConversation.class))).thenAnswer(invocation -> {
			((AiConversation) invocation.getArgument(0)).setId(7001L);
			return 1;
		});
		when(messageMapper.insert(any(AiMessage.class))).thenAnswer(invocation -> {
			AiMessage message = invocation.getArgument(0);
			message.setId("user".equals(message.getRole()) ? 8001L : 8002L);
			return 1;
		});
		AiChatServiceImpl service = new AiChatServiceImpl(conversationMapper, messageMapper, List.of(handler));
		AiChatRequest request = new AiChatRequest();
		request.setDomain("enterprise");
		request.setContent("为测试企业生成报告");

		AiChatResponseVO response = service.send(request, new AiAccessContext(100L, "tenant-a", List.of(2001L)));

		assertEquals(9001L, response.getAssistantMessage().getReportId());
		ArgumentCaptor<AiMessage> captor = ArgumentCaptor.forClass(AiMessage.class);
		org.mockito.Mockito.verify(messageMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
		assertEquals(9001L, captor.getAllValues().get(1).getReportId());
	}

	@Test
	void rejectsContinuingConversationInAnotherDomain() {
		AiConversationMapper conversationMapper = mock(AiConversationMapper.class);
		AiMessageMapper messageMapper = mock(AiMessageMapper.class);
		AiConversation existing = new AiConversation();
		existing.setId(7001L);
		existing.setDomain("property");
		when(conversationMapper.selectOne(any())).thenReturn(existing);
		AiChatServiceImpl service = new AiChatServiceImpl(conversationMapper, messageMapper, List.of(enterpriseHandler()));
		AiChatRequest request = new AiChatRequest();
		request.setConversationId(7001L);
		request.setDomain("enterprise");
		request.setContent("查询测试企业");

		ServiceException exception = assertThrows(ServiceException.class,
			() -> service.send(request, new AiAccessContext(100L, "tenant-a", List.of(2001L))));

		assertEquals("会话领域不匹配，请新建对话", exception.getMessage());
	}

	private AiDomainHandler enterpriseHandler() {
		AiDomainHandler handler = mock(AiDomainHandler.class);
		when(handler.domain()).thenReturn("enterprise");
		return handler;
	}
}
