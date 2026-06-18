package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 任务/执行监听器 实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_listener")
@Schema(name = "WfListener对象", description = "任务/执行监听器")
@EqualsAndHashCode(callSuper = true)
public class WfListenerEntity extends TenantEntity {

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
    /**
     * 分类
     */
    @Schema(description = "分类")
    private String category;
    /**
     * 事件
     */
    @Schema(description = "事件")
    private String event;
    /**
     * 监听类型
     */
    @Schema(description = "监听类型")
    private String type;
    /**
     * 值
     */
    @Schema(description = "值")
    private String val;

}
