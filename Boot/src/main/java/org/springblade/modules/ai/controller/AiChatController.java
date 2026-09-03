package org.springblade.modules.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiChatRequest;
import org.springblade.modules.ai.pojo.entity.AiConversation;
import org.springblade.modules.ai.pojo.vo.AiChatMessageVO;
import org.springblade.modules.ai.pojo.vo.AiChatResponseVO;
import org.springblade.modules.ai.service.IAiChatService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletResponse;

import java.util.concurrent.CompletableFuture;

import java.util.List;
import java.util.Map;

@NonDS
@RestController
@RequiredArgsConstructor
@PreAuth("hasMenu('rent_control') || hasMenu('settlement_customer')")
@RequestMapping("/blade-ai/chat")
@Tag(name = "园区 AI 助手", description = "房源问答与企业助手接口")
public class AiChatController extends BladeController {

	private final IAiChatService chatService;
	private final IParkPermissionService parkPermissionService;

	@GetMapping("/conversations")
	@PreAuth("(#domain == null || #domain == '' || #domain == 'property') && hasMenu('rent_control') || #domain == 'enterprise' && hasMenu('settlement_customer')")
	@Operation(summary = "当前用户会话列表")
	public R<List<AiConversation>> conversations(@RequestParam(defaultValue = "property") String domain) {
		return R.data(chatService.conversations(domain));
	}

	@GetMapping("/messages")
	@Operation(summary = "当前用户会话消息")
	public R<List<AiChatMessageVO>> messages(@RequestParam Long conversationId) {
		return R.data(chatService.messages(conversationId));
	}

	@PostMapping("/conversations/remove")
	@Operation(summary = "删除当前用户会话")
	public R<Boolean> removeConversation(@RequestParam Long conversationId) {
		chatService.removeConversation(conversationId);
		return R.data(true);
	}

	@PostMapping("/send")
	@PreAuth("#request.domain == 'enterprise' && hasMenu('settlement_customer') || (#request.domain == null || #request.domain == '' || #request.domain == 'property') && hasMenu('rent_control')")
	@Operation(summary = "发送 AI 助手消息")
	public R<AiChatResponseVO> send(@Valid @RequestBody AiChatRequest request) {
		return R.data(chatService.send(request, captureAccessContext()));
	}

	@PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@PreAuth("#request.domain == 'enterprise' && hasMenu('settlement_customer') || (#request.domain == null || #request.domain == '' || #request.domain == 'property') && hasMenu('rent_control')")
	@Operation(summary = "流式发送 AI 助手消息")
	public SseEmitter sendStream(@Valid @RequestBody AiChatRequest request, HttpServletResponse response) {
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("X-Accel-Buffering", "no");
		AiAccessContext accessContext = captureAccessContext();
		SseEmitter emitter = new SseEmitter(120000L);
		CompletableFuture.runAsync(() -> {
			try {
				chatService.sendStream(request, accessContext, event -> {
					try {
						emitter.send(SseEmitter.event().name(event.type()).data(event.data()));
					} catch (Exception exception) {
						throw new IllegalStateException("流式消息发送失败", exception);
					}
				});
				emitter.complete();
			} catch (Exception exception) {
				try {
					emitter.send(SseEmitter.event().name("error").data(Map.of("message", exception.getMessage() == null ? "AI问答失败" : exception.getMessage())));
				} catch (Exception ignored) {
					// 客户端已断开时无需再次发送错误事件
				}
				emitter.completeWithError(exception);
			}
		});
		return emitter;
	}

	AiAccessContext captureAccessContext() {
		return new AiAccessContext(AuthUtil.getUserId(), AuthUtil.getTenantId(), parkPermissionService.authorizedParkIds());
	}
}
