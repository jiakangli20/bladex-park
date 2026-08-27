package org.springblade.modules.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.service.DeepSeekChatClient;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;
import org.springblade.modules.park.service.IRentControlService;
import org.springblade.modules.park.service.IRoomService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiPropertyDomainHandlerTest {

	@Test
	void propertySnapshotUsesCapturedParkScope() {
		IRentControlService rentControlService = mock(IRentControlService.class);
		IRoomService roomService = mock(IRoomService.class);
		DeepSeekChatClient deepSeekChatClient = mock(DeepSeekChatClient.class);
		List<Long> authorizedParkIds = List.of(2001L);
		RoomVO rentedRoom = new RoomVO();
		rentedRoom.setStatus("7");
		rentedRoom.setParkName("产业园");
		rentedRoom.setBuildingName("A座");
		rentedRoom.setName("1901室");
		rentedRoom.setArea(new BigDecimal("65.37"));

		when(rentControlService.getBoard(null, null, null, null, "room", null, null, false, authorizedParkIds))
			.thenReturn(Map.of("overview", Map.of("rentedRoomCount", 1)));
		when(roomService.selectRoomList(any(Room.class), anyList())).thenReturn(List.of(rentedRoom));
		when(deepSeekChatClient.complete(anyString(), anyList(), anyString())).thenReturn(Optional.empty());
		AiPropertyDomainHandler handler = new AiPropertyDomainHandler(
			rentControlService, roomService, deepSeekChatClient, new ObjectMapper());

		String answer = handler.answer("我现在租了哪些房源？", List.of(),
			new AiAccessContext(100L, "000000", authorizedParkIds));

		assertTrue(answer.contains("当前在租房源共 1 间"));
		assertTrue(answer.contains("产业园 / A座 / 1901室"));
		verify(rentControlService).getBoard(null, null, null, null, "room", null, null, false, authorizedParkIds);
		verify(roomService).selectRoomList(any(Room.class), org.mockito.ArgumentMatchers.eq(authorizedParkIds));
	}
}
