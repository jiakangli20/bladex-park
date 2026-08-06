export type HouseItem = {
  id: string
  title: string
  image: string
  building: string
  room: string
  area: string
  layout: string
  floor: string
  decoration: string
  orientation: string
  tags: string[]
  price: string
  propertyFee: string
  status: string
  availableDate: string
  intro: string
  facilities: string[]
}

export type QuickAction = {
  key: string
  label: string
  tone: string
}

export type HomeServiceCard = {
  key: string
  title: string
  desc: string
  tone: string
}

export type HomePolicy = {
  id: string
  title: string
  time: string
  image: string
}

export type HomeActivity = {
  id: string
  title: string
  startTime: string
  endTime: string
  address?: string
  price: string
  image: string
}

export type ValueService = {
  id: string
  title: string
  image: string
  providerType: string
  providerTone: string
  desc: string
  applied: number
  tags: string[]
  rating: string
  reviews: number
}

export type OverviewMetric = {
  label: string
  value: string
  unit: string
  tone: string
}

export type OverviewTenant = {
  id: string
  companyName: string
  industry: string
  room: string
  area: string
  leasePeriod: string
  rentStatus: string
  contractStatus: string
}

export type ProcessStep = {
  title: string
  time: string
  desc: string
  done: boolean
}

export type WorkOrder = {
  id: string
  kind: 'property' | 'value' | 'settlement'
  type: string
  title: string
  companyName: string
  room: string
  contact: string
  phone: string
  status: string
  urgency: string
  applyTime: string
  handler: string
  description: string
  steps: ProcessStep[]
}

export type AdminNotification = {
  id: string
  type: string
  title: string
  content: string
  time: string
  status: string
  target: string
  targetId: string
}

export type ContractItem = {
  id: string
  title: string
  room: string
  period: string
  amount: string
  status: string
  signDate: string
}

export type BillItem = {
  id: string
  title: string
  period: string
  amount: string
  dueDate: string
  status: string
}

export const quickActions: QuickAction[] = [
  { key: 'house', label: '我要看房', tone: 'blue' },
  { key: 'property', label: '物业服务', tone: 'green' },
  { key: 'value', label: '增值服务', tone: 'orange' },
  { key: 'orders', label: '我的工单', tone: 'red' },
  { key: 'settle', label: '入驻申请', tone: 'sky' },
  { key: 'parking-pay', label: '停车缴费', tone: 'amber' },
  { key: 'overview', label: '园区概览', tone: 'blue' },
  { key: 'more', label: '更多', tone: 'gray' },
]

export const homeServiceCards: HomeServiceCard[] = [
  { key: 'repair', title: '在线报修', desc: '报修便捷高效', tone: 'purple' },
  { key: 'venue', title: '场地预约', desc: '合理规划使用', tone: 'orange' },
  { key: 'declare', title: '申报服务', desc: '业务快捷申报', tone: 'cyan' },
  { key: 'ip', title: '知产服务', desc: '权益保障服务', tone: 'pink' },
]

export const homePolicies: HomePolicy[] = [
  {
    id: 'policy-001',
    title: '智慧园区系统：开启未来社区新篇章_副本',
    time: '2026-09-15 10:06:22',
    image: '/assets/images/policy-1.png',
  },
  {
    id: 'policy-002',
    title: '中小企业突围战：智慧园区如何破解“效率内卷”困局？_副本',
    time: '2026-09-15 10:10:48',
    image: '/assets/images/policy-2.png',
  },
]

export const homeActivities: HomeActivity[] = [
  {
    id: 'activity-001',
    title: '活动已结束 “智启未来，慧享...”',
    startTime: '2026-09-21 16:13:59',
    endTime: '2026-09-20 16:13:59',
    price: '免费',
    image: '/assets/images/activity-1.png',
  },
]

export const notices = [
  {
    title: '关于园区疫情防控措施调整的通知',
    date: '今天',
    content: '根据最新疫情防控要求，自2023年10月15日起，园区出入不再查验核酸检测证明，请各位企业员工做好个人防护...',
  },
]

export const houses: HouseItem[] = [
  {
    id: 'A1201',
    title: 'A座12层 精装修办公室',
    image: '/assets/images/house-office-a.jpg',
    building: 'A座',
    room: '1201-1205',
    area: '280m²',
    layout: '3+1格局',
    floor: '高楼层',
    decoration: '精装修',
    orientation: '南北通透',
    tags: ['近地铁', '精装修', '南北通透', '免中介费'],
    price: '3.8',
    propertyFee: '0.8元/m²/天',
    status: '可租',
    availableDate: '随时可租',
    intro: '适合研发、运营、轻咨询团队使用，公共区域已完成基础装修，采光和通风条件良好，可快速入驻办公。',
    facilities: ['中央空调', '高速网络', '独立茶水间', '会议室', '地下停车'],
  },
  {
    id: 'C2001',
    title: 'C座20层 整层办公',
    image: '/assets/images/home-featured-house.png',
    building: 'C座',
    room: '2001整层',
    area: '1200m²',
    layout: '整层',
    floor: '高楼层',
    decoration: '可定制装修',
    orientation: '江景视野',
    tags: ['江景房', '可定制装修', '免租期6个月', '政策补贴'],
    price: '4.5',
    propertyFee: '0.9元/m²/天',
    status: '可租',
    availableDate: '2026-08-01',
    intro: '整层可独立规划前台、开放办公区、管理办公室与路演空间，适合总部型企业或成长型团队整合办公。',
    facilities: ['独立前厅', '专属电梯厅', '政策申报', '企业展厅', '访客接待'],
  },
]

