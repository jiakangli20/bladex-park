package org.springblade.modules.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.park.pojo.entity.RoomVehicle;

import java.util.List;

public interface RoomVehicleMapper extends BaseMapper<RoomVehicle> {

	List<RoomVehicle> selectVehiclePage(IPage<RoomVehicle> page, @Param("roomId") Long roomId);

	int countActivePlate(@Param("roomId") Long roomId, @Param("plateNo") String plateNo);

	int deleteVehicle(@Param("vehicleId") Long vehicleId, @Param("updateBy") String updateBy);
}
