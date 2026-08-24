import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { uploadFile } from '../../utils/request'
import { hasCapability, requireLogin } from '../../utils/session'

const formatDate = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

Page({
  data: {
    id: '', title: '新建活动', today: formatDate(new Date()), uploading: false, saving: false,
    form: { title: '', coverUrl: '', summary: '', startDate: '', startClock: '', endDate: '', endClock: '', address: '', priceText: '免费', contactName: '', contactPhone: '' },
  },
  async onLoad(options: Record<string, string | undefined>) {
    const id = options.id || ''
    if (!requireLogin(`/pages/customer-activity-form/index${id ? `?id=${id}` : ''}`)) return
    if (!hasCapability('customer.activity.submit')) { wx.showToast({ title: '当前账号没有活动申请权限', icon: 'none' }); navigateBackOr('/pages/customer-activities/index'); return }
    this.setData({ id, title: id ? '修改活动' : '新建活动' })
    if (id) { await this.loadActivity(); return }
    try {
      const company = await customerApi.company()
      this.setData({ 'form.contactName': company.contact || '', 'form.contactPhone': company.phone || '' })
    } catch (_) { /* 允许手动填写 */ }
  },
  async loadActivity() {
    const activity = await customerApi.activity(this.data.id)
    if (!['DRAFT', 'REJECTED'].includes(String(activity.auditStatus))) { wx.showToast({ title: '当前活动不可修改', icon: 'none' }); navigateBackOr(`/pages/customer-activity-detail/index?id=${this.data.id}`); return }
    this.setData({ form: { title: activity.title || '', coverUrl: activity.image || '', summary: activity.summary || '', startDate: String(activity.startTime || '').slice(0, 10), startClock: String(activity.startTime || '').slice(11, 16), endDate: String(activity.endTime || '').slice(0, 10), endClock: String(activity.endTime || '').slice(11, 16), address: activity.address || '', priceText: activity.price || '免费', contactName: activity.contactName || '', contactPhone: activity.contactPhone || '' } })
  },
  handleInput(event: WechatMiniprogram.Input) { const field = String(event.currentTarget.dataset.field || ''); if (field) this.setData({ [`form.${field}`]: event.detail.value }) },
  changePicker(event: WechatMiniprogram.PickerChange) { const field = String(event.currentTarget.dataset.field || ''); if (field) this.setData({ [`form.${field}`]: String(event.detail.value) }) },
  async chooseCover() {
    if (this.data.uploading) return
    try {
      const media = await wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'] })
      const file = media.tempFiles[0]
      if (!file) return
      this.setData({ uploading: true })
      const uploaded = await uploadFile(file.tempFilePath)
      this.setData({ 'form.coverUrl': uploaded.url })
    } finally { this.setData({ uploading: false }) }
  },
  async submitForm() {
    if (this.data.saving) return
    const form = this.data.form
    if (!form.title.trim() || !form.coverUrl || !form.summary.trim()) { wx.showToast({ title: '请填写标题、简介并上传封面', icon: 'none' }); return }
    if (!form.startDate || !form.startClock || !form.endDate || !form.endClock) { wx.showToast({ title: '请选择完整活动时间', icon: 'none' }); return }
    const startTime = `${form.startDate} ${form.startClock}:00`; const endTime = `${form.endDate} ${form.endClock}:00`
    if (endTime <= startTime) { wx.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' }); return }
    if (!form.address.trim() || !form.contactName.trim() || !/^1\d{10}$/.test(form.contactPhone)) { wx.showToast({ title: '请填写地点、联系人和正确手机号', icon: 'none' }); return }
    this.setData({ saving: true })
    try {
      const payload = { title: form.title.trim(), coverUrl: form.coverUrl, summary: form.summary.trim(), startTime, endTime, address: form.address.trim(), priceText: form.priceText.trim() || '免费', contactName: form.contactName.trim(), contactPhone: form.contactPhone }
      const result = this.data.id ? await customerApi.updateActivity(this.data.id, payload) : await customerApi.createActivity(payload)
      wx.showToast({ title: '草稿已保存', icon: 'success' })
      setTimeout(() => wx.redirectTo({ url: `/pages/customer-activity-detail/index?id=${this.data.id || result.id}` }), 500)
    } finally { this.setData({ saving: false }) }
  },
  goBack() { navigateBackOr(this.data.id ? `/pages/customer-activity-detail/index?id=${this.data.id}` : '/pages/customer-activities/index') },
})
