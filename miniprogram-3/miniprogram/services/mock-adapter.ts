import {
  adminNotifications,
  bills,
  companyProfile,
  contracts,
  homeActivities,
  homePolicies,
  houses,
  overviewMetrics,
  overviewProgress,
  overviewTenants,
  overviewTodos,
  propertyServices,
  valueServices,
  workOrders,
} from '../utils/mock'
import { ZHOUDAOHUA_AD_IMAGE } from '../utils/media'

const DEFAULT_AD_IMAGE = '/assets/images/service-business.jpg'

const publicAds = [{
  id: '2',
  title: '苏周到',
  image: ZHOUDAOHUA_AD_IMAGE,
  linkType: 'none',
  linkUrl: '',
  merchantName: '',
  remark: '周到花，人人有奖',
  startTime: '2026-08-31 00:00:00',
  endTime: '2026-09-11 00:00:00',
}]

const session = {
  needBind: false,
  accessToken: 'development-mock-token',
  refreshToken: 'development-mock-refresh-token',
  expiresIn: 7200,
  tenantId: '000000',
  parkId: '1',
  customerId: '1',
  roleCodes: ['mini_customer_admin'],
  capabilities: ['customer.profile.view', 'customer.profile.edit', 'customer.contract.view', 'customer.bill.view', 'customer.work-order.view', 'customer.appointment.create', 'customer.service.apply', 'customer.utility.view', 'customer.utility.submit', 'customer.ad.view', 'customer.ad.submit'],
  profile: { userId: '1', nickname: '开发账号', mobile: '13800008888', enterpriseName: companyProfile.name },
}

type MockRequestOptions = {
  method?: string
  data?: any
}

