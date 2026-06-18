package org.springblade.plugin.workflow.process.service;

import org.springblade.plugin.workflow.process.entity.WfNotice;

public interface IWfNoticeService {

    /**
     * 统一消息/业务入口
     */
    void sendNotice(WfNotice notice);

}
