<template>
  <basic-container class="home-container">
    <div v-loading="loading" class="home-workbench">
      <section class="metric-grid">
        <button
          v-for="item in metrics"
          :key="item.label"
          type="button"
          class="metric-card"
          :class="`metric-card--${item.tone}`"
          @click="go(item.path)"
        >
          <span class="metric-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </span>
          <span class="metric-copy">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </span>
          <em>详情&gt;</em>
        </button>
      </section>

      <section class="home-layout">
        <main class="home-main">
          <section class="hero-section">
            <div class="hero-copy">
              <h1>{{ banner.name }}</h1>
              <p>{{ banner.bannerDesc }}</p>
            </div>
            <div class="hero-visual" :style="heroStyle"></div>
          </section>

          <section class="panel investment-panel">
            <div class="panel-head">
              <div>
                <h2>招商闭环入口</h2>
                <p>从商机到审核、风险、客户和收缴的关键业务入口</p>
              </div>
              <el-button text type="primary" @click="go('/settlement/opportunity')">商机管理</el-button>
            </div>
            <div class="entrance-grid">
              <button
                v-for="item in lifecycleEntrances"
                :key="item.title"
                type="button"
                class="entrance-item"
                :class="`entrance-item--${item.tone}`"
                @click="go(item.path)"
              >
                <span>
                  <el-icon><component :is="item.icon" /></el-icon>
                </span>
                <strong>{{ item.title }}</strong>
                <em>{{ item.desc }}</em>
                <i class="entry-arrow el-icon-arrow-right"></i>
              </button>
            </div>
          </section>

          <section class="home-dual-row">
            <section class="panel policy-panel">
              <div class="panel-head">
                <div>
                  <h2>园区政策通知</h2>
                  <p>最新政策、服务通知与业务公告</p>
                </div>
                <el-button text type="primary" @click="go('/enterprise/policy-service')">政策</el-button>
              </div>
              <div class="policy-list">
                <button
                  v-for="item in policies"
                  :key="item.title"
                  type="button"
                  class="policy-item"
                  @click="go(item.path)"
                >
                  <span class="policy-icon">
                    <el-icon><Bell /></el-icon>
                  </span>
                  <span>
                    <strong>{{ item.title }}</strong>
                    <em>{{ item.date }}</em>
                  </span>
                  <b>查看</b>
                </button>
              </div>
            </section>

            <section class="panel quick-panel">
              <div class="panel-head">
                <div>
                  <h2>快捷入口</h2>
                  <p>常用业务快速进入</p>
                </div>
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
                  <span :class="`shortcut-icon shortcut-icon--${item.tone}`">
                    <el-icon><component :is="item.icon" /></el-icon>
                  </span>
                  <strong>{{ item.title }}</strong>
                </button>
              </div>
            </section>
          </section>

        </main>

        <aside class="home-side">
          <section class="panel calendar-panel">
            <div class="panel-head">
              <div>
                <h2>
                  <el-icon><Calendar /></el-icon>
                  日程安排
                </h2>
              </div>
            </div>
            <div class="calendar-toolbar">
              <span>{{ today.year }}</span>
              <span>{{ today.monthName }}</span>
              <strong>Month</strong>
              <span>Year</span>
            </div>
            <div class="calendar-grid">
              <span v-for="week in calendarWeeks" :key="week" class="calendar-week">{{ week }}</span>
              <span
                v-for="day in calendarDays"
                :key="day.key"
                class="calendar-day"
                :class="{ muted: !day.current, active: day.current && day.day === today.day }"
              >
                {{ day.dayText }}
              </span>
            </div>
          </section>

          <section class="panel todo-panel">
            <div class="panel-head">
              <div>
                <h2>
                  <el-icon><Tickets /></el-icon>
                  待办任务
                </h2>
              </div>
              <el-button text type="primary" @click="go('/plugin/workflow/pages/process/todo')">更多</el-button>
            </div>
            <div class="todo-list">
              <button
                v-for="item in todos"
                :key="item.title"
                type="button"
                class="todo-item"
                :class="`todo-item--${item.tone}`"
                @click="go(item.path)"
              >
                <span>
                  <el-icon><component :is="item.icon" /></el-icon>
                </span>
                <div>
                  <strong>{{ item.title }}</strong>
                  <em>{{ item.desc }}</em>
                </div>
              </button>
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
  name: 'DeskHome',
  inject: {
    index: {
      default: null,
    },
  },
  data() {
    return {
      loading: false,
      banner: {
        name: '智慧园区工作台',
        bannerDesc: '聚合房源、客户、合同、审批与任务，助力园区高效运营',
        imageUrl: '/img/bg/bg1.jpg',
      },
      metrics: [
        {
          label: '楼层管理',
          value: 0,
          path: '/park/floor',
          icon: 'OfficeBuilding',
          tone: 'blue',
        },
        {
          label: '客户管理',
          value: 0,
          path: '/settlement/customer',
          icon: 'User',
          tone: 'cyan',
        },
        {
          label: '合同即将到期',
          value: 0,
          path: '/contract/expiry-notice',
          icon: 'DocumentChecked',
          tone: 'orange',
        },
        {
          label: '审批待处理',
          value: 0,
          path: '/plugin/workflow/pages/process/todo',
          icon: 'Finished',
          tone: 'purple',
        },
        {
          label: '待办任务',
          value: 0,
          path: '/enterprise/property-workorder',
          icon: 'Calendar',
          tone: 'green',
        },
      ],
      lifecycleEntrances: [
        {
          title: '租金收缴',
          desc: '账单、催缴、流水闭环',
          path: '/finance/bills-all',
          icon: 'Money',
          tone: 'orange',
        },
        {
          title: '企业风险',
          desc: '高风险客户与背景排查',
          path: '/settlement/customer?riskLevel=3',
          icon: 'Warning',
          tone: 'red',
        },
        {
          title: '入驻审核',
          desc: '入驻申请发起与审批记录',
          path: '/settlement/project-approval',
          icon: 'Tickets',
          tone: 'purple',
        },
        {
          title: '客户管理',
          desc: '客户档案、合同与账单',
          path: '/settlement/customer',
          icon: 'User',
          tone: 'cyan',
        },
      ],
      shortcuts: [
        { title: '新增客户', path: '/settlement/customer', icon: 'UserFilled', tone: 'blue' },
        { title: '楼层管理', path: '/park/floor', icon: 'OfficeBuilding', tone: 'cyan' },
        { title: '新建合同', path: '/contract/archive', icon: 'DocumentAdd', tone: 'orange' },
        { title: '我的审批', path: '/plugin/workflow/pages/process/my-done', icon: 'Finished', tone: 'purple' },
        { title: '物业工单', path: '/enterprise/property-workorder', icon: 'Tools', tone: 'green' },
        { title: '租控管理', path: '/park/rent-control', icon: 'Tickets', tone: 'indigo' },
      ],
      todos: [
        {
          title: '审批待处理提醒',
          count: 0,
          desc: '暂无待处理审批',
          path: '/plugin/workflow/pages/process/todo',
          icon: 'Finished',
          tone: 'purple',
        },
        {
          title: '合同到期提醒',
          count: 0,
          desc: '暂无合同即将到期',
          path: '/contract/expiry-notice',
          icon: 'DocumentChecked',
          tone: 'orange',
        },
        {
          title: '物业工单提醒',
          count: 0,
          desc: '暂无物业工单待处理',
          path: '/enterprise/property-workorder',
          icon: 'Tools',
          tone: 'blue',
        },
		{
		  title: '我的逾期通知',
		  count: 0,
		  desc: '暂无未读逾期通知',
		  path: '/finance/overdue-notice',
		  icon: 'Bell',
		  tone: 'red',
		},
      ],
      policies: [],
      calendarWeeks: ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
      today: this.buildToday(),
      missingApis: [],
    };
  },
  computed: {
    heroStyle() {
      return {
        backgroundImage: `url(${this.banner.imageUrl || '/img/bg/bg1.jpg'})`,
      };
    },
    calendarDays() {
      const { year, monthIndex } = this.today;
      const firstDay = new Date(year, monthIndex, 1).getDay();
      const monthLength = new Date(year, monthIndex + 1, 0).getDate();
      const prevMonthLength = new Date(year, monthIndex, 0).getDate();
      const days = [];

      for (let index = firstDay - 1; index >= 0; index -= 1) {
        const day = prevMonthLength - index;
        days.push({
          key: `prev-${day}`,
          day,
          dayText: String(day).padStart(2, '0'),
          current: false,
        });
      }

      for (let day = 1; day <= monthLength; day += 1) {
        days.push({
          key: `current-${day}`,
          day,
          dayText: String(day).padStart(2, '0'),
          current: true,
        });
      }

      let nextDay = 1;
      while (days.length < 42) {
        days.push({
          key: `next-${nextDay}`,
          day: nextDay,
          dayText: String(nextDay).padStart(2, '0'),
          current: false,
        });
        nextDay += 1;
      }

      return days;
    },
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
        desc: this.todoDesc(item.title, this.todoValue(item.title, todos)),
      }));
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
        审批待处理提醒: 'approvalTodoCount',
        合同到期提醒: 'expiringContractCount',
        物业工单提醒: 'workorderTodoCount',
		我的逾期通知: 'overdueNoticeCount',
      };
      return Number(todos[map[title]]) || 0;
    },
    todoDesc(title, count) {
      if (!count) {
        const emptyMap = {
          审批待处理提醒: '暂无待处理审批',
          合同到期提醒: '暂无合同即将到期',
          物业工单提醒: '暂无物业工单待处理',
			  我的逾期通知: '暂无未读逾期通知',
        };
        return emptyMap[title] || '暂无待办任务';
      }
      const unitMap = {
        审批待处理提醒: `${count} 个审批待处理`,
        合同到期提醒: `${count} 个合同即将到期`,
        物业工单提醒: `${count} 条物业工单待处理`,
			我的逾期通知: `${count} 条逾期通知未读`,
      };
      return unitMap[title] || `${count} 条待办任务`;
    },
    normalizePolicies(list = []) {
      if (!Array.isArray(list) || list.length === 0) {
        return [];
      }
      return list.slice(0, 4).map(item => ({
        title: item.title || '未命名政策',
        date: item.publishTime || '待发布',
        path: item.linkUrl || '/enterprise/policy-service',
      }));
    },
    buildToday() {
      const now = new Date();
      const monthNames = [
        'Jan',
        'Feb',
        'Mar',
        'Apr',
        'May',
        'Jun',
        'Jul',
        'Aug',
        'Sep',
        'Oct',
        'Nov',
        'Dec',
      ];
      return {
        day: now.getDate(),
        year: now.getFullYear(),
        monthIndex: now.getMonth(),
        monthName: monthNames[now.getMonth()],
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
      this.syncSideMenu(routePath).finally(() => {
        this.$router.push({ path: routePath, query });
      });
    },
    syncSideMenu(routePath) {
      if (!routePath || routePath === this.$route.path) {
        return Promise.resolve();
      }
      const topMenus = this.$store.state.user.topMenu || [];
      const loadTopMenus = topMenus.length
        ? Promise.resolve(topMenus)
        : this.$store.dispatch('GetTopMenu');
      return loadTopMenus
        .then(list => {
          const activeTopMenu = this.findTopMenuByPath(list, routePath);
          if (!activeTopMenu || !activeTopMenu.id) {
            return Promise.resolve();
          }
          return this.$store.dispatch('GetMenu', activeTopMenu.id).then(data => {
            if (this.index && this.index.$refs && this.index.$refs.top) {
              this.index.$refs.top.setActiveMenu(activeTopMenu.id);
            }
            if (data.length !== 0 && this.$router.$avueRouter) {
              const menuAll = this.$store.state.user.menuAll || [];
              this.$router.$avueRouter.formatRoutes(menuAll.length ? menuAll : data, true);
            }
            return data;
          });
        })
        .catch(() => Promise.resolve());
    },
    findTopMenuByPath(topMenus = [], routePath) {
      const matchPath = menuItem => {
        if (!menuItem || !routePath) return false;
        if (menuItem.path && (routePath === menuItem.path || routePath.indexOf(`${menuItem.path}/`) === 0)) {
          return true;
        }
        return (menuItem.children || []).some(child => matchPath(child));
      };
      const topMenuCode = this.topMenuCodeByRoute(routePath);
      return (
        topMenus.find(menuItem => matchPath(menuItem)) ||
        topMenus.find(menuItem => topMenuCode && menuItem.code === topMenuCode)
      );
    },
    topMenuCodeByRoute(routePath) {
      const routeMap = [
        { prefix: '/plugin/workflow', code: 'office' },
        { prefix: '/settlement', code: 'entry' },
        { prefix: '/enterprise', code: 'service' },
        { prefix: '/contract', code: 'contract' },
        { prefix: '/finance', code: 'finance' },
        { prefix: '/park', code: 'park' },
      ];
      const matched = routeMap.find(
        item => routePath === item.prefix || routePath.indexOf(`${item.prefix}/`) === 0
      );
      return matched ? matched.code : '';
    },
  },
};
</script>

