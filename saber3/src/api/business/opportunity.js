import request from '@/axios';

export const getOpportunityList = (current, size, params = {}) => {
  return request({
    url: '/blade-park/opportunity/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getOpportunityStatistics = () => {
  return request({
    url: '/blade-park/opportunity/statistics',
    method: 'get',
  });
};

export const getOpportunityDetail = opportunityId => {
  return request({
    url: '/blade-park/opportunity/detail',
    method: 'get',
    params: {
      opportunityId,
    },
  });
};

export const addOpportunity = row => {
  return request({
    url: '/blade-park/opportunity/save',
    method: 'post',
    data: row,
  });
};

export const updateOpportunity = row => {
  return request({
    url: '/blade-park/opportunity/update',
    method: 'post',
    data: row,
  });
};

export const submitOpportunity = row => {
  return request({
    url: '/blade-park/opportunity/submit',
    method: 'post',
    data: row,
  });
};

export const removeOpportunity = ids => {
  return request({
    url: '/blade-park/opportunity/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const addOpportunityFollow = (opportunityId, row) => {
  return request({
    url: `/blade-park/opportunity/follow/add/${opportunityId}`,
    method: 'post',
    data: row,
  });
};

export const getOpportunityFollowList = opportunityId => {
  return request({
    url: `/blade-park/opportunity/follow/list/${opportunityId}`,
    method: 'get',
  });
};

export const uploadOpportunityFile = data => {
  return request({
    url: '/blade-park/opportunity/file/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const getOpportunityFileList = opportunityId => {
  return request({
    url: `/blade-park/opportunity/file/list/${opportunityId}`,
    method: 'get',
  });
};

export const getOpportunityTags = opportunityId => {
  return request({
    url: `/blade-park/opportunity/tag/list/${opportunityId}`,
    method: 'get',
  });
};

export const setOpportunityTags = (opportunityId, tagIds) => {
  return request({
    url: `/blade-park/opportunity/tag/set/${opportunityId}`,
    method: 'post',
    data: tagIds,
  });
};

export const getOpportunityBackground = opportunityId => {
  return request({
    url: `/blade-park/opportunity/background/${opportunityId}`,
    method: 'get',
  });
};

export const getOpportunityBackgroundByName = enterpriseName => {
  return request({
    url: '/blade-park/opportunity/background/byName',
    method: 'get',
    params: {
      enterpriseName,
    },
  });
};

export const submitOpportunityAudit = (opportunityId, flowId) => {
  return request({
    url: `/blade-park/opportunity/submitAudit/${opportunityId}`,
    method: 'post',
    params: flowId ? { flowId } : {},
  });
};

export const exportTenantEntryApprovalForm = (opportunityId, processInsId) => {
  return request({
    url: `/blade-park/opportunity/tenant-entry/approval-form/${opportunityId}`,
    method: 'get',
    params: processInsId ? { processInsId } : {},
    responseType: 'blob',
  });
};

export const previewTenantEntryApprovalForm = (opportunityId, processInsId) => {
  return request({
    url: `/blade-park/opportunity/tenant-entry/approval-form-preview/${opportunityId}`,
    method: 'get',
    params: processInsId ? { processInsId } : {},
  });
};
