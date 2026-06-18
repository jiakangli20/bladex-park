import request from '@/axios';

const prefix = '/blade-workflow/design/model/scope'

export const getList = (params) => {
  return request({
    url: `${prefix}/list`,
    method: 'get',
    params
  })
}

export const submit = (row) => {
  return request({
    url: `${prefix}/submit`,
    method: 'post',
    data: row
  })
}