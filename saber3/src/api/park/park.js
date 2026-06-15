import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-park/park/list',
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
    url: '/blade-park/park/detail',
    method: 'get',
    params: {
      id,
    },
  });
};

export const getStatistics = params => {
  return request({
    url: '/blade-park/park/statistics',
    method: 'get',
    params,
  });
};

export const remove = ids => {
  return request({
    url: '/blade-park/park/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/blade-park/park/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/blade-park/park/submit',
    method: 'post',
    data: row,
  });
};
