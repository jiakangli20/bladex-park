package org.springblade.modules.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.entity.AiMessage;
import org.springblade.modules.ai.service.AiDomainHandler;
import org.springblade.modules.ai.service.DeepSeekChatClient;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;
import org.springblade.modules.park.service.IRentControlService;
import org.springblade.modules.park.service.IRoomService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** 房源问答领域：先以规则守住边界，再将实时快照交给模型表达。 */
@Component
@RequiredArgsConstructor
public class AiPropertyDomainHandler implements AiDomainHandler {

	private static final String OUT_OF_SCOPE_REPLY = "目前我只支持房源相关问答，例如“我现在租了哪些房源？”、“出租率是多少？”或“空置房间有多少？”。";
	private static final List<String> PROPERTY_KEYWORDS = List.of("房源", "房间", "房屋", "出租", "租房", "租赁", "空置", "在租", "租金", "租控", "楼宇", "楼栋", "楼层", "房态", "面积", "户型");
	private static final List<String> FOLLOW_UP_KEYWORDS = List.of("哪些", "明细", "详情", "分别", "还有", "占比", "为什么", "按楼", "按层", "那", "它们");

	private final IRentControlService rentControlService;
	private final IRoomService roomService;
	private final DeepSeekChatClient deepSeekChatClient;
	private final ObjectMapper objectMapper;

	@Override
	public String domain() {
		return "property";
	}

	@Override
	public boolean supports(String question, List<AiMessage> recentMessages) {
		Optional<Boolean> aiDecision = deepSeekChatClient.classifyPropertyQuestion(recentMessages, question);
		if (aiDecision.isPresent()) {
			return aiDecision.get();
		}
		return supportsByLocalRule(question, recentMessages);
	}

	private boolean supportsByLocalRule(String question, List<AiMessage> recentMessages) {
		String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
		if (PROPERTY_KEYWORDS.stream().anyMatch(normalized::contains)) {
			return true;
		}
		boolean hasPropertyContext = recentMessages.stream().anyMatch(message -> domain().equals(message.getDomain()) && Boolean.TRUE.equals(message.getInScope()));
		return hasPropertyContext && FOLLOW_UP_KEYWORDS.stream().anyMatch(normalized::contains);
	}

	@Override
	public DomainAnswer answer(String question, List<AiMessage> recentMessages, AiAccessContext accessContext) {
		Map<String, Object> snapshot = buildSnapshot(accessContext.authorizedParkIds());
		String prompt = buildAnswerPrompt(snapshot);
		return DomainAnswer.text(deepSeekChatClient.complete(prompt, recentMessages, question).orElseGet(() -> fallbackAnswer(question, snapshot)));
	}

	@Override
	public DomainAnswer streamAnswer(String question, List<AiMessage> recentMessages, AiAccessContext accessContext, Consumer<String> chunkConsumer) {
		Map<String, Object> snapshot = buildSnapshot(accessContext.authorizedParkIds());
		DeepSeekChatClient.StreamResult result = deepSeekChatClient.stream(
			buildAnswerPrompt(snapshot), recentMessages, question, chunkConsumer
		);
		if (!result.emitted()) {
			chunkConsumer.accept(fallbackAnswer(question, snapshot));
		} else if (!result.completed()) {
			throw new ServiceException("AI流式回答中断，请重试");
		}
		return new DomainAnswer(null, null);
	}

	@Override
	public String outOfScopeReply() {
		return OUT_OF_SCOPE_REPLY;
	}

	private Map<String, Object> buildSnapshot(List<Long> authorizedParkIds) {
		Map<String, Object> board = rentControlService.getBoard(null, null, null, null, "room", null, null, false, authorizedParkIds);
		Object overviewObject = board.get("overview");
		Map<String, Object> overview = overviewObject instanceof Map ? (Map<String, Object>) overviewObject : Map.of();
		List<RoomVO> rooms = roomService.selectRoomList(new Room(), authorizedParkIds);
		List<Map<String, Object>> rentedRooms = new ArrayList<>();
		for (RoomVO room : rooms) {
			if (!"7".equals(room.getStatus())) {
				continue;
			}
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("园区", room.getParkName());
			item.put("楼宇", room.getBuildingName());
			item.put("房源", room.getName());
			item.put("楼层", room.getFloor());
			item.put("面积㎡", room.getArea());
			item.put("月租金元", room.getRentPrice());
			rentedRooms.add(item);
		}
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("总房源数", overview.getOrDefault("totalRoomCount", rooms.size()));
		summary.put("在租房间数", overview.getOrDefault("rentedRoomCount", rentedRooms.size()));
		summary.put("空置房间数", overview.getOrDefault("vacantRoomCount", 0));
		summary.put("出租率%", overview.getOrDefault("rentRate", 0));
		summary.put("在租面积㎡", overview.getOrDefault("rentedArea", BigDecimal.ZERO));
		summary.put("空置面积㎡", overview.getOrDefault("vacantArea", BigDecimal.ZERO));
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("统计口径", "出租率=在租面积/管理面积；在租房源以当前有效合同房态为准");
		snapshot.put("汇总", summary);
		snapshot.put("当前在租房源", rentedRooms);
		return snapshot;
	}

	private String fallbackAnswer(String question, Map<String, Object> snapshot) {
		Map<String, Object> summary = (Map<String, Object>) snapshot.get("汇总");
		String normalized = question.toLowerCase(Locale.ROOT);
		if (normalized.contains("哪些") || normalized.contains("房源") || normalized.contains("在租")) {
			List<Map<String, Object>> rooms = (List<Map<String, Object>>) snapshot.get("当前在租房源");
			if (rooms.isEmpty()) return "当前没有处于在租状态的房源。";
			StringBuilder answer = new StringBuilder("当前在租房源共 ").append(rooms.size()).append(" 间：");
			for (Map<String, Object> room : rooms) {
				answer.append("\n- ").append(room.get("园区")).append(" / ").append(room.get("楼宇")).append(" / ").append(room.get("房源"));
			}
			return answer.toString();
		}
		if (normalized.contains("空置")) return "当前空置房间为 " + summary.get("空置房间数") + " 间，空置面积 " + summary.get("空置面积㎡") + " ㎡。";
		return "当前出租率为 " + summary.get("出租率%") + "%；在租 " + summary.get("在租房间数") + " 间，空置 " + summary.get("空置房间数") + " 间。";
	}

	private String toJson(Map<String, Object> value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			return "{}";
		}
	}

	private String buildAnswerPrompt(Map<String, Object> snapshot) {
		return "你是园区运营平台的房源助手，只能回答房源、房态、出租率、空置、在租房源、面积和租金相关问题。"
			+ "必须只依据下方实时数据回答，不得编造数据或租客信息；回答使用简洁中文，需要时列点。"
			+ "用户问题超出上述范围时，明确引导其询问房源问题。实时房源数据：" + toJson(snapshot);
	}
}
