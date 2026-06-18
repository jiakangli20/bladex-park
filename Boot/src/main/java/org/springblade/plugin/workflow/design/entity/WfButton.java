package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 流程按钮实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_button")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfButton对象", description = "流程按钮")
public class WfButton extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "key")
	private String buttonKey;

	@Schema(description = "名称")
	private String name;

	@Schema(description = "默认是否显示")
	private Boolean display;

	@Schema(description = "排序")
	private Integer sort;


}
