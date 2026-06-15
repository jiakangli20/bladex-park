import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-ics/building/page',
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
    url: '/blade-ics/building/list',
    method: 'get',
    params,
  });
};

export const getDetail = id => {
  return request({
    url: '/blade-ics/building/detail',
    method: 'get',
    params: {
      id,
    },
  });
};

export const submit = row => {
  return request({
    url: '/blade-ics/building/submit',
    method: 'post',
    data: row,
  });
};

export const remove = ids => {
  return request({
    url: '/blade-ics/building/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const importBuilding = data => {
  return request({
    url: '/blade-ics/building/import',
    method: 'post',
    data,
  });
};
