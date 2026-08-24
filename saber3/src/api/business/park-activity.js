import request from '@/axios';

export const getActivityPage = (current, size, params = {}) => request({ url: '/blade-ics/park-activity/page', method: 'get', params: { ...params, current, size } });
export const submitActivity = data => request({ url: '/blade-ics/park-activity/submit', method: 'post', data });
export const auditActivity = (id, status, opinion = '') => request({ url: `/blade-ics/park-activity/${id}/audit`, method: 'post', params: { status, opinion } });
export const publishActivity = (id, status) => request({ url: `/blade-ics/park-activity/${id}/publish`, method: 'post', params: { status } });
export const removeActivity = id => request({ url: '/blade-ics/park-activity/remove', method: 'post', params: { id } });
