import { authApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const tabs = [
  { label: '待审核', value: 'PENDING' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
]
const formatTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '-'

Page({
  data: { tabs, activeStatus: 'PENDING', rows: [] as Record<string, any>[], loading: true, reviewingId: '' },
  onLoad() {
    if (!requireLogin('/pages/enterprise-join-review/index')) return
    this.load()
  },
  goBack() { wx.navigateBack() },
  switchStatus(e: WechatMiniprogram.TouchEvent) {
    const activeStatus = String(e.currentTarget.dataset.status)
    if (activeStatus === this.data.activeStatus) return
    this.setData({ activeStatus })
    this.load()
  },
  async load() {
    this.setData({ loading: true })
    try {
      const rows = await authApi.ownerJoins(this.data.activeStatus)
      const labels: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
      this.setData({ rows: (rows || []).map(item => ({ ...item, statusText: labels[item.status] || item.status, createTimeText: formatTime(item.createTime), reviewTimeText: formatTime(item.reviewTime) })) })
    } finally {
      this.setData({ loading: false })
    }
  },
  async approve(e: WechatMiniprogram.TouchEvent) {
    const id = String(e.currentTarget.dataset.id)
    const confirmed = await this.confirm('通过申请', '通过后，该员工将立即获得当前企业和园区权限。', false)
    if (!confirmed.confirm) return
    await this.review(id, 'APPROVE', '')
  },
  async reject(e: WechatMiniprogram.TouchEvent) {
    const id = String(e.currentTarget.dataset.id)
    const result = await this.confirm('驳回申请', '可填写驳回原因', true)
    if (!result.confirm) return
    await this.review(id, 'REJECT', result.content || '')
  },
  confirm(title: string, content: string, editable: boolean) {
    return new Promise<WechatMiniprogram.ShowModalSuccessCallbackResult>(resolve => wx.showModal({
      title,
      content,
      editable,
      placeholderText: editable ? '请输入审核说明（选填）' : '',
      confirmText: title.slice(0, 2),
      success: resolve,
    }))
  },
  async review(id: string, action: string, remark: string) {
    if (this.data.reviewingId) return
    this.setData({ reviewingId: id })
    try {
      await authApi.reviewJoin(id, { action, remark })
      wx.showToast({ title: '处理成功', icon: 'success' })
      await this.load()
    } finally {
      this.setData({ reviewingId: '' })
    }
  },
})
