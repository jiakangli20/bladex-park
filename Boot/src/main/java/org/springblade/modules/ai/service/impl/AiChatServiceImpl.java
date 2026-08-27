package org.springblade.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.ai.mapper.AiConversationMapper;
import org.springblade.modules.ai.mapper.AiMessageMapper;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiChatRequest;
import org.springblade.modules.ai.pojo.entity.AiConversation;
import org.springblade.modules.ai.pojo.entity.AiMessage;
import org.springblade.modules.ai.pojo.vo.AiChatMessageVO;
import org.springblade.modules.ai.pojo.vo.AiChatResponseVO;
import org.springblade.modules.ai.service.AiDomainHandler;
import org.springblade.modules.ai.service.IAiChatService;
import org.springblade.core.tool.utils.StringUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** 会话生命周期与用户隔离由本服务统一处理，领域处理器不能绕过该边界。 */
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements IAiChatService {

	private final AiConversationMapper conversationMapper;
	private final AiMessageMapper messageMapper;
	private final List<AiDomainHandler> domainHandlers;

	@Override
	public List<AiConversation> conversations() {
		return conversationMapper.selectList(Wrappers.<AiConversation>lambdaQuery()
			.eq(AiConversation::getTenantId, currentTenantId())
			.eq(AiConversation::getUserId, currentUserId())
			.orderByDesc(AiConversation::getLastMessageTime)
			.orderByDesc(AiConversation::getId));
	}

	@Override
	public List<AiChatMessageVO> messages(Long conversationId) {
		requireConversation(conversationId);
		return messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
			.eq(AiMessage::getConversationId, conversationId)
			.eq(AiMessage::getTenantId, currentTenantId())
			.eq(AiMessage::getUserId, currentUserId())
			.orderByAsc(AiMessage::getId)).stream().map(this::toVO).toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeConversation(Long conversationId) {
		requireConversation(conversationId);
		messageMapper.delete(Wrappers.<AiMessage>lambdaQuery()
			.eq(AiMessage::getConversationId, conversationId)
			.eq(AiMessage::getTenantId, currentTenantId())
			.eq(AiMessage::getUserId, currentUserId()));
		conversationMapper.delete(Wrappers.<AiConversation>lambdaQuery()
			.eq(AiConversation::getId, conversationId)
			.eq(AiConversation::getTenantId, currentTenantId())
			.eq(AiConversation::getUserId, currentUserId()));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AiChatResponseVO send(AiChatRequest request, AiAccessContext accessContext) {
		String content = request.getContent() == null ? "" : request.getContent().trim();
		if (StringUtil.isBlank(content)) {
			throw new ServiceException("请输入问题");
		}
		AiAccessContext context = normalizeContext(accessContext);
		AiConversation conversation = request.getConversationId() == null
			? createConversation(content, context.userId(), context.tenantId())
			: requireConversation(request.getConversationId(), context.userId(), context.tenantId());
		List<AiMessage> history = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
			.eq(AiMessage::getConversationId, conversation.getId())
			.eq(AiMessage::getTenantId, context.tenantId())
			.eq(AiMessage::getUserId, context.userId())
			.orderByAsc(AiMessage::getId));
		AiDomainHandler domainHandler = domainHandlers.stream()
			.filter(handler -> handler.supports(content, history))
			.findFirst().orElse(null);
		boolean inScope = domainHandler != null;
		String domain = inScope ? domainHandler.domain() : "property";
		AiMessage userMessage = saveMessage(conversation.getId(), "user", content, domain, inScope, context.userId(), context.tenantId());
		String answer = inScope ? domainHandler.answer(content, recentHistory(history), context) : outOfScopeReply();
		AiMessage assistantMessage = saveMessage(conversation.getId(), "assistant", answer, domain, inScope, context.userId(), context.tenantId());
		Date now = new Date();
		conversation.setDomain(domain);
		conversation.setLastMessageTime(now);
		conversation.setUpdateTime(now);
		conversationMapper.updateById(conversation);
		AiChatResponseVO response = new AiChatResponseVO();
		response.setConversationId(conversation.getId());
		response.setUserMessage(toVO(userMessage));
		response.setAssistantMessage(toVO(assistantMessage));
		return response;
	}

	@Override
	public void sendStream(AiChatRequest request, AiAccessContext accessContext, Consumer<IAiChatService.AiStreamEvent> eventConsumer) {
		String content = request.getContent() == null ? "" : request.getContent().trim();
		if (StringUtil.isBlank(content)) throw new ServiceException("请输入问题");
		AiAccessContext context = normalizeContext(accessContext);
		AiConversation conversation = request.getConversationId() == null
			? createConversation(content, context.userId(), context.tenantId())
			: requireConversation(request.getConversationId(), context.userId(), context.tenantId());
		List<AiMessage> history = messageMapper.selectList(Wrappers.<AiMessage>lambdaQuery()
			.eq(AiMessage::getConversationId, conversation.getId())
			.eq(AiMessage::getTenantId, context.tenantId())
			.eq(AiMessage::getUserId, context.userId())
			.orderByAsc(AiMessage::getId));
		AiDomainHandler domainHandler = domainHandlers.stream()
			.filter(handler -> handler.supports(content, history)).findFirst().orElse(null);
		boolean inScope = domainHandler != null;
		String domain = inScope ? domainHandler.domain() : "property";
		AiMessage userMessage = saveMessage(conversation.getId(), "user", content, domain, inScope, context.userId(), context.tenantId());
		Map<String, Object> meta = new LinkedHashMap<>();
		meta.put("conversationId", conversation.getId());
		meta.put("userMessage", toVO(userMessage));
		eventConsumer.accept(new IAiChatService.AiStreamEvent("meta", meta));
		StringBuilder answer = new StringBuilder();
		if (inScope) {
			domainHandler.streamAnswer(content, recentHistory(history), context, chunk -> {
				answer.append(chunk);
				eventConsumer.accept(new IAiChatService.AiStreamEvent("delta", Map.of("content", chunk)));
			});
		} else {
			String outOfScope = outOfScopeReply();
			answer.append(outOfScope);
			eventConsumer.accept(new IAiChatService.AiStreamEvent("delta", Map.of("content", outOfScope)));
		}
		AiMessage assistantMessage = saveMessage(conversation.getId(), "assistant", answer.toString(), domain, inScope, context.userId(), context.tenantId());
		Date now = new Date();
		conversation.setDomain(domain);
		conversation.setLastMessageTime(now);
		conversation.setUpdateTime(now);
		conversationMapper.updateById(conversation);
		eventConsumer.accept(new IAiChatService.AiStreamEvent("done", Map.of("assistantMessage", toVO(assistantMessage))));
	}

	private AiConversation createConversation(String firstQuestion) {
		return createConversation(firstQuestion, currentUserId(), currentTenantId());
	}

	private AiConversation createConversation(String firstQuestion, Long userId, String tenantId) {
		Date now = new Date();
		AiConversation conversation = new AiConversation();
		conversation.setTenantId(tenantId);
		conversation.setUserId(userId);
		conversation.setDomain("property");
		conversation.setTitle(firstQuestion.length() > 28 ? firstQuestion.substring(0, 28) + "..." : firstQuestion);
		conversation.setLastMessageTime(now);
		conversation.setCreateTime(now);
		conversation.setUpdateTime(now);
		conversationMapper.insert(conversation);
		return conversation;
	}

	private AiConversation requireConversation(Long conversationId) {
		return requireConversation(conversationId, currentUserId(), currentTenantId());
	}

	private AiConversation requireConversation(Long conversationId, Long userId, String tenantId) {
		if (conversationId == null) {
			throw new ServiceException("会话不存在或无权访问");
		}
		AiConversation conversation = conversationMapper.selectOne(Wrappers.<AiConversation>lambdaQuery()
			.eq(AiConversation::getId, conversationId)
			.eq(AiConversation::getTenantId, tenantId)
			.eq(AiConversation::getUserId, userId));
		if (conversation == null) {
			throw new ServiceException("会话不存在或无权访问");
		}
		return conversation;
	}

	private AiMessage saveMessage(Long conversationId, String role, String content, String domain, boolean inScope) {
		return saveMessage(conversationId, role, content, domain, inScope, currentUserId(), currentTenantId());
	}

	private AiMessage saveMessage(Long conversationId, String role, String content, String domain, boolean inScope, Long userId, String tenantId) {
		AiMessage message = new AiMessage();
		message.setConversationId(conversationId);
		message.setTenantId(tenantId);
		message.setUserId(userId);
		message.setRole(role);
		message.setContent(content);
		message.setDomain(domain);
		message.setInScope(inScope);
		message.setCreateTime(new Date());
		messageMapper.insert(message);
		return message;
	}

	private List<AiMessage> recentHistory(List<AiMessage> history) {
		return history.stream().sorted(Comparator.comparing(AiMessage::getId).reversed()).limit(8)
			.sorted(Comparator.comparing(AiMessage::getId)).toList();
	}

	private AiAccessContext normalizeContext(AiAccessContext accessContext) {
		if (accessContext == null || accessContext.userId() == null) {
			throw new ServiceException("未获取到当前登录用户");
		}
		String tenantId = StringUtil.isBlank(accessContext.tenantId()) ? "000000" : accessContext.tenantId();
		return new AiAccessContext(accessContext.userId(), tenantId, accessContext.authorizedParkIds());
	}

	private String outOfScopeReply() {
		return domainHandlers.stream()
			.filter(AiPropertyDomainHandler.class::isInstance)
			.map(handler -> ((AiPropertyDomainHandler) handler).outOfScopeReply())
			.findFirst()
			.orElse("目前仅支持房源相关问答，请询问房源、出租率或空置房间。");
	}

	private AiChatMessageVO toVO(AiMessage message) {
		AiChatMessageVO vo = new AiChatMessageVO();
		vo.setId(message.getId());
		vo.setRole(message.getRole());
		vo.setContent(message.getContent());
		vo.setDomain(message.getDomain());
		vo.setInScope(message.getInScope());
		vo.setCreateTime(message.getCreateTime());
		return vo;
	}

	private String currentTenantId() {
		String tenantId = AuthUtil.getTenantId();
		return StringUtil.isBlank(tenantId) ? "000000" : tenantId;
	}

	private Long currentUserId() {
		Long userId = AuthUtil.getUserId();
		if (userId == null) throw new ServiceException("未获取到当前登录用户");
		return userId;
	}
}
