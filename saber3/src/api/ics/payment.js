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

export const getOverdueReminderPage = (current, size, params) => {
  return request({
    url: '/blade-ics/payment/overdue-reminder-page',
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

export const createPayment = row => {
  return request({
    url: '/blade-ics/payment/create',
    method: 'post',
    data: row,
  });
};

export const getPaymentContractOptions = keyword => {
  return request({
    url: '/blade-ics/payment/contract-options',
    method: 'get',
    params: {
      keyword,
    },
  });
};

export const getOverdueReminderSummary = params => {
  return request({
    url: '/blade-ics/payment/overdue-reminder-summary',
    method: 'get',
    params,
  });
};

export const getOverdueDisposalDetail = paymentId => {
  return request({
    url: '/blade-ics/payment/overdue-disposal-detail',
    method: 'get',
    params: {
      paymentId,
    },
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

export const updatePaymentDeadline = (paymentId, payDeadline) => {
  return request({
    url: '/blade-ics/payment/deadline',
    method: 'post',
    params: {
      paymentId,
      payDeadline,
    },
  });
};

export const updatePaymentAttachment = (paymentId, row) => {
  return request({
    url: '/blade-ics/payment/attachment',
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

export const remindOverduePayment = paymentId => {
  return request({
    url: '/blade-ics/payment/overdue-reminder-remind',
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

export const getOverduePaymentLogs = contractId => {
  return request({
    url: '/blade-ics/payment/overdue-log-list',
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

export const getPaymentNoticePage = (current, size, params) => {
  return request({
    url: '/blade-ics/payment/notice-page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getPaymentNoticeSummary = params => {
  return request({
    url: '/blade-ics/payment/notice-summary',
    method: 'get',
    params,
  });
};

export const getPaymentNoticeBuildings = params => {
  return request({
    url: '/blade-ics/payment/notice-buildings',
    method: 'get',
    params,
  });
};

export const resendPaymentNotice = paymentId => {
  return request({
    url: '/blade-ics/payment/notice-resend',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const generatePaymentNotice = paymentId => {
  return request({
    url: '/blade-ics/payment/notice-generate',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const sendPaymentNoticeMiniApp = paymentId => {
  return request({
    url: '/blade-ics/payment/notice-miniapp-send',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const sendPaymentNoticeSms = paymentId => {
  return request({
    url: '/blade-ics/payment/notice-sms-send',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const sendPaymentNoticeEmail = paymentId => {
  return request({
    url: '/blade-ics/payment/notice-email-send',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const generateContractNotice = params => {
  return request({
    url: '/blade-contract/print/generate',
    method: 'post',
    params,
  });
};

export const sendMiniAppNotice = params => {
  return request({
    url: '/blade-contract/print/miniapp/send',
    method: 'post',
    params,
  });
};
