package org.springblade.modules.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.miniapp.pojo.entity.MiniCustomerMember;

/**
 * 小程序企业成员关系 Mapper。
 */
public interface MiniCustomerMemberMapper extends BaseMapper<MiniCustomerMember> {

	@Delete("DELETE FROM biz_mini_customer_member WHERE member_id = #{memberId} AND is_deleted = 1")
	int purgeDeletedByMemberId(@Param("memberId") Long memberId);
}
