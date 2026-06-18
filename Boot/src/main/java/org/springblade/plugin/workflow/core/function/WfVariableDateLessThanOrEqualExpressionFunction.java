package org.springblade.plugin.workflow.core.function;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.el.function.AbstractFlowableVariableExpressionFunction;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * 日期变量比较 - 小于等于
 *
 * @author ssc
 */
public class WfVariableDateLessThanOrEqualExpressionFunction extends AbstractFlowableVariableExpressionFunction {

	public WfVariableDateLessThanOrEqualExpressionFunction() {
		super(Arrays.asList("dateLessThanOrEqual", "dateLowerThanOrEqual", "dlte"), "dateLessThanOrEqual");
	}

	public static boolean dateLessThanOrEqual(VariableContainer variableContainer, String variableName, Object... values) {
		if (values.length != 2) {
			return false;
		}
		Object variableValue;
		// ${var:dlte(date, '2008-08-08', 'YYYY-MM-DD')} -> variableName = "date"
		// ${var:dlte(table.date, '2008-08-08', 'YYYY-MM-DD')} -> variableName = "2008-08-08"
		if (variableContainer.hasVariable(variableName)) {
			variableValue = getVariableValue(variableContainer, variableName);
		} else {
			variableValue = variableName;
		}
		if (variableValue != null) {
			String date = values[0].toString();
			String pattern = WfDayjsToJavaFormatConverter.convert(values[1].toString());
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			try {
				Date parse = dateFormat.parse(date);
				if (variableValue instanceof Date) {
					return ((Date) variableValue).before(parse) || variableValue.equals(parse);
				} else if (variableValue instanceof String) {
					Date parseVar = dateFormat.parse(variableValue.toString());
					return parseVar.before(parse) || parseVar.equals(parse);
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}

		return false;
	}
}
