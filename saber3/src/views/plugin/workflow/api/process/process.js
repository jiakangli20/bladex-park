import request from '@/axios';

const prefix = '/blade-workflow/process'

/**
 * 可发起流程列表
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
 * 待签列表
 */
export const claimList = (current, size, params) => {
  return request({
    url: `${prefix}/claimList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 待办列表
 */
export const todoList = (current, size, params) => {
  return request({
    url: `${prefix}/todoList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 我发起的请求
 */
export const sendList = (current, size, params) => {
  return request({
    url: `${prefix}/sendList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 办结列表
 */
export const doneList = (current, size, params) => {
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
 * 我的已办（流程不一定办结）
 */
export const myDoneList = (current, size, params) => {
  return request({
    url: `${prefix}/myDoneList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 抄送列表
 */
export const copyList = (current, size, params) => {
  return request({
    url: `${prefix}/copyList`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 根据流程id获取表单
 */
export const getFormByProcessId = (params) => {
  return request({
    url: `${prefix}/getFormByProcessId`,
    method: 'get',
    params
  })
}

/**
 * 根据流程定义key获取表单
 */
export const getFormByProcessDefKey = (params) => {
  return request({
    url: `${prefix}/getFormByProcessDefKey`,
    method: 'get',
    params
  })
}

/**
 * 流程详情
 */
export const detail = (params) => {
  return request({
    url: `${prefix}/detail`,
    method: 'get',
    params
  })
}

/**
 * 获取模型xml - 根据processDefId
 */
export const getXmlByProcessDefId = (params) => {
  return request({
    url: `${prefix}/getXmlByProcessDefId`,
    method: 'get',
    params
  })
}

/**
 * 获取模型xml - 根据processDefKey
 */
export const getXmlByProcessDefKey = (params) => {
  return request({
    url: `${prefix}/getXmlByProcessDefKey`,
    method: 'get',
    params
  })
}

/**
 * 发起流程
 */
export const startProcess = (data) => {
  return request({
    url: `${prefix}/startProcess`,
    method: 'post',
    data
  })
}

/**
 * 发起流程 - 根据流程定义key
 */
export const startProcessByKey = (data) => {
  return request({
    url: `${prefix}/startProcessByKey`,
    method: 'post',
    data
  })
}

/**
 * 任务审批
 */
export const completeTask = (data) => {
  return request({
    url: `${prefix}/completeTask`,
    method: 'post',
    data
  })
}

/**
 * 任务转办
 */
export const transferTask = (data) => {
  return request({
    url: `${prefix}/transferTask`,
    method: 'post',
    data
  })
}

/**
 * 任务委托
 */
export const delegateTask = (data) => {
  return request({
    url: `${prefix}/delegateTask`,
    method: 'post',
    data
  })
}

/**
 * 签收任务
 */
export const claimTask = (data) => {
  return request({
    url: `${prefix}/claimTask`,
    method: 'post',
    data
  })
}

/**
 * 获取可退回节点
 */
export const backNodes = (params) => {
  return request({
    url: `${prefix}/getBackNodes`,
    method: 'get',
    params
  })
}

/**
 * 退回到指定节点
 */
export const rollbackTask = (data) => {
  return request({
    url: `${prefix}/rollbackTask`,
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
 * 撤回/撤销
 */
export const withdrawTask = (data) => {
  return request({
    url: `${prefix}/withdrawTask`,
    method: 'post',
    data
  })
}

/**
 * 用户列表
 */
export const userList = (current, size, params) => {
  return request({
    url: `/api/blade-system/search/user`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 角色列表
 */
export const roleList = (params) => {
  return request({
    url: `/api/blade-system/search/role`,
    method: 'get',
    params
  })
}

/**
 * 部门列表
 */
export const deptList = (params) => {
  return request({
    url: `/api/blade-system/search/dept`,
    method: 'get',
    params
  })
}

/**
 * 岗位列表
 */
export const postList = (current, size, params) => {
  return request({
    url: `/api/blade-system/search/post`,
    method: 'get',
    params: {
      ...params,
      current,
      size,
    }
  })
}

/**
 * 根据流程定义key发起入口
 */
export const getStartFormByKey = processDefKey => {
  return request({
    url: `${prefix}/getFormByProcessDefKey`,
    method: 'get',
    params: {
      processDefKey,
    },
  })
}
