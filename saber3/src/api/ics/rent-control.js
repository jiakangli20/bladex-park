import request from '@/axios';

export const getBoard = params => {
  return request({
    url: '/blade-ics/rent-control/board',
    method: 'get',
    params,
  });
};

export const getWorkorders = params => {
  return request({
    url: '/blade-ics/rent-control/workorders',
    method: 'get',
    params,
  });
};

export const reportWorkorder = data => {
  return request({
    url: '/blade-ics/rent-control/workorders/report',
    method: 'post',
    data,
  });
};

export const getBuildingList = params => {
  return request({
    url: '/blade-ics/building/list',
    method: 'get',
    params,
  });
};

export const getFloorList = params => {
  return request({
    url: '/blade-ics/floor/list',
    method: 'get',
    params,
  });
};

export const getRoomList = params => {
  return request({
    url: '/blade-ics/room/list',
    method: 'get',
    params,
  });
};

export const getRoomDetail = id => {
  return request({
    url: '/blade-ics/room/detail',
    method: 'get',
    params: {
      id,
    },
  });
};

export const submitRoom = row => {
  return request({
    url: '/blade-ics/room/submit',
    method: 'post',
    data: row,
  });
};

export const removeRoom = ids => {
  return request({
    url: '/blade-ics/room/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const changeRoomStatus = (id, status) => {
  return request({
    url: '/blade-ics/room/change-status',
    method: 'post',
    params: {
      id,
      status,
    },
  });
};

export const syncRoomMini = id => {
  return request({
    url: '/blade-ics/room/sync-mini',
    method: 'post',
    params: {
      id,
    },
  });
};
