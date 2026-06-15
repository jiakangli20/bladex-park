import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-system/api-key-log/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getDetail = id => {
  return request({
    url: '/blade-system/api-key-log/detail',
    method: 'get',
    params: {
      id,
    },
  });
};
