import request from '@/axios';

export const getApprovalProjectList = (current, size, params = {}) => {
  return request({
    url: '/blade-approval/project/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getApprovalProjectStatistics = (params = {}) => {
  return request({
    url: '/blade-approval/project/statistics',
    method: 'get',
    params,
  });
};

export const getApprovalProjectDetail = projectId => {
  return request({
    url: `/blade-approval/project/get/${projectId}`,
    method: 'get',
  });
};

export const getApprovalForm = projectId => {
  return request({
    url: `/blade-approval/project/form/${projectId}`,
    method: 'get',
  });
};

export const exportApprovalForm = projectId => {
  return request({
    url: `/blade-approval/project/form/export/${projectId}`,
    method: 'get',
  });
};

export const approveApprovalProject = (projectId, data = {}) => {
  return request({
    url: `/blade-approval/project/approve/${projectId}`,
    method: 'post',
    data,
  });
};

export const rejectApprovalProject = (projectId, data = {}) => {
  return request({
    url: `/blade-approval/project/reject/${projectId}`,
    method: 'post',
    data,
  });
};

export const transferApprovalProject = (projectId, data = {}) => {
  return request({
    url: `/blade-approval/project/transfer/${projectId}`,
    method: 'post',
    data,
  });
};

export const resubmitApprovalProject = (projectId, data = {}) => {
  return request({
    url: `/blade-approval/project/resubmit/${projectId}`,
    method: 'post',
    data,
  });
};

export const archiveApprovalProject = projectId => {
  return request({
    url: `/blade-approval/project/archive/${projectId}`,
    method: 'post',
  });
};

export const withdrawApprovalProject = projectId => {
  return request({
    url: `/blade-approval/project/withdraw/${projectId}`,
    method: 'post',
  });
};

export const getApprovalMaterialList = params => {
  return request({
    url: '/blade-approval/material/list',
    method: 'get',
    params,
  });
};

export const getApprovalLogList = params => {
  return request({
    url: '/blade-approval/log/list',
    method: 'get',
    params,
  });
};

export const getApprovalFlowList = (current, size, params = {}) => {
  return request({
    url: '/blade-approval/flow/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getApprovalFlowAll = (params = {}) => {
  return request({
    url: '/blade-approval/flow/all',
    method: 'get',
    params,
  });
};

export const getApprovalFlowDetail = flowId => {
  return request({
    url: `/blade-approval/flow/get/${flowId}`,
    method: 'get',
  });
};

export const saveApprovalFlow = data => {
  return request({
    url: '/blade-approval/flow/save',
    method: 'post',
    data,
  });
};

export const updateApprovalFlow = data => {
  return request({
    url: '/blade-approval/flow/update',
    method: 'post',
    data,
  });
};

export const removeApprovalFlow = ids => {
  return request({
    url: '/blade-approval/flow/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const getApprovalFlowNodes = flowId => {
  return request({
    url: `/blade-approval/flow/nodes/${flowId}`,
    method: 'get',
  });
};

export const saveApprovalFlowNodes = (flowId, data = []) => {
  return request({
    url: `/blade-approval/flow/nodes/${flowId}`,
    method: 'post',
    data,
  });
};

export const publishApprovalFlow = flowId => {
  return request({
    url: `/blade-approval/flow/publish/${flowId}`,
    method: 'post',
  });
};

export const copyApprovalFlow = flowId => {
  return request({
    url: `/blade-approval/flow/copy/${flowId}`,
    method: 'post',
  });
};
