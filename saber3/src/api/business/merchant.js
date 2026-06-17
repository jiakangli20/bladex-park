import request from '@/axios';

export const getMerchantPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/merchant/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getMerchantList = (params = {}) => {
  return request({
    url: '/blade-ics/merchant/list',
    method: 'get',
    params,
  });
};

export const getMerchantStatistics = (params = {}) => {
  return request({
    url: '/blade-ics/merchant/statistics',
    method: 'get',
    params,
  });
};

export const getMerchantDetail = merchantId => {
  return request({
    url: '/blade-ics/merchant/detail',
    method: 'get',
    params: {
      merchantId,
    },
  });
};

export const addMerchant = row => {
  return request({
    url: '/blade-ics/merchant/save',
    method: 'post',
    data: row,
  });
};

export const updateMerchant = row => {
  return request({
    url: '/blade-ics/merchant/update',
    method: 'post',
    data: row,
  });
};

export const submitMerchant = row => {
  return request({
    url: '/blade-ics/merchant/submit',
    method: 'post',
    data: row,
  });
};

export const removeMerchant = ids => {
  return request({
    url: '/blade-ics/merchant/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const changeMerchantStatus = (merchantId, status) => {
  return request({
    url: `/blade-ics/merchant/changeStatus/${merchantId}`,
    method: 'post',
    params: {
      status,
    },
  });
};
