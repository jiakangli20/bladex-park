import request from '@/axios';

const prefix = '/blade-workflow/ops'

/**
 * 所有待办任务
 */
export const getList = (current, size, params) => {
  return request({
    url: `${prefix}/list`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 所有流程列表
 */
export const processList = (current, size, params) => {
  return request({
    url: `${prefix}/processList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 所有办结流程
 */
export const getDoneList = (current, size, params) => {
  return request({
    url: `${prefix}/doneList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 完成任务
 */
export const completeTask = (data) => {
  return request({
    url: `${prefix}/completeTask`,
    method: 'post',
    data
  })
}

/**
 * 变更任务状态
 */
export const changeTaskStatus = (data) => {
  return request({
    url: `${prefix}/changeTaskStatus`,
    method: 'post',
    data
  })
}

/**
 * 变更任务审核人
 */
export const changeTaskAssignee = (data) => {
  return request({
    url: `${prefix}/changeTaskAssignee`,
    method: 'post',
    data
  })
}

/**
 * 转办任务
 */
export const transferTask = (data) => {
  return request({
    url: `${prefix}/transferTask`,
    method: 'post',
    data
  })
}

/**
 * 委托任务
 */
export const delegateTask = (data) => {
  return request({
    url: `${prefix}/delegateTask`,
    method: 'post',
    data
  })
}

/**
 * 抄送任务
 */
export const copyTask = (data) => {
  return request({
    url: `${prefix}/copyTask`,
    method: 'post',
    data
  })
}

/**
 * 催办任务
 */
export const urgeTask = (data) => {
  return request({
    url: `${prefix}/urgeTask`,
    method: 'post',
    data
  })
}

/**
 * 终止流程
 */
export const terminateProcess = (data) => {
  return request({
    url: `${prefix}/terminateProcess`,
    method: 'post',
    data
  })
}

/**
 * 流程节点
 */
export const processNodes = (params) => {
  return request({
    url: `${prefix}/processNodes`,
    method: 'get',
    params
  })
}

/**
 * 指定回退
 */
export const rollbackTask = (data) => {
  return request({
    url: `${prefix}/rollbackTask`,
    method: 'post',
    data
  })
}

/**
 * 调度任务
 */
export const dispatchTask = (data) => {
  return request({
    url: `${prefix}/dispatchTask`,
    method: 'post',
    data
  })
}

/**
 * 加签
 */
export const addMultiInstance = (data) => {
  return request({
    url: `${prefix}/addMultiInstance`,
    method: 'post',
    data
  })
}

/**
 * 减签
 */
export const deleteMultiInstance = (data) => {
  return request({
    url: `${prefix}/deleteMultiInstance`,
    method: 'post',
    data
  })
}

/**
 * 删除流程
 */
export const deleteProcess = (data) => {
  return request({
    url: `${prefix}/deleteProcess`,
    method: 'post',
    data
  })
}