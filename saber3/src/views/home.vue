<template>
  <basic-container>
    <div v-loading="loading" class="home-workbench">
      <section class="hero-section">
        <div class="hero-copy">
          <span class="hero-eyebrow">智慧园区运营工作台</span>
          <h1>{{ banner.name }}</h1>
          <p>{{ banner.bannerDesc }}</p>
        </div>
        <div class="hero-actions">
          <el-button type="primary" icon="el-icon-guide" @click="go('/settlement/opportunity')">商机管理</el-button>
          <el-button icon="el-icon-s-check" @click="go('/approval/todo')">待办任务</el-button>
        </div>
      </section>

      <section class="metric-grid">
        <button v-for="item in metrics" :key="item.label" type="button" class="metric-card" @click="go(item.path)">
          <span class="metric-icon" :class="item.tone">{{ item.icon }}</span>
          <span class="metric-title">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.desc }}</em>
        </button>
      </section>

      <section class="content-grid">
        <div class="main-column">
          <section class="panel">
            <div class="panel-head">
              <h2>招商闭环</h2>
              <span>第一阶段仅保留入口</span>
            </div>
            <div class="entrance-grid">
              <button v-for="item in lifecycleEntrances" :key="item.title" type="button" class="entrance-item" @click="go(item.path)">
                <span>{{ item.icon }}</span>
                <strong>{{ item.title }}</strong>
                <em>{{ item.desc }}</em>
              </button>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>快捷入口</h2>
              <span>已迁模块可直接跳转</span>
            </div>
            <div class="shortcut-grid">
              <button
                v-for="item in shortcuts"
                :key="item.title"
                type="button"
                class="shortcut-item"
                :class="{ disabled: item.disabled }"
                @click="go(item.path, item.disabled)"
              >
                <span>{{ item.icon }}</span>
                <strong>{{ item.title }}</strong>
              </button>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>入驻企业展示</h2>
              <span>{{ enterprises.length ? '来自客户档案数据' : '暂无入驻企业数据' }}</span>
            </div>
            <div class="enterprise-list">
              <div v-for="item in enterprises" :key="item.customerId || item.name" class="enterprise-item">
                <span>{{ item.shortName }}</span>
                <div>
                  <strong>{{ item.name }}</strong>
                  <em>{{ item.industry }}</em>
                </div>
              </div>
            </div>
          </section>
        </div>

        <aside class="side-column">
          <section class="panel">
            <div class="panel-head">
              <h2>待办提醒</h2>
              <el-button text type="primary" @click="go('/approval/todo')">更多</el-button>
            </div>
            <div class="todo-list">
              <button v-for="item in todos" :key="item.title" type="button" class="todo-item" @click="go(item.path)">
                <span>{{ item.count }}</span>
                <div>
                  <strong>{{ item.title }}</strong>
                  <em>{{ item.desc }}</em>
                </div>
              </button>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>园区政策通知</h2>
              <el-button text type="primary" @click="go('/enterprise/policy-service')">查看</el-button>
            </div>
            <div class="policy-list">
              <button v-for="item in policies" :key="item.title" type="button" class="policy-item" @click="go(item.path)">
                <strong>{{ item.title }}</strong>
                <span>{{ item.date }}</span>
              </button>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <h2>日程安排</h2>
              <span>本期不新增日程表</span>
            </div>
            <div class="schedule-card">
              <strong>{{ today.day }}</strong>
              <span>{{ today.month }}</span>
              <em>暂无日程安排</em>
            </div>
          </section>
        </aside>
      </section>
    </div>
  </basic-container>
</template>

<script>
import { ElMessage } from 'element-plus';
import { getWorkbench } from '@/api/home/home';

