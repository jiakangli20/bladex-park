package org.springblade.plugin.workflow.demo.service.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.plugin.workflow.demo.entity.WfProcessLeave;
import org.springblade.plugin.workflow.demo.mapper.WfProcessLeaveMapper;
import org.springblade.plugin.workflow.demo.service.IWfProcessLeaveService;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springblade.plugin.workflow.process.entity.WfNotice.Type.*;

/**
 * 请假流程业务示例 服务实现类
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfProcessLeaveServiceImpl extends BaseServiceImpl<WfProcessLeaveMapper, WfProcessLeave> implements IWfProcessLeaveService {

    private final RuntimeService runtimeService;

    @Override
    public void businessWithNotice(WfNoticeDTO notice) {
        WfNotice.Type type = notice.getType();
        ProcessInstance processInstance = notice.getProcessInstance();
        Task task = notice.getTask();
        Map<String, Object> variables = notice.getVariables();

        String processInsId = processInstance.getProcessInstanceId();

        // 发起，插入数据
        if (START == type) {
            WfProcessLeave leave = JSON.parseObject(JSON.toJSONString(variables), WfProcessLeave.class);
            leave.setProcessInsId(processInsId);
            leave.setProcessDefId(processInstance.getProcessDefinitionId());
            leave.setCreateUser(AuthUtil.getUserId());
            leave.setCreateDept(Func.firstLong(AuthUtil.getDeptId()));
            leave.setUpdateUser(AuthUtil.getUserId());
            leave.setStatus(1);

            save(leave);
            // 保存的ID存入流程
            runtimeService.updateBusinessKey(processInsId, leave.getId() + "");
        }
        // 任务结束
        else if (TASK_COMPLETE == type) {
            String id = processInstance.getBusinessKey();
            if (StringUtils.isNotBlank(id)) {
                assert task != null;
                WfProcessLeave leave = JSON.parseObject(JSON.toJSONString(variables), WfProcessLeave.class);
                leave.setCurrentNode(task.getName());
                leave.setId(Long.valueOf(id));
                leave.setStatus(1);
                updateById(leave);
            }
        }
        // 流程删除
        else if (DELETE_PROCESS == type) {
            String id = processInstance.getBusinessKey();
            if (StringUtils.isNotBlank(id)) {
                deleteLogic(List.of(NumberUtils.toLong(id)));
            }
        }
        else {
            String id = processInstance.getBusinessKey();
            if (StringUtils.isNotBlank(id)) {
                WfProcessLeave leave = new WfProcessLeave();
                leave.setId(Long.valueOf(id));
                leave.setCurrentNode(task == null ? "结束": task.getName());
                leave.setStatus(type.getCode());
                updateById(leave);
            }
        }
    }
}
