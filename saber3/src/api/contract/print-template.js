import request from '@/axios';

export const getPrintTemplateList = () => request({
  url: '/blade-contract/print-template/list',
  method: 'get',
});

export const uploadPrintTemplate = data => request({
  url: '/blade-contract/print-template/upload',
  method: 'post',
  data,
  headers: { 'Content-Type': 'multipart/form-data' },
});

export const enablePrintTemplate = templateId => request({
  url: `/blade-contract/print-template/enable/${templateId}`,
  method: 'post',
});

export const removePrintTemplate = templateId => request({
  url: `/blade-contract/print-template/remove/${templateId}`,
  method: 'post',
});
