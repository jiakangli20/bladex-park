const serviceMap: Record<string, { title: string; desc: string; submitText: string }> = {
  repair: {
    title: '维修申请',
    desc: '提交维修位置、问题说明和联系方式，物业工程组会尽快受理。',
    submitText: '提交维修工单',
  },
  parking: {
    title: '车位申请',
    desc: '用于新增长期车位、临停车位或企业车辆备案。',
    submitText: '提交车位申请',
  },
  utility: {
    title: '水电缴纳',
    desc: '查看本期水电金额并登记缴费信息，后续接入线上支付。',
    submitText: '提交缴费记录',
  },
  complaint: {
    title: '投诉建议',
    desc: '提交园区服务、环境、安全等问题反馈，运营人员会跟进处理。',
    submitText: '提交反馈',
  },
  meeting: {
    title: '会议室预订',
    desc: '填写会议时间、人数和使用需求，物业会确认可用会议室。',
    submitText: '提交预订',
  },
  'parking-pay': {
    title: '停车缴费',
    desc: '登记车牌和缴费周期，后续可接入停车系统和微信支付。',
    submitText: '提交缴费',
  },
}

Page({
  data: {
    serviceKey: 'repair',
    service: serviceMap.repair,
    form: {
      contact: '',
      phone: '',
      room: 'A座12层1201-1205',
      carNo: '',
      useTime: '',
      amount: '',
      content: '',
    },
  },

  onLoad(options: Record<string, string | undefined>) {
    const serviceKey = options.type && serviceMap[options.type] ? options.type : 'repair'
    this.setData({
      serviceKey,
      service: serviceMap[serviceKey],
    })
  },

  goBack() {
    wx.navigateBack()
  },

  handleInput(event: WechatMiniprogram.Input) {
    const field = event.currentTarget.dataset.field as string | undefined
    if (!field) {
      return
    }
    this.setData({
      [`form.${field}`]: event.detail.value,
    })
  },

  submitForm() {
    const { contact, phone, content } = this.data.form
    if (!contact || !phone || !content) {
      wx.showToast({
        title: '请补充联系人、手机号和需求说明',
        icon: 'none',
      })
      return
    }
    wx.showModal({
      title: '提交成功',
      content: '已生成物业服务申请，可在我的工单查看处理进度。',
      confirmText: '查看工单',
      cancelText: '留在本页',
      success(result) {
        if (result.confirm) {
          wx.navigateTo({ url: '/pages/work-orders/index?tab=property' })
        }
      },
    })
  },
})
