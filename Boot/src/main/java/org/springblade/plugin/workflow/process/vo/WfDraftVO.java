package org.springblade.plugin.workflow.process.vo;

import org.springblade.plugin.workflow.process.entity.WfDraft;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程草稿箱视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfDraftVO对象", description = "流程草稿箱")
public class WfDraftVO extends WfDraft {
	@Serial
	private static final long serialVersionUID = 1L;

}
