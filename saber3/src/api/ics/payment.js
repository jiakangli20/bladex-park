import request from '@/axios';

export const getPaymentPage = (current, size, params) => {
  return request({
    url: '/blade-ics/payment/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getOverduePaymentPage = (current, size, params) => {
  return request({
    url: '/blade-ics/payment/overdue-page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getPaymentDetail = paymentId => {
  return request({
    url: '/blade-ics/payment/detail',
    method: 'get',
    params: {
      paymentId,
    },
  });
};

export const getPaymentByContract = contractId => {
  return request({
    url: '/blade-ics/payment/by-contract',
    method: 'get',
    params: {
      contractId,
    },
  });
};

export const getPaymentSummary = params => {
  return request({
    url: '/blade-ics/payment/summary',
    method: 'get',
    params,
  });
};

export const confirmPayment = (paymentId, row) => {
  return request({
    url: '/blade-ics/payment/confirm',
    method: 'post',
    params: {
      paymentId,
    },
    data: row,
  });
};

export const remindPayment = paymentId => {
  return request({
    url: '/blade-ics/payment/remind',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const getPaymentLogs = contractId => {
  return request({
    url: '/blade-ics/payment/log-list',
    method: 'get',
    params: {
      contractId,
    },
  });
};

export const getPaymentNoticePlaceholder = () => {
  return request({
    url: '/blade-ics/payment/notice-placeholder',
    method: 'get',
  });
};