export const propertyServices = [
  { key: 'repair', title: '维修申请', tone: 'green' },
  { key: 'parking', title: '车位申请', tone: 'orange' },
  { key: 'utility', title: '水电缴纳', tone: 'blue' },
  { key: 'complaint', title: '投诉', tone: 'cyan' },
  { key: 'meeting', title: '会议室预订', tone: 'purple' },
]

export const serviceCategories = ['全部', '工商服务', '财税代理', '知识产权', '法律服务', '政策申报', '人才招聘']

export const valueServices: ValueService[] = [
  {
    id: 'register',
    title: '公司注册代办',
    image: '/assets/images/service-business.jpg',
    providerType: '自营',
    providerTone: 'blue',
    desc: '专业顾问一对一服务，3-7个工作日快速拿证，包含营业执照正副本、公章、财务章、法人章',
    applied: 328,
    tags: ['极速办理', '无需到场', '全程代办'],
    rating: '4.8',
    reviews: 243,
  },
  {
    id: 'trademark',
    title: '商标注册申请',
    image: '/assets/images/service-business.jpg',
    providerType: '服务商',
    providerTone: 'orange',
    desc: '专业知识产权顾问提供商标查询、注册申请、驳回复审等服务，注册成功率高达90%以上',
    applied: 189,
    tags: ['免费查询', '不成功退款', '极速上报'],
    rating: '4.7',
    reviews: 126,
  },
]

export const serviceDetails = [
  {
    title: '极速办理',
    content: '3-7个工作日即可完成注册，最快3天拿证，比传统代办效率提升50%',
  },
  {
    title: '无需到场',
    content: '全程线上办理，无需您亲自跑工商局，所有流程我们代为办理',
  },
  {
    title: '明码标价',
    content: '一次性收费，无隐形消费，注册过程中不再收取任何额外费用',
  },
  {
    title: '专业顾问',
    content: '1对1专属顾问服务，注册前免费提供政策咨询和注册方案建议',
  },
]

export const profileMenus = [
  { key: 'orders', label: '我的工单', tone: 'red', badge: '3' },
  { key: 'contracts', label: '我的合同', tone: 'blue', badge: '' },
  { key: 'payments', label: '缴费记录', tone: 'green', badge: '' },
  { key: 'company', label: '企业信息', tone: 'purple', badge: '' },
  { key: 'account', label: '账号管理', tone: 'cyan', badge: '' },
  { key: 'contact', label: '联系方式', tone: 'gray', badge: '' },
]

export const companyProfile = {
  name: '上海科技有限公司',
  creditCode: '91310000MA1K2026X8',
  contact: '李经理',
  phone: '13800008888',
  industry: '软件研发',
  scale: '80-120人',
  room: 'A座12层1201-1205',
  certifyStatus: '已认证',
}

export const contracts: ContractItem[] = [
  {
    id: 'HT20260101',
    title: 'A座12层办公租赁合同',
    room: 'A座12层1201-1205',
    period: '2026.01.01 - 2027.12.31',
    amount: '86.4万/年',
    status: '履约中',
    signDate: '2025.12.18',
  },
  {
    id: 'WY20260101',
    title: '物业服务协议',
    room: 'A座12层1201-1205',
    period: '2026.01.01 - 2026.12.31',
    amount: '6.8万/年',
    status: '履约中',
    signDate: '2025.12.18',
  },
]

export const bills: BillItem[] = [
  {
    id: 'ZD202607',
    title: '2026年7月租金账单',
    period: '2026.07.01 - 2026.07.31',
    amount: '7.2万',
    dueDate: '2026.07.25',
    status: '待缴',
  },
  {
    id: 'ZD202606',
    title: '2026年6月物业及能耗账单',
    period: '2026.06.01 - 2026.06.30',
    amount: '1.18万',
    dueDate: '2026.06.25',
    status: '已缴',
  },
  {
    id: 'ZD202605',
    title: '2026年5月租金账单',
    period: '2026.05.01 - 2026.05.31',
    amount: '7.2万',
    dueDate: '2026.05.25',
    status: '已缴',
  },
]

