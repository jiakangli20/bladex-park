<template>
  <basic-container>
    <div v-loading="loading" class="enterprise-data-page">
      <div class="dashboard-row dashboard-row--top">
        <section class="dashboard-card overview-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-menu" /></span>
              <strong>数字概览</strong>
            </div>
          </div>
          <div class="overview-grid">
            <button
              v-for="item in digitalOverview"
              :key="item.key"
              type="button"
              class="overview-item"
              :class="`overview-item--${item.tone || 'blue'}`"
              @click="openOverviewDetail(item)"
            >
              <span>{{ item.label }}</span>
              <strong>{{ formatMoneyLike(item.value) }}</strong>
              <em>详情&gt;</em>
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
              <el-button text type="primary" @click="go('/contract/contract')">更多</el-button>
            </div>
            <div class="contract-execution">
              <div v-for="item in contractItems" :key="item.key" class="execution-item">
                <span class="execution-icon" :class="`execution-icon--${item.tone}`">
                  <i :class="item.icon" />
                </span>
                <p>{{ item.label }}<strong>{{ item.value }}</strong><em>个</em></p>
              </div>
            </div>
          </section>

          <section class="dashboard-card mini-card">
            <div class="card-title">
              <div>
                <span class="title-icon"><i class="el-icon-cpu" /></span>
                <strong>智能设备</strong>
              </div>
              <el-button text type="primary" disabled>更多</el-button>
            </div>
            <el-tabs v-model="deviceTab" class="device-tabs">
              <el-tab-pane
                v-for="item in deviceSummary"
                :key="item.key"
                :label="item.label"
                :name="item.key"
              />
            </el-tabs>
            <div class="device-summary">
              <div class="device-metric">
                <span class="metric-icon metric-icon--blue"><i class="el-icon-set-up" /></span>
                <p>数量<strong>{{ currentDevice.total || 0 }}</strong><em>个</em></p>
              </div>
              <div class="device-metric">
                <span class="metric-icon metric-icon--green"><i class="el-icon-connection" /></span>
                <p>在线<strong>{{ currentDevice.online || 0 }}</strong><em>个</em></p>
              </div>
              <div class="device-metric">
                <span class="metric-icon metric-icon--red"><i class="el-icon-close" /></span>
                <p>离线<strong>{{ currentDevice.offline || 0 }}</strong><em>个</em></p>
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
              <strong>房源概括</strong>
            </div>
          </div>
          <div ref="roomChart" class="chart chart--donut"></div>
          <div class="room-legend">
            <span v-for="item in roomLegend" :key="item.key">
              <i :style="{ backgroundColor: item.color }"></i>{{ item.label }}
            </span>
          </div>
        </section>

        <section class="dashboard-card metrics-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-data-line" /></span>
              <strong>利率/均价</strong>
            </div>
          </div>
          <div class="avg-rent">
            <strong>{{ rentMetrics.averageRent || 0 }}</strong>
            <span>在租实时均价(m²·月)</span>
          </div>
          <div class="rate-list">
            <div v-for="item in rateItems" :key="item.key" class="rate-item">
              <span>{{ item.label }}</span>
              <el-progress
                :percentage="safePercent(item.value)"
                :stroke-width="14"
                :show-text="false"
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
            <span class="legend-dot"><i></i>空置房间</span>
          </div>
          <div ref="vacancyChart" class="chart chart--bar"></div>
        </section>
      </div>

      <div class="dashboard-row dashboard-row--bottom">
        <section class="dashboard-card trend-card">
          <div class="card-title card-title--tabs">
            <div>
              <span class="title-icon"><i class="el-icon-s-data" /></span>
              <button
                type="button"
                :class="{ active: trendType === 'rent' }"
                @click="switchTrend('rent')"
              >
                近半年出租率走势
              </button>
              <button
                type="button"
                :class="{ active: trendType === 'contract' }"
                @click="switchTrend('contract')"
              >
                近半年合同成交走势
              </button>
            </div>
          </div>
          <div ref="trendChart" class="chart chart--line"></div>
        </section>

        <section class="dashboard-card list-card approval-card">
          <div class="card-title">
            <div>
              <span class="title-icon"><i class="el-icon-s-check" /></span>
              <strong>进行中的审批</strong>
            </div>
            <el-button text type="primary" @click="go('/plugin/workflow/pages/process/todo')">更多</el-button>
          </div>
          <div class="approval-list">
            <div v-for="item in approvalList" :key="item.id" class="approval-item">
              <span class="approval-icon"><i class="el-icon-document-copy" /></span>
              <div>
                <strong>{{ item.title || '-' }}</strong>
                <p>流程类型 <em>{{ item.flowType || '-' }}</em></p>
                <time>{{ item.createTime || '-' }}</time>
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
            <el-button text type="primary" @click="go('/finance/payment-notice')">更多</el-button>
          </div>
          <ul class="tenant-list">
            <li v-for="item in noticeTenantList" :key="item.id">
              <span></span>
              <strong>{{ item.tenantName || '-' }}</strong>
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
            <el-button text type="primary" @click="go('/business/opportunity')">更多</el-button>
          </div>
          <ul class="opportunity-list">
            <li v-for="item in opportunityReminderList" :key="item.id">
              <strong>{{ item.name || '-' }}</strong>
              <el-tag size="small" type="warning" effect="plain">接触</el-tag>
              <time>{{ item.remindTime || '-' }}</time>
            </li>
          </ul>
          <div v-if="opportunityReminderList.length === 0" class="empty-block">暂无数据</div>
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
      trendType: 'rent',
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
    };
  },
  computed: {
    contractItems() {
      return [
        { key: 'totalCount', label: '数量', value: this.contractExecution.totalCount || 0, tone: 'blue', icon: 'el-icon-collection' },
        { key: 'activeCount', label: '执行中', value: this.contractExecution.activeCount || 0, tone: 'cyan', icon: 'el-icon-document' },
        { key: 'terminatedCount', label: '已退租', value: this.contractExecution.terminatedCount || 0, tone: 'orange', icon: 'el-icon-folder-delete' },
        { key: 'expiredCount', label: '已到期', value: this.contractExecution.expiredCount || 0, tone: 'purple', icon: 'el-icon-files' },
      ];
    },
    currentDevice() {
      return this.deviceSummary.find(item => item.key === this.deviceTab) || {};
    },
    roomLegend() {
      return [
        { key: 'vacantRooms', label: '空置', value: this.roomSummary.vacantRooms || 0, color: ROOM_COLORS[0] },
        { key: 'lockedRooms', label: '已锁定', value: this.roomSummary.lockedRooms || 0, color: ROOM_COLORS[1] },
        { key: 'rentedRooms', label: '已出租', value: this.roomSummary.rentedRooms || 0, color: ROOM_COLORS[2] },
        { key: 'overdueRooms', label: '已逾期', value: this.roomSummary.overdueRooms || 0, color: ROOM_COLORS[3] },
        { key: 'expiredRooms', label: '已到期', value: this.roomSummary.expiredRooms || 0, color: ROOM_COLORS[4] },
      ];
    },
    rateItems() {
      return [
        { key: 'rentRate', label: '出租率', value: this.rentMetrics.rentRate || 0 },
        { key: 'vacancyRate', label: '空置率', value: this.rentMetrics.vacancyRate || 0 },
        { key: 'billingRate', label: '计租率', value: this.rentMetrics.billingRate || 0 },
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
          this.$nextTick(this.renderCharts);
        })
        .finally(() => {
          this.loading = false;
        });
    },
    renderCharts() {
      this.renderRoomChart();
      this.renderVacancyChart();
      this.renderTrendChart();
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
              formatter: `总房间数\n${this.roomSummary.totalRooms || 0}`,
              color: '#303133',
              lineHeight: 22,
              fontSize: 14,
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
        grid: { left: 38, right: 18, top: 30, bottom: 28 },
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
            barWidth: 20,
            data: values,
            itemStyle: {
              borderRadius: [12, 12, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#2f75ff' },
                { offset: 1, color: 'rgba(47,117,255,0.08)' },
              ]),
            },
          },
        ],
      });
    },
    renderTrendChart() {
      const chart = this.getChart('trendChart');
      const source = this.trendType === 'rent' ? this.rentalTrend : this.contractDealTrend;
      const labels = source.map(item => item.month);
      const values = source.map(item => Number(this.trendType === 'rent' ? item.rentRate : item.dealCount) || 0);
      chart.setOption({
        color: ['#5b6fd6'],
        grid: { left: 36, right: 18, top: 34, bottom: 30 },
        tooltip: {
          trigger: 'axis',
          valueFormatter: value => (this.trendType === 'rent' ? `${value}%` : `${value}个`),
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
          minInterval: this.trendType === 'rent' ? 0 : 1,
          name: this.trendType === 'rent' ? '单位/%' : '单位/份',
          nameTextStyle: { color: '#8a94a6', align: 'left' },
          splitLine: { lineStyle: { type: 'dashed', color: '#e8edf3' } },
          axisLabel: { color: '#8a94a6' },
        },
        series: [
          {
            name: this.trendType === 'rent' ? '出租率' : '成交合同',
            type: 'line',
            smooth: true,
            symbolSize: 6,
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(91,111,214,0.28)' },
                { offset: 1, color: 'rgba(91,111,214,0)' },
              ]),
            },
            data: values,
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
    switchTrend(type) {
      this.trendType = type;
      this.$nextTick(this.renderTrendChart);
    },
    resizeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.resize());
    },
    safePercent(value) {
      const percent = Number(value) || 0;
      return Math.max(0, Math.min(100, percent));
    },
    formatMoneyLike(value) {
      const num = Number(value || 0);
      return Number.isInteger(num) ? String(num) : num.toFixed(2);
    },
    openOverviewDetail(item) {
      if (!item || !item.key) return;
      const today = this.formatDate(new Date());
      const tomorrow = this.formatDate(new Date(Date.now() + DAY_MS));
      const next30Days = this.formatDate(new Date(Date.now() + 30 * DAY_MS));
      const detailMap = {
        dueReceivableAmount: {
          path: '/finance/bills-all',
          query: { direction: 'receivable', hideFuture: 'true', settleStatus: 'unsettled' },
        },
        duePayableAmount: {
          path: '/finance/bills-all',
          query: { direction: 'payable', hideFuture: 'true', settleStatus: 'unsettled' },
        },
        next30ReceivableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'receivable',
            deadlineStartDate: tomorrow,
            deadlineEndDate: next30Days,
            settleStatus: 'unsettled',
          },
        },
        next30PayableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'payable',
            deadlineStartDate: tomorrow,
            deadlineEndDate: next30Days,
            settleStatus: 'unsettled',
          },
        },
        overdueTenantDebtAmount: {
          path: '/finance/bills-overdue',
        },
        dueTenantCount: {
          path: '/finance/bills-all',
          query: { direction: 'receivable', hideFuture: 'true', settleStatus: 'unsettled' },
        },
        todayOtherReceivableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'receivable',
            deadlineStartDate: today,
            deadlineEndDate: today,
            settleStatus: 'unsettled',
          },
        },
        todayOtherPayableAmount: {
          path: '/finance/bills-all',
          query: {
            direction: 'payable',
            deadlineStartDate: today,
            deadlineEndDate: today,
            settleStatus: 'unsettled',
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
      this.$router.push({ path });
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

.dashboard-row {
  display: grid;
  gap: 16px;
}

.dashboard-row--top {
  grid-template-columns: minmax(0, 1.55fr) minmax(420px, 0.95fr);
}

.dashboard-row--middle {
  grid-template-columns: minmax(300px, 0.75fr) minmax(420px, 1fr) minmax(420px, 1fr);
}

.dashboard-row--bottom {
  grid-template-columns: minmax(400px, 1.2fr) minmax(300px, 0.8fr) minmax(300px, 0.9fr) minmax(300px, 0.9fr);
}

.dashboard-card {
  min-width: 0;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 4px 16px rgba(30, 64, 120, 0.04);
}

.overview-card,
.chart-card,
.metrics-card,
.trend-card,
.list-card {
  padding: 18px;
}

.side-stack {
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 16px;
}

.mini-card {
  padding: 18px 20px;
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
  border-radius: 9px;
  background: #409eff;
  color: #fff;
  font-size: 16px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-item {
  position: relative;
  min-height: 130px;
  overflow: hidden;
  padding: 16px;
  border: 0;
  border-radius: 10px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.overview-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(27, 64, 116, 0.08);
}

.overview-item::before,
.overview-item::after {
  position: absolute;
  right: -4px;
  bottom: 0;
  display: block;
  width: 54px;
  height: 54px;
  border-radius: 12px 0 10px 0;
  content: '';
  opacity: 0.72;
}

.overview-item::after {
  right: 24px;
  width: 30px;
  height: 30px;
  opacity: 0.45;
}

.overview-item span,
.overview-item strong,
.overview-item em {
  position: relative;
  z-index: 1;
  display: block;
}

.overview-item span {
  color: #303133;
  font-size: 14px;
  line-height: 1.4;
}

.overview-item strong {
  margin-top: 28px;
  color: #303133;
  font-size: 19px;
  font-weight: 700;
}

.overview-item em {
  margin-top: 22px;
  color: #2f75ff;
  font-size: 12px;
  font-style: normal;
}

.overview-item--blue {
  background: #e8f3ff;
}

.overview-item--blue::before,
.overview-item--blue::after {
  background: #409eff;
}

.overview-item--green {
  background: #e4f8ed;
}

.overview-item--green::before,
.overview-item--green::after {
  background: #30c78d;
}

.overview-item--orange,
.overview-item--amber {
  background: #fff3e7;
}

.overview-item--orange::before,
.overview-item--orange::after,
.overview-item--amber::before,
.overview-item--amber::after {
  background: #ffb45b;
}

.overview-item--red {
  background: #ffe9e8;
}

.overview-item--red::before,
.overview-item--red::after {
  background: #ff6f67;
}

.overview-item--purple {
  background: #f4eefc;
}

.overview-item--purple::before,
.overview-item--purple::after {
  background: #a579ef;
}

.overview-item--sky {
  background: #e7f3ff;
}

.overview-item--sky::before,
.overview-item--sky::after {
  background: #5ca8ff;
}

.overview-item--cyan {
  background: #e6f8ef;
}

.overview-item--cyan::before,
.overview-item--cyan::after {
  background: #24c6d8;
}

.contract-execution {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.execution-item p,
.device-metric p {
  margin: 0;
  color: #606266;
  font-size: 13px;
}

.execution-item strong,
.device-metric strong {
  margin-left: 4px;
  color: #2f75ff;
  font-size: 18px;
  font-weight: 700;
}

.execution-item em,
.device-metric em {
  margin-left: 2px;
  color: #909399;
  font-style: normal;
}

.device-tabs {
  margin-top: -8px;
}

.device-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.device-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.device-tabs :deep(.el-tabs__item) {
  height: 30px;
  padding: 0 30px;
  border-radius: 16px;
  color: #303133;
  line-height: 30px;
}

.device-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.device-tabs :deep(.el-tabs__item.is-active) {
  background: #2f75ff;
  color: #fff;
}

.device-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.chart {
  width: 100%;
  height: 240px;
}

.chart--donut {
  height: 272px;
}

.chart--bar {
  height: 278px;
}

.chart--line {
  height: 334px;
}

.room-legend {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
}

.room-legend span {
  display: inline-flex;
  align-items: center;
  color: #606266;
  font-size: 13px;
}

.room-legend i,
.legend-dot i {
  display: inline-block;
  width: 12px;
  height: 12px;
  margin-right: 6px;
  border-radius: 50%;
}

.legend-dot {
  display: inline-flex;
  align-items: center;
  color: #606266;
  font-size: 13px;
}

.legend-dot i {
  border-radius: 0;
  background: #2f75ff;
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
  min-height: 148px;
  margin-bottom: 20px;
  border-radius: 4px;
  background: #f5f8ff;
}

.avg-rent strong {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 94px;
  height: 94px;
  border: 10px solid #2f75ff;
  border-radius: 50%;
  color: #2f75ff;
  font-size: 16px;
  font-weight: 700;
}

.avg-rent span {
  margin-top: 10px;
  color: #303133;
  font-size: 14px;
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

.card-title--tabs button {
  margin-right: 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #606266;
  font-size: 15px;
  cursor: pointer;
}

.card-title--tabs button.active {
  color: #2f75ff;
  font-weight: 600;
}

.list-card {
  min-height: 370px;
}

.approval-list {
  display: flex;
  max-height: 334px;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
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
  overflow: hidden;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-item p {
  margin: 10px 0;
  color: #8a94a6;
  font-size: 13px;
}

.approval-item p em {
  margin-left: 12px;
  color: #303133;
  font-style: normal;
}

.approval-item time {
  display: block;
  padding: 8px 12px;
  border-radius: 4px;
  background: #fff;
  color: #8a94a6;
  font-size: 13px;
}

.tenant-list,
.opportunity-list {
  max-height: 334px;
  margin: 0;
  padding: 0;
  overflow: hidden;
  list-style: none;
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
  grid-template-columns: 18px minmax(0, 1fr) 92px;
  gap: 12px;
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
  .dashboard-row--bottom {
    grid-template-columns: 1fr;
  }

  .side-stack {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto;
  }
}

@media (max-width: 960px) {
  .overview-grid,
  .contract-execution,
  .device-summary,
  .side-stack {
    grid-template-columns: 1fr;
  }

  .opportunity-list li {
    grid-template-columns: minmax(0, 1fr) 54px;
  }

  .opportunity-list time {
    grid-column: 1 / -1;
    text-align: left;
  }
}
</style>
