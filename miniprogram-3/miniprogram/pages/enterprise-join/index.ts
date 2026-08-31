import { authApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const statusText: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
const formatTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '-'

Page({
  data: {
    form: { inviteCode: '', name: '', mobile: '', email: '', idType: '', idNo: '', birthDate: '', gender: '' },
    inviteInfo: null as Record<string, any> | null,
    resolving: false,
    records: [] as Record<string, any>[],
    submitting: false,
  },
  onLoad() {
    if (!requireLogin('/pages/enterprise-join/index')) return
    this.load()
  },
  async load() {
    const records = await authApi.joins().catch(() => [])
    this.setData({
      records: (records || []).map(item => ({
        ...item,
        statusText: statusText[item.status] || item.status,
        statusClass: item.status === 'APPROVED' ? 'status-green' : item.status === 'REJECTED' ? 'status-red' : 'status-blue',
        createTimeText: formatTime(item.createTime),
      })),
    })
  },
  goBack() { wx.navigateBack() },
  input(e: WechatMiniprogram.Input) {
    const key = e.currentTarget.dataset.key
    const updates: Record<string, any> = { [`form.${key}`]: e.detail.value }
    if (key === 'inviteCode') updates.inviteInfo = null
    this.setData(updates)
  },
  async resolveInvite() {
    const code = this.data.form.inviteCode.trim().toUpperCase()
    if (!code || this.data.resolving) return
    this.setData({ resolving: true, 'form.inviteCode': code })
    try {
      const inviteInfo = await authApi.resolveInvite(code)
      this.setData({ inviteInfo })
    } catch (_) {
      this.setData({ inviteInfo: null })
    } finally {
      this.setData({ resolving: false })
    }
  },
  chooseIdType() {
    const values = ['身份证', '护照', '港澳居民居住证', '台湾居民来往大陆通行证']
    wx.showActionSheet({ itemList: values, success: result => this.setData({ 'form.idType': values[result.tapIndex] }) })
  },
  chooseGender() {
    const values = ['男', '女', '其他']
    wx.showActionSheet({ itemList: values, success: result => this.setData({ 'form.gender': values[result.tapIndex] }) })
  },
  changeBirthDate(e: WechatMiniprogram.PickerChange) {
    this.setData({ 'form.birthDate': String(e.detail.value) })
  },
  async submit() {
    if (this.data.submitting) return
    if (Object.values(this.data.form).some(value => !String(value).trim())) {
      wx.showToast({ title: '请完整填写资料', icon: 'none' })
      return
    }
    if (!this.data.inviteInfo) {
      wx.showToast({ title: '请先校验企业邀请码', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(this.data.form.mobile)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!/^\S+@\S+\.\S+$/.test(this.data.form.email)) {
      wx.showToast({ title: '请输入正确的邮箱', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await authApi.submitJoin(this.data.form)
      wx.showToast({ title: '已提交，等待企业管理员审核', icon: 'none' })
      this.setData({ form: { ...this.data.form, inviteCode: '' }, inviteInfo: null })
      await this.load()
    } finally {
      this.setData({ submitting: false })
    }
  },
})
