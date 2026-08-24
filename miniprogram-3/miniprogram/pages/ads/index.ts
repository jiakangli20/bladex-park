import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'

Page({
  data: { loading: true, error: '', ads: [] as Record<string, any>[] },
  onLoad() { this.loadAds() },
  async loadAds() {
    this.setData({ loading: true, error: '' })
    try { this.setData({ ads: await publicApi.ads() }) }
    catch (_) { this.setData({ error: '广告加载失败，请稍后重试' }) }
    finally { this.setData({ loading: false }) }
  },
  openAd(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/ad-detail/index?id=${event.currentTarget.dataset.id}` })
  },
  retry() { this.loadAds() },
  goBack() { navigateBackOr('/pages/index/index') },
})
