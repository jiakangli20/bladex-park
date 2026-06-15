import request from '@/axios';

export const getAuthLockPage = (current, size, params) => {
  return request({
    url: '/blade-system/auth-lock/page',
    method: 'get',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getAuthLockDetail = id => {
  return request({
    url: '/blade-system/auth-lock/detail',
    method: 'get',
    params: { id },
  });
};

export const authLockUnlock = (id, unlockReason) => {
  return request({
    url: '/blade-system/auth-lock/unlock',
    method: 'post',
    params: {
      id,
      unlockReason,
    },
  });
};

export const lockUser = (userId, lockReason, lockBeginTime, lockEndTime) => {
  return request({
    url: '/blade-system/auth-lock/lock',
    method: 'post',
    params: {
      userId,
      lockReason,
      lockBeginTime,
      lockEndTime,
    },
  });
};
