package org.springblade.plugin.workflow.core.utils;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.util.io.StringStreamSource;
import org.flowable.engine.form.FormProperty;
import org.flowable.engine.impl.form.FormPropertyHandler;
import org.flowable.engine.impl.form.FormPropertyImpl;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.constant.WfExtendConstant;

import java.util.*;

/**
 * 模型工具类
 *
 * @author ssc
 */
public class WfModelUtil {

	/**
	 * xml转bpmnModel对象
	 *
	 * @param xml xml
	 * @return bpmnModel对象
	 */
	public static BpmnModel getBpmnModel(String xml) {
		BpmnXMLConverter converter = new BpmnXMLConverter();
		return converter.convertToBpmnModel(new StringStreamSource(xml), false, false);
	}

	/**
	 * 获取开始节点
	 *
	 * @param model bpmnModel对象
	 * @return 开始节点
	 */
	public static StartEvent getStartEvent(BpmnModel model) {
		Process process = model.getMainProcess();
		StartEvent event = (StartEvent) process.getFlowElement("startEvent_1");
		if (event == null) {
			Collection<FlowElement> elements = process.getFlowElements();
			for (FlowElement element : elements) {
				if (element instanceof StartEvent) {
					return (StartEvent) element;
				}
			}
		}
		return event;
	}

	/**
	 * 获取开始节点
	 *
	 * @param xml xml
	 * @return 开始节点
	 */
	public static StartEvent getStartEvent(String xml) {
		BpmnModel model = getBpmnModel(xml);
		Process process = model.getMainProcess();
		StartEvent event = (StartEvent) process.getFlowElement("startEvent_1");
		if (event == null) {
			Collection<FlowElement> elements = process.getFlowElements();
			for (FlowElement element : elements) {
				if (element instanceof StartEvent) {
					return (StartEvent) element;
				}
			}
		}
		return event;
	}

	/**
	 * 获取开始节点扩展元素
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static List<ExtensionElement> getStartEventExtensionElements(BpmnModel model, String key) {
		StartEvent event = getStartEvent(model);
		if (event != null) {
			return event.getExtensionElements().get(key);
		}
		return null;
	}

	/**
	 * 获取开始节点扩展元素属性
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static Map<String, List<ExtensionAttribute>> getStartEventExtensionElementAttributes(BpmnModel model, String key) {
		List<ExtensionElement> extensionElements = getStartEventExtensionElements(model, key);
		if (extensionElements != null && extensionElements.size() > 0) {
			return extensionElements.get(0).getAttributes();
		}
		return null;
	}

	/**
	 * 获取第一个用户节点
	 *
	 * @param model bpmnModel对象
	 * @return 第一个用户节点
	 */
	public static UserTask getUserTask(BpmnModel model) {
		Process process = model.getMainProcess();
		Collection<FlowElement> elements = process.getFlowElements();
		for (FlowElement element : elements) {
			if (element instanceof UserTask) {
				return (UserTask) element;
			}
		}
		return null;
	}

	/**
	 * 获取第一个用户节点
	 *
	 * @param xml xml
	 * @return 第一个用户节点
	 */
	public static UserTask getUserTask(String xml) {
		BpmnModel model = getBpmnModel(xml);
		Process process = model.getMainProcess();
		Collection<FlowElement> elements = process.getFlowElements();
		for (FlowElement element : elements) {
			if (element instanceof UserTask) {
				return (UserTask) element;
			}
		}
		return null;
	}

