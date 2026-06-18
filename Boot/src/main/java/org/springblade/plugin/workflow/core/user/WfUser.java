package org.springblade.plugin.workflow.core.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.modules.system.pojo.entity.User;

/**
 * 系统用户
 * 统一用户入口，防止更改包名的情况再次出现
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfUser extends User {

}
