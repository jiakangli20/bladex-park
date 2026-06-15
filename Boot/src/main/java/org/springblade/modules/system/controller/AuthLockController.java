package org.springblade.modules.system.controller;

import java.util.Date;

import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.IsAdmin;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.pojo.entity.AuthLock;
import org.springblade.modules.system.pojo.vo.AuthLockVO;
import org.springblade.modules.system.service.IAuthLockService;
import org.springblade.modules.system.wrapper.AuthLockWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;

/**
 * 认证锁定记录 控制器
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/auth-lock")
@Tag(name = "认证锁定记录", description = "认证锁定记录")
public class AuthLockController {

	private final IAuthLockService authLockService;

	/**
	 * 分页查询锁定记录
	 */
	@IsAdmin
	@GetMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页查询", description = "传入查询条件")
	public R<IPage<AuthLockVO>> page(@Parameter(hidden = true) AuthLock authLock, Query query) {
		IPage<AuthLock> pages = authLockService.page(Condition.getPage(query), Condition.getQueryWrapper(authLock).orderByDesc("lock_begin_time"));
		return R.data(AuthLockWrapper.build().pageVO(pages));
	}

	/**
	 * 查看锁定记录详情
	 */
	@IsAdmin
	@GetMapping("/detail")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "查看详情", description = "传入id")
	public R<AuthLockVO> detail(AuthLock authLock) {
		AuthLock detail = authLockService.getOne(Condition.getQueryWrapper(authLock));
		return R.data(AuthLockWrapper.build().entityVO(detail));
	}

	/**
	 * 手动解锁
	 */
	@IsAdmin
	@PostMapping("/unlock")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "手动解锁", description = "传入锁定记录ID和解锁原因")
	public R unlock(@Parameter(description = "锁定记录ID", required = true) @RequestParam Long id,
					@Parameter(description = "解锁原因") @RequestParam(required = false) String unlockReason) {
		return R.status(authLockService.manualUnlock(id, unlockReason));
	}

	/**
	 * 手动锁定用户
	 */
	@IsAdmin
	@PostMapping("/lock")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "手动锁定用户", description = "传入用户ID、锁定原因、锁定时间")
	public R lock(@Parameter(description = "用户ID", required = true) @RequestParam Long userId,
				  @Parameter(description = "锁定原因", required = true) @RequestParam String lockReason,
				  @Parameter(description = "锁定开始时间") @RequestParam(required = false) Date lockBeginTime,
				  @Parameter(description = "锁定结束时间") @RequestParam(required = false) Date lockEndTime) {
		return R.status(authLockService.manualLock(userId, lockReason, lockBeginTime, lockEndTime));
	}

}
