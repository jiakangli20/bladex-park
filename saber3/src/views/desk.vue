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
            <strong>{{ item.value }}<small>{{ item.unit }}</small></strong>
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

          <section class="panel common-panel">
            <div class="panel-head">
              <div>
                <h2>常用功能</h2>
                <p>常用业务快速进入</p>
              </div>
              <el-button text type="primary" class="shortcut-customize" @click="openShortcutEditor">
                <el-icon><Grid /></el-icon>
                自定义
              </el-button>
            </div>
            <div v-if="shortcuts.length" class="shortcut-grid">
              <button
                v-for="item in shortcuts"
                :key="item.path"
                type="button"
                class="shortcut-item"
                @click="go(item.path)"
              >
                <span :class="`shortcut-icon shortcut-icon--${item.tone}`">
                  <el-icon><component :is="item.icon" /></el-icon>
                </span>
                <span class="shortcut-copy">
                  <strong>{{ item.title }}</strong>
                  <em>{{ item.desc }}</em>
                </span>
              </button>
            </div>
            <el-empty v-else description="暂无常用功能" :image-size="82" />
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
                  通知提醒
                </h2>
              </div>
              <el-button text type="primary" @click="go('/enterprise/property-workorder')">更多</el-button>
            </div>
            <div class="todo-list" v-if="todos.length">
              <button
                v-for="(item, index) in todos"
                :key="`${item.title}-${index}`"
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
            <el-empty v-else description="暂无待办提醒" :image-size="92" />
          </section>
        </aside>
      </section>
    </div>

    <el-dialog
      v-model="shortcutEditorVisible"
      title="编辑常用功能"
      width="600px"
      append-to-body
      destroy-on-close
      class="shortcut-editor-dialog"
    >
      <el-tabs v-model="shortcutEditorTab" class="shortcut-editor-tabs">
        <el-tab-pane label="所有应用" name="all">
          <el-input
            v-model.trim="shortcutSearch"
            clearable
            placeholder="输入应用名称搜索"
            class="shortcut-search"
          >
            <template #suffix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <div v-if="filteredShortcutOptions.length" class="shortcut-editor-list">
            <div v-for="item in filteredShortcutOptions" :key="item.path" class="shortcut-editor-item">
              <span :class="`shortcut-icon shortcut-icon--${item.tone}`">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="shortcut-editor-copy">
                <strong>{{ item.title }}</strong>
                <em>{{ item.desc }}</em>
              </span>
              <span v-if="isShortcutSelected(item.path)" class="shortcut-added">已添加</span>
              <el-button v-else text type="primary" @click="addShortcut(item.path)">添加</el-button>
            </div>
          </div>
          <el-empty v-else description="未找到匹配的应用" :image-size="82" />
        </el-tab-pane>

        <el-tab-pane label="常用应用" name="selected">
          <div class="shortcut-sort-tip">拖拽左侧图标进行排序。</div>
          <div v-if="editingShortcutItems.length" class="shortcut-editor-list shortcut-sort-list">
            <div
              v-for="item in editingShortcutItems"
              :key="item.path"
              class="shortcut-editor-item shortcut-sort-item"
              :class="{ dragging: draggingShortcutPath === item.path }"
              draggable="true"
              @dragstart="startShortcutDrag(item.path, $event)"
              @dragover.prevent
              @drop="dropShortcut(item.path)"
              @dragend="draggingShortcutPath = ''"
            >
              <span class="shortcut-drag-handle" title="拖拽排序">
                <el-icon><Rank /></el-icon>
              </span>
              <span :class="`shortcut-icon shortcut-icon--${item.tone}`">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span class="shortcut-editor-copy">
                <strong>{{ item.title }}</strong>
                <em>{{ item.desc }}</em>
              </span>
              <el-button plain type="danger" size="small" @click="removeShortcut(item.path)">移除</el-button>
            </div>
          </div>
          <el-empty v-else description="暂未添加常用应用" :image-size="82" />
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button type="primary" @click="saveShortcutPreferences">确定</el-button>
        <el-button @click="shortcutEditorVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { ElMessage } from 'element-plus';
