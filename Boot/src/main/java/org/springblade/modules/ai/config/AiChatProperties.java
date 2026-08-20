package org.springblade.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 模型配置。密钥建议通过 DEEPSEEK_API_KEY 环境变量注入，不写入仓库。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.deepseek")
public class AiChatProperties {

	private boolean enabled = true;
	private String apiKey = "";
	private String baseUrl = "https://api.deepseek.com";
	private String model = "deepseek-chat";
	private int timeoutSeconds = 30;
}
