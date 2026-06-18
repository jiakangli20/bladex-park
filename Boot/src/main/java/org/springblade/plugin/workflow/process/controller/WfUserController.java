package org.springblade.plugin.workflow.process.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserCache;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户 控制器
 *
 * @author ssc
 */
@RestController
@RequestMapping("/blade-workflow/process/user")
@RequiredArgsConstructor
@PreAuth(menu = "wf_process")
public class WfUserController {

	@GetMapping("detail")
	@Operation(summary = "详情")
	@Parameters({
		@Parameter(name = "id", description = "用户id"),
	})
	public R<WfUser> detail(Long id) {
		return R.data(WfUserCache.getUser(ObjectUtil.isEmpty(id) ? AuthUtil.getUserId() : id));
	}
}