import { getWorkbench } from '@/api/home/home';
import { getStore, setStore } from '@/utils/store';

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
          label: '房源管理',
          value: 0,
          unit: '间',
          path: '/park/floor',
          icon: 'OfficeBuilding',
          tone: 'blue',
        },
        {
          label: '客户管理',
          value: 0,
          unit: '家',
          path: '/settlement/customer',
          icon: 'User',
          tone: 'cyan',
        },
        {
          label: '合同即将到期',
          value: 0,
          unit: '份',
          path: '/contract/expiry-notice',
          icon: 'DocumentChecked',
          tone: 'orange',
        },
        {
          label: '审批待处理',
          value: 0,
          unit: '项',
          path: '/plugin/workflow/pages/process/todo',
          icon: 'Finished',
          tone: 'purple',
        },
        {
          label: '待办工单',
          value: 0,
          unit: '条',
          path: '/enterprise/property-workorder',
          icon: 'Calendar',
          tone: 'green',
        },
      ],
      shortcutCandidates: [
        {
          title: '新增客户',
          desc: '录入企业客户档案',
          path: '/settlement/customer',
          icon: 'UserFilled',
          tone: 'blue',
        },
        {
          title: '商机管理',
          desc: '线索跟进与转化',
          path: '/settlement/opportunity',
          icon: 'Promotion',
          tone: 'orange',
        },
        {
          title: '背景调查',
          desc: '企业准入与风险核验',
          path: '/settlement/background-investigation',
          icon: 'Search',
          tone: 'green',
        },
        {
          title: '租控管理',
          desc: '房态与面积管理',
          path: '/park/rent-control',
          icon: 'Tickets',
          tone: 'indigo',
        },
        {
          title: '房源管理',
          desc: '维护楼宇与楼层',
          path: '/park/floor',
          icon: 'OfficeBuilding',
          tone: 'cyan',
        },
        {
          title: '合同列表',
          desc: '合同台账与履约',
          path: '/contract/contract',
          icon: 'Document',
          tone: 'orange',
        },
        {
          title: '合同归档',
          desc: '归档文件管理',
          path: '/contract/archive',
          icon: 'FolderChecked',
          tone: 'blue',
        },
        {
          title: '收款通知',
          desc: '开票与收款提醒',
          path: '/contract/payment-notice',
          icon: 'Postcard',
          tone: 'green',
        },
        {
          title: '所有账单',
          desc: '收付款账单管理',
          path: '/finance/bills-all',
          icon: 'Money',
          tone: 'purple',
        },
        {
          title: '我的消息',
          desc: '逾期处置闭环',
          path: '/finance/overdue-notice',
          icon: 'Bell',
          tone: 'orange',
        },
        {
          title: '物业工单',
          desc: '企业诉求处理',
          path: '/enterprise/property-workorder',
          icon: 'Tools',
          tone: 'green',
          group: 'enterprise',
        },
        {
          title: '商户管理',
          desc: '合作商户档案',
          path: '/enterprise/merchant',
          icon: 'Shop',
          tone: 'cyan',
          group: 'enterprise',
        },
        {
          title: '我的审批',
          desc: '审批办理记录',
          path: '/plugin/workflow/pages/process/my-done',
          icon: 'Checked',
          tone: 'indigo',
          group: 'workflow',
        },
      ],
      favoriteShortcutPaths: null,
      shortcutEditorVisible: false,
      shortcutEditorTab: 'all',
      shortcutSearch: '',
      editingShortcutPaths: [],
      draggingShortcutPath: '',
      todos: [],
      calendarWeeks: ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
      today: this.buildToday(),
      missingApis: [],
    };
  },
  computed: {
    availableShortcuts() {
      const menuList = this.flattenMenus(this.$store.state.user.menuAll || []);
      const menuByPath = new Map(menuList.filter(item => item.path).map(item => [item.path, item]));
      const authorizedCandidates = this.shortcutCandidates.filter(item =>
        menuByPath.has(item.path)
      );
      const commonEntries = authorizedCandidates.filter(item => !item.group);
      const enterpriseEntries = authorizedCandidates.filter(item => item.group === 'enterprise');
      const workflowEntries = authorizedCandidates.filter(item => item.group === 'workflow');
      const enterpriseMenu = menuByPath.get('/enterprise');
      const enterpriseSecondMenus = (enterpriseMenu?.children || [])
        .filter(
          item => item && item.path && (item.category === undefined || Number(item.category) === 1)
        )
        .map((item, index) => this.enterpriseShortcut(item, index));
      const seenPaths = new Set();

      return [
        ...commonEntries,
        ...enterpriseSecondMenus,
        ...enterpriseEntries,
        ...workflowEntries,
      ].filter(item => {
        if (!item.path || seenPaths.has(item.path)) return false;
        seenPaths.add(item.path);
        return true;
      });
    },
    shortcuts() {
      if (!Array.isArray(this.favoriteShortcutPaths)) return this.availableShortcuts;
      const shortcutByPath = new Map(this.availableShortcuts.map(item => [item.path, item]));
      return this.favoriteShortcutPaths.map(path => shortcutByPath.get(path)).filter(Boolean);
    },
    filteredShortcutOptions() {
      const keyword = this.shortcutSearch.trim().toLowerCase();
      if (!keyword) return this.availableShortcuts;
      return this.availableShortcuts.filter(item =>
        `${item.title} ${item.desc}`.toLowerCase().includes(keyword)
      );
    },
    editingShortcutItems() {
      const shortcutByPath = new Map(this.availableShortcuts.map(item => [item.path, item]));
      return this.editingShortcutPaths.map(path => shortcutByPath.get(path)).filter(Boolean);
    },
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
    this.loadShortcutPreferences();
    this.loadWorkbench();
  },
  methods: {
    shortcutPreferenceKey() {
      const userState = this.$store.state.user || {};
      const user = userState.userInfo || {};
      const tenantId = userState.tenantId || user.tenantId || user.tenant_id || 'default';
      const userId = user.userId || user.user_id || user.account || 'anonymous';
      return `home-common-functions-${tenantId}-${userId}`;
    },
    loadShortcutPreferences() {
      const storedPaths = getStore({ name: this.shortcutPreferenceKey() });
      this.favoriteShortcutPaths = Array.isArray(storedPaths)
        ? storedPaths.filter(path => typeof path === 'string')
        : null;
    },
    openShortcutEditor() {
      this.shortcutEditorTab = 'all';
      this.shortcutSearch = '';
      this.draggingShortcutPath = '';
      this.editingShortcutPaths = Array.isArray(this.favoriteShortcutPaths)
        ? [...this.favoriteShortcutPaths]
        : this.availableShortcuts.map(item => item.path);
      this.shortcutEditorVisible = true;
    },
    isShortcutSelected(path) {
      return this.editingShortcutPaths.includes(path);
    },
    addShortcut(path) {
      if (!path || this.isShortcutSelected(path)) return;
      this.editingShortcutPaths.push(path);
    },
    removeShortcut(path) {
      this.editingShortcutPaths = this.editingShortcutPaths.filter(item => item !== path);
    },
    startShortcutDrag(path, event) {
      this.draggingShortcutPath = path;
      if (!event.dataTransfer) return;
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', path);
    },
    dropShortcut(targetPath) {
      const sourcePath = this.draggingShortcutPath;
      const sourceIndex = this.editingShortcutPaths.indexOf(sourcePath);
      const targetIndex = this.editingShortcutPaths.indexOf(targetPath);
      if (sourceIndex === -1 || targetIndex === -1 || sourceIndex === targetIndex) return;
      const reorderedPaths = [...this.editingShortcutPaths];
      const [movedPath] = reorderedPaths.splice(sourceIndex, 1);
      reorderedPaths.splice(targetIndex, 0, movedPath);
      this.editingShortcutPaths = reorderedPaths;
    },
    saveShortcutPreferences() {
      const authorizedPaths = new Set(this.availableShortcuts.map(item => item.path));
      this.favoriteShortcutPaths = this.editingShortcutPaths.filter(path => authorizedPaths.has(path));
      setStore({ name: this.shortcutPreferenceKey(), content: this.favoriteShortcutPaths });
      this.shortcutEditorVisible = false;
      ElMessage.success('常用功能已更新');
    },
    flattenMenus(menuList = []) {
      return menuList.reduce((result, item) => {
        if (!item) return result;
        result.push(item);
        return result.concat(this.flattenMenus(item.children || []));
      }, []);
    },
    enterpriseShortcut(menuItem, index) {
      const presets = {
        '/enterprise/property-service': {
          desc: '服务配置与工单处理',
          icon: 'SetUp',
          tone: 'blue',
        },
        '/enterprise/merchant-service': {
          desc: '商户档案与增值服务',
          icon: 'Shop',
          tone: 'cyan',
        },
        '/enterprise/merchant-ad': {
          desc: '园区广告审核管理',
          icon: 'Document',
          tone: 'orange',
        },
        '/enterprise/policy-service': {
          desc: '政策发布与维护',
          icon: 'Postcard',
          tone: 'green',
        },
        '/enterprise/enterprise-data': {
          desc: '企业运营数据看板',
          icon: 'OfficeBuilding',
          tone: 'purple',
        },
        '/enterprise/settlement-todo': {
          desc: '入驻意向跟进处理',
          icon: 'Tickets',
          tone: 'indigo',
        },
        '/enterprise/notice': {
          desc: '园区通知发布管理',
          icon: 'Bell',
          tone: 'orange',
        },
        '/enterprise/park-activity': {
          desc: '园区活动审核发布',
          icon: 'Calendar',
          tone: 'cyan',
        },
        '/enterprise/enterprise-auth': {
          desc: '企业认证申请审核',
          icon: 'Finished',
          tone: 'green',
        },
      };
      const tones = ['blue', 'cyan', 'orange', 'green', 'purple', 'indigo'];
      const preset = presets[menuItem.path] || {
        desc: `进入${menuItem.name || '企业服务'}`,
        icon: 'Grid',
        tone: tones[index % tones.length],
      };
      return {
        title: menuItem.name || '企业服务',
        path: menuItem.path,
        ...preset,
      };
    },
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
      this.todos = Array.isArray(todos.items) ? todos.items : [];
      this.missingApis = data.missingApis || [];
    },
    metricValue(label, overview) {
      const map = {
        房源管理: 'roomCount',
        客户管理: 'customerCount',
        合同即将到期: 'expiringContractCount',
        审批待处理: 'approvalTodoCount',
        待办工单: 'workorderTodoCount',
      };
      return Number(overview[map[label]]) || 0;
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
    go(path) {
      if (!path) return;
      const queryIndex = path.indexOf('?');
      const routePath = queryIndex === -1 ? path : path.slice(0, queryIndex);
      const queryString = queryIndex === -1 ? '' : path.slice(queryIndex + 1);
      const query = Object.fromEntries(new URLSearchParams(queryString));
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
  gap: 10px;
  color: #1f2d3d;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.metric-card {
  position: relative;
  display: flex;
  align-items: center;
  min-height: 86px;
  padding: 16px 58px 16px 16px;
  border: none;
  border-radius: 10px;
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
.shortcut-icon,
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

.metric-copy strong small {
  margin-left: 4px;
  color: #6f7b8a;
  font-size: 12px;
  font-weight: 500;
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
.shortcut-icon--cyan {
  background: #26c4bf;
}

.metric-card--orange .metric-icon,
.shortcut-icon--orange,
.todo-item--orange > span {
  background: #ff8e23;
}

.metric-card--purple .metric-icon,
.shortcut-icon--purple,
.todo-item--purple > span {
  background: #8c54df;
}

.metric-card--green .metric-icon,
.shortcut-icon--green {
  background: #52c41a;
}

.todo-item--red > span {
  background: #ff4757;
}

.shortcut-icon--indigo {
  background: #3454f5;
}

.home-layout {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(240px, 1fr);
  gap: 12px;
  align-items: stretch;
}

.home-main,
.home-side {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  gap: 12px;
  align-self: stretch;
}

.hero-section {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36%;
  min-height: 182px;
  height: 182px;
  overflow: hidden;
  border-radius: 10px;
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
  min-height: 182px;
  background-position: center;
  background-size: cover;
}

.common-panel {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  min-height: 354px;
}

.panel {
  box-sizing: border-box;
  padding: 16px;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(30, 64, 120, 0.06);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
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

.shortcut-customize {
  min-height: 32px;
  padding: 0 2px;
  font-size: 13px;
}

.shortcut-customize .el-icon {
  margin-right: 4px;
}

.shortcut-item strong,
.todo-item strong {
  display: block;
  color: #172033;
  font-size: 14px;
  font-weight: 700;
}

.todo-item em {
  display: block;
  margin-top: 4px;
  color: #8b98aa;
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
}

.todo-panel {
  display: flex;
  flex-direction: column;
  flex: 1 1 0;
  min-height: 354px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.todo-panel :deep(.el-empty) {
  flex: 1;
}

.todo-item:last-child {
  border-bottom: none;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  flex: 1;
  align-content: start;
}

.shortcut-item {
  display: flex;
  align-items: center;
  min-height: 58px;
  padding: 10px;
  border: 1px solid #e6edf6;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.shortcut-item strong {
  font-weight: 500;
}

.shortcut-copy {
  min-width: 0;
  margin-left: 10px;
}

.shortcut-item em {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #8b98aa;
  font-size: 12px;
  font-style: normal;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.common-panel > :deep(.el-empty) {
  flex: 1;
  padding: 12px 0;
}

:global(.shortcut-editor-dialog) {
  max-width: calc(100vw - 32px);
  border-radius: 4px;
}

:global(.shortcut-editor-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 22px 20px 16px;
  border-bottom: 1px solid #dfe4ec;
}

:global(.shortcut-editor-dialog .el-dialog__title) {
  color: #172033;
  font-size: 16px;
  font-weight: 600;
}

:global(.shortcut-editor-dialog .el-dialog__body) {
  padding: 0 20px;
}

:global(.shortcut-editor-dialog .el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid #dfe4ec;
}

.shortcut-editor-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.shortcut-editor-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #edf0f5;
}

.shortcut-editor-tabs :deep(.el-tabs__item) {
  height: 52px;
  padding: 0 24px;
  font-size: 14px;
}

.shortcut-editor-tabs :deep(.el-tabs__item:first-child) {
  padding-left: 0;
}

.shortcut-search {
  margin-bottom: 8px;
}

.shortcut-sort-tip {
  margin-bottom: 8px;
  padding: 10px 12px;
  border: 1px solid #91caff;
  border-radius: 6px;
  background: #e6f4ff;
  color: #344054;
  font-size: 13px;
}

.shortcut-editor-list {
  min-height: 300px;
  max-height: 430px;
  overflow-y: auto;
  padding: 4px 4px 12px;
}

.shortcut-editor-item {
  display: flex;
  align-items: center;
  min-height: 64px;
  padding: 6px 4px;
  border-bottom: 1px solid transparent;
}

.shortcut-editor-copy {
  min-width: 0;
  margin-left: 10px;
}

.shortcut-editor-copy strong,
.shortcut-editor-copy em {
  display: block;
}

.shortcut-editor-copy strong {
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut-editor-copy em {
  overflow: hidden;
  margin-top: 4px;
  color: #8b98aa;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shortcut-editor-item > .shortcut-editor-copy {
  flex: 1;
}

.shortcut-added {
  flex-shrink: 0;
  color: #606266;
  font-size: 13px;
}

.shortcut-drag-handle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 28px;
  margin-right: 6px;
  color: #606266;
  cursor: grab;
  font-size: 18px;
}

.shortcut-sort-item {
  cursor: grab;
  transition: background-color 0.15s ease, opacity 0.15s ease;
}

.shortcut-sort-item:hover {
  background: #f7f9fc;
}

.shortcut-sort-item.dragging {
  background: #ecf5ff;
  opacity: 0.55;
}

.shortcut-sort-item > .el-button,
.shortcut-editor-item > .el-button {
  flex-shrink: 0;
}

.calendar-panel {
  min-height: 314px;
}

.calendar-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 6px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5eaf2;
  color: #667085;
  font-size: 12px;
}

.calendar-toolbar span,
.calendar-toolbar strong {
  box-sizing: border-box;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 22px;
  min-width: 42px;
  padding: 0 5px;
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
  height: 28px;
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
    grid-template-columns: minmax(0, 3fr) minmax(240px, 1fr);
  }
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .metric-card {
    min-height: 78px;
    padding: 12px;
  }

  .metric-card em {
    display: none;
  }

  .metric-copy span {
    white-space: nowrap;
  }

  .shortcut-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .home-layout {
    grid-template-columns: minmax(0, 3fr) minmax(240px, 1fr);
  }
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .home-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .metric-grid,
  .shortcut-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-section {
    grid-template-columns: 1fr;
    height: auto;
  }

  .hero-visual {
    min-height: 140px;
  }
}

@media (max-width: 640px) {
  .metric-grid,
  .shortcut-grid {
    grid-template-columns: 1fr;
  }

  .metric-card {
    min-height: 78px;
  }

  :global(.shortcut-editor-dialog .el-dialog__header),
  :global(.shortcut-editor-dialog .el-dialog__body),
  :global(.shortcut-editor-dialog .el-dialog__footer) {
    padding-right: 14px;
    padding-left: 14px;
  }

  .shortcut-editor-list {
    max-height: 54vh;
  }
}
</style>
