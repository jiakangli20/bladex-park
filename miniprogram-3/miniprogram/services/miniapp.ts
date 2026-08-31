import { request, createRequestId } from '../utils/request'
import { MiniSession } from '../utils/session'

export type JsonMap = Record<string, any>

export const authApi = {
  wechatLogin: (code: string, nickname = '') => request<MiniSession>('/blade-miniapp/auth/wechat-login', {
    method: 'POST', auth: false, data: { code, nickname },
  }),
  mockLogin: (data: JsonMap) => request<MiniSession>('/blade-miniapp/auth/mock-login', { method: 'POST', auth: false, data, real: true }),
  bind: (bindTicket: string, phoneCode: string, nickname = '') => request<MiniSession>('/blade-miniapp/auth/bind', {
    method: 'POST', auth: false, data: { bindTicket, phoneCode, nickname },
  }),
  session: () => request<MiniSession>('/blade-miniapp/me/session'),
  logout: () => request<void>('/blade-miniapp/auth/logout', { method: 'POST' }),
  enterpriseContext: () => request<JsonMap>('/blade-miniapp/enterprise/context'),
  switchEnterprise: (data: JsonMap) => request<void>('/blade-miniapp/enterprise/switch', { method: 'POST', data }),
  submitCertification: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/enterprise/certifications', data),
  certifications: () => request<JsonMap[]>('/blade-miniapp/enterprise/certifications'),
  submitParkApplication: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/enterprise/park-applications', data),
  submitJoin: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/enterprise/joins', data),
  joins: () => request<JsonMap[]>('/blade-miniapp/enterprise/joins'),
  createInvite: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/enterprise/invites', data),
  currentInvite: () => request<JsonMap>('/blade-miniapp/enterprise/invites/current'),
  resolveInvite: (code: string) => request<JsonMap>(`/blade-miniapp/enterprise/invites/resolve?code=${encodeURIComponent(code)}`),
  ownerJoins: (status = 'PENDING') => request<JsonMap[]>(`/blade-miniapp/enterprise/owner-joins?status=${encodeURIComponent(status)}`),
  pendingJoins: () => request<JsonMap[]>('/blade-miniapp/enterprise/pending-joins'),
  reviewJoin: (id: string, data: JsonMap) => request<void>(`/blade-miniapp/enterprise/joins/${id}/review`, { method: 'POST', data }),
}

export const publicApi = {
  home: () => request<JsonMap>('/blade-miniapp/public/home', { auth: false }),
  notices: () => request<JsonMap[]>('/blade-miniapp/public/notices', { auth: false }),
  notice: (id: string) => request<JsonMap>(`/blade-miniapp/public/notices/${id}`, { auth: false }),
	policies: () => request<JsonMap[]>('/blade-miniapp/public/policies', { auth: false }),
	policy: (id: string) => request<JsonMap>(`/blade-miniapp/public/policies/${id}`, { auth: false }),
	ads: () => request<JsonMap[]>('/blade-miniapp/public/ads', { auth: false }),
	ad: (id: string) => request<JsonMap>(`/blade-miniapp/public/ads/${id}`, { auth: false }),
	activities: () => request<JsonMap[]>('/blade-miniapp/public/activities', { auth: false }),
	activity: (id: string) => request<JsonMap>(`/blade-miniapp/public/activities/${id}`, { auth: false }),
  houses: (keyword = '') => request<JsonMap[]>(`/blade-miniapp/public/houses?keyword=${encodeURIComponent(keyword)}`, { auth: false, real: true }),
  house: (id: string) => request<JsonMap>(`/blade-miniapp/public/houses/${id}`, { auth: false, real: true }),
  propertyServices: () => request<JsonMap[]>('/blade-miniapp/public/property-services', { auth: false }),
  valueServices: (keyword = '') => request<JsonMap[]>(`/blade-miniapp/public/value-services?keyword=${encodeURIComponent(keyword)}`, { auth: false }),
  valueService: (id: string) => request<JsonMap>(`/blade-miniapp/public/value-services/${id}`, { auth: false }),
}

const postOnce = <T>(path: string, data: JsonMap) => request<T>(path, {
  method: 'POST', data, requestId: createRequestId(),
})

