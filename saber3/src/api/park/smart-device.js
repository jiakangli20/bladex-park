import request from '@/axios';

export const getDevicePage = (current, size, params = {}) => {
  return request({
    url: '/blade-park/smart-device/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getDeviceDetail = deviceId => {
  return request({
    url: '/blade-park/smart-device/detail',
    method: 'get',
    params: { deviceId },
  });
};

export const getDeviceStatistics = (params = {}) => {
  return request({
    url: '/blade-park/smart-device/statistics',
    method: 'get',
    params,
  });
};

export const submitDevice = row => {
  return request({
    url: '/blade-park/smart-device/submit',
    method: 'post',
    data: row,
  });
};

export const removeDevice = ids => {
  return request({
    url: '/blade-park/smart-device/remove',
    method: 'post',
    params: { ids },
  });
};
