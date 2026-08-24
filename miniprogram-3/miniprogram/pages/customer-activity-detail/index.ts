import { customerApi, publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { hasCapability, requireLogin } from '../../utils/session'

const statusMap: Record<string, { text: string; tone: string }> = {
  DRAFT: { text: '草稿', tone: 'gray' }, PENDING: { text: '待审核', tone: 'blue' },
  APPROVED: { text: '已通过', tone: 'green' }, REJECTED: { text: '已驳回', tone: 'red' },
}
const actionMap: Record<string, string> = { CREATE_DRAFT: '创建草稿', UPDATE_DRAFT: '修改草稿', SUBMIT: '提交审核', WITHDRAW: '撤回审核', APPROVE: '园区审核通过', REJECT: '园区驳回', PUBLISH: '活动发布', UNPUBLISH: '活动下架' }

Page({
  data: { id: '', publicMode: false, loading: true, error: '', acting: false, canManage: false, canEdit: false, canSubmit: false, canWithdraw: false, activity: {} as Record<string, any>, logs: [] as Record<string, any>[] },
  onLoad(options: Record<string, string | undefined>) {
    const id = options.id || ''
    const publicMode = options.mode === 'public'
    if (!publicMode && !requireLogin(`/pages/customer-activity-detail/index?id=${id}`)) return
    this.setData({ id, publicMode, canManage: !publicMode && hasCapability('customer.activity.submit') })
  },
  onShow() { if (this.data.id) this.load() },
  async load() { this.setData({ loading: true, error: '' }); try { const activity = this.data.publicMode ? await publicApi.activity(this.data.id) : await customerApi.activity(this.data.id); const meta = statusMap[String(activity.auditStatus)] || { text: '-', tone: 'gray' }; const editable = ['DRAFT', 'REJECTED'].includes(String(activity.auditStatus)); this.setData({ activity: { ...activity, auditText: meta.text, tone: meta.tone, publishText: Number(activity.publishStatus) === 1 ? '已发布' : '未发布' }, logs: (activity.logs || []).map((item: Record<string, any>) => ({ ...item, actionText: actionMap[String(item.action)] || item.action })), canEdit: !this.data.publicMode && this.data.canManage && editable, canSubmit: !this.data.publicMode && this.data.canManage && editable, canWithdraw: !this.data.publicMode && this.data.canManage && String(activity.auditStatus) === 'PENDING' }) } catch (_) { this.setData({ error: this.data.publicMode ? '活动不存在、已下架或加载失败' : '活动申请加载失败，请稍后重试' }) } finally { this.setData({ loading: false }) } },
  edit() { wx.navigateTo({ url: `/pages/customer-activity-form/index?id=${this.data.id}` }) },
  async submit() { if (this.data.acting) return; const modal = await wx.showModal({ title: '提交审核', content: '提交后由园区后台审核，审核期间不能修改。' }); if (!modal.confirm) return; this.setData({ acting: true }); try { await customerApi.submitActivity(this.data.id); wx.showToast({ title: '已提交审核', icon: 'success' }); await this.load() } finally { this.setData({ acting: false }) } },
  async withdraw() { if (this.data.acting) return; const modal = await wx.showModal({ title: '撤回审核', content: '撤回后恢复为草稿，可继续修改。' }); if (!modal.confirm) return; this.setData({ acting: true }); try { await customerApi.withdrawActivity(this.data.id); wx.showToast({ title: '已撤回', icon: 'success' }); await this.load() } finally { this.setData({ acting: false }) } },
  preview() { if (this.data.activity.image) wx.previewImage({ current: this.data.activity.image, urls: [this.data.activity.image] }) },
  retry() { this.load() },
  goBack() { navigateBackOr(this.data.publicMode ? '/pages/customer-activities/index?mode=public' : '/pages/customer-activities/index') },
})
