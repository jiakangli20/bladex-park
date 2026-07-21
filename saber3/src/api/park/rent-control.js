import request from '@/axios';

export const getBoard = params => {
  return request({
    url: '/blade-park/rent-control/board',
    method: 'get',
    params,
  });
};

export const getWorkorders = params => {
  return request({
    url: '/blade-park/rent-control/workorders',
    method: 'get',
    params,
  });
};

export const reportWorkorder = data => {
  return request({
    url: '/blade-park/rent-control/workorders/report',
    method: 'post',
    data,
  });
};

export const getBuildingList = params => {
  return request({
    url: '/blade-park/building/list',
    method: 'get',
    params,
  });
};

export const getFloorList = params => {
  return request({
    url: '/blade-park/floor/list',
    method: 'get',
    params,
  });
};

export const getRoomList = params => {
  return request({
    url: '/blade-park/room/list',
    method: 'get',
    params,
  });
};

export const getRoomDetail = id => {
  return request({
    url: '/blade-park/room/detail',
    method: 'get',
    params: {
      id,
    },
  });
};

export const getRoomPaymentPage = (roomId, current = 1, size = 10) => {
  return request({
    url: '/blade-park/rent-control/room-payments',
    method: 'get',
    params: {
      roomId,
      current,
      size,
    },
  });
};

export const getRoomContractPage = (roomId, current = 1, size = 10) => {
  return request({
    url: '/blade-park/rent-control/room-contracts',
    method: 'get',
    params: { roomId, current, size },
  });
};

export const getRoomUtilityPage = (roomId, current = 1, size = 10) => request({
  url: '/blade-park/room-extension/utility/page',
  method: 'get',
  params: { roomId, current, size },
});

export const submitRoomUtility = data => request({
  url: '/blade-park/room-extension/utility/submit',
  method: 'post',
  data,
});

export const removeRoomUtility = recordId => request({
  url: '/blade-park/room-extension/utility/remove',
  method: 'post',
  params: { recordId },
});

export const getRoomVehiclePage = (roomId, current = 1, size = 10) => request({
  url: '/blade-park/room-extension/vehicle/page',
  method: 'get',
  params: { roomId, current, size },
});

export const submitRoomVehicle = data => request({
  url: '/blade-park/room-extension/vehicle/submit',
  method: 'post',
  data,
});

export const removeRoomVehicle = vehicleId => request({
  url: '/blade-park/room-extension/vehicle/remove',
  method: 'post',
  params: { vehicleId },
});

export const submitRoom = row => {
  return request({
    url: '/blade-park/room/submit',
    method: 'post',
    data: row,
  });
};

export const removeRoom = ids => {
  return request({
    url: '/blade-park/room/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const changeRoomStatus = (id, status) => {
  return request({
    url: '/blade-park/room/change-status',
    method: 'post',
    params: {
      id,
      status,
    },
  });
};

export const syncRoomMini = id => {
  return request({
    url: '/blade-park/room/sync-mini',
    method: 'post',
    params: {
      id,
    },
  });
};