export default {
  name: 'Home',
  data() {
    return {
      loading: false,
      banner: {
        name: '首页',
        bannerDesc: '聚合房源、客户、合同、审批、物业工单和政策通知，后续将逐步接入真实业务数据。',
        imageUrl: '/img/bg/bg1.jpg',
      },
      metrics: [
        { label: '楼层管理', value: 0, desc: '房源与楼层概览', path: '/park/floor', icon: '楼', tone: 'blue' },
        { label: '客户管理', value: 0, desc: '入驻客户总览', path: '/settlement/customer', icon: '客', tone: 'green' },
        { label: '合同即将到期', value: 0, desc: '待关注合同', path: '/contract/expiring', icon: '合', tone: 'amber' },
        { label: '审批待处理', value: 0, desc: '当前待办审批', path: '/approval/todo', icon: '审', tone: 'purple' },
        { label: '待办任务', value: 0, desc: '物业工单待受理', path: '/enterprise/property-workorder', icon: '单', tone: 'red' },
      ],
      lifecycleEntrances: [
        { title: '租金收缴', desc: '账单、确认缴费、催缴闭环', path: '/finance/payment', icon: '收' },
        { title: '企业风险', desc: '跳转客户管理并保留风险筛选入口', path: '/settlement/customer?riskLevel=3', icon: '险' },
        { title: '项目审核', desc: '入驻服务项目审核', path: '/settlement/project-approval', icon: '审' },
        { title: '客户管理', desc: '入驻客户档案', path: '/settlement/customer', icon: '客' },
      ],
      shortcuts: [
        { title: '新增客户', path: '/settlement/customer', icon: '增' },
        { title: '楼层管理', path: '/park/floor', icon: '楼' },
        { title: '新建合同', path: '/contract/archive', icon: '合' },
        { title: '我的审批', path: '/approval/my-flow', icon: '审' },
        { title: '物业工单', path: '/enterprise/property-workorder', icon: '单' },
        { title: '租控管理', path: '/park/rent-control', icon: '控' },
      ],
      enterprises: [
        { shortName: '企', name: '入驻企业占位', industry: '等待客户数据接入' },
        { shortName: '园', name: '园区企业画像', industry: '后续接入聚合接口' },
        { shortName: '数', name: '在园企业数据', industry: '跳转企业服务入口' },
      ],
      todos: [
        { title: '审批待处理', count: 0, desc: '暂无待处理审批', path: '/approval/todo' },
        { title: '合同到期提醒', count: 0, desc: '暂无即将到期合同', path: '/contract/expiring' },
        { title: '物业工单提醒', count: 0, desc: '暂无待受理工单', path: '/enterprise/property-workorder' },
        { title: '审批超时提醒', count: 0, desc: '暂无超时审批', path: '/approval/todo?timeout=1' },
      ],
      policies: [
        { title: '园区政策通知入口已预留', date: '待接入', path: '/enterprise/policy-service' },
        { title: '工作台 Banner 后续接入', date: '待接入', path: '/enterprise/policy-service' },
        { title: '政策服务模块入口', date: '待接入', path: '/enterprise/policy-service' },
      ],
      today: this.buildToday(),
      missingApis: [],
    };
  },
  created() {
    this.loadWorkbench();
  },
  methods: {
    loadWorkbench() {
      this.loading = true;
      getWorkbench()
        .then(res => {
          const data = res.data.data || {};
          this.applyWorkbench(data);
        })
        .catch(() => {
          ElMessage.warning('首页聚合数据加载失败，已展示默认骨架');
        })
        .finally(() => {
          this.loading = false;
        });
    },
    applyWorkbench(data) {
      const overview = data.overview || {};
      const todos = data.todos || {};
      this.banner = {
        ...this.banner,
        ...(data.banner || {}),
      };
      this.metrics = this.metrics.map(item => ({
        ...item,
        value: this.metricValue(item.label, overview),
      }));
      this.todos = this.todos.map(item => ({
        ...item,
        count: this.todoValue(item.title, todos),
      }));
      this.enterprises = this.normalizeEnterprises(data.enterprises);
      this.policies = this.normalizePolicies(data.policyNotices);
      this.missingApis = data.missingApis || [];
    },
    metricValue(label, overview) {
      const map = {
        楼层管理: 'roomCount',
        客户管理: 'customerCount',
        合同即将到期: 'expiringContractCount',
        审批待处理: 'approvalTodoCount',
        待办任务: 'workorderTodoCount',
      };
      return Number(overview[map[label]]) || 0;
    },
    todoValue(title, todos) {
      const map = {
        审批待处理: 'approvalTodoCount',
        合同到期提醒: 'expiringContractCount',
        物业工单提醒: 'workorderTodoCount',
        审批超时提醒: 'timeoutApprovalCount',
      };
      return Number(todos[map[title]]) || 0;
    },
    normalizeEnterprises(list = []) {
      if (!Array.isArray(list) || list.length === 0) {
        return [
          { shortName: '企', name: '暂无入驻企业', industry: '客户数据接入后自动展示' },
        ];
      }
      return list.map(item => ({
        customerId: item.customerId,
        shortName: item.shortName || (item.enterpriseName || '企').slice(0, 1),
        name: item.enterpriseName || '未命名企业',
        industry: item.industry || '暂未填写行业',
      }));
    },
    normalizePolicies(list = []) {
      if (!Array.isArray(list) || list.length === 0) {
        return [
          { title: '园区政策通知接口待第三步接入', date: '待接入', path: '/enterprise/policy-service' },
          { title: '工作台 Banner 管理待第三步接入', date: '待接入', path: '/enterprise/policy-service' },
          { title: '政策服务模块入口', date: '待接入', path: '/enterprise/policy-service' },
        ];
      }
      return list.slice(0, 3).map(item => ({
        title: item.title || '未命名政策',
        date: item.publishTime || '待发布',
        path: item.linkUrl || '/enterprise/policy-service',
      }));
    },
    buildToday() {
      const now = new Date();
      return {
        day: String(now.getDate()).padStart(2, '0'),
        month: `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`,
      };
    },
    go(path, disabled = false) {
      if (disabled || !path) {
        ElMessage.info('该入口将在后续模块接入后开放');
        return;
      }
      const [routePath, queryString] = path.split('?');
      const query = {};
      if (queryString) {
        queryString.split('&').forEach(pair => {
          const [key, value] = pair.split('=');
          if (key) query[key] = value || '';
        });
      }
      this.$router.push({ path: routePath, query });
    },
  },
};
</script>

