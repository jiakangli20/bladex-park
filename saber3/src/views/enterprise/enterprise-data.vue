<template>
  <basic-container>
    <div v-loading="loading" class="enterprise-data-page">
      <header class="dashboard-header">
        <div class="dashboard-header__title">
          <strong>在园企业数据</strong>
          <span>园区经营、合同、房源与设备运行概览</span>
        </div>
        <div class="dashboard-header__actions">
          <el-tag type="primary" effect="plain">全部园区</el-tag>
          <el-button circle plain title="刷新数据" :loading="loading" @click="loadData">
            <i v-if="!loading" class="el-icon-refresh" />
          </el-button>
        </div>
      </header>

      <div class="dashboard-row dashboard-row--top">
        <section class="dashboard-card overview-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-menu" /></span>
              <strong>数字概览</strong>
            </div>
            <span class="card-note">关键经营指标</span>
          </div>
          <div class="overview-grid">
            <button
              v-for="item in overviewCards"
              :key="item.key"
              type="button"
              class="overview-item"
              :class="`overview-item--${item.tone || 'blue'}`"
              @click="openOverviewDetail(item)"
            >
              <span class="overview-item__icon"><i :class="item.icon" /></span>
              <span class="overview-item__content">
                <em>{{ item.label }}</em>
                <strong>{{ formatMoneyLike(item.value) }}</strong>
              </span>
              <i class="el-icon-arrow-right overview-item__arrow" />
            </button>
          </div>
        </section>

        <div class="side-stack">
          <section class="dashboard-card mini-card">
            <div class="card-title">
              <div>
                <span class="title-icon"><i class="el-icon-document" /></span>
                <strong>合同执行</strong>
              </div>
              <div class="card-actions">
                <span>{{ contractExecution.activeCount || 0 }} 份执行中</span>
                <el-button text type="primary" @click="go('/contract/contract')">更多</el-button>
              </div>
            </div>
            <div class="contract-execution">
              <div v-for="item in contractItems" :key="item.key" class="execution-item">
                <span class="execution-icon" :class="`execution-icon--${item.tone}`">
                  <i :class="item.icon" />
                </span>
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}<em>份</em></strong>
              </div>
            </div>
          </section>

        </div>
      </div>

      <div class="dashboard-row dashboard-row--middle">
        <section class="dashboard-card chart-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-tickets" /></span>
              <strong>房源概况</strong>
            </div>
            <span class="card-note">共 {{ roomSummary.totalRooms || 0 }} 间</span>
          </div>
          <div ref="roomChart" class="chart chart--donut"></div>
          <div class="room-legend">
            <span v-for="item in roomLegend" :key="item.key">
              <i :style="{ backgroundColor: item.color }"></i>
              <em>{{ item.label }}</em>
              <strong>{{ item.value }}</strong>
            </span>
          </div>
        </section>

        <section class="dashboard-card metrics-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-data-line" /></span>
              <strong>租赁指标</strong>
            </div>
          </div>
          <div class="avg-rent">
            <span>在租实时均价</span>
            <strong>{{ rentMetrics.averageRent || 0 }}</strong>
            <em>元 / m²·月</em>
          </div>
          <div class="rate-list">
            <div v-for="item in rateItems" :key="item.key" class="rate-item">
              <span>{{ item.label }}</span>
              <el-progress
                :percentage="safePercent(item.value)"
                :stroke-width="14"
                :show-text="false"
                :color="item.color"
              />
              <em>{{ item.value || 0 }}%</em>
            </div>
          </div>
        </section>

        <section class="dashboard-card chart-card chart-card--wide">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-warning-outline" /></span>
              <strong>空置预警</strong>
            </div>
            <span class="card-note">当前空置 {{ roomSummary.vacantRooms || 0 }} 间</span>
          </div>
          <div ref="vacancyChart" class="chart chart--bar"></div>
        </section>
      </div>

      <div class="dashboard-row dashboard-row--trends">
        <section class="dashboard-card trend-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-s-data" /></span>
              <strong>合同趋势</strong>
            </div>
            <span class="card-note">近6个月</span>
          </div>
          <div class="trend-summary">
            <div v-for="item in contractTrendSummary" :key="item.label" class="trend-metric">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em v-if="item.caption" :class="item.tone ? `is-${item.tone}` : ''">
                {{ item.caption }}
              </em>
            </div>
          </div>
          <div ref="contractTrendChart" class="chart chart--trend"></div>
        </section>

        <section class="dashboard-card trend-card">
          <div class="card-title">
            <div>
              <span class="title-icon title-icon--green"
                ><i class="el-icon-office-building"
              /></span>
              <strong>房源趋势</strong>
            </div>
            <span class="card-note">近6个月</span>
          </div>
          <div class="trend-summary">
            <div v-for="item in roomTrendSummary" :key="item.label" class="trend-metric">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em v-if="item.caption">{{ item.caption }}</em>
            </div>
          </div>
          <div ref="roomTrendChart" class="chart chart--trend"></div>
        </section>
      </div>

      <div class="dashboard-row dashboard-row--bottom">
        <section class="dashboard-card list-card approval-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-s-check" /></span>
              <strong>进行中的审批</strong>
            </div>
            <el-button text type="primary" @click="go('/plugin/workflow/pages/process/todo')"
              >更多</el-button
            >
          </div>
          <div class="approval-list">
            <div v-for="item in approvalList" :key="item.id" class="approval-item">
              <span class="approval-icon"><i class="el-icon-document-copy" /></span>
              <div>
                <strong>{{ item.title || '-' }}</strong>
                <p>
                  {{ item.flowType || '审批流程' }}
                  <em v-if="item.currentNode">· {{ item.currentNode }}</em>
                </p>
                <time>{{ formatApprovalTime(item.createTime) }}</time>
              </div>
              <el-tag type="primary" effect="plain">{{ item.statusText || '审批中' }}</el-tag>
            </div>
            <div v-if="approvalList.length === 0" class="empty-block">暂无数据</div>
          </div>
        </section>

        <section class="dashboard-card list-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-message-solid" /></span>
              <strong>待发通知单租客</strong>
            </div>
            <el-button text type="primary" @click="go('/contract/payment-notice')">更多</el-button>
          </div>
          <ul class="tenant-list">
            <li v-for="item in noticeTenantList" :key="item.id">
              <span></span>
              <strong :title="item.tenantName || ''">{{ item.tenantName || '-' }}</strong>
              <em>{{ formatMoneyLike(item.amount) }}</em>
            </li>
          </ul>
          <div v-if="noticeTenantList.length === 0" class="empty-block">暂无数据</div>
        </section>

        <section class="dashboard-card list-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-s-shop" /></span>
              <strong>商机提醒</strong>
            </div>
            <el-button text type="primary" @click="go('/settlement/opportunity')">更多</el-button>
          </div>
          <div
            ref="opportunityStatusChart"
            class="chart chart--opportunity-status"
            :class="{ 'chart--opportunity-status-empty': opportunityReminderList.length === 0 }"
          ></div>
          <ul v-if="opportunityReminderList.length" class="opportunity-list">
            <li v-for="item in opportunityReminderList" :key="item.id">
              <strong>{{ item.name || '-' }}</strong>
              <el-tag size="small" type="warning" effect="plain">接触</el-tag>
              <time>{{ item.remindTime || '-' }}</time>
            </li>
          </ul>
          <div v-else class="opportunity-summary">
            <div v-for="item in opportunityMetrics" :key="item.label">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </section>
      </div>
    </div>
  </basic-container>
