package org.springblade.plugin.workflow.process.service;

import org.springblade.plugin.workflow.design.entity.WfDeploymentForm;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;

import java.util.List;

/**
 * 工作流结合低代码 服务类
 *
 * @author ssc
 */
public interface IWfLowcodeService {

    /**
     * 建表
     */
    void createTable(List<WfDeploymentForm> list);

    /**
     * 插入/更新数据表
     */
    void updateWithNotice(WfNoticeDTO notice);

    void dropTable(List<WfDeploymentForm> list);

}
