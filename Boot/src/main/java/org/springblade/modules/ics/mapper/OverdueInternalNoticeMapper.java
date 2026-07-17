/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.ics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.ics.pojo.entity.OverdueInternalNotice;
import org.springblade.modules.ics.pojo.vo.OverdueInternalNoticeVO;

import java.util.List;

/**
 * 逾期内部通知数据访问层.
 *
 * @author Chill
 */
public interface OverdueInternalNoticeMapper extends BaseMapper<OverdueInternalNotice> {

	List<OverdueInternalNotice> selectByPaymentId(@Param("paymentId") Long paymentId);

	int insertIgnore(OverdueInternalNotice notice);

	Long countUnread(@Param("userId") Long userId);

	List<OverdueInternalNoticeVO> selectNoticePage(IPage<OverdueInternalNoticeVO> page,
													 @Param("userId") Long userId,
													 @Param("customerName") String customerName,
													 @Param("readStatus") String readStatus);

	int markRead(@Param("paymentId") Long paymentId, @Param("userId") Long userId);

}
