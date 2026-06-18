<template>
  <section class="wf-process-dashboard" v-loading="loading">
    <div
      v-for="item in cards"
      :key="item.key"
      class="wf-process-dashboard__card"
      :class="{ 'is-active': active === item.key }"
    >
      <div class="wf-process-dashboard__meta">
        <span class="wf-process-dashboard__label">{{ item.label }}</span>
        <strong class="wf-process-dashboard__value">{{ item.value }}</strong>
      </div>
      <span class="wf-process-dashboard__today">{{ item.subLabel }}：{{ item.today }}</span>
    </div>
  </section>
</template>

<script>
import { index } from '../../api/statistics/index';

const emptyStats = () => ({
  process: {},
  task: {},
});

export default {
  name: 'NfProcessDashboard',
  props: {
    active: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      loading: false,
      stats: emptyStats(),
    };
  },
  computed: {
    cards() {
      const process = this.stats.process || {};
      const task = this.stats.task || {};
      return [
        {
          key: 'todo',
          label: '待办任务',
          value: task.unfinished || 0,
          subLabel: '今日待办',
          today: task.todayUnfinished || 0,
        },
        {
          key: 'done',
          label: '已办任务',
          value: task.task || 0,
          subLabel: '今日已办',
          today: task.todayTask || 0,
        },
        {
          key: 'timeout',
          label: '超时任务',
          value: task.timeout || 0,
          subLabel: '今日超时',
          today: task.todayTimeout || 0,
        },
        {
          key: 'unfinished',
          label: '进行中流程',
          value: process.unfinished || 0,
          subLabel: '今日新增',
          today: process.todayUnfinished || 0,
        },
        {
          key: 'finished',
          label: '已结束流程',
          value: process.finished || 0,
          subLabel: '今日结束',
          today: process.todayFinished || 0,
        },
        {
          key: 'reject',
          label: '已驳回流程',
          value: process.reject || 0,
          subLabel: '今日驳回',
          today: process.todayReject || 0,
        },
      ];
    },
  },
  mounted() {
    this.loadStats();
  },
  methods: {
    loadStats() {
      this.loading = true;
      index()
        .then(res => {
          this.stats = res.data.data || emptyStats();
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style lang="scss" scoped>
.wf-process-dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
  margin-bottom: 12px;

  &__card {
    min-height: 84px;
    padding: 14px 16px;
    border: 1px solid #e6e9f0;
    border-radius: 8px;
    background: #fff;
    transition:
      border-color 0.2s ease,
      box-shadow 0.2s ease,
      transform 0.2s ease;

    &.is-active {
      border-color: #409eff;
      box-shadow: 0 8px 18px rgba(64, 158, 255, 0.12);
    }

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 8px 18px rgba(31, 45, 61, 0.08);
    }
  }

  &__meta {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  &__label {
    color: #606266;
    font-size: 13px;
    line-height: 20px;
  }

  &__value {
    color: #1f2d3d;
    font-size: 26px;
    font-weight: 650;
    line-height: 1;
  }

  &__today {
    display: block;
    margin-top: 12px;
    color: #909399;
    font-size: 12px;
    line-height: 18px;
  }
}
</style>
