import request from '@/axios';

const prefix = '/blade-workflow/design/category'

export const tree = () => {
  return request({
    url: `${prefix}/tree`,
    method: 'get',
  })
}

export const treeList = () => {
  return request({
    url: `${prefix}/allTree`,
    method: 'get',
  })
}

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