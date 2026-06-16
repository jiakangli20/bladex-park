import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-ics/floor/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getSimpleList = params => {
  return request({
    url: '/blade-ics/floor/list',
    method: 'get',
    params,
  });
};

export const getDetail = (id, params = {}) => {
  return request({
    url: '/blade-ics/floor/detail',
    method: 'get',
    params: {
      ...params,
      id,
    },
  });
};

export const getStatistics = params => {
  return request({
    url: '/blade-ics/floor/statistics',
    method: 'get',
    params,
  });
};

export const submit = row => {
  return request({
    url: '/blade-ics/floor/submit',
    method: 'post',
    data: row,
  });
};

export const remove = ids => {
  return request({
    url: '/blade-ics/floor/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const syncBuilding = buildingId => {
  return request({
    url: `/blade-ics/floor/sync/${buildingId}`,
    method: 'post',
  });
};

export const syncAll = () => {
  return request({
    url: '/blade-ics/floor/sync-all',
    method: 'post',
  });
};
