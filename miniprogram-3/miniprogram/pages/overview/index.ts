import { adminApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

Page({
  data: { metrics: [] as Record<string, any>[], progress: {} as Record<string, any>, todos: [] as Record<string, any>[], tenants: [] as Record<string, any>[] },
  onLoad() {
    if (!requireLogin('/pages/overview/index') || !hasCapability('admin.overview.view')) return
    this.loadOverview()
  },
  async loadOverview() {
    const [overview, tenants] = await Promise.all([adminApi.overview(), adminApi.tenants()])
    const room = overview.roomSummary || {}
    const rent = overview.rentMetrics || {}
    const digital = overview.digitalOverview || []
    this.setData({
      metrics: [
        { label: '房源总数', value: room.totalRooms || 0, unit: '间', tone: 'blue' },
        { label: '空置房源', value: room.vacantRooms || 0, unit: '间', tone: 'orange' },
        { label: '在租房源', value: room.occupiedRooms || 0, unit: '间', tone: 'green' },
        { label: '未读提醒', value: overview.unreadNotifications || 0, unit: '条', tone: 'red' },
      ],
      progress: { rentedArea: room.occupiedRooms || 0, totalArea: room.totalRooms || 0, rentRate: Number(rent.rentRate || 0), monthReceived: digital[0]?.value || 0, monthReceivable: digital[2]?.value || 0, collectionRate: 0 },
      todos: [
        { label: '物业申请', count: '-', tone: 'blue' }, { label: '增值服务', count: '-', tone: 'orange' }, { label: '通知提醒', count: overview.unreadNotifications || 0, tone: 'red' },
      ],
      tenants: tenants.map(item => ({ ...item, room: '-', area: '-', leasePeriod: '-', rentStatus: item.status || '正常', contractStatus: '-' })),
    })
  },
  goBack() {
    const returnHome = () => wx.reLaunch({ url: '/pages/index/index' })
    if (getCurrentPages().length <= 1) {
      returnHome()
      return
    }
    wx.navigateBack({
      delta: 1,
      fail: returnHome,
    })
  },
  openTenant(event: WechatMiniprogram.TouchEvent) { wx.navigateTo({ url: `/pages/tenant-detail/index?id=${event.currentTarget.dataset.id}` }) },
  openTodo(event: WechatMiniprogram.TouchEvent) {
    const label = event.currentTarget.dataset.label
    if (label === '物业申请') return void wx.navigateTo({ url: '/pages/admin-work-orders/index?type=property' })
    if (label === '增值服务') return void wx.navigateTo({ url: '/pages/admin-work-orders/index?type=value' })
    wx.navigateTo({ url: '/pages/notifications/index' })
  },
})
