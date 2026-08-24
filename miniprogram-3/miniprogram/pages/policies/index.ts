import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'

Page({
  data: { loading: true, error: '', policies: [] as Record<string, any>[] },
  onLoad() { this.loadPolicies() },
  async loadPolicies() {
    this.setData({ loading: true, error: '' })
    try { this.setData({ policies: await publicApi.policies() }) }
    catch (_) { this.setData({ error: '政策加载失败，请稍后重试' }) }
    finally { this.setData({ loading: false }) }
  },
  openPolicy(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/policy-detail/index?id=${event.currentTarget.dataset.id}` })
  },
  retry() { this.loadPolicies() },
  goBack() { navigateBackOr('/pages/index/index') },
})
