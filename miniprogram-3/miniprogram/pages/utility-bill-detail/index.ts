import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { uploadFile } from '../../utils/request'
import { hasCapability, requireLogin } from '../../utils/session'

const statusMeta = (bill: Record<string, any>) => {
  if (String(bill.status) === '1') return { text: '已结清', tone: 'green' }
  if (String(bill.status) === '3') return { text: '部分缴费', tone: 'orange' }
  if (bill.dueDate && new Date(`${bill.dueDate}T23:59:59`).getTime() < Date.now()) return { text: '已逾期', tone: 'red' }
  return { text: '待缴费', tone: 'blue' }
}

Page({
  data: {
    id: '',
    loading: true,
    error: '',
    bill: {} as Record<string, any>,
    submissions: [] as Record<string, any>[],
    canSubmit: false,
    uploading: false,
    submitting: false,
    form: { submitAmount: '', voucherName: '', voucherUrl: '' },
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/utility-bill-detail/index?id=${options.id || ''}`)) return
    this.setData({ id: options.id || '' })
  },

  onShow() {
    if (this.data.id && requireLogin(`/pages/utility-bill-detail/index?id=${this.data.id}`)) this.loadDetail()
  },

  async loadDetail() {
    this.setData({ loading: true, error: '' })
    try {
      const raw = await customerApi.utilityBill(this.data.id)
      const status = statusMeta(raw)
	  const submissionStatusText: Record<string, string> = { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已驳回' }
	  const submissionTone: Record<string, string> = { PENDING: 'blue', CONFIRMED: 'green', REJECTED: 'red' }
      const submissions = (raw.submissions || []).map((item: Record<string, any>) => {
		const itemStatus = String(item.status || '')
		return {
		  ...item,
		  statusText: submissionStatusText[itemStatus] || itemStatus,
		  tone: submissionTone[itemStatus] || 'gray',
		  amountText: Number(item.amount || 0).toFixed(2),
		}
	  })
      const hasPending = submissions.some((item: Record<string, any>) => item.status === 'PENDING')
      const remaining = Number(raw.remainingAmount || 0)
      this.setData({
        bill: {
          ...raw,
          typeText: raw.type === 'water' ? '水费' : '电费',
          statusText: status.text,
          tone: status.tone,
          amountText: Number(raw.amount || 0).toFixed(2),
          paidText: Number(raw.paidAmount || 0).toFixed(2),
          remainingText: remaining.toFixed(2),
          unitPriceText: Number(raw.unitPrice || 0).toFixed(4),
          usageText: Number(raw.usage || 0).toFixed(2),
        },
        submissions,
        canSubmit: hasCapability('customer.utility.submit') && remaining > 0 && !hasPending,
        form: { submitAmount: remaining > 0 ? remaining.toFixed(2) : '', voucherName: '', voucherUrl: '' },
      })
    } catch (error) {
      this.setData({ error: '水电账单详情加载失败' })
    } finally {
      this.setData({ loading: false })
    }
  },

  handleAmount(event: WechatMiniprogram.Input) {
    this.setData({ 'form.submitAmount': event.detail.value })
  },

  async chooseVoucher() {
    if (this.data.uploading) return
    try {
      const media = await wx.chooseMedia({ count: 1, mediaType: ['image'], sourceType: ['album', 'camera'] })
      const file = media.tempFiles[0]
      if (!file) return
      this.setData({ uploading: true })
      const uploaded = await uploadFile(file.tempFilePath)
      this.setData({ form: { ...this.data.form, voucherName: uploaded.name, voucherUrl: uploaded.url } })
      wx.showToast({ title: '凭证已上传', icon: 'success' })
    } finally {
      this.setData({ uploading: false })
    }
  },

  async submitVoucher() {
    if (!this.data.canSubmit || this.data.submitting) return
    const amount = Number(this.data.form.submitAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      wx.showToast({ title: '请输入有效付款金额', icon: 'none' })
      return
    }
    if (amount > Number(this.data.bill.remainingAmount || 0)) {
      wx.showToast({ title: '付款金额不能超过待缴金额', icon: 'none' })
      return
    }
    if (!this.data.form.voucherUrl) {
      wx.showToast({ title: '请先上传付款凭证', icon: 'none' })
      return
    }
    const confirm = await wx.showModal({ title: '提交付款凭证', content: `确认提交付款金额 ¥${amount.toFixed(2)}？园区管理员核验后更新账单状态。` })
    if (!confirm.confirm) return
    this.setData({ submitting: true })
    try {
      await customerApi.submitUtilityPayment(this.data.id, {
        submitAmount: amount,
        voucherName: this.data.form.voucherName || '付款凭证',
        voucherUrl: this.data.form.voucherUrl,
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      await this.loadDetail()
    } finally {
      this.setData({ submitting: false })
    }
  },

  previewVoucher(event: WechatMiniprogram.TouchEvent) {
    const url = String(event.currentTarget.dataset.url || '')
    if (url) wx.previewImage({ current: url, urls: [url] })
  },

  retry() { this.loadDetail() },
  goBack() { navigateBackOr('/pages/utility-bills/index') },
})
