package org.springblade.plugin.workflow.demo.service;

import org.springblade.plugin.workflow.demo.entity.WfProcessLeave;
import org.springblade.core.mp.base.BaseService;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;
import org.springblade.plugin.workflow.process.entity.WfNotice;

/**
 * 请假流程业务示例 服务类
 *
 * @author ssc
 */
public interface IWfProcessLeaveService extends BaseService<WfProcessLeave> {

    void businessWithNotice(WfNoticeDTO notice);
}