<style scoped>
.home-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #1f2937;
}

.hero-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-height: 178px;
  padding: 28px 32px;
  border-radius: 6px;
  background:
    linear-gradient(115deg, rgba(24, 88, 199, 0.9), rgba(17, 126, 167, 0.78)),
    url('/img/bg/bg1.jpg') center/cover no-repeat;
  color: #fff;
}

.hero-eyebrow {
  display: inline-block;
  margin-bottom: 12px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
}

.hero-copy h1 {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 600;
}

.hero-copy p {
  max-width: 620px;
  margin: 0;
  color: rgba(255, 255, 255, 0.88);
  font-size: 14px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-shrink: 0;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(145px, 1fr));
  gap: 12px;
}

.metric-card,
.entrance-item,
.shortcut-item,
.todo-item,
.policy-item {
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.metric-card:hover,
.entrance-item:hover,
.shortcut-item:hover,
.todo-item:hover,
.policy-item:hover {
  border-color: #2563eb;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.08);
}

.metric-card {
  min-height: 126px;
  padding: 16px;
}

.metric-icon,
.entrance-item span,
.shortcut-item span,
.enterprise-item span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 6px;
  color: #fff;
  font-weight: 600;
}

.metric-icon.blue {
  background: #2563eb;
}

.metric-icon.green {
  background: #16a34a;
}

.metric-icon.amber {
  background: #d97706;
}

.metric-icon.purple {
  background: #7c3aed;
}

.metric-icon.red {
  background: #dc2626;
}

.metric-title {
  display: block;
  margin-top: 12px;
  color: #64748b;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  font-weight: 600;
}

.metric-card em,
.entrance-item em,
.enterprise-item em,
.todo-item em {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
}

.main-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  padding: 18px;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  background: #fff;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}

.panel-head span {
  color: #64748b;
  font-size: 12px;
}

.entrance-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 12px;
}

.entrance-item {
  min-height: 116px;
  padding: 14px;
}

.entrance-item span,
.shortcut-item span {
  background: #0f766e;
}

.entrance-item strong {
  display: block;
  margin-top: 12px;
  font-size: 15px;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(90px, 1fr));
  gap: 10px;
}

.shortcut-item {
  min-height: 84px;
  padding: 12px;
}

.shortcut-item strong {
  display: block;
  margin-top: 10px;
  font-size: 13px;
}

.shortcut-item.disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.enterprise-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(160px, 1fr));
  gap: 12px;
}

.enterprise-item {
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 74px;
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 6px;
}

.enterprise-item span {
  flex-shrink: 0;
  background: #1d4ed8;
}

.enterprise-item strong {
  display: block;
  font-size: 14px;
}

.todo-list,
.policy-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
}

.todo-item span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 6px;
  background: #eef2ff;
  color: #1d4ed8;
  font-size: 18px;
  font-weight: 600;
}

.todo-item strong,
.policy-item strong {
  display: block;
  font-size: 14px;
}

.policy-item {
  padding: 12px;
}

.policy-item span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.schedule-card {
  min-height: 118px;
  padding: 18px;
  border-radius: 6px;
  background: #f8fafc;
}

.schedule-card strong {
  display: block;
  font-size: 34px;
  font-weight: 600;
}

.schedule-card span,
.schedule-card em {
  display: block;
  color: #64748b;
  font-size: 13px;
  font-style: normal;
}

.schedule-card em {
  margin-top: 16px;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(145px, 1fr));
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-section {
    display: block;
  }

  .hero-actions {
    margin-top: 18px;
  }

  .entrance-grid,
  .shortcut-grid,
  .enterprise-list {
    grid-template-columns: repeat(2, minmax(130px, 1fr));
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .entrance-grid,
  .shortcut-grid,
  .enterprise-list {
    grid-template-columns: 1fr;
  }

  .hero-section {
    padding: 22px;
  }

  .hero-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
