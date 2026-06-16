/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.approval.pojo.entity.ApprovalLog;

import java.util.List;

/**
 * 审批日志 Mapper.
 *
 * @author BladeX
 */
public interface ApprovalLogMapper extends BaseMapper<ApprovalLog> {

	List<ApprovalLog> selectApprovalLogList(@Param("log") ApprovalLog log);

	int insertApprovalLog(ApprovalLog log);

}
