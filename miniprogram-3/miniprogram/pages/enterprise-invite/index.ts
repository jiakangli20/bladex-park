import { authApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const formatTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 16) : '-'
const statusText: Record<string, string> = { NONE: '尚未生成', ACTIVE: '使用中', EXPIRED: '已过期', EXHAUSTED: '已用尽' }

Page({
  data: {
    loading: true,
    generating: false,
    invite: null as Record<string, any> | null,
    validOptions: [
      { label: '24 小时', value: 24 },
      { label: '72 小时', value: 72 },
      { label: '7 天', value: 168 },
      { label: '30 天', value: 720 },
    ],
    validIndex: 1,
    maxUses: '10',
  },
  onLoad() {
    if (!requireLogin('/pages/enterprise-invite/index')) return
    this.load()
  },
  async load() {
    this.setData({ loading: true })
    try {
      const invite = await authApi.currentInvite()
      this.setData({ invite: this.decorate(invite) })
    } finally {
      this.setData({ loading: false })
    }
  },
  decorate(invite: Record<string, any>) {
    return {
      ...invite,
      statusText: statusText[invite.status] || invite.status,
      expireTimeText: formatTime(invite.expireTime),
    }
  },
  goBack() { wx.navigateBack() },
  changeValid(e: WechatMiniprogram.PickerChange) {
    this.setData({ validIndex: Number(e.detail.value) })
  },
  changeMaxUses(e: WechatMiniprogram.Input) {
    this.setData({ maxUses: e.detail.value })
  },
  async generate() {
    if (this.data.generating) return
    const maxUses = Number(this.data.maxUses)
    if (!Number.isInteger(maxUses) || maxUses < 1 || maxUses > 10000) {
      wx.showToast({ title: '可用次数请输入 1-10000 的整数', icon: 'none' })
      return
    }
    if (this.data.invite?.code) {
      const confirmed = await new Promise<boolean>(resolve => wx.showModal({
        title: '重新生成邀请码',
        content: '原邀请码将立即失效，是否继续？',
        confirmText: '重新生成',
        success: result => resolve(result.confirm),
        fail: () => resolve(false),
      }))
      if (!confirmed) return
    }
    this.setData({ generating: true })
    try {
      const validHours = this.data.validOptions[this.data.validIndex].value
      const invite = await authApi.createInvite({ validHours, maxUses })
      this.setData({ invite: this.decorate(invite) })
      wx.showToast({ title: '邀请码已生成', icon: 'success' })
    } finally {
      this.setData({ generating: false })
    }
  },
  copyCode() {
    const code = this.data.invite?.code
    if (!code) return
    wx.setClipboardData({ data: code, success: () => wx.showToast({ title: '邀请码已复制', icon: 'success' }) })
  },
})
