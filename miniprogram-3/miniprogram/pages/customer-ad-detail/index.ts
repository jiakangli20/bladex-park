import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { hasCapability, requireLogin } from '../../utils/session'

const statusMap: Record<string, { text: string; tone: string }> = {
  DRAFT: { text: '草稿', tone: 'gray' }, PENDING: { text: '待审核', tone: 'blue' },
  APPROVED: { text: '已通过', tone: 'green' }, REJECTED: { text: '已驳回', tone: 'red' },
}
const actionMap: Record<string, string> = {
  CREATE_DRAFT: '创建草稿', UPDATE_DRAFT: '修改草稿', SUBMIT: '提交审核', WITHDRAW: '撤回审核',
  APPROVE: '园区审核通过', REJECT: '园区驳回', ONLINE: '广告上架', OFFLINE: '广告下架',
}

Page({
  data: {
    id: '', loading: true, error: '', canManage: false, canEdit: false, canSubmit: false, canWithdraw: false,
    acting: false, ad: {} as Record<string, any>, logs: [] as Record<string, any>[],
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/customer-ad-detail/index?id=${options.id || ''}`)) return
    this.setData({ id: options.id || '', canManage: hasCapability('customer.ad.submit') })
  },

  onShow() { if (this.data.id) this.loadAd() },

  async loadAd() {
    this.setData({ loading: true, error: '' })
    try {
      const raw = await customerApi.ad(this.data.id)
      const meta = statusMap[String(raw.auditStatus)] || { text: raw.auditStatus || '-', tone: 'gray' }
      const logs = (raw.logs || []).map((item: Record<string, any>) => ({ ...item, actionText: actionMap[String(item.action)] || item.action || '状态更新' }))
      const editable = ['DRAFT', 'REJECTED'].includes(String(raw.auditStatus))
      this.setData({
        ad: { ...raw, auditStatusText: meta.text, tone: meta.tone, onlineText: String(raw.onlineStatus) === '0' ? '已上架' : '未上架' }, logs,
        canEdit: this.data.canManage && editable, canSubmit: this.data.canManage && editable,
        canWithdraw: this.data.canManage && String(raw.auditStatus) === 'PENDING',
      })
    } catch (error) { this.setData({ error: '广告详情加载失败' }) }
    finally { this.setData({ loading: false }) }
  },

  editAd() { wx.navigateTo({ url: `/pages/customer-ad-form/index?id=${this.data.id}` }) },

  async submitAd() {
    if (this.data.acting) return
    const modal = await wx.showModal({ title: '提交审核', content: '提交后将由园区管理员在后台审核，待审核期间不能修改。' })
    if (!modal.confirm) return
    this.setData({ acting: true })
    try { await customerApi.submitAd(this.data.id); wx.showToast({ title: '已提交审核', icon: 'success' }); await this.loadAd() }
    finally { this.setData({ acting: false }) }
  },

  async withdrawAd() {
    if (this.data.acting) return
    const modal = await wx.showModal({ title: '撤回审核', content: '撤回后广告恢复为草稿，可以继续修改。' })
    if (!modal.confirm) return
    this.setData({ acting: true })
    try { await customerApi.withdrawAd(this.data.id); wx.showToast({ title: '已撤回', icon: 'success' }); await this.loadAd() }
    finally { this.setData({ acting: false }) }
  },

  previewCover() { if (this.data.ad.image) wx.previewImage({ current: this.data.ad.image, urls: [this.data.ad.image] }) },
  retry() { this.loadAd() },
  goBack() { navigateBackOr('/pages/customer-ads/index') },
})
