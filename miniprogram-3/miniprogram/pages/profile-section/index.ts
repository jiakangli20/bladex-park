import { customerApi } from '../../services/miniapp'
import { getSession, hasCapability, requireLogin } from '../../utils/session'

type SectionKey = 'company' | 'contracts' | 'payments' | 'account' | 'contact'
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
      this.setData({ accountRows: [
        { label: '绑定手机号', value: session?.profile?.mobile || '-' },
        { label: '微信绑定', value: '已绑定' },
        { label: '企业角色', value: session?.roleCodes.join(', ') || '-' },
        { label: '企业', value: session?.profile?.enterpriseName || '-' },
      ] })
    }
  },

  normalizeSection(type?: string): SectionKey { return ['contracts', 'payments', 'account', 'contact'].includes(type || '') ? type as SectionKey : 'company' },
  goBack() { wx.navigateBack() },
  handleInput(event: WechatMiniprogram.Input) { const field = event.currentTarget.dataset.field as string | undefined; if (field) this.setData({ [`company.${field}`]: event.detail.value }) },
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
