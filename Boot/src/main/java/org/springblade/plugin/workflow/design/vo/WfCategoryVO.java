
package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfCategory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.util.List;

/**
 * 流程分类视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfCategoryVO对象", description = "流程分类")
public class WfCategoryVO extends WfCategory {
	@Serial
	private static final long serialVersionUID = 1L;

	List<WfCategoryVO> children;

}
