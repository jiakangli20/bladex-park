import { customerApi } from '../../services/miniapp'
import { getSession, hasCapability, requireLogin } from '../../utils/session'

type SectionKey = 'company' | 'contracts' | 'payments' | 'account' | 'contact'
const inviteRoles = [
  { label: '普通成员', value: 'mini_customer_member' },
  { label: '企业管理员', value: 'mini_customer_admin' },
]
const sectionMeta: Record<SectionKey, { title: string; sub: string }> = {
  company: { title: '企业信息', sub: '维护企业基础资料和入驻信息' },
  contracts: { title: '我的合同', sub: '查看租赁合同、物业协议和合同周期' },
  payments: { title: '租金缴纳', sub: '仅展示账单及缴费状态，首版不发起支付' },
  account: { title: '账号管理', sub: '查看手机号、微信绑定和成员权限' },
  contact: { title: '联系方式', sub: '查看企业联系人和服务通知接收信息' },
}

Page({
  data: {
    sectionKey: 'company' as SectionKey, title: '', sub: '', canSave: false,
    company: {} as Record<string, any>, contracts: [] as Record<string, any>[], bills: [] as Record<string, any>[],
    accountRows: [] as Array<{ label: string; value: string }>, contactRows: [] as Array<{ label: string; value: string }>,
    canManageMembers: false,
    members: [] as Record<string, any>[],
    invites: [] as Record<string, any>[],
    inviteRoles,
    inviteRoleIndex: 0,
    inviteForm: { mobile: '', validHours: '72' },
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/profile-section/index')) return
    const sectionKey = this.normalizeSection(options.type)
    this.setData({ sectionKey, title: sectionMeta[sectionKey].title, sub: sectionMeta[sectionKey].sub, canSave: sectionKey === 'company' && hasCapability('customer.profile.edit') })
    this.loadData()
  },

  async loadData() {
    const session = getSession()
    if (this.data.sectionKey === 'company' || this.data.sectionKey === 'contact') {
      const raw = await customerApi.company()
      const company = { ...raw, name: raw.companyName, contact: raw.contact, phone: raw.phone }
      this.setData({ company, contactRows: [
        { label: '企业联系人', value: raw.contact || '-' }, { label: '通知手机号', value: raw.phone || '-' }, { label: '服务邮箱', value: raw.email || '-' },
      ] })
    }
    if (this.data.sectionKey === 'contracts') {
      const contracts = (await customerApi.contracts()).map(item => ({ ...item, period: `${item.periodStart || '-'} 至 ${item.periodEnd || '-'}` }))
      this.setData({ contracts })
    }
    if (this.data.sectionKey === 'payments') {
      const bills = (await customerApi.bills()).map(item => ({ ...item, period: `${item.periodStart || '-'} 至 ${item.periodEnd || '-'}` }))
      this.setData({ bills })
    }
    if (this.data.sectionKey === 'account') {
      const canManageMembers = hasCapability('customer.member.manage') && hasCapability('customer.invite.manage')
      this.setData({ accountRows: [
        { label: '绑定手机号', value: session?.profile?.mobile || '-' },
        { label: '微信绑定', value: '已绑定' },
        { label: '企业角色', value: session?.roleCodes.join(', ') || '-' },
        { label: '企业', value: session?.profile?.enterpriseName || '-' },
      ], canManageMembers })
      if (canManageMembers) await this.loadMembers()
    }
  },

  normalizeSection(type?: string): SectionKey { return ['contracts', 'payments', 'account', 'contact'].includes(type || '') ? type as SectionKey : 'company' },
  goBack() { wx.navigateBack() },
  handleInput(event: WechatMiniprogram.Input) { const field = event.currentTarget.dataset.field as string | undefined; if (field) this.setData({ [`company.${field}`]: event.detail.value }) },
  handleInviteInput(event: WechatMiniprogram.Input) {
    const field = String(event.currentTarget.dataset.field || '')
    if (field) this.setData({ [`inviteForm.${field}`]: event.detail.value })
  },
  changeInviteRole(event: WechatMiniprogram.PickerChange) { this.setData({ inviteRoleIndex: Number(event.detail.value || 0) }) },
  async loadMembers() {
    const [members, invites] = await Promise.all([customerApi.members(), customerApi.invites()])
    const roleText: Record<string, string> = { mini_customer_member: '普通成员', mini_customer_admin: '企业管理员' }
    this.setData({
      members: members.map(item => ({ ...item, avatarText: String(item.nickname || '企').slice(0, 1), roleText: roleText[item.roleCode] || item.roleCode, statusText: Number(item.status) === 1 ? '正常' : '已停用' })),
      invites: invites.map(item => ({ ...item, roleText: roleText[item.roleCode] || item.roleCode, usageText: `${item.usedCount || 0}/${item.maxUses || 1}` })),
    })
  },
  async createInvite() {
    const mobile = this.data.inviteForm.mobile.trim()
    if (mobile && !/^1\d{10}$/.test(mobile)) { wx.showToast({ title: '请输入正确的11位手机号', icon: 'none' }); return }
    const validHours = Number(this.data.inviteForm.validHours)
    if (!Number.isInteger(validHours) || validHours < 1 || validHours > 720) { wx.showToast({ title: '有效期应为1至720小时', icon: 'none' }); return }
    const created = await customerApi.createInvite({ mobile: mobile || null, validHours, maxUses: 1, roleCode: this.data.inviteRoles[this.data.inviteRoleIndex].value })
    await wx.showModal({ title: '邀请码已生成', content: `${created.code}\n\n邀请码仅在本次生成时展示，请发送给对应企业成员。`, showCancel: false, confirmText: '知道了' })
    this.setData({ inviteForm: { mobile: '', validHours: '72' }, inviteRoleIndex: 0 })
    await this.loadMembers()
  },
  async disableMember(event: WechatMiniprogram.TouchEvent) {
    const modal = await wx.showModal({ title: '停用成员', content: `确定停用成员 ${event.currentTarget.dataset.name || ''} 吗？` })
    if (!modal.confirm) return
    await customerApi.disableMember(String(event.currentTarget.dataset.id))
    wx.showToast({ title: '成员已停用', icon: 'success' })
    await this.loadMembers()
  },
  async openContract(event: WechatMiniprogram.TouchEvent) {
    const contract = await customerApi.contract(String(event.currentTarget.dataset.id))
    wx.showModal({ title: contract.title, content: `合同编号：${contract.contractNo || '-'}\n房间：${contract.room || '-'}\n周期：${contract.periodStart || '-'} 至 ${contract.periodEnd || '-'}\n月租金：${contract.amount || '-'}`, showCancel: false })
  },
  async payBill(event: WechatMiniprogram.TouchEvent) {
    const bill = await customerApi.bill(String(event.currentTarget.dataset.id))
    wx.showModal({ title: '账单详情', content: `${bill.title}\n金额：${bill.amount}\n截止日期：${bill.dueDate || '-'}\n当前状态：${bill.status}\n\n首版暂不支持微信支付。`, showCancel: false })
  },
  async saveInfo() {
    await customerApi.saveCompany({
      enterpriseName: this.data.company.name, industry: this.data.company.industry, scale: this.data.company.scale,
      contactName: this.data.company.contact, contactPhone: this.data.company.phone,
      contactEmail: this.data.company.email, address: this.data.company.address, businessScope: this.data.company.businessScope,
    })
    wx.showToast({ title: '信息已保存', icon: 'success' })
  },
})
