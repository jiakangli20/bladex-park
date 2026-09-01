import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { DEFAULT_AD_IMAGE, resolveAdImage } from '../../utils/media'

Page({
  data: { loading: true, ad: {} as Record<string, any> },
  async onLoad(options: Record<string, string>) {
    try {
      const ad = await publicApi.ad(options.id)
      this.setData({ ad: { ...ad, ...resolveAdImage(ad) } })
    }
    catch (_) { wx.showToast({ title: '广告不存在或已下架', icon: 'none' }) }
    finally { this.setData({ loading: false }) }
  },
  goBack() { navigateBackOr('/pages/ads/index') },
  handleImageError() { this.setData({ 'ad.image': this.data.ad.fallbackImage || DEFAULT_AD_IMAGE }) },
})