const nowText = (): string => {
  const date = new Date()
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

let customerAds: Record<string, any>[] = [{
  id: 'ad-1',
  title: '企业服务月宣传',
  image: DEFAULT_AD_IMAGE,
  linkType: 'none',
  linkUrl: '',
  startTime: '2026-08-01 00:00:00',
  endTime: '2026-08-31 23:59:59',
  auditStatus: 'REJECTED',
  onlineStatus: '1',
  auditOpinion: '请补充活动主办企业名称并调整封面文字',
  auditTime: '2026-08-04 10:20:00',
  auditUserName: '园区管理员',
  remark: '用于展示被驳回后修改并重新提交的流程',
  createTime: '2026-08-03 09:30:00',
  logs: [
    { id: 'log-2', action: 'REJECT', operatorName: '园区管理员', opinion: '请补充活动主办企业名称并调整封面文字', operateTime: '2026-08-04 10:20:00' },
    { id: 'log-1', action: 'SUBMIT', operatorName: '开发账号', opinion: '', operateTime: '2026-08-03 09:35:00' },
  ],
}]

const mockCustomerAds = (path: string, options: MockRequestOptions): unknown => {
  const cleanPath = path.split('?')[0]
  const match = cleanPath.match(/\/customer\/ads\/([^/]+)(?:\/(submit|withdraw))?$/)
  const method = options.method || 'GET'
  if (!match) {
    if (method === 'POST') {
      const id = `ad-${Date.now()}`
      const input = options.data || {}
      const created = {
        id,
        title: input.adTitle || '未命名广告',
        image: input.coverUrl || DEFAULT_AD_IMAGE,
        linkType: input.linkType || 'none',
        linkUrl: input.linkUrl || '',
        startTime: input.startTime || '',
        endTime: input.endTime || '',
        auditStatus: 'DRAFT',
        onlineStatus: '1',
        auditOpinion: '',
        remark: input.remark || '',
        createTime: nowText(),
        logs: [{ id: `log-${Date.now()}`, action: 'CREATE_DRAFT', operatorName: '开发账号', opinion: '', operateTime: nowText() }],
      }
      customerAds = [created, ...customerAds]
      return created
    }
    return customerAds.map(({ logs, ...item }) => item)
  }

  const [, id, action] = match
  const index = customerAds.findIndex(item => String(item.id) === id)
  const current = customerAds[index] || customerAds[0]
  if (!current) return null
  if (method !== 'POST') return current

  if (action === 'submit') {
    current.auditStatus = 'PENDING'
    current.auditOpinion = ''
    current.logs = [{ id: `log-${Date.now()}`, action: 'SUBMIT', operatorName: '开发账号', opinion: '', operateTime: nowText() }, ...(current.logs || [])]
    return null
  }
  if (action === 'withdraw') {
    current.auditStatus = 'DRAFT'
    current.logs = [{ id: `log-${Date.now()}`, action: 'WITHDRAW', operatorName: '开发账号', opinion: '', operateTime: nowText() }, ...(current.logs || [])]
    return null
  }

  const input = options.data || {}
  const updated = {
    ...current,
    title: input.adTitle || current.title,
    image: input.coverUrl || current.image,
    linkType: input.linkType || 'none',
    linkUrl: input.linkUrl || '',
    startTime: input.startTime || '',
    endTime: input.endTime || '',
    remark: input.remark || '',
    auditStatus: 'DRAFT',
    auditOpinion: '',
    logs: [{ id: `log-${Date.now()}`, action: 'UPDATE_DRAFT', operatorName: '开发账号', opinion: '', operateTime: nowText() }, ...(current.logs || [])],
  }
  customerAds[index] = updated
  return updated
}

export const mockRequest = async <T>(path: string, options: MockRequestOptions = {}): Promise<T> => {
  await new Promise(resolve => setTimeout(resolve, 120))
  let data: unknown = null
  if (path.includes('/auth/') || path.endsWith('/me/session')) data = session
  else if (path.endsWith('/public/home')) data = { policies: homePolicies, activities: homeActivities, banners: publicAds }
  else if (path.includes('/public/ads/')) data = publicAds.find(item => String(item.id) === path.split('/').pop()) || publicAds[0]
  else if (path.endsWith('/public/ads')) data = publicAds
  else if (path.includes('/public/activities/')) data = homeActivities.find(item => String(item.id) === path.split('/').pop()) || homeActivities[0]
  else if (path.endsWith('/public/activities')) data = homeActivities
  else if (path.includes('/public/houses/')) data = houses.find(item => String(item.id) === path.split('/').pop()) || houses[0]
  else if (path.includes('/public/houses')) data = houses
  else if (path.endsWith('/public/property-services')) data = propertyServices.map((item, index) => ({ id: String(index + 1), title: item.title, type: item.key, desc: '开发环境物业服务' }))
  else if (path.includes('/public/value-services/')) data = valueServices.find(item => String(item.id) === path.split('/').pop()) || valueServices[0]
  else if (path.includes('/public/value-services')) data = valueServices.map(item => ({ ...item, category: item.title, desc: item.desc }))
  else if (path.endsWith('/customer/company')) data = { companyName: companyProfile.name, ...companyProfile }
  else if (path.endsWith('/customer/contracts')) data = contracts
  else if (path.endsWith('/customer/bills')) data = bills
  else if (path.includes('/customer/utility-bills/')) data = {
    id: 'utility-1', paymentId: 'payment-1', type: 'electric', room: 'A栋 101', periodStart: '2026-07-01', periodEnd: '2026-07-31',
    amount: 328.5, paidAmount: 0, remainingAmount: 328.5, dueDate: '2026-08-15', status: '0', previousReading: 1250,
    currentReading: 1578.5, usage: 328.5, unitPrice: 1, submissions: [],
  }
  else if (path.endsWith('/customer/utility-bills')) data = [{
    id: 'utility-1', paymentId: 'payment-1', type: 'electric', room: 'A栋 101', periodStart: '2026-07-01', periodEnd: '2026-07-31',
    amount: 328.5, paidAmount: 0, remainingAmount: 328.5, dueDate: '2026-08-15', status: '0',
  }]
  else if (path.includes('/customer/ads')) data = mockCustomerAds(path, options)
  else if (path.endsWith('/customer/work-orders')) data = workOrders
  else if (path.includes('/customer/work-orders/')) data = workOrders.find(item => path.includes(item.id)) || workOrders[0]
  else if (path.endsWith('/admin/notifications')) data = adminNotifications
  else if (path.includes('/admin/work-orders')) data = workOrders
  else if (path.endsWith('/admin/overview')) data = { metrics: overviewMetrics, progress: overviewProgress, todos: overviewTodos, roomSummary: {}, rentMetrics: {} }
  else if (path.endsWith('/admin/tenants')) data = overviewTenants
  else if (path.includes('/admin/tenants/')) data = { ...overviewTenants[0], contracts, bills }
  else data = { id: `mock-${Date.now()}`, status: '0' }
  return data as T
}
