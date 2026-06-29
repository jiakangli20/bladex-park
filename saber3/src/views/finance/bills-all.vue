<template>
  <basic-container>
    <div class="bills-all-page">
      <section class="bill-overview">
        <div class="bill-direction">
          <button
            v-for="item in directionOptions"
            :key="item.value"
            :class="['bill-direction__item', { 'is-active': direction === item.value }]"
            type="button"
            @click="changeDirection(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="bill-summary-grid">
          <div v-for="item in summaryCards" :key="item.key" class="bill-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>

      <section class="bill-table-card">
        <div class="bill-toolbar">
          <el-tabs v-model="direction" class="bill-tabs" @tab-change="changeDirection">
            <el-tab-pane label="收款账单" name="receivable" />
            <el-tab-pane label="付款账单" name="payable" />
          </el-tabs>
          <div class="bill-tools">
            <el-input
              v-model="query.customerName"
              class="bill-filter-input"
              clearable
              placeholder="对方名称"
              @keyup.enter="searchChange"
            />
            <el-input
              v-model="query.contractNo"
              class="bill-filter-input"
              clearable
              placeholder="合同编号"
              @keyup.enter="searchChange"
            />
            <el-date-picker
              v-model="query.deadlineRange"
              class="bill-date-picker"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              range-separator="-"
              clearable
            />
            <el-switch
              v-model="query.hideFuture"
              class="bill-hide-switch"
              inline-prompt
              active-text="隐藏"
              inactive-text="显示"
              @change="searchChange"
            />
            <span class="bill-hide-label">{{ hideFutureLabel }}</span>
            <el-button icon="el-icon-refresh-left" @click="searchReset">清空</el-button>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-download" @click="handleExport">导出</el-button>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="paymentId"
          class="bill-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column prop="customerName" label="对方名称" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="buildingName" label="楼宇名称" width="130" align="center" show-overflow-tooltip />
          <el-table-column prop="payStatus" label="账单状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.payStatus)" effect="plain">{{ payStatusText(row.payStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" min-width="130" align="center" show-overflow-tooltip />
          <el-table-column :label="amountDueLabel" prop="amountDue" width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column :label="amountPaidLabel" prop="amountPaid" width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column :label="amountPendingLabel" width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatMoney(pendingAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="periodStart" label="开始日期" width="130" align="center" />
          <el-table-column prop="periodEnd" label="结束时间" width="130" align="center" />
          <el-table-column prop="payDeadline" :label="deadlineLabel" width="130" align="center" />
        </el-table>

        <div class="bill-pagination">
          <el-pagination
            background
            :current-page="page.currentPage"
            :page-sizes="[10, 20, 30, 40, 50, 100]"
            :page-size="page.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="page.total"
            @size-change="sizeChange"
            @current-change="currentChange"
          />
        </div>
      </section>
    </div>
  </basic-container>
</template>

<script>
import { getPaymentPage, getPaymentSummary } from '@/api/ics/payment';
import { payStatusDic } from '@/option/finance/payment';

export default {
  name: 'FinanceBillsAll',
  data() {
    return {
      direction: 'receivable',
      query: {
        customerName: '',
        contractNo: '',
        deadlineRange: [],
        hideFuture: false,
      },
      loading: false,
      data: [],
      selectionList: [],
      summary: {},
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      directionOptions: [
        { label: '收款', value: 'receivable' },
        { label: '付款', value: 'payable' },
      ],
      payStatusOptions: payStatusDic,
    };
  },
  computed: {
    isPayable() {
      return this.direction === 'payable';
    },
    amountDueLabel() {
      return this.isPayable ? '应付金额' : '应收金额';
    },
    amountPaidLabel() {
      return this.isPayable ? '实付金额' : '实收金额';
    },
    amountPendingLabel() {
      return this.isPayable ? '需付金额' : '需收金额';
    },
    deadlineLabel() {
      return this.isPayable ? '应付日期' : '应收日期';
    },
    hideFutureLabel() {
      return `已隐藏未到${this.isPayable ? '应付' : '应收'}期账单`;
    },
    pendingCount() {
      const total = Number(this.summary.totalCount || 0);
      const paid = Number(this.summary.paidCount || 0);
      return Math.max(total - paid, 0);
    },
    summaryCards() {
      return [
        {
          key: 'due',
          label: `${this.isPayable ? '应付' : '应收'}（${this.summary.totalCount || 0}笔）`,
          value: this.formatMoney(this.summary.amountDue),
        },
        { key: 'lateFee', label: '滞纳金', value: this.formatMoney(0) },
        {
          key: 'paid',
          label: this.isPayable ? '实付' : '实收',
          value: this.formatMoney(this.summary.amountPaid),
        },
        { key: 'increase', label: '调增', value: this.formatMoney(0) },
        { key: 'decrease', label: '调减', value: this.formatMoney(0) },
        {
          key: 'pending',
          label: `${this.isPayable ? '需付' : '需收'}（${this.pendingCount}笔）`,
          value: this.formatMoney(this.summary.amountPending),
        },
      ];
    },
  },
  mounted() {
    this.applyRouteQuery();
    this.reload();
  },
  watch: {
    '$route.query': {
      handler() {
        this.applyRouteQuery();
        this.page.currentPage = 1;
        this.reload();
      },
      deep: true,
    },
  },
  methods: {
    applyRouteQuery() {
      const routeQuery = this.$route.query || {};
      const nextQuery = {
        customerName: routeQuery.customerName || '',
        contractNo: routeQuery.contractNo || '',
        deadlineRange: [],
        hideFuture: routeQuery.hideFuture === true || routeQuery.hideFuture === 'true',
        settleStatus: routeQuery.settleStatus || '',
      };
      if (routeQuery.direction === 'payable' || routeQuery.direction === 'receivable') {
        this.direction = routeQuery.direction;
      }
      if (this.hasRouteDate(routeQuery.deadlineStartDate) && this.hasRouteDate(routeQuery.deadlineEndDate)) {
        nextQuery.deadlineRange = [
          this.normalizeRouteDate(routeQuery.deadlineStartDate),
          this.normalizeRouteDate(routeQuery.deadlineEndDate),
        ];
      }
      this.query = nextQuery;
    },
    reload() {
      this.loadSummary();
      this.loadPage();
    },
    loadPage() {
      this.loading = true;
      getPaymentPage(this.page.currentPage, this.page.pageSize, this.buildQueryParams())
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = result.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadSummary() {
      getPaymentSummary(this.buildQueryParams()).then(res => {
        this.summary = res.data.data || {};
      });
    },
    buildQueryParams() {
      const params = {
        direction: this.direction,
        customerName: this.query.customerName,
        contractNo: this.query.contractNo,
        hideFuture: this.query.hideFuture,
        settleStatus: this.query.settleStatus,
      };
      if (this.isPayable) {
        params.feeType = 'deposit_refund';
      }
      if (Array.isArray(this.query.deadlineRange) && this.query.deadlineRange.length === 2) {
        params.deadlineStartDate = this.toDayStart(this.query.deadlineRange[0]);
        params.deadlineEndDate = this.toDayEnd(this.query.deadlineRange[1]);
      }
      return Object.keys(params).reduce((result, key) => {
        if (params[key] !== '' && params[key] !== null && params[key] !== undefined) {
          result[key] = params[key];
        }
        return result;
      }, {});
    },
    hasRouteDate(value) {
      return typeof value === 'string' && value.trim() !== '';
    },
    normalizeRouteDate(value) {
      if (!this.hasRouteDate(value)) return '';
      return value.trim().slice(0, 10);
    },
    toDayStart(value) {
      const date = this.normalizeRouteDate(value);
      return date ? `${date} 00:00:00` : '';
    },
    toDayEnd(value) {
      const date = this.normalizeRouteDate(value);
      return date ? `${date} 23:59:59` : '';
    },
    changeDirection(value) {
      const nextDirection = value && value.paneName ? value.paneName : value;
      if (nextDirection && this.direction !== nextDirection) {
        this.direction = nextDirection;
      }
      this.page.currentPage = 1;
      this.reload();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {
        customerName: '',
        contractNo: '',
        deadlineRange: [],
        hideFuture: false,
        settleStatus: '',
      };
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadPage();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadPage();
    },
    selectionChange(selection) {
      this.selectionList = selection;
    },
    handleExport() {
      this.$message.info('导出接口预留中，后续接入账单导出服务');
    },
    pendingAmount(row) {
      return Number(row.amountDue || 0) - Number(row.amountPaid || 0);
    },
    payStatusText(value) {
      const item = this.payStatusOptions.find(option => option.value === value);
      return item ? item.label : '未知';
    },
    payStatusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'danger',
        3: 'warning',
      };
      return map[value] || 'info';
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.bills-all-page {
  min-width: 0;
}

.bill-overview {
  display: flex;
  min-height: 112px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.bill-direction {
  width: 64px;
  padding: 14px 0;
  border-right: 1px solid #edf0f5;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 8px;
}

.bill-direction__item {
  position: relative;
  height: 34px;
  border: 0;
  background: transparent;
  color: #303133;
  font-size: 14px;
  line-height: 34px;
  cursor: pointer;
}

.bill-direction__item.is-active {
  color: #1677ff;
  font-weight: 600;
}

.bill-direction__item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  width: 3px;
  height: 28px;
  background: #1677ff;
  border-radius: 0 3px 3px 0;
}

.bill-summary-grid {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
}

.bill-summary-card {
  min-width: 0;
  padding: 22px 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.bill-summary-card span {
  color: #909399;
  font-size: 14px;
  line-height: 20px;
}

.bill-summary-card strong {
  margin-top: 8px;
  color: #303133;
  font-size: 22px;
  line-height: 30px;
  font-weight: 600;
  white-space: nowrap;
}

.bill-table-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
}

.bill-toolbar {
  min-height: 74px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #ebeef5;
}

.bill-tabs {
  flex: 0 0 auto;
  min-width: 180px;
}

.bill-tools {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.bill-filter-input {
  width: 150px;
}

.bill-date-picker {
  width: 250px;
}

.bill-hide-switch {
  flex: 0 0 auto;
}

.bill-hide-label {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.bill-table {
  width: 100%;
}

.bill-pagination {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
}

.bills-all-page :deep(.el-tabs__header) {
  margin: 0;
}

.bills-all-page :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}

.bills-all-page :deep(.el-button),
.bills-all-page :deep(.el-input__wrapper),
.bills-all-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

.bills-all-page :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #606266;
  font-weight: 600;
}

.bills-all-page :deep(.el-table .cell) {
  white-space: nowrap;
}
</style>
