package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 表单 - 主题 实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_form_theme")
@Schema(name = "WfFormTheme对象", description = "表单 - 主题")
@EqualsAndHashCode(callSuper = true)
public class WfFormTheme extends TenantEntity {

	/**
	 * 名称
	 */
	@Schema(description = "名称")
	private String name;
	/**
	 * 主题key
	 */
	@Schema(description = "主题key")
	private String themeKey;
	/**
	 * 边框宽度
	 */
	@Schema(description = "边框宽度")
	private String borderWidth;
	/**
	 * 边框颜色
	 */
	@Schema(description = "边框颜色")
	private String borderColor;
	/**
	 * label字体颜色
	 */
	@Schema(description = "label字体颜色")
	private String labelColor;
	/**
	 * label背景颜色
	 */
	@Schema(description = "label背景颜色")
	private String labelBg;
	/**
	 * label宽度
	 */
	@Schema(description = "label宽度")
	private String labelWidth;
	/**
	 * label字体大小
	 */
	@Schema(description = "label字体大小")
	private String labelFontSize;
	/**
	 * value字体颜色
	 */
	@Schema(description = "value字体颜色")
	private String valueColor;
	/**
	 * value背景颜色
	 */
	@Schema(description = "value背景颜色")
	private String valueBg;
	/**
	 * value字体大小
	 */
	@Schema(description = "value字体大小")
	private String valueFontSize;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field0;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field1;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field2;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field3;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field4;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field5;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field6;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field7;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field8;
	/**
	 * 预留字段
	 */
	@Schema(description = "预留字段")
	private String field9;

}
