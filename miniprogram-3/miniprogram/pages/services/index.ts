import { ValueService, propertyServices, serviceCategories, valueServices } from '../../utils/mock'

type PropertyServiceCard = {
  key: string
  title: string
  tone: string
  desc: string
  badge: string
  eta: string
}

type ValueServiceCard = ValueService & {
  category: string
}

const propertyCopy: Record<string, { desc: string; badge: string; eta: string }> = {
  repair: {
    desc: '空调、照明、门禁、给排水等现场问题快速登记',
    badge: '发起报修',
    eta: '2小时响应',
  },
  parking: {
    desc: '企业车辆备案、固定车位和临停需求统一申请',
    badge: '申请车位',
    eta: '当天受理',
  },
  utility: {
    desc: '水费、电费、能耗账单登记，后续可接线上支付',
    badge: '去缴费',
    eta: '账单核验',
  },
  complaint: {
    desc: '服务、环境、安全等问题反馈，运营专员跟进闭环',
    badge: '提交反馈',
    eta: '专人跟进',
  },
  meeting: {
    desc: '会议室、路演厅、培训空间预约和使用确认',
    badge: '预约场地',
    eta: '资源确认',
  },
}

const valueCategoryMap: Record<string, string> = {
  register: '工商服务',
  trademark: '知识产权',
}

const buildPropertyCards = (): PropertyServiceCard[] =>
  propertyServices.map((item) => ({
    ...item,
    ...(propertyCopy[item.key] || {
      desc: '提交服务需求，园区运营人员会跟进处理',
      badge: '立即办理',
      eta: '待受理',
    }),
  }))

const buildValueCards = (category: string): ValueServiceCard[] =>
  valueServices
    .map((item) => ({
      ...item,
      category: valueCategoryMap[item.id] || '企业服务',
    }))
    .filter((item) => category === '全部' || item.category === category)

Page({
  data: {
    activeTab: 'property',
    activeCategory: '全部',
    serviceStats: [
      { value: '5', label: '物业事项' },
      { value: '2', label: '增值服务' },
      { value: '3', label: '进行中' },
    ],
    processSteps: ['提交申请', '管理员受理', '进度反馈'],
    propertyCards: buildPropertyCards(),
    serviceCategories,
    valueCards: buildValueCards('全部'),
  },

  onLoad(options: Record<string, string | undefined>) {
    if (options.tab === 'value') {
      this.setData({ activeTab: 'value' })
    }
  },

  goBack() {
    wx.redirectTo({ url: '/pages/index/index' })
  },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const tab = event.currentTarget.dataset.tab
    if (tab === this.data.activeTab) {
      return
    }
    this.setData({ activeTab: tab })
  },

  selectCategory(event: WechatMiniprogram.TouchEvent) {
    const category = String(event.currentTarget.dataset.category || '全部')
    this.setData({
      activeCategory: category,
      valueCards: buildValueCards(category),
    })
  },

  openPropertyService(event: WechatMiniprogram.TouchEvent) {
    const key = event.currentTarget.dataset.key
    wx.navigateTo({
      url: `/pages/property-form/index?type=${key}`,
    })
  },

  openServiceDetail(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/service-detail/index?id=${id}`,
    })
  },

  openWorkOrders() {
    wx.navigateTo({
      url: `/pages/work-orders/index?tab=${this.data.activeTab}`,
    })
  },
})
