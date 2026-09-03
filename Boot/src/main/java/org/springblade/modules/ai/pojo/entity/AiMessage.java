package org.springblade.modules.ai.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** AI 对话消息。 */
@Data
@TableName("biz_ai_message")
public class AiMessage {

	@TableId(type = IdType.AUTO)
	private Long id;
	private Long conversationId;
	private String tenantId;
	private Long userId;
	private String role;
	private String content;
	private String domain;
	private Boolean inScope;
	private Long reportId;
	private Date createTime;
}
