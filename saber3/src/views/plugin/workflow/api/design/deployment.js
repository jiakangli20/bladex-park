import request from '@/axios';

const prefix = '/blade-workflow/design/deployment'

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

export const remove = (data) => {
  return request({
    url: `${prefix}/remove`,
    method: 'post',
    data
  })
}

export const changeStatus = (data) => {
  return request({
    url: `${prefix}/changeStatus`,
    method: 'post',
    data
  })
}

export const changeCategory = (data) => {
  return request({
    url: `${prefix}/changeCategory`,
    method: 'post',
    data
  })
}