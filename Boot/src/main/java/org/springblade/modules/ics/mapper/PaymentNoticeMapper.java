/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.ics.pojo.entity.PaymentNotice;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeSummaryVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;

import java.util.List;

/**
 * 收款通知 Mapper.
 *
 * @author BladeX
 */
public interface PaymentNoticeMapper extends BaseMapper<PaymentNotice> {

	/**
	 * 收款通知分页.
	 *
	 * @param page  分页
	 * @param query 查询条件
	 * @return 收款通知列表
	 */
	List<PaymentNoticeVO> selectNoticePage(IPage<PaymentNoticeVO> page, @Param("query") PaymentNoticeVO query);

	/**
	 * 收款通知汇总.
	 *
	 * @param query 查询条件
	 * @return 汇总
	 */
	PaymentNoticeSummaryVO selectNoticeSummary(@Param("query") PaymentNoticeVO query);

	/**
	 * 按账单查询通知记录.
	 *
	 * @param paymentId 账单ID
	 * @return 通知记录
	 */
	PaymentNotice selectByPaymentId(@Param("paymentId") Long paymentId);

	/**
	 * 查询楼宇选项.
	 *
	 * @param query 查询条件
	 * @return 楼宇名称
	 */
	List<String> selectBuildingOptions(@Param("query") PaymentNoticeVO query);

	/**
	 * 查询通知详情.
	 *
	 * @param paymentId 账单ID
	 * @return 通知详情
	 */
	PaymentNoticeVO selectNoticeByPaymentId(@Param("paymentId") Long paymentId);

}
