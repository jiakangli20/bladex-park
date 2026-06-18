package org.springblade.plugin.workflow.demo.wrapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springblade.common.cache.SysCache;
import org.springblade.core.mp.support.BaseEntityWrapper;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.modules.system.pojo.entity.Dept;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserCache;
import org.springblade.plugin.workflow.demo.entity.WfProcessLeave;
import org.springblade.plugin.workflow.demo.vo.WfProcessLeaveVO;

import java.util.List;
import java.util.Objects;

/**
 * 请假流程业务示例包装类,返回视图层所需的字段
 *
 * @author ssc
 */
public class WfProcessLeaveWrapper extends BaseEntityWrapper<WfProcessLeave, WfProcessLeaveVO> {

	public static WfProcessLeaveWrapper build() {
		return new WfProcessLeaveWrapper();
	}

	@Override
	public WfProcessLeaveVO entityVO(WfProcessLeave wfProcessLeave) {
		WfProcessLeaveVO wfProcessLeaveVO = Objects.requireNonNull(BeanUtil.copyProperties(wfProcessLeave, WfProcessLeaveVO.class));

		//User createUser = UserCache.getUser(wfProcessLeave.getCreateUser());
		//User updateUser = UserCache.getUser(wfProcessLeave.getUpdateUser());
		//wfProcessLeaveVO.setCreateUserName(createUser.getName());
		//wfProcessLeaveVO.setUpdateUserName(updateUser.getName());

		return wfProcessLeaveVO;
	}

	@Override
	public IPage<WfProcessLeaveVO> pageVO(IPage<WfProcessLeave> pages) {
		List<WfProcessLeaveVO> records = this.listVO(pages.getRecords());
		records.forEach(r -> {
			WfUser user = WfUserCache.getUser(r.getCreateUser());
			if (user != null) {
				r.setCreator(user.getName());
			}
			Dept dept = SysCache.getDept(r.getCreateDept());
			if (dept != null) {
				r.setCreatorDept(dept.getDeptName());
			}
		});
		IPage<WfProcessLeaveVO> pageVo = new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
		pageVo.setRecords(records);
		return pageVo;
	}
}
