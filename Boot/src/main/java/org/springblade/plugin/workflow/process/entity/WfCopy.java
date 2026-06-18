
package org.springblade.plugin.workflow.process.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.springblade.core.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程抄送实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_copy")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfCopy对象", description = "流程抄送")
public class WfCopy extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 用户id
	 */
	@Schema(description = "用户id")
	private Long userId;
	/**
	 * 标题
	 */
	@Schema(description = "标题")
	private String title;
	/**
	 * 发起者
	 */
	@Schema(description = "发起者")
	private String initiator;
	/**
	 * 任务id
	 */
	@Schema(description = "任务id")
	private String taskId;
	/**
	 * 流程实例id
	 */
	@Schema(description = "流程实例id")
	private String processId;

	@Schema(description = "外置表单key")
	private String formKey;

	@Schema(description = "外置表单url")
	private String formUrl;

	@Schema(description = "流程部署id")
	private String deploymentId;

}