</template>

<script>
import * as echarts from 'echarts';
import { getEnterpriseDataOverview } from '@/api/business/enterprise-data';

const ROOM_COLORS = ['#ff5b66', '#ffaf34', '#2f8dfd', '#12c6a4', '#7c6ff6'];
const DAY_MS = 24 * 60 * 60 * 1000;

export default {
  name: 'EnterpriseData',
  data() {
    return {
      loading: false,
      deviceTab: 'electric',
      charts: {},
      digitalOverview: [],
      contractExecution: {},
      deviceSummary: [],
      roomSummary: {},
      rentMetrics: {},
      vacancyWarning: [],
      rentalTrend: [],
      contractDealTrend: [],
      approvalList: [],
      noticeTenantList: [],
      opportunityReminderList: [],
      opportunityStatusSummary: [],
    };
  },
  computed: {
    overviewCards() {
      const iconMap = {
        dueReceivableAmount: 'el-icon-download',
        duePayableAmount: 'el-icon-upload2',
        next30ReceivableAmount: 'el-icon-date',
        next30PayableAmount: 'el-icon-timer',
        overdueTenantDebtAmount: 'el-icon-warning-outline',
        dueTenantCount: 'el-icon-user',
        todayOtherReceivableAmount: 'el-icon-wallet',
        todayOtherPayableAmount: 'el-icon-bank-card',
      };
      return this.digitalOverview.map(item => ({
        ...item,
        icon: iconMap[item.key] || 'el-icon-data-analysis',
      }));
    },
    contractItems() {
      return [
        {
          key: 'totalCount',
          label: '合同总数',
          value: this.contractExecution.totalCount || 0,
          tone: 'blue',
          icon: 'el-icon-collection',
        },
        {
          key: 'activeCount',
          label: '执行中',
          value: this.contractExecution.activeCount || 0,
          tone: 'cyan',
          icon: 'el-icon-document',
        },
        {
          key: 'terminatedCount',
          label: '已退租',
          value: this.contractExecution.terminatedCount || 0,
          tone: 'orange',
          icon: 'el-icon-folder-delete',
        },
        {
          key: 'expiredCount',
          label: '已到期',
          value: this.contractExecution.expiredCount || 0,
          tone: 'purple',
          icon: 'el-icon-files',
        },
      ];
    },
    currentDevice() {
      return this.deviceSummary.find(item => item.key === this.deviceTab) || {};
    },
    currentDeviceOnlineRate() {
      const total = Number(this.currentDevice.total) || 0;
      const online = Number(this.currentDevice.online) || 0;
      return total > 0 ? ((online * 100) / total).toFixed(0) : 0;
    },
    roomLegend() {
      return [
        {
          key: 'vacantRooms',
          label: '空置',
          value: this.roomSummary.vacantRooms || 0,
          color: ROOM_COLORS[0],
        },
        {
          key: 'reservedRooms',
          label: '预留',
          value: this.roomSummary.reservedRooms || 0,
          color: ROOM_COLORS[1],
        },
        {
          key: 'pendingRooms',
          label: '待清退/待退出',
          value: this.roomSummary.pendingRooms || 0,
          color: ROOM_COLORS[2],
        },
        {
          key: 'expiringRooms',
          label: '到期预警',
          value: this.roomSummary.expiringRooms || 0,
          color: ROOM_COLORS[3],
        },
        {
          key: 'rentedRooms',
          label: '已出租',
          value: this.roomSummary.rentedRooms || 0,
          color: ROOM_COLORS[4],
        },
      ];
    },
    rateItems() {
      return [
        {
          key: 'rentRate',
          label: '出租率',
          value: this.rentMetrics.rentRate || 0,
          color: '#2f75ff',
        },
        {
          key: 'vacancyRate',
          label: '空置率',
          value: this.rentMetrics.vacancyRate || 0,
          color: '#ff8f3d',
        },
        {
          key: 'billingRate',
          label: '计租率',
          value: this.rentMetrics.billingRate || 0,
          color: '#12a594',
        },
      ];
    },
    contractTrendSummary() {
      const current = this.trendNumber(this.contractDealTrend, 'dealCount');
      const previous = this.trendNumber(this.contractDealTrend, 'dealCount', 1);
      const total = this.contractDealTrend.reduce(
        (sum, item) => sum + (Number(item.dealCount) || 0),
        0
      );
      const delta = current - previous;
      return [
        { label: '本月成交', value: `${current}份` },
        { label: '近半年成交', value: `${total}份` },
        {
          label: '较上月',
          value: `${delta > 0 ? '+' : ''}${delta}份`,
          caption: delta === 0 ? '持平' : delta > 0 ? '增长' : '回落',
          tone: delta > 0 ? 'up' : delta < 0 ? 'down' : 'flat',
        },
      ];
    },
    roomTrendSummary() {
      const currentRate = this.trendNumber(this.rentalTrend, 'rentRate');
      const currentNewRent = this.trendNumber(this.rentalTrend, 'newRentCount');
      const averageRate = this.rentalTrend.length
        ? this.rentalTrend.reduce((sum, item) => sum + (Number(item.rentRate) || 0), 0) /
          this.rentalTrend.length
        : 0;
      return [
        { label: '当前出租率', value: `${currentRate.toFixed(1)}%` },
        { label: '近半年平均', value: `${averageRate.toFixed(1)}%`, caption: '出租率' },
        { label: '本月新增出租', value: `${currentNewRent}间` },
      ];
    },
    opportunityMetrics() {
      const summary = this.opportunityStatusSummary.reduce((result, item) => {
        result[item.status] = Number(item.count) || 0;
        return result;
      }, {});
      const total = Object.values(summary).reduce((sum, count) => sum + count, 0);
      const completed = summary.DEAL || 0;
      const lost = summary.LOST || 0;
      return [
        { label: '商机总数', value: total },
        { label: '跟进中', value: Math.max(total - completed - lost, 0) },
        { label: '达成意向', value: completed },
      ];
    },
  },
  mounted() {
    this.loadData();
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.resizeCharts);
    Object.values(this.charts).forEach(chart => chart && chart.dispose());
  },
  methods: {
    loadData() {
      this.loading = true;
      getEnterpriseDataOverview()
        .then(res => {
          const data = res.data.data || {};
          this.digitalOverview = data.digitalOverview || [];
          this.contractExecution = data.contractExecution || {};
          this.deviceSummary = data.deviceSummary || [];
          this.deviceTab = (this.deviceSummary[0] && this.deviceSummary[0].key) || 'electric';
          this.roomSummary = data.roomSummary || {};
          this.rentMetrics = data.rentMetrics || {};
          this.vacancyWarning = data.vacancyWarning || [];
          this.rentalTrend = data.rentalTrend || [];
          this.contractDealTrend = data.contractDealTrend || [];
          this.approvalList = data.approvalList || [];
          this.noticeTenantList = data.noticeTenantList || [];
          this.opportunityReminderList = data.opportunityReminderList || [];
          this.opportunityStatusSummary = data.opportunityStatusSummary || [];
          this.$nextTick(this.renderCharts);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    renderCharts() {
      this.renderRoomChart();
      this.renderVacancyChart();
      this.renderContractTrendChart();
      this.renderRoomTrendChart();
      this.renderOpportunityStatusChart();
    },
    renderRoomChart() {
      const chart = this.getChart('roomChart');
      const data = this.roomLegend.map(item => ({
        name: item.label,
        value: item.value,
      }));
      chart.setOption({
        color: ROOM_COLORS,
        tooltip: { trigger: 'item' },
        series: [
          {
            type: 'pie',
            radius: ['58%', '76%'],
            center: ['50%', '50%'],
            avoidLabelOverlap: true,
            label: {
              show: true,
              position: 'center',
              formatter: `{label|房源总数}\n{value|${this.roomSummary.totalRooms || 0}间}`,
              rich: {
                label: { color: '#909399', fontSize: 12, lineHeight: 22 },
                value: { color: '#303133', fontSize: 22, fontWeight: 700, lineHeight: 30 },
              },
            },
            labelLine: { show: false },
            data,
          },
        ],
      });
    },
    renderVacancyChart() {
      const chart = this.getChart('vacancyChart');
      const labels = this.vacancyWarning.map(item => item.label);
      const values = this.vacancyWarning.map(item => Number(item.value) || 0);
      chart.setOption({
        color: ['#2f75ff'],
        grid: { left: 38, right: 18, top: 44, bottom: 28 },
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: labels,
          axisTick: { show: false },
          axisLine: { lineStyle: { color: '#d7dce5' } },
          axisLabel: { color: '#8a94a6' },
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          name: '单位/间',
          nameTextStyle: { color: '#8a94a6', align: 'left' },
          splitLine: { lineStyle: { type: 'dashed', color: '#e8edf3' } },
          axisLabel: { color: '#8a94a6' },
        },
        series: [
          {
            name: '空置房间',
            type: 'bar',
            barMaxWidth: 26,
            label: {
              show: true,
              position: 'top',
              color: '#606266',
              fontSize: 12,
            },
            data: values,
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: '#2f75ff',
            },
          },
        ],
      });
    },
    renderContractTrendChart() {
      const chart = this.getChart('contractTrendChart');
      const labels = this.contractDealTrend.map(item => item.month);
      const values = this.contractDealTrend.map(item => Number(item.dealCount) || 0);
      chart.setOption({
        color: ['#2f75ff'],
        grid: { left: 42, right: 18, top: 34, bottom: 30 },
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => `${value}份`,
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: labels,
          axisTick: { show: false },
          axisLine: { lineStyle: { color: '#d7dce5' } },
          axisLabel: { color: '#8a94a6' },
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          name: '单位/份',
          nameTextStyle: { color: '#8a94a6', align: 'left' },
          splitLine: { lineStyle: { type: 'dashed', color: '#e8edf3' } },
          axisLabel: { color: '#8a94a6' },
        },
        series: [
          {
            name: '成交合同',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 7,
            lineStyle: { width: 3 },
            itemStyle: { borderWidth: 2, borderColor: '#fff' },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(47,117,255,0.22)' },
                { offset: 1, color: 'rgba(47,117,255,0.02)' },
              ]),
            },
            data: values,
          },
        ],
      });
    },
    renderRoomTrendChart() {
      const chart = this.getChart('roomTrendChart');
      const labels = this.rentalTrend.map(item => item.month);
      const rentRates = this.rentalTrend.map(item => Number(item.rentRate) || 0);
      const newRentCounts = this.rentalTrend.map(item => Number(item.newRentCount) || 0);
      chart.setOption({
        color: ['#12a594', '#ffaf34'],
        grid: { left: 42, right: 46, top: 48, bottom: 30 },
        tooltip: { trigger: 'axis' },
        legend: {
          top: 0,
          right: 8,
          itemWidth: 14,
          itemHeight: 8,
          textStyle: { color: '#606266', fontSize: 12 },
        },
        xAxis: {
          type: 'category',
          data: labels,
          axisTick: { show: false },
          axisLine: { lineStyle: { color: '#d7dce5' } },
          axisLabel: { color: '#8a94a6' },
        },
        yAxis: [
          {
            type: 'value',
            min: 0,
            max: 100,
            name: '出租率/%',
            nameTextStyle: { color: '#8a94a6', align: 'left' },
            splitLine: { lineStyle: { type: 'dashed', color: '#e8edf3' } },
            axisLabel: { color: '#8a94a6', formatter: '{value}%' },
          },
          {
            type: 'value',
            minInterval: 1,
            name: '新增/间',
            nameTextStyle: { color: '#8a94a6', align: 'right' },
            splitLine: { show: false },
            axisLabel: { color: '#8a94a6' },
          },
        ],
        series: [
          {
            name: '出租率',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 7,
            lineStyle: { width: 3 },
            itemStyle: { borderWidth: 2, borderColor: '#fff' },
            data: rentRates,
          },
          {
            name: '新增出租',
            type: 'bar',
            yAxisIndex: 1,
            barMaxWidth: 22,
            itemStyle: { borderRadius: [4, 4, 0, 0] },
            data: newRentCounts,
          },
        ],
      });
    },
    renderOpportunityStatusChart() {
      const chart = this.getChart('opportunityStatusChart');
      const statusMap = {
        DRAFT: '初步沟通',
        AUDIT: '初步沟通',
        LEAD: '潜在线索',
        INITIAL: '初步沟通',
        DEEP: '深入洽谈',
        DEAL: '达成意向',
        LOST: '流失',
      };
      const counts = this.opportunityStatusSummary.reduce((result, item) => {
        const label = statusMap[item.status] || '其他';
        result[label] = (result[label] || 0) + (Number(item.count) || 0);
        return result;
      }, {});
      const total = Object.values(counts).reduce((sum, count) => sum + count, 0);
      chart.setOption({
        color: ['#2f8dfd', '#12a594', '#ffaf34', '#7c6ff6', '#ff5b66', '#a0a8b8'],
        tooltip: { trigger: 'item' },
        legend: { bottom: 0, type: 'scroll', textStyle: { color: '#8a94a6', fontSize: 11 } },
        series: [
          {
            type: 'pie',
            radius: ['50%', '76%'],
            center: ['50%', '43%'],
            label: {
              show: true,
              position: 'center',
              formatter: `{label|商机总数}\n{value|${total}}`,
              rich: {
                label: { color: '#909399', fontSize: 12, lineHeight: 20 },
                value: { color: '#303133', fontSize: 22, fontWeight: 700, lineHeight: 28 },
              },
            },
            labelLine: { show: false },
            data: Object.keys(counts).map(name => ({ name, value: counts[name] })),
          },
        ],
      });
    },
    getChart(refName) {
      if (!this.charts[refName]) {
        this.charts[refName] = echarts.init(this.$refs[refName]);
      }
      return this.charts[refName];
    },
    resizeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.resize());
    },
    safePercent(value) {
      const percent = Number(value) || 0;
      return Math.max(0, Math.min(100, percent));
    },
    trendNumber(source, key, offset = 0) {
      if (!Array.isArray(source) || source.length <= offset) return 0;
      return Number(source[source.length - 1 - offset]?.[key]) || 0;
    },
    formatMoneyLike(value) {
      const num = Number(value || 0);
      return Number.isInteger(num) ? String(num) : num.toFixed(2);
    },
    formatApprovalTime(value) {
      if (!value) return '-';
      const normalized = String(value).replace('T', ' ');
      const parts = normalized.trim().split(/\s+/, 2);
      if (parts.length < 2) return parts[0] || '-';
      return `${parts[0]}\n${parts[1]}`;
    },
    openOverviewDetail(item) {
      if (!item || !item.key) return;
      const today = this.formatDate(new Date());
      const tomorrow = this.formatDate(new Date(Date.now() + DAY_MS));
      const next30Days = this.formatDate(new Date(Date.now() + 30 * DAY_MS));
      const unsettled = 'unsettled';
      const detailMap = {
        dueReceivableAmount: {
          path: '/finance/bills-all',
          query: { direction: 'receivable', hideFuture: 'true', settleStatus: unsettled },
        },
        duePayableAmount: {
          path: '/finance/bills-all',
          query: { direction: 'payable', hideFuture: 'true', settleStatus: unsettled },
        },
        next30ReceivableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'receivable',
            deadlineStartDate: tomorrow,
            deadlineEndDate: next30Days,
            settleStatus: unsettled,
          },
        },
        next30PayableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'payable',
            deadlineStartDate: tomorrow,
            deadlineEndDate: next30Days,
            settleStatus: unsettled,
          },
        },
        overdueTenantDebtAmount: {
          path: '/finance/bills-overdue',
          query: { settleStatus: unsettled },
        },
        dueTenantCount: {
          path: '/finance/bills-all',
          query: { direction: 'receivable', hideFuture: 'true', settleStatus: unsettled },
        },
        todayOtherReceivableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'receivable',
            deadlineStartDate: today,
            deadlineEndDate: today,
            settleStatus: unsettled,
          },
        },
        todayOtherPayableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'payable',
            deadlineStartDate: today,
            deadlineEndDate: today,
            settleStatus: unsettled,
          },
        },
      };
      const target = detailMap[item.key];
      if (!target) return;
      this.$router.push(target);
    },
    formatDate(date) {
      if (!(date instanceof Date) || Number.isNaN(date.getTime())) {
        return '';
      }
      const year = date.getFullYear();
      const month = `${date.getMonth() + 1}`.padStart(2, '0');
      const day = `${date.getDate()}`.padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    go(path) {
      this.$router.push(path);
    },
  },
};
</script>

