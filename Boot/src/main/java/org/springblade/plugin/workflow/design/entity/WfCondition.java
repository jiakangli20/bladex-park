
package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.springblade.core.mp.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程表达式实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_condition")
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfCondition对象", description = "流程表达式")
public class WfCondition extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
    /**
     * 表达式
     */
    @Schema(description = "表达式")
    private String expression;
    /**
     * 类型
     */
    @Schema(description = "类型")
    private String type;


}