<style scoped>
.home-container :deep(.basic-container__card) {
  border: none !important;
  background: transparent;
  box-shadow: none;
}

.home-container :deep(.basic-container__card > .el-card__body) {
  padding: 0;
}

.home-workbench {
  display: flex;
  flex-direction: column;
  gap: 16px;
  color: #1f2d3d;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(160px, 1fr));
  gap: 16px;
}

.metric-card {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 86px;
  padding: 16px 58px 16px 16px;
  border: none;
  border-radius: 0;
  background: #fff;
  box-shadow: 0 4px 16px rgba(30, 64, 120, 0.06);
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.metric-card:hover {
  box-shadow: 0 8px 18px rgba(16, 89, 198, 0.12);
  transform: translateY(-1px);
}

.metric-icon,
.entrance-item span,
.shortcut-icon,
.policy-icon,
.todo-item > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  color: #fff;
  font-size: 18px;
}

.metric-copy {
  display: block;
  margin-left: 12px;
}

.metric-copy span {
  display: block;
  color: #6f7b8a;
  font-size: 13px;
}

.metric-copy strong {
  display: block;
  margin-top: 6px;
  color: #172033;
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.metric-card em {
  position: absolute;
  right: 16px;
  bottom: 14px;
  color: #1c73f4;
  font-size: 12px;
  font-style: normal;
}

.metric-card--blue .metric-icon,
.shortcut-icon--blue,
.todo-item--blue > span {
  background: #1c73f4;
}

.metric-card--cyan .metric-icon,
.entrance-item--cyan span,
.shortcut-icon--cyan {
  background: #26c4bf;
}

.metric-card--orange .metric-icon,
.entrance-item--orange span,
.shortcut-icon--orange,
.todo-item--orange > span {
  background: #ff8e23;
}

.metric-card--purple .metric-icon,
.entrance-item--purple span,
.shortcut-icon--purple,
.todo-item--purple > span {
  background: #8c54df;
}

.metric-card--green .metric-icon,
.shortcut-icon--green {
  background: #52c41a;
}

.entrance-item--red span,
.todo-item--red > span {
  background: #ff4757;
}

.shortcut-icon--indigo {
  background: #3454f5;
}

.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 448px;
  grid-template-rows: auto auto minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.home-main,
.home-side {
  display: contents;
}

.hero-section {
  grid-column: 1;
  grid-row: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36%;
  min-height: 198px;
  overflow: hidden;
  border-radius: 0;
  background: linear-gradient(110deg, #1167e8 0%, #1f7cff 58%, #d8e8ff 100%);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 28px 38px;
  color: #fff;
}

.hero-copy h1 {
  margin: 0 0 18px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 0;
}

.hero-copy p {
  max-width: 620px;
  margin: 0;
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  line-height: 1.8;
}

.hero-visual {
  min-height: 198px;
  background-position: center;
  background-size: cover;
}

.investment-panel {
  grid-column: 1;
  grid-row: 2;
}

.panel {
  padding: 20px;
  border-radius: 0;
  background: #fff;
  box-shadow: 0 4px 16px rgba(30, 64, 120, 0.06);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head h2 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
  color: #172033;
  font-size: 16px;
  font-weight: 700;
}

.panel-head p {
  margin: 6px 0 0;
  color: #8b98aa;
  font-size: 12px;
}

.entrance-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 12px;
}

.entrance-item {
  position: relative;
  min-height: 68px;
  padding: 14px 40px 14px 62px;
  border: 1px solid #e6edf6;
  border-radius: 0;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.entrance-item span {
  position: absolute;
  top: 16px;
  left: 14px;
}

.entrance-item strong,
.shortcut-item strong,
.policy-item strong,
.todo-item strong {
  display: block;
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.entrance-item em,
.policy-item em,
.todo-item em {
  display: block;
  margin-top: 4px;
  color: #8b98aa;
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
}

.entry-arrow {
  position: absolute;
  top: 26px;
  right: 14px;
  color: #b6c0ce;
  font-size: 16px;
}

.home-dual-row {
  grid-column: 1;
  grid-row: 3;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
  flex: 1;
  align-items: stretch;
}

.policy-panel,
.quick-panel,
.todo-panel {
  display: flex;
  flex-direction: column;
}

.policy-list,
.todo-list {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.policy-item {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 74px;
  padding: 12px 62px 12px 0;
  border: none;
  border-bottom: 1px solid #edf1f6;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.policy-item:last-child,
.todo-item:last-child {
  border-bottom: none;
}

.policy-icon {
  margin-right: 14px;
  background: #1c73f4;
}

.policy-item b {
  position: absolute;
  right: 0;
  color: #1c73f4;
  font-size: 13px;
  font-weight: 500;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 12px;
  flex: 1;
  align-content: start;
}

.shortcut-item {
  display: flex;
  align-items: center;
  min-height: 58px;
  padding: 10px;
  border: 1px solid #e6edf6;
  border-radius: 0;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.shortcut-item strong {
  margin-left: 10px;
  font-weight: 500;
}

.shortcut-item.disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.calendar-panel {
  grid-column: 2;
  grid-row: 1 / span 2;
  min-height: 334px;
}

.todo-panel {
  grid-column: 2;
  grid-row: 3;
}

.calendar-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5eaf2;
  color: #667085;
  font-size: 13px;
}

.calendar-toolbar span,
.calendar-toolbar strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  min-width: 58px;
  padding: 0 8px;
  border: 1px solid #d9deea;
  background: #fff;
  font-weight: 400;
}

.calendar-toolbar strong {
  border-color: #1c73f4;
  color: #1c73f4;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 0;
  padding-top: 8px;
}

.calendar-week,
.calendar-day {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  color: #667085;
  font-size: 13px;
}

.calendar-day.muted {
  color: #c3cad5;
}

.calendar-day.active {
  color: #fff;
}

.calendar-day {
  position: relative;
}

.calendar-day.active {
  z-index: 0;
}

.calendar-day.active::after {
  content: '';
  position: absolute;
  z-index: -1;
  width: 22px;
  height: 22px;
  border-radius: 2px;
  background: #1c73f4;
}

.todo-item {
  display: flex;
  align-items: center;
  min-height: 58px;
  padding: 10px 0;
  border: none;
  border-bottom: 1px solid #edf1f6;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.todo-item > span {
  width: 32px;
  height: 32px;
  margin-right: 12px;
  border-radius: 8px;
  font-size: 16px;
}

@media (max-width: 1500px) {
  .home-layout {
    grid-template-columns: minmax(0, 1fr) 390px;
  }
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(160px, 1fr));
  }

  .home-layout,
  .home-dual-row {
    grid-template-columns: 1fr;
  }

  .home-layout {
    grid-template-rows: none;
  }

  .home-main,
  .home-side {
    display: flex;
    flex-direction: column;
    gap: 16px;
    height: auto;
  }

  .hero-section,
  .investment-panel,
  .home-dual-row,
  .calendar-panel,
  .todo-panel {
    grid-column: auto;
    grid-row: auto;
  }
}

@media (max-width: 900px) {
  .metric-grid,
  .entrance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-section {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    min-height: 140px;
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .entrance-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    min-height: 78px;
  }
}
</style>
