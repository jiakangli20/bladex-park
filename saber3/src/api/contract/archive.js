import request from '@/axios';

export const getArchiveList = (current, size, params) => {
  return request({
    url: '/blade-contract/archive/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getArchiveDetail = contractId => {
  return request({
    url: `/blade-contract/archive/detail/${contractId}`,
    method: 'get',
  });
};

export const exportApproval = contractId => {
  return request({
    url: `/blade-contract/archive/export-approval/${contractId}`,
    method: 'get',
  });
};

export const printContract = contractId => {
  return request({
    url: `/blade-contract/archive/print/${contractId}`,
    method: 'get',
  });
};
