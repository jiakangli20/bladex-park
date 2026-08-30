/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;

/**
 * 小程序成员 Mapper。
 *
 * @author Chill
 */
public interface MiniMemberMapper extends BaseMapper<MiniMember> {

	/**
	 * 清理同一微信绑定键上历史逻辑删除记录，避免再次逻辑删除时撞唯一索引。
	 */
	@Delete("DELETE FROM biz_mini_member WHERE app_id = #{appId} AND open_id = #{openId} AND is_deleted = 1")
	int purgeDeletedByAppOpen(@Param("appId") String appId, @Param("openId") String openId);
}
