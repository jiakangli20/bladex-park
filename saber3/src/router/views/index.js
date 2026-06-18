import Layout from '@/page/index/index.vue';
import Store from '@/store/';

export default [
  {
    path: '/ics/home',
    redirect: '/home',
  },
  {
    path: '/wel',
    component: () =>
      Store.getters.isMacOs ? import('@/mac/index.vue') : import('@/page/index/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'index',
        name: '首页',
        meta: {
          i18n: 'dashboard',
        },
        redirect: '/home',
      },
      {
        path: 'dashboard',
        name: '控制台',
        meta: {
          i18n: 'dashboard',
          menu: false,
        },
        component: () => import(/* webpackChunkName: "views" */ '@/views/wel/dashboard.vue'),
      },
    ],
  },
  {
    path: '/test',
    component: Layout,
    redirect: '/test/index',
    children: [
      {
        path: 'index',
        name: '测试页',
        meta: {
          i18n: 'test',
        },
        component: () => import(/* webpackChunkName: "views" */ '@/views/util/test.vue'),
      },
    ],
  },
  {
    path: '/dict-horizontal',
    component: Layout,
    redirect: '/dict-horizontal/index',
    children: [
      {
        path: 'index',
        name: '字典管理',
        meta: {
          i18n: 'dict',
        },
        component: () =>
          import(/* webpackChunkName: "views" */ '@/views/util/demo/dict-horizontal.vue'),
      },
    ],
  },
  {
    path: '/dict-vertical',
    component: Layout,
    redirect: '/dict-vertical/index',
    children: [
      {
        path: 'index',
        name: '字典管理',
        meta: {
          i18n: 'dict',
        },
        component: () =>
          import(/* webpackChunkName: "views" */ '@/views/util/demo/dict-vertical.vue'),
      },
    ],
  },
  {
    path: '/info',
    component: Layout,
    redirect: '/info/index',
    children: [
      {
        path: 'index',
        name: '个人信息',
        meta: {
          i18n: 'info',
        },
        component: () => import(/* webpackChunkName: "views" */ '@/views/system/userinfo.vue'),
      },
    ],
  },
  {
    path: '/work/process/leave',
    component: Layout,
    redirect: '/work/process/leave/form',
    children: [
      {
        path: 'form/:processDefinitionId',
        name: '请假流程',
        meta: {
          i18n: 'work',
        },
        component: () =>
          import(/* webpackChunkName: "views" */ '@/views/work/process/leave/form.vue'),
      },
      {
        path: 'handle/:taskId/:processInstanceId/:businessId',
        name: '处理请假流程',
        meta: {
          i18n: 'work',
        },
        component: () =>
          import(/* webpackChunkName: "views" */ '@/views/work/process/leave/handle.vue'),
      },
      {
        path: 'detail/:processInstanceId/:businessId',
        name: '请假流程详情',
        meta: {
          i18n: 'work',
        },
        component: () =>
          import(/* webpackChunkName: "views" */ '@/views/work/process/leave/detail.vue'),
      },
    ],
  },
  {
    path: '/plugin/workflow/pages/design/process',
    component: Layout,
    children: [
      {
        path: ':id',
        name: '流程设计器',
        meta: {
          menu: false,
        },
        component: () =>
          import(
            /* webpackChunkName: "views" */ '@/views/plugin/workflow/pages/design/index.vue'
          ),
      },
    ],
  },
  {
    path: '/plugin/workflow/pages/design/model-history',
    component: Layout,
    children: [
      {
        path: ':id',
        name: '模型历史',
        meta: {
          menu: false,
        },
        component: () =>
          import(
            /* webpackChunkName: "views" */ '@/views/plugin/workflow/pages/design/model-history.vue'
          ),
      },
    ],
  },
  {
    path: '/plugin/workflow/pages/design/form-history',
    component: Layout,
    children: [
      {
        path: ':id',
        name: '表单历史',
        meta: {
          menu: false,
        },
        component: () =>
          import(
            /* webpackChunkName: "views" */ '@/views/plugin/workflow/pages/design/form-history.vue'
          ),
      },
    ],
  },
  {
    path: '/plugin/workflow/pages/process/form',
    component: Layout,
    children: [
      {
        path: 'start/:params',
        name: '发起流程',
        meta: {
          menu: false,
        },
        component: () =>
          import(
            /* webpackChunkName: "views" */ '@/views/plugin/workflow/pages/process/form/start.vue'
          ),
      },
      {
        path: 'detail/:params',
        name: '流程详情',
        meta: {
          menu: false,
        },
        component: () =>
          import(
            /* webpackChunkName: "views" */ '@/views/plugin/workflow/pages/process/form/detail.vue'
          ),
      },
    ],
  },
];
