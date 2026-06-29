import request from '@/axios';

export const getAdPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/merchant-ad/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getAdList = (params = {}) => {
  return request({
    url: '/blade-ics/merchant-ad/list',
    method: 'get',
    params,
  });
};

export const getAdStatistics = (params = {}) => {
  return request({
    url: '/blade-ics/merchant-ad/statistics',
    method: 'get',
    params,
  });
};

export const getAdDetail = adId => {
  return request({
    url: '/blade-ics/merchant-ad/detail',
    method: 'get',
    params: {
      adId,
    },
  });
};

export const addAd = row => {
  return request({
    url: '/blade-ics/merchant-ad/save',
    method: 'post',
    data: row,
  });
};

export const updateAd = row => {
  return request({
    url: '/blade-ics/merchant-ad/update',
    method: 'post',
    data: row,
  });
};

export const submitAd = row => {
  return request({
    url: '/blade-ics/merchant-ad/submit',
    method: 'post',
    data: row,
  });
};

export const removeAd = ids => {
  return request({
    url: '/blade-ics/merchant-ad/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const changeAdStatus = (adId, status) => {
  return request({
    url: `/blade-ics/merchant-ad/changeStatus/${adId}`,
    method: 'post',
    params: {
      status,
    },
  });
};

export const getMiniAppAdList = (params = {}) => {
  return request({
    url: '/blade-ics/merchant-ad/miniapp/list',
    method: 'get',
    params,
  });
};

export const getMiniAppAdDetail = adId => {
  return request({
    url: '/blade-ics/merchant-ad/miniapp/detail',
    method: 'get',
    params: {
      adId,
    },
  });
};
