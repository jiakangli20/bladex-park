import request from '@/axios';

export const getWorkbench = () => {
  return request({
    url: '/blade-home/home/workbench',
    method: 'get',
  });
};

export const getMissingApis = () => {
  return request({
    url: '/blade-home/home/missing-apis',
    method: 'get',
  });
};
