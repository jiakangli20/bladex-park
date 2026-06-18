package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 表单 - 主题 视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormThemeVO extends WfFormTheme {
	@Serial
	private static final long serialVersionUID = 1L;

}