<style scoped>
.enterprise-data-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  box-sizing: border-box;
  padding: 14px 18px;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  background: #fff;
}

.dashboard-header__title strong,
.dashboard-header__title span {
  display: block;
  letter-spacing: 0;
}

.dashboard-header__title strong {
  color: #303133;
  font-size: 18px;
  line-height: 1.4;
}

.dashboard-header__title span {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
}

.dashboard-header__actions,
.card-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-actions > span {
  color: #909399;
  font-size: 12px;
}

.dashboard-row {
  display: grid;
  gap: 16px;
}

.dashboard-row--top {
  grid-template-columns: minmax(0, 1.8fr) minmax(380px, 0.8fr);
}

.dashboard-row--middle {
  grid-template-columns: minmax(300px, 0.8fr) minmax(320px, 0.9fr) minmax(480px, 1.4fr);
}

.dashboard-row--trends {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-row--bottom {
  grid-template-columns: repeat(3, minmax(300px, 1fr));
}

.dashboard-card {
  min-width: 0;
  border: 1px solid #e8edf3;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(30, 64, 120, 0.03);
}

.overview-card,
.chart-card,
.metrics-card,
.trend-card,
.list-card {
  padding: 18px;
}

.side-stack {
  min-width: 0;
}

.mini-card {
  display: flex;
  height: 100%;
  box-sizing: border-box;
  flex-direction: column;
  padding: 18px 20px;
}

.overview-card {
  padding: 16px;
}

.overview-card .card-title {
  margin-bottom: 12px;
}

.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 36px;
  margin-bottom: 16px;
}

