import { customerApi, publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { hasCapability, requireLogin } from '../../utils/session'

const auditMeta: Record<string, { text: string; tone: string }> = {
  DRAFT: { text: '草稿', tone: 'gray' },
  PENDING: { text: '待审核', tone: 'blue' },
  APPROVED: { text: '已通过', tone: 'green' },
  REJECTED: { text: '已驳回', tone: 'red' },
}

Page({
  data: { loading: true, error: '', publicMode: false, canCreate: false, activities: [] as Record<string, any>[] },
  onLoad(options: Record<string, string | undefined>) {
    const publicMode = options.mode === 'public'
    this.setData({ publicMode })
    if (publicMode) return
    if (!requireLogin('/pages/customer-activities/index')) return
    this.setData({ canCreate: hasCapability('customer.activity.submit') })
  },
  onShow() {
    if (this.data.publicMode || requireLogin('/pages/customer-activities/index')) this.loadActivities()
  },
  async loadActivities() {
    this.setData({ loading: true, error: '' })
    try {
      const source = this.data.publicMode ? await publicApi.activities() : await customerApi.activities()
      const activities = source.map(item => {
        const meta = auditMeta[String(item.auditStatus)] || { text: '待审核', tone: 'blue' }
        return {
          ...item,
          auditText: meta.text,
          tone: meta.tone,
          publishText: Number(item.publishStatus) === 1 ? '已发布' : '未发布',
          rangeText: item.startTime || item.endTime ? `${item.startTime || '待定'} 至 ${item.endTime || '待定'}` : '时间待定',
        }
      })
      this.setData({ activities })
    } catch (_) { this.setData({ error: this.data.publicMode ? '园区活动加载失败，请稍后重试' : '活动申请加载失败，请稍后重试' }) }
    finally { this.setData({ loading: false }) }
  },
  createActivity() { wx.navigateTo({ url: '/pages/customer-activity-form/index' }) },
  openActivity(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/customer-activity-detail/index?id=${event.currentTarget.dataset.id}${this.data.publicMode ? '&mode=public' : ''}` }) },
  retry() { this.loadActivities() },
  goBack() { navigateBackOr('/pages/index/index') },
})
