import { houses } from '../../utils/mock'

Page({
  data: {
    mode: 'appointment',
    title: '预约看房',
    submitText: '提交预约',
    house: houses[0],
    form: {
      contact: '',
      phone: '',
      companyName: '',
      visitTime: '',
      intentArea: '',
      industry: '',
      scale: '',
      settleTime: '',
      remark: '',
    },
  },

  onLoad(options: Record<string, string | undefined>) {
    const isSettlement = options.mode === 'settlement'
    const house = houses.find((item) => item.id === options.id)
    this.setData({
      mode: isSettlement ? 'settlement' : 'appointment',
      title: isSettlement ? '入驻意向' : '预约看房',
      submitText: isSettlement ? '提交意向' : '提交预约',
      house: house || houses[0],
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
    const { contact, phone, companyName } = this.data.form
    if (!contact || !phone || !companyName) {
      wx.showToast({
        title: '请补充联系人、手机号和企业名称',
        icon: 'none',
      })
      return
    }
    wx.showToast({
      title: this.data.mode === 'settlement' ? '入驻意向已提交' : '看房预约已提交',
      icon: 'success',
    })
  },
})
