import request from '@/axios';

export const getEnterpriseCertificationList = (params = {}) => request({ url: '/blade-park/enterprise-certification/list', method: 'get', params });
export const reviewEnterpriseCertification = (id, data) => request({ url: `/blade-park/enterprise-certification/${id}/review`, method: 'post', data });
