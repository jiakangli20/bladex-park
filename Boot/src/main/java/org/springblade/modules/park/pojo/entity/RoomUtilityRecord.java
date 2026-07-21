package org.springblade.modules.park.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("biz_room_utility_record")
@Schema(description = "房源水电抄表记录")
public class RoomUtilityRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "record_id", type = IdType.AUTO)
	private Long recordId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long roomId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	private String recordType;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date readingTime;

	private BigDecimal previousReading;

	private BigDecimal currentReading;

	private BigDecimal usageAmount;

	private BigDecimal amount;

	private String operatorName;

	private String remark;

	private String delFlag;

	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;

	private String updateBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date updateTime;

	@TableField(exist = false)
	private String roomName;

	@TableField(exist = false)
	private String deviceName;

	@TableField(exist = false)
	private String deviceCode;
}
