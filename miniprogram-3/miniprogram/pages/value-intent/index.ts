import { customerApi, publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { getSession, hasCapability, requireLogin } from '../../utils/session'

const formatDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

Page({
  data: {
    service: {} as Record<string, any>,
    today: formatDate(new Date()),
    latestDate: formatDate(new Date(new Date().getFullYear() + 5, 11, 31)),
    form: { contact: '', phone: '', companyName: '', budget: '', needDate: '', needClock: '', content: '' },
  },

  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/value-intent/index?id=${options.id || ''}`) || !options.id) return
    if (!hasCapability('customer.profile.view')) {
      wx.showToast({ title: '当前账号暂无增值服务权限', icon: 'none' })
      navigateBackOr('/pages/index/index')
      return
    }
    const service = await publicApi.valueService(options.id)
    this.setData({
      service,
      'form.companyName': getSession()?.profile?.enterpriseName || '',
      'form.phone': getSession()?.profile?.mobile || '',
    })
  },

  goBack() { navigateBackOr('/pages/services/index?tab=value') },
  handleInput(event: WechatMiniprogram.Input) {
    const field = event.currentTarget.dataset.field as string | undefined
    if (field) this.setData({ [`form.${field}`]: event.detail.value })
  },
  changeNeedDate(event: WechatMiniprogram.PickerChange) {
    this.setData({ 'form.needDate': String(event.detail.value) })
  },
  changeNeedClock(event: WechatMiniprogram.PickerChange) {
    this.setData({ 'form.needClock': String(event.detail.value) })
  },

  async submitForm() {
    const { contact, phone, companyName, budget, needDate, needClock, content } = this.data.form
    if (!contact || !/^1\d{10}$/.test(phone) || !companyName || !content) {
      wx.showToast({ title: '请填写联系人、正确手机号、企业和需求', icon: 'none' })
      return
    }
    if ((needDate && !needClock) || (!needDate && needClock)) {
      wx.showToast({ title: '请完整选择期望日期和时间', icon: 'none' })
      return
    }
    const needTime = needDate && needClock ? `${needDate} ${needClock}` : ''
    await customerApi.createValueOrder({
      merchantId: this.data.service.id,
      serviceType: this.data.service.category,
      contactName: contact,
      contactPhone: phone,
      serviceScope: this.data.service.desc,
      demandDesc: `${content}${budget ? `；预算：${budget}` : ''}${needTime ? `；期望时间：${needTime}` : ''}`,
    })
    wx.showModal({
      title: '提交成功', content: '增值服务意向已提交，管理员受理后会更新服务进度。',
      confirmText: '查看工单', cancelText: '留在本页',
      success: result => result.confirm && wx.navigateTo({ url: '/pages/work-orders/index?tab=value' }),
    })
  },
})
