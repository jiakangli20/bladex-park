import request from '@/axios';

export const getCustomerList = (current, size, params = {}) => {
  return request({
    url: '/blade-park/customer/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getCustomerStatistics = (params = {}) => {
  return request({
    url: '/blade-park/customer/statistics',
    method: 'get',
    params,
  });
};

export const getCustomerDetail = customerId => {
  return request({
    url: '/blade-park/customer/detail',
    method: 'get',
    params: {
      customerId,
    },
  });
};

export const addCustomer = row => {
  return request({
    url: '/blade-park/customer/save',
    method: 'post',
    data: row,
  });
};

export const updateCustomer = row => {
  return request({
    url: '/blade-park/customer/update',
    method: 'post',
    data: row,
  });
};

export const submitCustomer = row => {
  return request({
    url: '/blade-park/customer/submit',
    method: 'post',
    data: row,
  });
};

export const removeCustomer = ids => {
  return request({
    url: '/blade-park/customer/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const changeCustomerStatus = (customerId, status) => {
  return request({
    url: `/blade-park/customer/changeStatus/${customerId}`,
    method: 'post',
    params: {
      status,
    },
  });
};

export const checkCustomer = customerId => {
  return request({
    url: `/blade-park/customer/check/${customerId}`,
    method: 'post',
  });
};

export const setCustomerTags = (customerId, tagIds) => {
  return request({
    url: `/blade-park/customer/tag/set/${customerId}`,
    method: 'post',
    data: tagIds,
  });
};
