import request from '@/axios';

export const getEnterpriseDataOverview = (params = {}) => {
  return request({
    url: '/blade-ics/enterprise-data/overview',
    method: 'get',
    params,
  });
};
