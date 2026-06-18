package org.springblade.plugin.workflow.process.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.plugin.workflow.process.entity.WfDraft;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 流程草稿箱 Mapper 接口
 *
 * @author ssc
 */
public interface WfDraftMapper extends BaseMapper<WfDraft> {

	void deleteByProcessDefId(@Param("processDefId") String processDefId, @Param("userId") Long userId, @Param("platform") Object platform);

	@Delete("DELETE FROM blade_wf_draft WHERE form_key = #{formKey}")
	void deleteByFormKey(@Param("formKey") String formKey);
}