.card-title > div {
  display: flex;
  align-items: center;
  min-width: 0;
}

.card-title strong {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin-right: 10px;
  border-radius: 8px;
  background: #409eff;
  color: #fff;
  font-size: 16px;
}

.title-icon--green {
  background: #12a594;
}

.card-note {
  color: #909399;
  font-size: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.overview-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) 14px;
  align-items: center;
  gap: 8px;
  min-height: 76px;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #edf1f6;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.overview-item:hover {
  border-color: #c9dcff;
  box-shadow: 0 6px 14px rgba(47, 117, 255, 0.08);
}

.overview-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #eaf2ff;
  color: #2f75ff;
  font-size: 16px;
}

.overview-item--green .overview-item__icon,
.overview-item--cyan .overview-item__icon {
  background: #e8f8f2;
  color: #12a594;
}

.overview-item--orange .overview-item__icon,
.overview-item--amber .overview-item__icon {
  background: #fff3e5;
  color: #e8892f;
}

.overview-item--red .overview-item__icon {
  background: #fff0ef;
  color: #f56c6c;
}

.overview-item--purple .overview-item__icon {
  background: #f2efff;
  color: #7c6ff6;
}

.overview-item__content {
  min-width: 0;
}

.overview-item__content em,
.overview-item__content strong {
  display: block;
  letter-spacing: 0;
}

