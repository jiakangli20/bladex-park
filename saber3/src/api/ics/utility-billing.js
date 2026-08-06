import request from '@/axios';

export const previewUtilityBill = data =>
  request({
    url: '/blade-ics/utility-billing/preview',
    method: 'post',
    data,
  });

export const generateUtilityBill = data =>
  request({
    url: '/blade-ics/utility-billing/generate',
    method: 'post',
    data,
  });

export const publishUtilityBill = id =>
  request({
    url: `/blade-ics/utility-billing/${id}/publish`,
    method: 'post',
  });

export const getUtilityPaymentSubmissions = (current, size, params = {}) =>
  request({
    url: '/blade-ics/utility-billing/payment-submissions/page',
    method: 'get',
    params: { ...params, current, size },
  });

export const confirmUtilityPaymentSubmission = (id, data = {}) =>
  request({
    url: `/blade-ics/utility-billing/payment-submissions/${id}/confirm`,
    method: 'post',
    data,
  });

export const rejectUtilityPaymentSubmission = (id, data) =>
  request({
    url: `/blade-ics/utility-billing/payment-submissions/${id}/reject`,
    method: 'post',
    data,
  });
