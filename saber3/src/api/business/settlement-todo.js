import request from '@/axios';

export const getList = (current, size, params = {}) => {
  return request({
    url: '/blade-park/settlement-todo/list',
    method: 'get',
    params: { current, size, ...params },
  });
};

export const getDetail = id => {
  return request({
    url: '/blade-park/settlement-todo/detail',
    method: 'get',
    params: { id },
  });
};

export const getStatistics = (params = {}) => {
  return request({
    url: '/blade-park/settlement-todo/statistics',
    method: 'get',
    params,
  });
};

export const processTodo = (id, data) => {
  return request({
    url: `/blade-park/settlement-todo/${id}/actions`,
    method: 'post',
    data,
  });
};
