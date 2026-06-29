import request from '@/axios';

export const getServiceOrderPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/merchant-service-order/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getServiceOrderList = (params = {}) => {
  return request({
    url: '/blade-ics/merchant-service-order/list',
    method: 'get',
    params,
  });
};

export const getServiceOrderStatistics = (params = {}) => {
  return request({
    url: '/blade-ics/merchant-service-order/statistics',
    method: 'get',
    params,
  });
};

export const getServiceOrderDetail = orderId => {
  return request({
    url: '/blade-ics/merchant-service-order/detail',
    method: 'get',
    params: {
      orderId,
    },
  });
};

export const submitServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/submit',
    method: 'post',
    data: row,
  });
};

export const addServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/save',
    method: 'post',
    data: row,
  });
};

export const updateServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/update',
    method: 'post',
    data: row,
  });
};

export const removeServiceOrder = ids => {
  return request({
    url: '/blade-ics/merchant-service-order/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const assignServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/assign',
    method: 'post',
    data: row,
  });
};

export const followServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/follow',
    method: 'post',
    data: row,
  });
};

export const dealServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/deal',
    method: 'post',
    data: row,
  });
};

export const closeServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/close',
    method: 'post',
    data: row,
  });
};

export const getServiceOrderLog = orderId => {
  return request({
    url: '/blade-ics/merchant-service-order/log-list',
    method: 'get',
    params: {
      orderId,
    },
  });
};

export const miniAppApplyServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/miniapp/apply',
    method: 'post',
    data: row,
  });
};

export const getMiniAppServiceOrderPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/merchant-service-order/miniapp/my-page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getMiniAppServiceOrderDetail = orderId => {
  return request({
    url: '/blade-ics/merchant-service-order/miniapp/detail',
    method: 'get',
    params: {
      orderId,
    },
  });
};

export const getMiniAppServiceOrderLog = orderId => {
  return request({
    url: '/blade-ics/merchant-service-order/miniapp/log-list',
    method: 'get',
    params: {
      orderId,
    },
  });
};

export const miniAppAdminFollowServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/miniapp/admin/follow',
    method: 'post',
    data: row,
  });
};

export const miniAppAdminDealServiceOrder = row => {
  return request({
    url: '/blade-ics/merchant-service-order/miniapp/admin/deal',
    method: 'post',
    data: row,
  });
};
