import { customerApi, publicApi } from '../../services/miniapp'
import { getSession, hasCapability, requireLogin } from '../../utils/session'

const industryOptions = ['软件与信息技术', '人工智能', '金融服务', '文化创意', '智能制造', '商务服务', '批发零售', '其他']
const scaleOptions = ['1-20人', '21-50人', '51-100人', '101-300人', '301-500人', '500人以上']
const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

Page({
  data: {
    mode: 'appointment',
    publicMode: false,
    title: '预约看房',
    submitText: '提交预约',
    today: formatDate(new Date()),
    latestDate: formatDate(new Date(new Date().getFullYear() + 5, 11, 31)),
    industryOptions,
    scaleOptions,
    industryIndex: 0,
    scaleIndex: 0,
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
    const publicMode = options.public === '1'
    if (!publicMode && !requireLogin(`/pages/house-intent/index?mode=${options.mode || 'appointment'}&id=${options.id || ''}`)) return
    const isSettlement = options.mode === 'settlement'
    const house = options.id ? await publicApi.house(options.id) : {}
    this.setData({
      mode: isSettlement ? 'settlement' : 'appointment',
      publicMode,
      title: isSettlement ? '入驻意向' : '预约看房',
      submitText: isSettlement ? '提交意向' : '提交预约',
      house,
    })
    if (isSettlement && getSession()?.accessToken && hasCapability('customer.profile.view')) {
      try {
        const company = await customerApi.company()
        this.setData({
          'form.companyName': company.companyName || '',
          'form.creditCode': company.creditCode || '',
          'form.industry': company.industry || '',
          'form.scale': company.scale || '',
          'form.contact': company.contact || '',
          'form.phone': company.phone || '',
          industryIndex: Math.max(0, industryOptions.indexOf(company.industry)),
          scaleIndex: Math.max(0, scaleOptions.indexOf(company.scale)),
        })
      } catch (_) {
        // 未完善企业资料时仍允许用户手动填写申请。
      }
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

  handleCreditCodeInput(event: WechatMiniprogram.Input) {
    const value = String(event.detail.value || '').toUpperCase().replace(/[^0-9A-Z]/g, '').slice(0, 18)
    this.setData({ 'form.creditCode': value })
  },

  handleAreaInput(event: WechatMiniprogram.Input) {
    let value = String(event.detail.value || '').replace(/[^\d.]/g, '')
    const parts = value.split('.')
    value = `${parts[0].slice(0, 5)}${parts.length > 1 ? `.${parts.slice(1).join('').slice(0, 2)}` : ''}`
    this.setData({ 'form.intentArea': value })
  },

  changeIndustry(event: WechatMiniprogram.PickerChange) {
    const index = Number(event.detail.value)
    this.setData({ industryIndex: index, 'form.industry': industryOptions[index] })
  },

  changeScale(event: WechatMiniprogram.PickerChange) {
    const index = Number(event.detail.value)
    this.setData({ scaleIndex: index, 'form.scale': scaleOptions[index] })
  },

  changeSettleDate(event: WechatMiniprogram.PickerChange) {
    this.setData({ 'form.settleTime': String(event.detail.value) })
  },

  async submitForm() {
    if (this.data.publicMode && !requireLogin(`/pages/house-intent/index?mode=${this.data.mode}`)) return
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
      const { creditCode, intentArea, industry, scale, settleTime } = this.data.form
      if (!/^[0-9A-Z]{18}$/.test(creditCode)) {
        wx.showToast({ title: '请输入18位统一社会信用代码', icon: 'none' })
        return
      }
      const area = Number(intentArea)
      if (!Number.isFinite(area) || area < 1 || area > 99999) {
        wx.showToast({ title: '意向面积应为1-99999平方米', icon: 'none' })
        return
      }
      if (!industry || !scale) {
        wx.showToast({ title: '请选择行业类型和企业规模', icon: 'none' })
        return
      }
      if (!settleTime || settleTime < this.data.today) {
        wx.showToast({ title: '请选择有效的预计入驻日期', icon: 'none' })
        return
      }
      await customerApi.createSettlement({
        roomId: this.data.house.id || null,
        enterpriseName: companyName,
        creditCode: this.data.form.creditCode,
        industryType: this.data.form.industry,
        enterpriseScale: this.data.form.scale,
        intentArea: area,
        expectedEntryDate: settleTime,
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