.overview-item__content em {
  overflow: hidden;
  color: #606266;
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-item__content strong {
  margin-top: 4px;
  overflow: hidden;
  color: #303133;
  font-size: 17px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-item__arrow {
  color: #c0c4cc;
  font-size: 13px;
}

.contract-execution {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: center;
  flex: 1;
  gap: 14px;
  padding-top: 4px;
}

.execution-item,
.device-metric {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.execution-icon,
.metric-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 10px;
  font-size: 20px;
}

.execution-icon--blue,
.metric-icon--blue {
  background: #eaf2ff;
  color: #2f75ff;
}

.execution-icon--cyan {
  background: #e9f8ff;
  color: #26a7f2;
}

.execution-icon--orange {
  background: #fff3df;
  color: #ffb13d;
}

.execution-icon--purple {
  background: #f2ecff;
  color: #8064f4;
}

.metric-icon--green {
  background: #e7faf0;
  color: #21c88a;
}

.metric-icon--red {
  background: #fff0ee;
  color: #ff766b;
}

.execution-item > span:not(.execution-icon),
.device-metric > span:not(.metric-icon) {
  color: #909399;
  font-size: 12px;
  letter-spacing: 0;
}

.execution-item > strong,
.device-metric > strong {
  color: #303133;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
}

.execution-item em,
.device-metric em {
  margin-left: 3px;
  color: #909399;
  font-size: 12px;
  font-weight: 400;
  font-style: normal;
}

.device-tabs {
  margin-top: -8px;
}

.device-tabs :deep(.el-tabs__nav-scroll) {
  width: 100%;
}

.device-tabs :deep(.el-tabs__nav) {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  float: none;
  width: 100%;
}

.device-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #ebeef5;
}

.device-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.device-tabs :deep(.el-tabs__item) {
  height: 32px;
  justify-content: center;
  padding: 0;
  color: #606266;
  font-size: 13px;
  line-height: 32px;
  text-align: center;
  letter-spacing: 0;
}

.device-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
  background: #2f75ff;
}

