import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'

Page({
  data: { loading: true, ad: {} as Record<string, any> },
  async onLoad(options: Record<string, string>) {
    try { this.setData({ ad: await publicApi.ad(options.id) }) }
    catch (_) { wx.showToast({ title: '广告不存在或已下架', icon: 'none' }) }
    finally { this.setData({ loading: false }) }
  },
  goBack() { navigateBackOr('/pages/ads/index') },
})
