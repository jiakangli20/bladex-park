import { bills, companyProfile, contracts } from '../../utils/mock'

type SectionKey = 'company' | 'contracts' | 'payments' | 'account' | 'contact'

const sectionMeta: Record<SectionKey, { title: string; sub: string; canSave: boolean }> = {
  company: {
    title: '企业信息',
    sub: '维护企业基础资料和入驻房间信息',
    canSave: true,
  },
  contracts: {
    title: '我的合同',
    sub: '查看租赁合同、物业协议和合同周期',
    canSave: false,
  },
  payments: {
    title: '租金缴纳',
    sub: '查看应缴账单、逾期账单和缴费记录',
    canSave: false,
  },
  account: {
    title: '账号管理',
    sub: '查看手机号、微信绑定和成员权限',
    canSave: true,
  },
  contact: {
    title: '联系方式',
    sub: '维护企业联系人和服务通知接收人',
    canSave: true,
  },
}

Page({
  data: {
    sectionKey: 'company',
    title: sectionMeta.company.title,
    sub: sectionMeta.company.sub,
    canSave: sectionMeta.company.canSave,
    company: { ...companyProfile },
    contracts,
    bills,
    accountRows: [
      { label: '绑定手机号', value: '13800008888' },
      { label: '微信绑定', value: '已绑定' },
      { label: '企业角色', value: '企业管理员' },
      { label: '成员数量', value: '12人' },
    ],
    contactRows: [
      { label: '企业联系人', value: '李经理' },
      { label: '物业联系人', value: '王女士' },
      { label: '通知手机号', value: '13800008888' },
      { label: '服务邮箱', value: 'service@example.com' },
    ],
  },

  onLoad(options: Record<string, string | undefined>) {
    const sectionKey = this.normalizeSection(options.type)
    this.setData({
      sectionKey,
      title: sectionMeta[sectionKey].title,
      sub: sectionMeta[sectionKey].sub,
      canSave: sectionMeta[sectionKey].canSave,
    })
  },

  normalizeSection(type?: string): SectionKey {
    if (type === 'contracts' || type === 'payments' || type === 'account' || type === 'contact') {
      return type
    }
    return 'company'
  },

  goBack() {
    wx.navigateBack()
  },

  handleInput(event: WechatMiniprogram.Input) {
    const field = event.currentTarget.dataset.field as string | undefined
    if (!field) {
      return
    }
    this.setData({
      [`company.${field}`]: event.detail.value,
    })
  },

  openContract(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    const contract = contracts.find((item) => item.id === id)
    if (!contract) {
      return
    }
    wx.showModal({
      title: contract.title,
      content: `合同编号：${contract.id}\n房间：${contract.room}\n周期：${contract.period}\n金额：${contract.amount}`,
      confirmText: '知道了',
      showCancel: false,
    })
  },

  payBill(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    const bill = bills.find((item) => item.id === id)
    if (!bill) {
      return
    }
    wx.showModal({
      title: bill.status === '待缴' ? '缴费确认' : '账单详情',
      content: `${bill.title}\n金额：${bill.amount}\n截止日期：${bill.dueDate}\n当前状态：${bill.status}`,
      confirmText: bill.status === '待缴' ? '去缴费' : '知道了',
      showCancel: bill.status === '待缴',
    })
  },

  saveInfo() {
    wx.showToast({
      title: '信息已保存',
      icon: 'success',
    })
  },
})
