import request from '@/axios';

const prefix = '/blade-workflow/design/model'

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

export const getDetail = (id) => {
  return request({
    url: `${prefix}/detail`,
    method: 'get',
    params: {
      id
    }
  })
}

export const remove = (data) => {
  return request({
    url: `${prefix}/remove`,
    method: 'post',
    data
  })
}

export const submit = (row) => {
  return request({
    url: `${prefix}/submit`,
    method: 'post',
    data: row
  })
}

export const deploy = (row) => {
  return request({
    url: `${prefix}/deploy`,
    method: 'post',
    data: row
  })
}

export const changeCategory = (row) => {
  return request({
    url: `${prefix}/changeCategory`,
    method: 'post',
    data: row
  })
}

export const changeIcon = (row) => {
  return request({
    url: `${prefix}/changeIcon`,
    method: 'post',
    data: row
  })
}