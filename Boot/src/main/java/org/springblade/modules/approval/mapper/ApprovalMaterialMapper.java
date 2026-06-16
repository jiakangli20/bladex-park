/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.approval.pojo.entity.ApprovalMaterial;

import java.util.List;

/**
 * 审批资料 Mapper.
 *
 * @author BladeX
 */
public interface ApprovalMaterialMapper extends BaseMapper<ApprovalMaterial> {

	List<ApprovalMaterial> selectApprovalMaterialList(@Param("material") ApprovalMaterial material);

	int insertApprovalMaterial(ApprovalMaterial material);

	int deleteApprovalMaterialByIds(@Param("ids") List<Long> ids, @Param("parkId") Long parkId);

}
