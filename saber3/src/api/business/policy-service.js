import request from '@/axios';

export const getPolicyPage = (current, size, params = {}) => {
  return request({
    url: '/blade-ics/policy-service/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getPolicyList = (params = {}) => {
  return request({
    url: '/blade-ics/policy-service/list',
    method: 'get',
    params,
  });
};

export const getPolicyStatistics = (params = {}) => {
  return request({
    url: '/blade-ics/policy-service/statistics',
    method: 'get',
    params,
  });
};

export const getPolicyDetail = policyId => {
  return request({
    url: '/blade-ics/policy-service/detail',
    method: 'get',
    params: {
      policyId,
    },
  });
};

export const addPolicy = row => {
  return request({
    url: '/blade-ics/policy-service/save',
    method: 'post',
    data: row,
  });
};

export const updatePolicy = row => {
  return request({
    url: '/blade-ics/policy-service/update',
    method: 'post',
    data: row,
  });
};

export const submitPolicy = row => {
  return request({
    url: '/blade-ics/policy-service/submit',
    method: 'post',
    data: row,
  });
};

export const removePolicy = ids => {
  return request({
    url: '/blade-ics/policy-service/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const changePolicyOnline = (policyId, onlineFlag) => {
  return request({
    url: `/blade-ics/policy-service/changeOnline/${policyId}`,
    method: 'post',
    params: {
      onlineFlag,
    },
  });
};

export const copyPolicy = policyId => {
  return request({
    url: `/blade-ics/policy-service/copy/${policyId}`,
    method: 'post',
  });
};

export const getPolicySubmitRecord = policyId => {
  return request({
    url: '/blade-ics/policy-service/submit-record',
    method: 'get',
    params: {
      policyId,
    },
  });
};

export const getMiniAppPolicyList = (params = {}) => {
  return request({
    url: '/blade-ics/policy-service/miniapp/list',
    method: 'get',
    params,
  });
};

export const getMiniAppPolicyDetail = policyId => {
  return request({
    url: '/blade-ics/policy-service/miniapp/detail',
    method: 'get',
    params: {
      policyId,
    },
  });
};
