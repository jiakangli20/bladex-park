package org.springblade.modules.ai.pojo.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AiChatMessageVO {
	private Long id;
	private String role;
	private String content;
	private String domain;
	private Boolean inScope;
	private Date createTime;
}
