package org.springblade.modules.ai.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** AI 问答会话，按租户与用户双重隔离。 */
@Data
@TableName("biz_ai_conversation")
public class AiConversation {

	@TableId(type = IdType.AUTO)
	private Long id;
	private String tenantId;
	private Long userId;
	private String domain;
	private String title;
	private Date lastMessageTime;
	private Date createTime;
	private Date updateTime;
}
