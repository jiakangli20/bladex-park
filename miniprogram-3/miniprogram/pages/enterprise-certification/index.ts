import { authApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const statusText: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
}

Page({
  data: {
    form: {
      subjectType: 'ENTERPRISE',
      enterpriseName: '',
      contactName: '',
      contactPhone: '',
      contactEmail: '',
      creditCode: '',
      legalRepresentative: '',
      registeredCapital: '',
    },
    parks: [] as Record<string, any>[],
    selectedParks: [] as string[],
    selectedParkText: '请选择园区',
    parkPickerVisible: false,
    submitting: false,
    records: [] as Record<string, any>[],
  },

  onLoad() {
    if (!requireLogin('/pages/enterprise-certification/index')) return
    this.load()
  },

  async load() {
    const [context, records] = await Promise.all([
      authApi.enterpriseContext().catch(() => ({ parks: [] })),
      authApi.certifications().catch(() => []),
    ])
    this.setData({
      parks: context.parks || [],
      records: (records || []).filter(item => item.applicationType !== 'ADD_PARK').map(item => ({
        ...item,
        statusText: statusText[item.status] || item.status,
      })),
    })
  },

  goBack() {
    wx.navigateBack()
  },

  handleInput(e: WechatMiniprogram.Input) {
    const key = String(e.currentTarget.dataset.key)
    this.setData({ [`form.${key}`]: e.detail.value })
  },

  changeSubjectType(e: WechatMiniprogram.TouchEvent) {
    this.setData({ 'form.subjectType': String(e.currentTarget.dataset.value) })
  },

  openParkPicker() {
    this.setData({ parkPickerVisible: true })
  },

  closeParkPicker() {
    this.setData({ parkPickerVisible: false })
  },

  togglePark(e: WechatMiniprogram.TouchEvent) {
    const id = String(e.currentTarget.dataset.id)
    const selected = this.data.selectedParks.includes(id)
      ? this.data.selectedParks.filter(item => item !== id)
      : [...this.data.selectedParks, id]
    const names = this.data.parks
      .filter(item => selected.includes(String(item.id)))
      .map(item => item.name)
    this.setData({
      selectedParks: selected,
      selectedParkText: names.length ? names.join('、') : '请选择园区',
    })
  },

  async submit() {
    if (this.data.submitting) return
    const form = this.data.form
    const isEnterprise = form.subjectType === 'ENTERPRISE'
    if (!form.enterpriseName.trim() || !form.contactName.trim() || !form.contactPhone.trim()
      || !form.contactEmail.trim() || !this.data.selectedParks.length) {
      wx.showToast({ title: '请填写完整信息并选择园区', icon: 'none' })
      return
    }
    if (isEnterprise && (!form.creditCode.trim() || !form.legalRepresentative.trim()
      || !form.registeredCapital.trim())) {
      wx.showToast({ title: '请填写完整工商信息', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(form.contactPhone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!/^\S+@\S+\.\S+$/.test(form.contactEmail)) {
      wx.showToast({ title: '请输入正确的邮箱地址', icon: 'none' })
      return
    }
    if (isEnterprise && (!/^\d+(\.\d{1,2})?$/.test(form.registeredCapital)
      || Number(form.registeredCapital) < 0)) {
      wx.showToast({ title: '注册资本最多保留两位小数', icon: 'none' })
      return
    }

    const payload = {
      ...form,
      enterpriseName: form.enterpriseName.trim(),
      contactName: form.contactName.trim(),
      contactPhone: form.contactPhone.trim(),
      contactEmail: form.contactEmail.trim(),
      creditCode: isEnterprise ? form.creditCode.trim() : '',
      legalRepresentative: isEnterprise ? form.legalRepresentative.trim() : '',
      registeredCapital: isEnterprise ? form.registeredCapital : null,
      parkIds: this.data.selectedParks,
    }
    this.setData({ submitting: true })
    try {
      await authApi.submitCertification(payload)
      wx.showToast({ title: '已提交审核', icon: 'success' })
      await this.load()
    } finally {
      this.setData({ submitting: false })
    }
  },
})
