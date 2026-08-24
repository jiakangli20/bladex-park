import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { uploadFile } from '../../utils/request'
import { hasCapability, requireLogin } from '../../utils/session'

const linkTypes = [
  { label: '不跳转', value: 'none' },
  { label: '外部链接', value: 'url' },
]

Page({
  data: {
    id: '',
    title: '新建广告',
    linkTypes,
    linkTypeIndex: 0,
    loading: false,
    uploading: false,
    saving: false,
    form: {
      adTitle: '', coverUrl: '', linkType: 'none', linkUrl: '', startDate: '', startClock: '', endDate: '', endClock: '', remark: '',
    },
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/customer-ad-form/index${options.id ? `?id=${options.id}` : ''}`)) return
    if (!hasCapability('customer.ad.submit')) {
      wx.showToast({ title: '当前账号仅可查看广告', icon: 'none' })
      navigateBackOr('/pages/customer-ads/index')
      return
    }
    const id = options.id || ''
    this.setData({ id, title: id ? '修改广告' : '新建广告' })
    if (id) this.loadAd()
  },

  async loadAd() {
    this.setData({ loading: true })
    try {
      const ad = await customerApi.ad(this.data.id)
      if (!['DRAFT', 'REJECTED'].includes(String(ad.auditStatus))) {
        wx.showToast({ title: '当前广告不可修改', icon: 'none' })
        navigateBackOr(`/pages/customer-ad-detail/index?id=${this.data.id}`)
        return
      }
      const linkType = ad.linkType === 'url' ? 'url' : 'none'
      this.setData({
        linkTypeIndex: linkType === 'url' ? 1 : 0,
        form: {
          adTitle: ad.title || '', coverUrl: ad.image || '', linkType, linkUrl: ad.linkUrl || '',
          startDate: String(ad.startTime || '').slice(0, 10), startClock: String(ad.startTime || '').slice(11, 16),
          endDate: String(ad.endTime || '').slice(0, 10), endClock: String(ad.endTime || '').slice(11, 16), remark: ad.remark || '',
        },
      })
    } finally { this.setData({ loading: false }) }
  },

  handleInput(event: WechatMiniprogram.Input) {
    const field = String(event.currentTarget.dataset.field || '')
    if (field) this.setData({ [`form.${field}`]: event.detail.value })
  },

  changeLinkType(event: WechatMiniprogram.PickerChange) {
    const linkTypeIndex = Number(event.detail.value || 0)
    this.setData({ linkTypeIndex, 'form.linkType': this.data.linkTypes[linkTypeIndex].value })
  },

  changeDate(event: WechatMiniprogram.PickerChange) {
    const field = String(event.currentTarget.dataset.field || '')
    if (field) this.setData({ [`form.${field}`]: event.detail.value })
  },

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

  async saveAd() {
    if (this.data.saving) return
    const form = this.data.form
    if (!form.adTitle.trim()) { wx.showToast({ title: '请输入广告标题', icon: 'none' }); return }
    if (!form.coverUrl) { wx.showToast({ title: '请上传广告封面', icon: 'none' }); return }
    if (form.linkType === 'url' && !form.linkUrl.trim()) { wx.showToast({ title: '请输入跳转链接', icon: 'none' }); return }
    if (!form.startDate || !form.startClock || !form.endDate || !form.endClock) { wx.showToast({ title: '请选择完整展示时间', icon: 'none' }); return }
    const startTime = `${form.startDate} ${form.startClock}:00`
    const endTime = `${form.endDate} ${form.endClock}:00`
    if (endTime <= startTime) { wx.showToast({ title: '结束时间必须晚于开始时间', icon: 'none' }); return }
    this.setData({ saving: true })
    try {
      const payload = {
        adTitle: form.adTitle.trim(), coverUrl: form.coverUrl, linkType: form.linkType,
        linkUrl: form.linkType === 'url' ? form.linkUrl.trim() : '',
        startTime,
        endTime,
        remark: form.remark,
      }
      const result = this.data.id ? await customerApi.updateAd(this.data.id, payload) : await customerApi.createAd(payload)
      wx.showToast({ title: '草稿已保存', icon: 'success' })
      if (this.data.id) setTimeout(() => navigateBackOr('/pages/customer-ads/index'), 500)
      else setTimeout(() => wx.redirectTo({ url: `/pages/customer-ad-detail/index?id=${result.id}` }), 500)
    } finally { this.setData({ saving: false }) }
  },

  goBack() { navigateBackOr('/pages/customer-ads/index') },
})