.device-tabs :deep(.el-tabs__item.is-active) {
  color: #2f75ff;
  font-weight: 600;
}

.device-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
  gap: 18px;
  padding: 2px 18px 0;
}

.chart {
  width: 100%;
  height: 240px;
}

.chart--donut {
  height: 244px;
}

.chart--bar {
  height: 278px;
}

.chart--trend {
  height: 286px;
}

.room-legend {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
  padding-top: 4px;
}

.room-legend span {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
  min-width: 0;
  padding: 7px 9px;
  border-radius: 4px;
  background: #f7f9fc;
}

.room-legend i {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.room-legend em {
  overflow: hidden;
  color: #606266;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: 0;
}

.room-legend strong {
  color: #303133;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
}

.metrics-card {
  display: flex;
  flex-direction: column;
}

.avg-rent {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 126px;
  margin-bottom: 24px;
  border: 1px solid #edf1f6;
  border-radius: 4px;
  background: #f8faff;
}

.avg-rent strong {
  margin-top: 10px;
  color: #2f75ff;
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: 0;
}

.avg-rent span {
  color: #606266;
  font-size: 13px;
  letter-spacing: 0;
}

.avg-rent em {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
  font-style: normal;
  letter-spacing: 0;
}

.rate-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.rate-item {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) 54px;
  align-items: center;
  gap: 12px;
}

