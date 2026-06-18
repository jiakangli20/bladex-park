import request from '@/axios';

const prefix = '/blade-workflow/statistics'

export const index = (params) => {
  return request({
    url: `${prefix}/index`,
    method: 'get',
    params
  })
}

export const bar = (params) => {
  return request({
    url: `${prefix}/bar`,
    method: 'get',
    params
  })
}