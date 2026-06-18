package org.springblade.plugin.workflow.design.dto;

import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 表单 - 主题 数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormThemeDTO extends WfFormTheme {
	@Serial
	private static final long serialVersionUID = 1L;

}