.rate-item span,
.rate-item em {
  color: #606266;
  font-size: 13px;
  font-style: normal;
}

.rate-item em {
  text-align: right;
}

.trend-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin: 0 0 14px;
  padding: 12px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.trend-metric {
  min-width: 0;
  padding: 0 18px;
}

.trend-metric:first-child {
  padding-left: 0;
}

.trend-metric:last-child {
  padding-right: 0;
}

.trend-metric + .trend-metric {
  border-left: 1px solid #ebeef5;
}

.trend-metric span,
.trend-metric strong,
.trend-metric em {
  display: block;
  letter-spacing: 0;
}

.trend-metric span {
  overflow: hidden;
  color: #909399;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.trend-metric strong {
  margin-top: 7px;
  color: #303133;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.trend-metric em {
  min-height: 18px;
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.trend-metric em.is-up {
  color: #12a594;
}

.trend-metric em.is-down {
  color: #f56c6c;
}

.trend-metric em.is-flat {
  color: #909399;
}

.list-card {
  min-height: 370px;
}

.approval-list {
  display: flex;
  max-height: 334px;
  flex-direction: column;
  gap: 16px;
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 6px;
  scrollbar-color: #cbd7e8 transparent;
  scrollbar-width: thin;
}

.approval-list::-webkit-scrollbar {
  width: 6px;
}

.approval-list::-webkit-scrollbar-track {
  background: transparent;
}

.approval-list::-webkit-scrollbar-thumb {
  border-radius: 6px;
  background: #cbd7e8;
}

.approval-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 16px;
  border-radius: 4px;
  background: #f7f9fc;
}

