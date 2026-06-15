import request from '@/axios';

export const getTagList = (current, size, params) => {
  return request({
    url: '/blade-park/tag/list',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getAllTags = params => {
  return request({
    url: '/blade-park/tag/all',
    method: 'get',
    params,
  });
};

export const getTagDetail = tagId => {
  return request({
    url: '/blade-park/tag/detail',
    method: 'get',
    params: {
      tagId,
    },
  });
};

export const getTagsByType = (tagType, params = {}) => {
  return request({
    url: `/blade-park/tag/list-by-type/${tagType}`,
    method: 'get',
    params,
  });
};

export const getTagsByCustomer = customerId => {
  return request({
    url: `/blade-park/tag/list-by-customer/${customerId}`,
    method: 'get',
  });
};

export const getTagsByOpportunity = opportunityId => {
  return request({
    url: `/blade-park/tag/list-by-opportunity/${opportunityId}`,
    method: 'get',
  });
};

export const submitTag = row => {
  return request({
    url: '/blade-park/tag/submit',
    method: 'post',
    data: row,
  });
};

export const addTag = row => {
  return request({
    url: '/blade-park/tag/save',
    method: 'post',
    data: row,
  });
};

export const updateTag = row => {
  return request({
    url: '/blade-park/tag/update',
    method: 'post',
    data: row,
  });
};

export const removeTag = (ids, force = false) => {
  return request({
    url: '/blade-park/tag/remove',
    method: 'post',
    params: {
      ids,
      force,
    },
  });
};

export const setCustomerTags = (customerId, tagIds) => {
  return request({
    url: `/blade-park/tag/set-customer-tags/${customerId}`,
    method: 'post',
    data: tagIds,
  });
};

export const setOpportunityTags = (opportunityId, tagIds) => {
  return request({
    url: `/blade-park/tag/set-opportunity-tags/${opportunityId}`,
    method: 'post',
    data: tagIds,
  });
};

export const getTagTypeList = params => {
  return request({
    url: '/blade-park/tag-type/list',
    method: 'get',
    params,
  });
};

export const getTagTypeDetail = typeId => {
  return request({
    url: '/blade-park/tag-type/detail',
    method: 'get',
    params: {
      typeId,
    },
  });
};

export const submitTagType = row => {
  return request({
    url: '/blade-park/tag-type/submit',
    method: 'post',
    data: row,
  });
};

export const addTagType = row => {
  return request({
    url: '/blade-park/tag-type/save',
    method: 'post',
    data: row,
  });
};

export const updateTagType = row => {
  return request({
    url: '/blade-park/tag-type/update',
    method: 'post',
    data: row,
  });
};

export const removeTagType = typeId => {
  return request({
    url: `/blade-park/tag-type/remove/${typeId}`,
    method: 'post',
  });
};
