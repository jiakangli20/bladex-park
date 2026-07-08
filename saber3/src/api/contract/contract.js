import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/blade-contract/contract/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getDetail = contractId => {
  return request({
    url: '/blade-contract/contract/detail',
    method: 'get',
    params: {
      contractId,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/blade-contract/contract/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/blade-contract/contract/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/blade-contract/contract/submit',
    method: 'post',
    data: row,
  });
};

export const renew = (contractId, row) => {
  return request({
    url: '/blade-contract/contract/renew',
    method: 'post',
    params: {
      contractId,
    },
    data: row,
  });
};

export const terminate = contractId => {
  return request({
    url: '/blade-contract/contract/terminate',
    method: 'post',
    params: {
      contractId,
    },
  });
};

export const uploadSignedContract = (contractId, row) => {
  return request({
    url: '/blade-contract/contract/signed-file',
    method: 'post',
    params: {
      contractId,
    },
    data: row,
  });
};

export const getStats = params => {
  return request({
    url: '/blade-contract/contract/stats',
    method: 'get',
    params,
  });
};

export const getExpiring = (current, size, params) => {
  return request({
    url: '/blade-contract/contract/expiring',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getPayment = contractId => {
  return request({
    url: '/blade-contract/contract/payment',
    method: 'get',
    params: {
      contractId,
    },
  });
};

export const getDepositRefundPayment = contractId => {
  return request({
    url: '/blade-contract/contract/payment/deposit-refund',
    method: 'get',
    params: {
      contractId,
    },
  });
};

export const ensureDepositRefundPayment = contractId => {
  return request({
    url: '/blade-contract/contract/payment/deposit-refund',
    method: 'post',
    params: {
      contractId,
    },
  });
};

export const offlineRoomReview = (contractId, row) => {
  return request({
    url: '/blade-contract/contract/room-review/offline',
    method: 'post',
    params: {
      contractId,
    },
    data: row,
  });
};

export const offlineDepositRefund = (contractId, row) => {
  return request({
    url: '/blade-contract/contract/payment/deposit-refund/offline-confirm',
    method: 'post',
    params: {
      contractId,
    },
    data: row,
  });
};

export const getPaymentList = (current, size, params) => {
  return request({
    url: '/blade-contract/contract/payment/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const confirmPayment = (paymentId, row) => {
  return request({
    url: '/blade-contract/contract/payment/confirm',
    method: 'post',
    params: {
      paymentId,
    },
    data: row,
  });
};

export const remindPayment = paymentId => {
  return request({
    url: '/blade-contract/contract/payment/remind',
    method: 'post',
    params: {
      paymentId,
    },
  });
};

export const getLogs = contractId => {
  return request({
    url: '/blade-contract/contract/log',
    method: 'get',
    params: {
      contractId,
    },
  });
};

export const getWorkflowRecords = contractId => {
  return request({
    url: `/blade-contract/workflow-record/contract/${contractId}`,
    method: 'get',
  });
};

export const getLatestWorkflowRecord = (contractId, businessType) => {
  return request({
    url: '/blade-contract/workflow-record/latest',
    method: 'get',
    params: {
      contractId,
      businessType,
    },
  });
};

export const getWorkflowRecordPage = (current, size, params) => {
  return request({
    url: '/blade-contract/workflow-record/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const uploadWorkflowRecordAttachment = (recordId, row) => {
  return request({
    url: `/blade-contract/workflow-record/attachment/${recordId}`,
    method: 'post',
    data: row,
  });
};

export const removeWorkflowRecordAttachment = (recordId, params) => {
  return request({
    url: `/blade-contract/workflow-record/attachment/${recordId}`,
    method: 'delete',
    params,
  });
};
