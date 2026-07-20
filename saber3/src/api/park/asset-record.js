import request from '@/axios';

export const getAssetPage = (current, size, params = {}) => {
  return request({
    url: '/blade-park/asset-record/page',
    method: 'get',
    params: { ...params, current, size },
  });
};

export const getAssetDetail = assetId => {
  return request({
    url: '/blade-park/asset-record/detail',
    method: 'get',
    params: { assetId },
  });
};

export const submitAsset = row => {
  return request({
    url: '/blade-park/asset-record/submit',
    method: 'post',
    data: row,
  });
};

export const removeAsset = ids => {
  return request({
    url: '/blade-park/asset-record/remove',
    method: 'post',
    params: { ids },
  });
};
