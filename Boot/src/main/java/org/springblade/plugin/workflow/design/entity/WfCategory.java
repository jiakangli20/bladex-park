package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 流程分类实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_category")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfCategory对象", description = "流程分类")
public class WfCategory extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 分类名称
	 */
	@Schema(description = "分类名称")
	private String name;
	/**
	 * 上级id
	 */
	@Schema(description = "上级id")
	private Long pid;
	/**
	 * 排序
	 */
	@Schema(description = "排序")
	private Integer sort;


}
