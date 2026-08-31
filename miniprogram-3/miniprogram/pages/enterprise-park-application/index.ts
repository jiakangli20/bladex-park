import { authApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const statusText: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }

Page({
  data: {
    contextName: '',
    currentSubjectId: '',
    parks: [] as Record<string, any>[],
    selectedIds: [] as string[],
    records: [] as Record<string, any>[],
    submitting: false,
  },
  onLoad() {
    if (!requireLogin('/pages/enterprise-park-application/index')) return
    this.load()
  },
  async load() {
    const [context, certifications] = await Promise.all([authApi.enterpriseContext(), authApi.certifications()])
    const subjectId = String(context.currentEnterpriseSubjectId || '')
    const currentRelations = (context.enterprises || []).filter((item: Record<string, any>) => String(item.enterpriseSubjectId) === subjectId)
    const occupied = currentRelations.map((item: Record<string, any>) => String(item.parkId))
    const current = currentRelations.find((item: Record<string, any>) => String(item.parkId) === String(context.currentParkId)) || currentRelations[0]
    const parks = (context.parks || []).filter((item: Record<string, any>) => !occupied.includes(String(item.id))).map((item: Record<string, any>) => ({ ...item, selected: false }))
    const records = (certifications || []).filter(item => item.applicationType === 'ADD_PARK' && String(item.enterpriseSubjectId) === subjectId).map(item => ({
      ...item,
      parkNamesText: (item.parkNames || []).join('、'),
      statusText: statusText[item.status] || item.status,
      statusClass: item.status === 'APPROVED' ? 'status-green' : item.status === 'REJECTED' ? 'status-red' : 'status-blue',
    }))
    this.setData({ contextName: current?.enterpriseName || '', currentSubjectId: subjectId, parks, records, selectedIds: [] })
  },
  goBack() { wx.navigateBack() },
  togglePark(e: WechatMiniprogram.TouchEvent) {
    const id = String(e.currentTarget.dataset.id)
    const selectedIds = this.data.selectedIds.includes(id) ? this.data.selectedIds.filter(item => item !== id) : [...this.data.selectedIds, id]
    this.setData({ selectedIds, parks: this.data.parks.map(item => ({ ...item, selected: selectedIds.includes(String(item.id)) })) })
  },
  async submit() {
    if (this.data.submitting) return
    if (!this.data.selectedIds.length) {
      wx.showToast({ title: '请至少选择一个园区', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await authApi.submitParkApplication({ parkIds: this.data.selectedIds })
      wx.showToast({ title: '已提交，等待后台审核', icon: 'none' })
      await this.load()
    } finally {
      this.setData({ submitting: false })
    }
  },
})
