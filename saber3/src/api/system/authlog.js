import request from '@/axios';

export const getAuthLogPage = (current, size, params) => {
  return request({
    url: '/blade-system/auth-log/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getAuthLogDetail = id => {
  return request({
    url: '/blade-system/auth-log/detail',
    method: 'get',
    params: { id },
  });
};
