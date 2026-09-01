import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { DEFAULT_AD_IMAGE, resolveAdImage } from '../../utils/media'

Page({
  data: { loading: true, error: '', ads: [] as Record<string, any>[] },
  onLoad() { this.loadAds() },
  async loadAds() {
    this.setData({ loading: true, error: '' })
    try {
      const ads = (await publicApi.ads()).map(item => ({
        ...item,
        ...resolveAdImage(item),
      }))
      this.setData({ ads })
    }
    catch (_) { this.setData({ error: '广告加载失败，请稍后重试' }) }
    finally { this.setData({ loading: false }) }
  },
  openAd(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/ad-detail/index?id=${event.currentTarget.dataset.id}` })
  },
  handleImageError(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id)
    const index = this.data.ads.findIndex(item => String(item.id) === id)
    if (index >= 0) this.setData({ [`ads[${index}].image`]: this.data.ads[index].fallbackImage || DEFAULT_AD_IMAGE })
  },
  retry() { this.loadAds() },
  goBack() { navigateBackOr('/pages/index/index') },
})
