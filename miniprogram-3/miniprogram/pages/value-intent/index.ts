import { valueServices } from '../../utils/mock'

Page({
  data: {
    service: valueServices[0],
    form: {
      contact: '',
      phone: '',
      companyName: '上海科技有限公司',
      budget: '',
      needTime: '',
      content: '',
    },
  },

  onLoad(options: Record<string, string | undefined>) {
    const service = valueServices.find((item) => item.id === options.id)
    if (service) {
      this.setData({ service })
    }
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
    wx.showModal({
      title: '提交成功',
      content: '增值服务意向已提交，管理员受理后会更新服务进度。',
      confirmText: '查看工单',
      cancelText: '留在本页',
      success(result) {
        if (result.confirm) {
          wx.navigateTo({ url: '/pages/work-orders/index?tab=value' })
        }
      },
    })
  },
})
