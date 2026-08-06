import { request, createRequestId } from '../utils/request'
import { MiniSession } from '../utils/session'

export type JsonMap = Record<string, any>

export const authApi = {
  wechatLogin: (code: string, nickname = '') => request<MiniSession>('/blade-miniapp/auth/wechat-login', {
    method: 'POST', auth: false, data: { code, nickname },
  }),
  bind: (bindTicket: string, phoneCode: string, inviteCode: string, nickname = '') => request<MiniSession>('/blade-miniapp/auth/bind', {
    method: 'POST', auth: false, data: { bindTicket, phoneCode, inviteCode, nickname },
  }),
  session: () => request<MiniSession>('/blade-miniapp/me/session'),
  logout: () => request<void>('/blade-miniapp/auth/logout', { method: 'POST' }),
}

export const publicApi = {
  home: () => request<JsonMap>('/blade-miniapp/public/home', { auth: false }),
  houses: (keyword = '') => request<JsonMap[]>(`/blade-miniapp/public/houses?keyword=${encodeURIComponent(keyword)}`, { auth: false }),
  house: (id: string) => request<JsonMap>(`/blade-miniapp/public/houses/${id}`, { auth: false }),
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
  workOrders: () => request<JsonMap[]>('/blade-miniapp/customer/work-orders'),
  workOrder: (type: string, id: string) => request<JsonMap>(`/blade-miniapp/customer/work-orders/${type}/${id}`),
  workOrderAction: (type: string, id: string, data: JsonMap) => request<void>(`/blade-miniapp/customer/work-orders/${type}/${id}/actions`, { method: 'POST', data }),
  members: () => request<JsonMap[]>('/blade-miniapp/customer/members'),
  disableMember: (id: string) => request<void>(`/blade-miniapp/customer/members/${id}/disable`, { method: 'POST' }),
  invites: () => request<JsonMap[]>('/blade-miniapp/customer/invites'),
  createInvite: (data: JsonMap) => request<JsonMap>('/blade-miniapp/customer/invites', { method: 'POST', data }),
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
