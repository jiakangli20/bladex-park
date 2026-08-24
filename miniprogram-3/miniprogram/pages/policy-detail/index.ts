import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'

Page({
  data: { loading: true, error: '', policy: {} as Record<string, any> },
  async onLoad(options: Record<string, string | undefined>) {
    if (!options.id) {
      this.setData({ loading: false, error: '政策参数不正确' })
      return
    }
    try { this.setData({ policy: await publicApi.policy(options.id) }) }
    catch (_) { this.setData({ error: '政策不存在、已下架或已过期' }) }
    finally { this.setData({ loading: false }) }
  },
  goBack() { navigateBackOr('/pages/policies/index') },
})
