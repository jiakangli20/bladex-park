import { customerApi, publicApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

Page({
  data: {
    mode: 'appointment',
    title: '预约看房',
    submitText: '提交预约',
    house: {} as Record<string, any>,
    form: {
      contact: '',
      phone: '',
      companyName: '',
      creditCode: '',
      visitTime: '',
      intentArea: '',
      industry: '',
      scale: '',
      settleTime: '',
      remark: '',
    },
  },

  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/house-intent/index?mode=${options.mode || 'appointment'}&id=${options.id || ''}`)) return
    const isSettlement = options.mode === 'settlement'
    const house = options.id ? await publicApi.house(options.id) : {}
    this.setData({
      mode: isSettlement ? 'settlement' : 'appointment',
      title: isSettlement ? '入驻意向' : '预约看房',
      submitText: isSettlement ? '提交意向' : '提交预约',
      house,
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

  async submitForm() {
    const { contact, phone, companyName } = this.data.form
    if (!contact || !phone || !companyName) {
      wx.showToast({
        title: '请补充联系人、手机号和企业名称',
        icon: 'none',
      })
      return
    }
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的11位手机号', icon: 'none' })
      return
    }
    if (this.data.mode === 'settlement') {
      if (!this.data.form.creditCode) {
        wx.showToast({ title: '请填写统一社会信用代码', icon: 'none' })
        return
      }
      await customerApi.createSettlement({
        roomId: this.data.house.id || null,
        enterpriseName: companyName,
        creditCode: this.data.form.creditCode,
        industryType: this.data.form.industry,
        enterpriseScale: this.data.form.scale,
        intentArea: Number(this.data.form.intentArea.replace(/[^\d.]/g, '')) || null,
        expectedEntryDate: this.data.form.settleTime || null,
        contactName: contact,
        contactPhone: phone,
        demandDesc: this.data.form.remark,
      })
    } else {
      await customerApi.createAppointment({
        roomId: this.data.house.id,
        enterpriseName: companyName,
        contactName: contact,
        contactPhone: phone,
        preferredTime: this.data.form.visitTime || null,
        demandDesc: this.data.form.remark,
      })
    }
    wx.showToast({ title: this.data.mode === 'settlement' ? '入驻意向已提交' : '看房预约已提交', icon: 'success' })
  },
})
