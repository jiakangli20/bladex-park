package org.springblade.plugin.workflow.process.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.process.entity.WfNotice;

import java.util.List;

/**
 * 流程消息数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class WfNoticeDTO extends WfBaseDTO {

    private WfUser fromUser;

    private List<WfUser> toUser;

    private WfUser startUser;

    private WfNotice.Type type;

}
