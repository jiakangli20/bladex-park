import request from '@/axios';

const prefix = '/blade-workflow/process/leave'

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

export const remove = (ids) => {
  return request({
    url: `${prefix}/remove`,
    method: 'post',
    params: {
      ids,
    }
  })
}

export const submit = (data) => {
  return request({
    url: `${prefix}/submit`,
    method: 'post',
    data
  })
}