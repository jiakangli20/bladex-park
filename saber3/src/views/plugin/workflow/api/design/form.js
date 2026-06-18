import request from '@/axios';

const prefix = '/blade-workflow/design/form'

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

export const getDetail = (params) => {
  return request({
    url: `${prefix}/detail`,
    method: 'get',
    params
  })
}

export const remove = (data) => {
  return request({
    url: `${prefix}/remove`,
    method: 'post',
    data
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

export const listType = (params) => {
  return request({
    url: `${prefix}/listType`,
    method: 'get',
    params
  })
}

export const changeCategory = (row) => {
  return request({
    url: `${prefix}/changeCategory`,
    method: 'post',
    data: row
  })
}