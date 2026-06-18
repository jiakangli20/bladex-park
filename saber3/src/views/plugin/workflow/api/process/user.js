import request from '@/axios';

const prefix = '/blade-workflow/process/user'

export const getUser = (id) => {
  return request({
    url: `${prefix}/detail`,
    method: 'get',
    params: {
      id
    }
  })
}
