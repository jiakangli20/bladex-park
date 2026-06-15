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