export const customerApi = {
  createAppointment: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/customer/appointments', data),
  appointments: () => request<JsonMap[]>('/blade-miniapp/customer/appointments'),
  cancelAppointment: (id: string, reason = '') => request<void>(`/blade-miniapp/customer/appointments/${id}/cancel?reason=${encodeURIComponent(reason)}`, { method: 'POST' }),
  createSettlement: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/customer/settlement-intentions', data),
  settlements: () => request<JsonMap[]>('/blade-miniapp/customer/settlement-intentions'),
  createPropertyOrder: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/customer/property-work-orders', data),
  createValueOrder: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/customer/value-service-orders', data),
  company: () => request<JsonMap>('/blade-miniapp/customer/company'),
  saveCompany: (data: JsonMap) => request<void>('/blade-miniapp/customer/company', { method: 'POST', data }),
  contracts: () => request<JsonMap[]>('/blade-miniapp/customer/contracts'),
  contract: (id: string) => request<JsonMap>(`/blade-miniapp/customer/contracts/${id}`),
  bills: () => request<JsonMap[]>('/blade-miniapp/customer/bills'),
  bill: (id: string) => request<JsonMap>(`/blade-miniapp/customer/bills/${id}`),
  utilityBills: () => request<JsonMap[]>('/blade-miniapp/customer/utility-bills'),
  utilityBill: (id: string) => request<JsonMap>(`/blade-miniapp/customer/utility-bills/${id}`),
  utilityBillSubmissions: (id: string) => request<JsonMap[]>(`/blade-miniapp/customer/utility-bills/${id}/submissions`),
  submitUtilityPayment: (id: string, data: JsonMap) => postOnce<JsonMap>(`/blade-miniapp/customer/utility-bills/${id}/submissions`, data),
  ads: () => request<JsonMap[]>('/blade-miniapp/customer/ads'),
  ad: (id: string) => request<JsonMap>(`/blade-miniapp/customer/ads/${id}`),
  createAd: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/customer/ads', data),
  updateAd: (id: string, data: JsonMap) => request<JsonMap>(`/blade-miniapp/customer/ads/${id}`, { method: 'POST', data }),
  submitAd: (id: string) => postOnce<void>(`/blade-miniapp/customer/ads/${id}/submit`, {}),
  withdrawAd: (id: string) => request<void>(`/blade-miniapp/customer/ads/${id}/withdraw`, { method: 'POST' }),
	activities: () => request<JsonMap[]>('/blade-miniapp/customer/activities'),
	activity: (id: string) => request<JsonMap>(`/blade-miniapp/customer/activities/${id}`),
	createActivity: (data: JsonMap) => postOnce<JsonMap>('/blade-miniapp/customer/activities', data),
	updateActivity: (id: string, data: JsonMap) => request<JsonMap>(`/blade-miniapp/customer/activities/${id}`, { method: 'POST', data }),
	submitActivity: (id: string) => postOnce<void>(`/blade-miniapp/customer/activities/${id}/submit`, {}),
	withdrawActivity: (id: string) => request<void>(`/blade-miniapp/customer/activities/${id}/withdraw`, { method: 'POST' }),
  workOrders: () => request<JsonMap[]>('/blade-miniapp/customer/work-orders'),
  workOrder: (type: string, id: string) => request<JsonMap>(`/blade-miniapp/customer/work-orders/${type}/${id}`),
  workOrderAction: (type: string, id: string, data: JsonMap) => request<void>(`/blade-miniapp/customer/work-orders/${type}/${id}/actions`, { method: 'POST', data }),
  members: () => request<JsonMap[]>('/blade-miniapp/customer/members'),
  disableMember: (id: string) => request<void>(`/blade-miniapp/customer/members/${id}/disable`, { method: 'POST' }),
}

export const adminApi = {
  notifications: () => request<JsonMap[]>('/blade-miniapp/admin/notifications'),
  readNotification: (id: string) => request<void>(`/blade-miniapp/admin/notifications/${id}/read`, { method: 'POST' }),
  workOrders: (type = '') => request<JsonMap[]>(`/blade-miniapp/admin/work-orders?type=${encodeURIComponent(type)}`),
  workOrder: (type: string, id: string) => request<JsonMap>(`/blade-miniapp/admin/work-orders/${type}/${id}`),
  action: (type: string, id: string, data: JsonMap) => request<void>(`/blade-miniapp/admin/work-orders/${type}/${id}/actions`, { method: 'POST', data }),
  overview: () => request<JsonMap>('/blade-miniapp/admin/overview'),
  tenants: (keyword = '') => request<JsonMap[]>(`/blade-miniapp/admin/tenants?keyword=${encodeURIComponent(keyword)}`),
  tenant: (id: string) => request<JsonMap>(`/blade-miniapp/admin/tenants/${id}`),
}
