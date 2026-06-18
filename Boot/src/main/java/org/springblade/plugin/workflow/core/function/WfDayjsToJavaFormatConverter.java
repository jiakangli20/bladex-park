package org.springblade.plugin.workflow.core.function;

import java.util.LinkedHashMap;
import java.util.Map;

public class WfDayjsToJavaFormatConverter {
	private static final Map<String, String> FORMAT_MAPPING = new LinkedHashMap<>();

	static {
		// 必须从长匹配到短，否则会误替换
		FORMAT_MAPPING.put("YYYY", "yyyy");
		FORMAT_MAPPING.put("YY", "yy");
		FORMAT_MAPPING.put("DD", "dd");
		FORMAT_MAPPING.put("D", "d");
		FORMAT_MAPPING.put("MM", "MM");
		FORMAT_MAPPING.put("M", "M");
		FORMAT_MAPPING.put("HH", "HH");
		FORMAT_MAPPING.put("H", "H");
		FORMAT_MAPPING.put("hh", "hh");
		FORMAT_MAPPING.put("h", "h");
		FORMAT_MAPPING.put("mm", "mm");
		FORMAT_MAPPING.put("m", "m");
		FORMAT_MAPPING.put("ss", "ss");
		FORMAT_MAPPING.put("s", "s");
		FORMAT_MAPPING.put("A", "a");
		FORMAT_MAPPING.put("a", "a");
	}

	public static String convert(String dayjsFormat) {
		String javaFormat = dayjsFormat;
		for (Map.Entry<String, String> entry : FORMAT_MAPPING.entrySet()) {
			javaFormat = javaFormat.replace(entry.getKey(), entry.getValue());
		}
		return javaFormat;
	}
}
