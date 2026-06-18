package org.springblade.plugin.workflow.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.springblade.core.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 请假流程业务示例实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_process_leave")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfProcessLeave对象", description = "请假流程业务示例")
public class WfProcessLeave extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 流程实例id
	 */
	@Schema(description = "流程实例id")
	private String processInsId;
	/**
	 * 流程定义id
	 */
	@Schema(description = "流程定义id")
	private String processDefId;

	@Schema(description = "外置表单key")
	private String formKey;

	@Schema(description = "外置表单url")
	private String formUrl;

	@Schema(description = "当前节点")
	private String currentNode;
	/**
	 * 时间范围
	 */
	@Schema(description = "时间范围")
	private String datetime;
	/**
	 * 请假天数
	 */
	@Schema(description = "请假天数")
	private Integer days;
	/**
	 * 请假理由
	 */
	@Schema(description = "请假理由")
	private String reason;
	/**
	 * 附件
	 */
	@Schema(description = "附件")
	private String attachment;


}
