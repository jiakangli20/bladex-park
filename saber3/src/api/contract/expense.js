import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-contract/expense/list',
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
    url: '/blade-contract/expense/detail',
    method: 'get',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/blade-contract/expense/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/blade-contract/expense/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/blade-contract/expense/submit',
    method: 'post',
    data: row,
  });
};

export const changeStatus = row => {
  return request({
    url: '/blade-contract/expense/status',
    method: 'post',
    data: row,
  });
};