export const workOrders: WorkOrder[] = [
  {
    id: 'WO20260722001',
    kind: 'property',
    type: '维修申请',
    title: 'A座12层空调出风异常',
    companyName: '上海科技有限公司',
    room: 'A座12层1201-1205',
    contact: '李经理',
    phone: '13800008888',
    status: '处理中',
    urgency: '普通',
    applyTime: '2026-07-22 09:20',
    handler: '物业工程组',
    description: '会议室空调制冷效果较弱，希望安排工程人员上门检查。',
    steps: [
      { title: '企业提交', time: '2026-07-22 09:20', desc: '已提交维修申请', done: true },
      { title: '物业受理', time: '2026-07-22 09:35', desc: '物业工程组已受理', done: true },
      { title: '处理中', time: '2026-07-22 10:10', desc: '工程人员已预约上门', done: true },
      { title: '完成确认', time: '待更新', desc: '等待企业确认处理结果', done: false },
    ],
  },
  {
    id: 'WO20260721003',
    kind: 'property',
    type: '车位申请',
    title: '新增2个长期停车位',
    companyName: '上海科技有限公司',
    room: 'A座12层1201-1205',
    contact: '王女士',
    phone: '13900006666',
    status: '待受理',
    urgency: '普通',
    applyTime: '2026-07-21 16:40',
    handler: '待分配',
    description: '企业新增员工车辆，希望申请两个地下固定车位。',
    steps: [
      { title: '企业提交', time: '2026-07-21 16:40', desc: '已提交车位申请', done: true },
      { title: '物业受理', time: '待更新', desc: '等待管理员受理', done: false },
    ],
  },
  {
    id: 'VS20260720002',
    kind: 'value',
    type: '政策申报',
    title: '高企补贴申报咨询',
    companyName: '上海科技有限公司',
    room: 'A座12层1201-1205',
    contact: '李经理',
    phone: '13800008888',
    status: '沟通中',
    urgency: '重要',
    applyTime: '2026-07-20 14:08',
    handler: '企业服务中心',
    description: '需要确认2026年高新技术企业补贴申报条件和材料清单。',
    steps: [
      { title: '提交意向', time: '2026-07-20 14:08', desc: '企业已提交服务意向', done: true },
      { title: '管理员受理', time: '2026-07-20 15:30', desc: '企业服务中心已受理', done: true },
      { title: '沟通中', time: '2026-07-21 10:00', desc: '顾问正在整理申报方案', done: true },
      { title: '服务完成', time: '待更新', desc: '等待服务结果反馈', done: false },
    ],
  },
]

export const adminNotifications: AdminNotification[] = [
  {
    id: 'NT20260722001',
    type: '物业服务申请',
    title: 'A座12层空调出风异常',
    content: '上海科技有限公司提交了维修申请，请尽快处理。',
    time: '2026-07-22 09:20',
    status: '未读',
    target: 'property-order',
    targetId: 'WO20260722001',
  },
  {
    id: 'NT20260721002',
    type: '客户入驻申请',
    title: '新增入驻意向待跟进',
    content: '苏州智造产业服务有限公司提交了入驻意向，意向面积约1200m²。',
    time: '2026-07-21 17:15',
    status: '未读',
    target: 'settlement',
    targetId: 'tenant-002',
  },
  {
    id: 'NT20260720003',
    type: '增值服务申请',
    title: '高企补贴申报咨询',
    content: '上海科技有限公司申请了政策申报服务，当前处于沟通中。',
    time: '2026-07-20 14:08',
    status: '已读',
    target: 'value-order',
    targetId: 'VS20260720002',
  },
  {
    id: 'NT20260718004',
    type: '逾期缴费',
    title: '租金账单即将逾期',
    content: '2026年7月租金账单还有3天到期，请提醒企业缴纳。',
    time: '2026-07-18 09:00',
    status: '未读',
    target: 'bill',
    targetId: 'ZD202607',
  },
]

export const overviewMetrics: OverviewMetric[] = [
  { label: '出租率', value: '86', unit: '%', tone: 'blue' },
  { label: '入驻企业', value: '128', unit: '家', tone: 'green' },
  { label: '可租面积', value: '2.4', unit: '万m²', tone: 'orange' },
  { label: '待办工单', value: '16', unit: '件', tone: 'red' },
]

export const overviewProgress = {
  totalArea: '12.8万m²',
  rentedArea: '10.4万m²',
  rentRate: 86,
  monthReceivable: '326.8万',
  monthReceived: '284.5万',
  collectionRate: 87,
}

export const overviewTodos = [
  { label: '物业申请', count: 8, tone: 'green' },
  { label: '增值服务', count: 5, tone: 'orange' },
  { label: '通知中心', count: 3, tone: 'red' },
]

export const overviewTenants: OverviewTenant[] = [
  {
    id: 'tenant-001',
    companyName: '上海科技有限公司',
    industry: '软件研发',
    room: 'A座12层1201-1205',
    area: '820m²',
    leasePeriod: '2025.01.01 - 2027.12.31',
    rentStatus: '正常',
    contractStatus: '履约中',
  },
  {
    id: 'tenant-002',
    companyName: '苏州智造产业服务有限公司',
    industry: '智能制造',
    room: 'C座20层整层',
    area: '1200m²',
    leasePeriod: '2024.08.01 - 2027.07.31',
    rentStatus: '待缴',
    contractStatus: '履约中',
  },
  {
    id: 'tenant-003',
    companyName: '金控企业服务中心',
    industry: '企业服务',
    room: 'B座8层801-806',
    area: '560m²',
    leasePeriod: '2023.05.01 - 2026.04.30',
    rentStatus: '正常',
    contractStatus: '即将到期',
  },
]
