import request from '@/axios';

export const listEnterpriseReports = () =>
  request({
    url: '/blade-ai/report/list',
    method: 'get',
  });

export const getEnterpriseReport = reportId =>
  request({
    url: '/blade-ai/report/detail',
    method: 'get',
    params: { reportId },
  });

export const generateEnterpriseReport = data =>
  request({
    url: '/blade-ai/report/generate',
    method: 'post',
    data,
  });

export const deleteEnterpriseReport = reportId =>
  request({
    url: '/blade-ai/report/remove',
    method: 'post',
    params: { reportId },
  });