	/**
	 * 获取第一个用户节点扩展元素
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static List<ExtensionElement> getUserTaskExtensionElements(BpmnModel model, String key) {
		UserTask userTask = getUserTask(model);
		if (userTask != null) {
			return userTask.getExtensionElements().get(key);
		}
		return null;
	}

	/**
	 * 获取第一个用户节点扩展属性
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static List<ExtensionAttribute> getUserTaskExtensionAttributes(BpmnModel model, String key) {
		UserTask userTask = getUserTask(model);
		if (userTask != null) {
			return userTask.getAttributes().get(key);
		}
		return null;
	}

	/**
	 * 获取第一个用户节点扩展属性
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static String getUserTaskExtensionAttribute(BpmnModel model, String key) {
		List<ExtensionAttribute> attributes = getUserTaskExtensionAttributes(model, key);
		if (attributes != null && attributes.size() > 0) {
			return attributes.get(0).getValue();
		}
		return null;
	}

	/**
	 * 获取用户节点
	 *
	 * @param taskKey 任务key
	 * @param model   bpmnModel对象
	 * @return 用户节点
	 */
	public static UserTask getUserTaskByKey(String taskKey, BpmnModel model) {
		if (StringUtil.isBlank(taskKey)) {
			return getUserTask(model);
		}
		Process process = model.getMainProcess();
		FlowElement flowElement = process.getFlowElement(taskKey, true);
		if (flowElement instanceof UserTask) {
			return (UserTask) flowElement;
		} else { // 子流程中查找
			Collection<FlowElement> flowElements = process.getFlowElements();
			for (FlowElement element : flowElements) {
				if (element instanceof SubProcess) {
					SubProcess subProcess = (SubProcess) element;
					flowElement = subProcess.getFlowElement(taskKey);
					if (flowElement instanceof UserTask) {
						return (UserTask) flowElement;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 获取用户节点扩展元素
	 *
	 * @param taskKey 任务key
	 * @param model   bpmnModel对象
	 * @param key     扩展元素key
	 */
	public static List<ExtensionElement> getUserTaskExtensionElements(String taskKey, BpmnModel model, String key) {
		UserTask userTask = getUserTaskByKey(taskKey, model);
		if (userTask != null) {
			return userTask.getExtensionElements().get(key);
		}
		return null;
	}

	/**
	 * 获取用户节点扩展属性
	 *
	 * @param taskKey 任务key
	 * @param model   bpmnModel对象
	 * @param key     扩展元素key
	 */
	public static List<ExtensionAttribute> getUserTaskExtensionAttributes(String taskKey, BpmnModel model, String key) {
		UserTask userTask = getUserTaskByKey(taskKey, model);
		if (userTask != null) {
			return userTask.getAttributes().get(key);
		}
		return null;
	}

	/**
	 * 获取用户节点扩展属性
	 *
	 * @param taskKey 任务key
	 * @param model   bpmnModel对象
	 * @param key     扩展元素key
	 */
	public static String getUserTaskExtensionAttribute(String taskKey, BpmnModel model, String key) {
		List<ExtensionAttribute> attributes = getUserTaskExtensionAttributes(taskKey, model, key);
		if (attributes != null && attributes.size() > 0) {
			return attributes.get(0).getValue();
		}
		return null;
	}

	/**
	 * 获取根节点扩展元素列表
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static List<ExtensionElement> getProcessExtensionElements(BpmnModel model, String key) {
		Process process = model.getMainProcess();
		return process.getExtensionElements().get(key);
	}

	/**
	 * 获取根节点扩展元素属性
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static Map<String, List<ExtensionAttribute>> getProcessExtensionElementAttributes(BpmnModel model, String key) {
		List<ExtensionElement> elements = getProcessExtensionElements(model, key);
		if (elements != null && elements.size() > 0) {
			return elements.get(0).getAttributes();
		}
		return null;
	}

	/**
	 * 获取根节点扩展属性列表
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static List<ExtensionAttribute> getProcessExtensionAttributes(BpmnModel model, String key) {
		return model.getMainProcess().getAttributes().get(key);
	}

	/**
	 * 获取根节点扩展属性
	 *
	 * @param model bpmnModel对象
	 * @param key   扩展元素key
	 */
	public static String getProcessExtensionAttribute(BpmnModel model, String key) {
		List<ExtensionAttribute> attributes = getProcessExtensionAttributes(model, key);
		if (attributes != null && attributes.size() > 0) {
			return attributes.get(0).getValue();
		}
		return null;
	}

	/**
	 * 获取子流程节点
	 *
	 * @param taskKey key
	 * @param model   bpmnModel
	 */
	public static SubProcess getSubProcessByKey(String taskKey, BpmnModel model) {
		Process process = model.getMainProcess();
		FlowElement flowElement = process.getFlowElement(taskKey, true);
		if (flowElement instanceof SubProcess) {
			return (SubProcess) flowElement;
		} else { // 子流程中查找
			Collection<FlowElement> flowElements = process.getFlowElements();
			for (FlowElement element : flowElements) {
				if (element instanceof SubProcess) {
					SubProcess subProcess = (SubProcess) element;
					flowElement = subProcess.getFlowElement(taskKey);
					if (flowElement instanceof SubProcess) {
						return (SubProcess) flowElement;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 获取子流程节点扩展元素
	 *
	 * @param subKey 子流程key
	 * @param model  bpmnModel对象
	 * @param key    扩展元素key
	 */
	public static List<ExtensionElement> getSubProcessExtensionElements(String subKey, BpmnModel model, String key) {
		SubProcess subProcess = getSubProcessByKey(subKey, model);
		if (subProcess != null) {
			return subProcess.getExtensionElements().get(key);
		}
		return null;
	}

	/**
	 * 获取子流程节点扩展属性
	 *
	 * @param subKey 子流程key
	 * @param model  bpmnModel对象
	 * @param key    扩展元素key
	 */
	public static List<ExtensionAttribute> getSubProcessExtensionAttributes(String subKey, BpmnModel model, String key) {
		SubProcess subProcess = getSubProcessByKey(subKey, model);
		if (subProcess != null) {
			return subProcess.getAttributes().get(key);
		}
		return null;
	}

	/**
	 * 获取子流程节点扩展属性
	 *
	 * @param subKey 子流程key
	 * @param model  bpmnModel对象
	 * @param key    扩展元素key
	 */
	public static String getSubProcessExtensionAttribute(String subKey, BpmnModel model, String key) {
		List<ExtensionAttribute> attributes = getSubProcessExtensionAttributes(subKey, model, key);
		if (attributes != null && attributes.size() > 0) {
			return attributes.get(0).getValue();
		}
		return null;
	}

	/**
	 * 获取结束节点
	 *
	 * @param model bpmnModel对象
	 */
	public static EndEvent getEndEvent(BpmnModel model) {
		Collection<FlowElement> elements = model.getMainProcess().getFlowElements();
		for (FlowElement element : elements) {
			if (element instanceof EndEvent) {
				return (EndEvent) element;
			}
		}
		return null;
	}

	/**
	 * 获取调用活动节点
	 *
	 * @param taskKey key
	 * @param model   bpmnModel
	 */
	public static CallActivity getCallActivityByKey(String taskKey, BpmnModel model) {
		Process process = model.getMainProcess();
		FlowElement flowElement = process.getFlowElement(taskKey, true);
		if (flowElement instanceof CallActivity) {
			return (CallActivity) flowElement;
		} else { // 子流程中查找
			Collection<FlowElement> flowElements = process.getFlowElements();
			for (FlowElement element : flowElements) {
				if (element instanceof SubProcess) {
					SubProcess subProcess = (SubProcess) element;
					flowElement = subProcess.getFlowElement(taskKey);
					if (flowElement instanceof CallActivity) {
						return (CallActivity) flowElement;
					}
				}
			}
		}
		return null;
	}

	/**
	 * 获取调用活动节点扩展元素
	 *
	 * @param subKey 调用活动key
	 * @param model  bpmnModel对象
	 * @param key    扩展元素key
	 */
	public static List<ExtensionElement> getCallActivityExtensionElements(String subKey, BpmnModel model, String key) {
		CallActivity callActivity = getCallActivityByKey(subKey, model);
		if (callActivity != null) {
			return callActivity.getExtensionElements().get(key);
		}
		return null;
	}

	public static List<FormProperty> getStartFormProp(BpmnModel bpmnModel) {
		List<ExtensionElement> formExtension = WfModelUtil.getStartEventExtensionElements(bpmnModel, WfExtendConstant.FORM_PROP);
		return convertFormProperty(formExtension);
	}

	public static List<FormProperty> getUserTaskFormProp(String taskKey, BpmnModel bpmnModel) {
		List<ExtensionElement> formExtension = WfModelUtil.getUserTaskExtensionElements(taskKey, bpmnModel, WfExtendConstant.FORM_PROP);
		return convertFormProperty(formExtension);
	}

	private static  List<FormProperty> convertFormProperty(List<ExtensionElement> formExtension) {
		if (formExtension != null && !formExtension.isEmpty()) {
			List<FormProperty> formProperties = new ArrayList<>();
			formExtension.forEach(ele -> {
				Map<String, List<ExtensionAttribute>> attributes = ele.getAttributes();
				FormPropertyHandler handler = new FormPropertyHandler();
				for (List<ExtensionAttribute> value : attributes.values()) {
					ExtensionAttribute extensionAttribute = value.get(0);
					switch (extensionAttribute.getName()) {
						case "id":
							handler.setId(extensionAttribute.getValue());
							break;
						case "name":
							handler.setName(extensionAttribute.getValue());
							break;
						case "readable":
							handler.setReadable(Boolean.parseBoolean(extensionAttribute.getValue()));
							break;
						case "writable":
							handler.setWritable(Boolean.parseBoolean(extensionAttribute.getValue()));
							break;
						case "required":
							handler.setRequired(Boolean.parseBoolean(extensionAttribute.getValue()));
							break;
					}
				}
				FormPropertyImpl formProperty = new FormPropertyImpl(handler);
				formProperties.add(formProperty);
			});
			return formProperties;
		}
		return null;
	}

	/**
	 * 是否在并行/相容网关中
	 */
	public static boolean isInParallelGateway(FlowNode node, BpmnModel model) {
		return hasUpstreamForkGateway(node, model) &&
			hasDownstreamJoinGateway(node, model);
	}

	static boolean hasUpstreamForkGateway(FlowNode node, BpmnModel model) {
		Queue<FlowNode> queue = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		queue.add(node);

		while (!queue.isEmpty()) {
			FlowNode current = queue.poll();
			if (!visited.add(current.getId())) continue;

			for (SequenceFlow flow : current.getIncomingFlows()) {
				FlowElement source = model.getMainProcess().getFlowElement(flow.getSourceRef());

				if ((source instanceof ParallelGateway || source instanceof InclusiveGateway) &&
					((Gateway) source).getOutgoingFlows().size() > 1) {
					return true;
				}

				if (source instanceof FlowNode) {
					queue.add((FlowNode) source);
				}
			}
		}
		return false;
	}

	static boolean hasDownstreamJoinGateway(FlowNode node, BpmnModel model) {
		Queue<FlowNode> queue = new LinkedList<>();
		Set<String> visited = new HashSet<>();
		queue.add(node);

		while (!queue.isEmpty()) {
			FlowNode current = queue.poll();
			if (!visited.add(current.getId())) continue;

			for (SequenceFlow flow : current.getOutgoingFlows()) {
				FlowElement target = model.getMainProcess().getFlowElement(flow.getTargetRef());

				if ((target instanceof ParallelGateway || target instanceof InclusiveGateway) &&
					((Gateway) target).getIncomingFlows().size() > 1) {
					return true;
				}

				if (target instanceof FlowNode) {
					queue.add((FlowNode) target);
				}
			}
		}
		return false;
	}

}
