import request from '@/axios';

export const getServicePage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/property-service/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getServiceList = (params = {}) => {
  return request({
    url: '/blade-ics/property-service/list',
    method: 'get',
    params,
  });
};

export const getServiceDetail = serviceId => {
  return request({
    url: '/blade-ics/property-service/detail',
    method: 'get',
    params: {
      serviceId,
    },
  });
};

export const getMiniAppServiceList = (params = {}) => {
  return request({
    url: '/blade-ics/property-service/miniapp/list',
    method: 'get',
    params,
  });
};

export const getMiniAppServiceDetail = serviceId => {
  return request({
    url: '/blade-ics/property-service/miniapp/detail',
    method: 'get',
    params: {
      serviceId,
    },
  });
};

export const submitService = row => {
  return request({
    url: '/blade-ics/property-service/submit',
    method: 'post',
    data: row,
  });
};

export const addService = row => {
  return request({
    url: '/blade-ics/property-service/save',
    method: 'post',
    data: row,
  });
};

export const updateService = row => {
  return request({
    url: '/blade-ics/property-service/update',
    method: 'post',
    data: row,
  });
};

export const removeService = ids => {
  return request({
    url: '/blade-ics/property-service/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const getWorkorderPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/property-workorder/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getWorkorderStatistics = (params = {}) => {
  return request({
    url: '/blade-ics/property-workorder/statistics',
    method: 'get',
    params,
  });
};

export const getWorkorderDetail = orderId => {
  return request({
    url: '/blade-ics/property-workorder/detail',
    method: 'get',
    params: {
      orderId,
    },
  });
};

export const submitWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/submit',
    method: 'post',
    data: row,
  });
};

export const updateWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/update',
    method: 'post',
    data: row,
  });
};

export const removeWorkorder = ids => {
  return request({
    url: '/blade-ics/property-workorder/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const assignWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/assign',
    method: 'post',
    data: row,
  });
};

export const finishWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/finish',
    method: 'post',
    data: row,
  });
};

export const closeWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/close',
    method: 'post',
    data: row,
  });
};

export const rateWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/rate',
    method: 'post',
    data: row,
  });
};

export const getWorkorderLog = orderId => {
  return request({
    url: '/blade-ics/property-workorder/log-list',
    method: 'get',
    params: {
      orderId,
    },
  });
};

export const miniAppApplyWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/miniapp/apply',
    method: 'post',
    data: row,
  });
};

export const getMiniAppWorkorderPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/property-workorder/miniapp/my-page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getMiniAppAdminWorkorderPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/property-workorder/miniapp/admin/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const miniAppAdminHandleWorkorder = row => {
  return request({
    url: '/blade-ics/property-workorder/miniapp/admin/handle',
    method: 'post',
    data: row,
  });
};
