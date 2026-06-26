import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-contract/expiring-rule/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const submit = row => {
  return request({
    url: '/blade-contract/expiring-rule/submit',
    method: 'post',
    data: row,
  });
};

export const remove = ids => {
  return request({
    url: '/blade-contract/expiring-rule/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};
