package org.springblade.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.modules.ai.config.AiChatProperties;
import org.springblade.modules.ai.pojo.entity.AiMessage;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** DeepSeek OpenAI-compatible chat completion client. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekChatClient {

	public record StreamResult(boolean emitted, boolean completed) {
	}

	private final AiChatProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient = HttpClient.newBuilder().build();

	public Optional<String> complete(String systemPrompt, List<AiMessage> history, String question) {
		if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			return Optional.empty();
		}
		try {
			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(Map.of("role", "system", "content", systemPrompt));
			for (AiMessage message : history) {
				if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
					messages.add(Map.of("role", message.getRole(), "content", message.getContent()));
				}
			}
			messages.add(Map.of("role", "user", "content", question));
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("model", properties.getModel());
			body.put("messages", messages);
			body.put("temperature", 0.2);
			body.put("max_tokens", 700);
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions"))
				.timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
				.header("Authorization", "Bearer " + properties.getApiKey())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				log.warn("DeepSeek request failed with status {}", response.statusCode());
				return Optional.empty();
			}
			JsonNode content = objectMapper.readTree(response.body()).path("choices").path(0).path("message").path("content");
			return content.isTextual() && !content.asText().isBlank() ? Optional.of(content.asText().trim()) : Optional.empty();
		} catch (Exception exception) {
			log.warn("DeepSeek request failed", exception);
			return Optional.empty();
		}
	}

	/**
	 * 使用模型判断问题是否属于指定问答领域。分类响应必须是 JSON，解析失败时交给上层本地规则兜底。
	 */
	public Optional<Boolean> classifyPropertyQuestion(List<AiMessage> history, String question) {
		String prompt = "你是园区运营平台的问答路由器。请判断用户最新问题是否属于房源领域。"
			+ "房源领域包括：房源、房间、房态、楼宇、楼层、面积、租金、出租率、空置、在租、租赁和租控数据。"
			+ "与客户、合同审批、财务、工单、系统操作、天气、闲聊等无关的问题不属于房源领域。"
			+ "结合历史对话判断省略表达，但不要因为历史对话而扩大领域范围。"
			+ "只输出 JSON，不要 Markdown，不要额外解释，格式：{\"inScope\":true,\"reason\":\"简短原因\"}";
		Optional<String> result = complete(prompt, history, question);
		if (result.isEmpty()) {
			return Optional.empty();
		}
		try {
			String content = result.get().trim().replaceAll("^```json\\s*|\\s*```$", "");
			JsonNode node = objectMapper.readTree(content);
			if (node.has("inScope") && node.get("inScope").isBoolean()) {
				return Optional.of(node.get("inScope").asBoolean());
			}
		} catch (Exception exception) {
			log.warn("DeepSeek scope classification response is not valid JSON");
		}
		return Optional.empty();
	}

	/** 读取 DeepSeek SSE 增量，仅转发文本 delta。 */
	public StreamResult stream(String systemPrompt, List<AiMessage> history, String question, Consumer<String> chunkConsumer) {
		if (!properties.isEnabled() || properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			return new StreamResult(false, false);
		}
		boolean emitted = false;
		try {
			List<Map<String, String>> messages = new ArrayList<>();
			messages.add(Map.of("role", "system", "content", systemPrompt));
			for (AiMessage message : history) {
				if ("user".equals(message.getRole()) || "assistant".equals(message.getRole())) {
					messages.add(Map.of("role", message.getRole(), "content", message.getContent()));
				}
			}
			messages.add(Map.of("role", "user", "content", question));
			Map<String, Object> body = new LinkedHashMap<>();
			body.put("model", properties.getModel());
			body.put("messages", messages);
			body.put("temperature", 0.2);
			body.put("max_tokens", 700);
			body.put("stream", true);
			URL url = URI.create(properties.getBaseUrl().replaceAll("/+$", "") + "/chat/completions").toURL();
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(properties.getTimeoutSeconds() * 1000);
			connection.setReadTimeout(properties.getTimeoutSeconds() * 1000);
			connection.setDoOutput(true);
			connection.setRequestProperty("Authorization", "Bearer " + properties.getApiKey());
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Accept", "text/event-stream");
			try (var output = connection.getOutputStream()) {
				output.write(objectMapper.writeValueAsBytes(body));
			}
			int statusCode = connection.getResponseCode();
			if (statusCode < 200 || statusCode >= 300) {
				log.warn("DeepSeek stream request failed with status {}", statusCode);
				connection.disconnect();
				return new StreamResult(false, false);
			}
			boolean completed = false;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (!line.startsWith("data:")) continue;
					String payload = line.substring(5).trim();
					if ("[DONE]".equals(payload)) {
						completed = true;
						break;
					}
					JsonNode content = objectMapper.readTree(payload).path("choices").path(0).path("delta").path("content");
					if (content.isTextual() && !content.asText().isEmpty()) {
						chunkConsumer.accept(content.asText());
						emitted = true;
					}
				}
			}
			connection.disconnect();
			return new StreamResult(emitted, completed);
		} catch (Exception exception) {
			log.warn("DeepSeek stream request failed", exception);
			return new StreamResult(emitted, false);
		}
	}
}