.approval-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #e6f2ff;
  color: #2f75ff;
}

.approval-item strong {
  display: block;
  overflow: visible;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  white-space: normal;
  word-break: break-all;
}

.approval-item p {
  margin: 10px 0;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.5;
}

.approval-item p em {
  margin-left: 8px;
  color: #303133;
  font-style: normal;
}

.approval-item time {
  display: block;
  min-width: 106px;
  box-sizing: border-box;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fff;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.35;
  white-space: pre-line;
}

.tenant-list,
.opportunity-list {
  max-height: 334px;
  margin: 0;
  padding: 0;
  overflow: hidden;
  list-style: none;
}

.chart--opportunity-status {
  height: 220px;
  margin: -4px 0 6px;
}

.chart--opportunity-status-empty {
  height: 270px;
  margin-bottom: 0;
}

.opportunity-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding-top: 14px;
  border-top: 1px solid #ebeef5;
}

.opportunity-summary > div {
  min-width: 0;
  text-align: center;
}

.opportunity-summary > div + div {
  border-left: 1px solid #ebeef5;
}

.opportunity-summary strong,
.opportunity-summary span {
  display: block;
  letter-spacing: 0;
}

.opportunity-summary strong {
  color: #303133;
  font-size: 18px;
  line-height: 1.3;
}

.opportunity-summary span {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
}

.tenant-list li,
.opportunity-list li {
  display: grid;
  align-items: center;
  min-height: 42px;
  color: #8a94a6;
  font-size: 13px;
}

.tenant-list li {
  grid-template-columns: 14px minmax(0, 1fr) 72px;
  gap: 6px;
}

.tenant-list span {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #dce3ef;
}

.tenant-list strong,
.opportunity-list strong {
  overflow: hidden;
  color: #8a94a6;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-list strong {
  overflow: visible;
  min-width: 0;
  color: #7b8aa5;
  font-size: 13px;
  letter-spacing: 0;
  text-overflow: clip;
  white-space: normal;
  word-break: break-all;
  line-height: 1.4;
}

.tenant-list em {
  color: #8a94a6;
  font-style: normal;
  text-align: right;
}

.opportunity-list li {
  grid-template-columns: minmax(0, 1fr) 54px 150px;
  gap: 12px;
}

.opportunity-list time {
  text-align: right;
}

.empty-block {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  color: #a8b1c2;
  font-size: 13px;
}

@media (max-width: 1500px) {
  .dashboard-row--top,
  .dashboard-row--middle,
  .dashboard-row--trends,
  .dashboard-row--bottom {
    grid-template-columns: 1fr;
  }

  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .dashboard-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .dashboard-header__actions {
    justify-content: space-between;
    width: 100%;
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }

  .contract-execution {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .device-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10px;
    padding-right: 0;
    padding-left: 0;
  }

  .room-legend {
    grid-template-columns: 1fr;
  }

  .trend-metric {
    padding: 0 12px;
  }

  .opportunity-list li {
    grid-template-columns: minmax(0, 1fr) 54px;
  }

  .opportunity-list time {
    grid-column: 1 / -1;
    text-align: left;
  }
}

@media (max-width: 560px) {
  .contract-execution,
  .device-summary,
  .trend-summary {
    grid-template-columns: 1fr;
  }

  .execution-item,
  .device-metric {
    display: grid;
    grid-template-columns: 44px minmax(0, 1fr) auto;
    justify-items: start;
    text-align: left;
  }

  .execution-item > strong,
  .device-metric > strong {
    grid-column: 3;
    grid-row: 1;
    align-self: center;
  }

  .execution-item > span:not(.execution-icon),
  .device-metric > span:not(.metric-icon) {
    grid-column: 2;
    grid-row: 1;
    align-self: center;
  }

  .trend-metric,
  .trend-metric:first-child,
  .trend-metric:last-child {
    padding: 10px 0;
  }

  .trend-metric + .trend-metric {
    border-top: 1px solid #ebeef5;
    border-left: 0;
  }
}
</style>
