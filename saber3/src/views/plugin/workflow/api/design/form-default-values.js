import request from '@/axios';

const prefix = '/blade-workflow/design/form/default-values'

export const getList = (current, size, params) => {
  return request({
    url: `${prefix}/list`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const listType = (params) => {
  return request({
    url: `${prefix}/listType`,
    method: 'get',
    params
  })
}

export const getDetail = (id) => {
  return request({
    url: `${prefix}/detail`,
    method: 'get',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: `${prefix}/remove`,
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: `${prefix}/submit`,
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: `${prefix}/submit`,
    method: 'post',
    data: row
  })
}