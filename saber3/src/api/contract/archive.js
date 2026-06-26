import request from '@/axios';
import { baseUrl } from '@/config/env';

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

export const contractApprovalPrintUrl = contractId => {
  return `${baseUrl}/blade-contract/print/contract-approval/${contractId}`;
};

export const contractTextPrintUrl = (contractId, rentMode = 'fixed') => {
  const mode = rentMode === 'floating' ? 'floating' : 'fixed';
  return `${baseUrl}/blade-contract/print/contract-text/${mode}/${contractId}`;
};

export const getWorkflowRecords = contractId => {
  return request({
    url: `/blade-contract/workflow-record/contract/${contractId}`,
    method: 'get',
  });
};

export const getSupplementAgreements = contractId => {
  return request({
    url: `/blade-contract/archive/supplement/list/${contractId}`,
    method: 'get',
  });
};

export const saveSupplementAgreement = data => {
  return request({
    url: '/blade-contract/archive/supplement/save',
    method: 'post',
    data,
  });
};

export const removeSupplementAgreement = agreementId => {
  return request({
    url: `/blade-contract/archive/supplement/remove/${agreementId}`,
    method: 'delete',
  });
};
