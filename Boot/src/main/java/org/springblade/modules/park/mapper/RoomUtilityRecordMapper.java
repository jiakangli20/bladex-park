package org.springblade.modules.park.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.park.pojo.entity.RoomUtilityRecord;

import java.util.List;

public interface RoomUtilityRecordMapper extends BaseMapper<RoomUtilityRecord> {

	List<RoomUtilityRecord> selectRecordPage(IPage<RoomUtilityRecord> page, @Param("roomId") Long roomId);

	int deleteRecord(@Param("recordId") Long recordId, @Param("updateBy") String updateBy);
}
