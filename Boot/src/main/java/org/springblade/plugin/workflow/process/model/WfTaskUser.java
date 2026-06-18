package org.springblade.plugin.workflow.process.model;

import lombok.Data;
import org.springblade.plugin.workflow.core.user.WfUser;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Data
public class WfTaskUser implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	public WfTaskUser() {
		this.userList = new ArrayList<>();
		this.assignee = null;
		this.candidateUserIds = new LinkedHashSet<>();
		this.candidateGroupIds = new LinkedHashSet<>();
	}

	/**
	 * 所有用户列表
	 */
	private List<WfUser> userList;

	/**
	 * 唯一审核人
	 */
	private String assignee;

	/**
	 * 候选人集合
	 */
	private LinkedHashSet<String> candidateUserIds;

	/**
	 * 候选组集合
	 */
	private LinkedHashSet<String> candidateGroupIds;
}
