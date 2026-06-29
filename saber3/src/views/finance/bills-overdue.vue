<template>
  <basic-container>
    <div class="overdue-bills-page">
      <section class="overdue-summary">
        <div v-for="item in summaryCards" :key="item.key" class="overdue-summary__item">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </section>

      <section class="overdue-search-card">
        <el-form :model="query" label-position="top">
          <div class="overdue-search-grid">
            <el-form-item label="账单来源">
              <el-select v-model="query.billSource" clearable placeholder="请选择账单来源">
                <el-option label="合同账单" value="contract" />
              </el-select>
            </el-form-item>
            <el-form-item label="费用类型">
              <el-select v-model="query.feeType" clearable placeholder="请选择费用类型">
                <el-option v-for="item in feeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="开始时间">
              <el-date-picker v-model="query.periodStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择开始时间" clearable />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker v-model="query.periodEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择结束时间" clearable />
            </el-form-item>

            <el-form-item label="创建时间">
              <el-date-picker
                v-model="query.createRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                range-separator="-"
                clearable
              />
            </el-form-item>
            <el-form-item label="结清状态">
              <el-select v-model="query.settleStatus" clearable placeholder="请选择结清状态">
                <el-option label="未结清" value="unsettled" />
                <el-option label="已结清" value="settled" />
              </el-select>
            </el-form-item>
            <el-form-item label="应收时间">
              <el-date-picker
                v-model="query.deadlineRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                range-separator="-"
                clearable
              />
            </el-form-item>
            <el-form-item label="项目名称">
              <el-select v-model="query.projectName" clearable placeholder="请选择项目名称" disabled />
            </el-form-item>

            <el-form-item label="楼宇名称">
              <el-select v-model="query.buildingNameQuery" clearable filterable placeholder="请选择楼宇名称">
                <el-option v-for="item in buildingOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="租客名称">
              <el-input v-model="query.customerName" clearable placeholder="请填写租客名称" @keyup.enter="searchChange" />
            </el-form-item>
            <el-form-item label="所属公司">
              <el-select v-model="query.companyName" clearable placeholder="请选择所属公司" disabled />
            </el-form-item>
            <div class="overdue-search-actions">
              <el-button icon="el-icon-refresh-left" @click="searchReset">重置</el-button>
              <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            </div>
          </div>
        </el-form>
      </section>

      <section class="overdue-table-card">
        <div class="overdue-toolbar">
          <span></span>
          <div class="overdue-toolbar__actions">
            <el-button icon="el-icon-download" @click="handleExport">导出</el-button>
            <el-button type="primary" plain icon="el-icon-bell" :disabled="selectionList.length === 0" @click="batchRemind">
              一键批量催缴
            </el-button>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="paymentId"
          class="overdue-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="50" align="center" />
          <el-table-column prop="customerName" label="对方名称" min-width="180" align="center">
            <template #default="{ row }">
              <span class="link-like">{{ row.customerName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="buildingName" label="楼宇名称" width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="payStatus" label="结清状态" width="110" align="center">
            <template #default="{ row }">
              <span>{{ row.payStatus === '1' ? '已结清' : '逾期' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" width="120" align="center" show-overflow-tooltip />
          <el-table-column prop="amountDue" label="应收金额" width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="实收金额" width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column label="需收金额" width="130" align="right" header-align="right">
            <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="periodStart" label="开始日期" width="130" align="center" />
          <el-table-column prop="periodEnd" label="结束日期" width="130" align="center" />
          <el-table-column prop="payDeadline" label="应收日期" width="130" align="center" />
          <el-table-column prop="contractNo" label="合同编号" min-width="170" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="link-like">{{ row.contractNo || '--' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="逾期天数" width="110" align="center" fixed="right">
            <template #default="{ row }">{{ overdueDays(row) }}天</template>
          </el-table-column>
        </el-table>

        <div class="overdue-pagination">
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
import {
  getOverduePaymentPage,
  getPaymentSummary,
  getPaymentNoticeBuildings,
  remindOverduePayment,
} from '@/api/ics/payment';
import { feeTypeDic } from '@/option/finance/payment';

export default {
  name: 'FinanceBillsOverdue',
  data() {
    return {
      query: {
        billSource: '',
        feeType: '',
        periodStartDate: '',
        periodEndDate: '',
        createRange: [],
        settleStatus: '',
        deadlineRange: [],
        projectName: '',
        buildingNameQuery: '',
        customerName: '',
        companyName: '',
      },
      summary: {},
      buildingOptions: [],
      loading: false,
      data: [],
      selectionList: [],
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
    };
  },
  computed: {
    feeTypeOptions() {
      return feeTypeDic;
    },
    summaryCards() {
      return [
        { key: 'count', label: '总逾期收款单数', value: this.summary.overdueCount || this.page.total || 0 },
        { key: 'amount', label: '总逾期收款金额', value: this.formatMoney(this.summary.amountDue) },
        { key: 'tenant', label: '逾期租客数', value: this.overdueTenantCount },
        { key: 'lateFee', label: '滞纳金金额', value: this.formatMoney(0) },
      ];
    },
    overdueTenantCount() {
      const names = new Set((this.data || []).map(item => item.customerName).filter(Boolean));
      return names.size || 0;
    },
  },
  mounted() {
    this.applyRouteQuery();
    this.reload();
    this.loadBuildingOptions();
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
      this.query = {
        ...this.query,
        feeType: routeQuery.feeType || '',
        settleStatus: routeQuery.settleStatus || '',
        buildingNameQuery: routeQuery.buildingNameQuery || '',
        customerName: routeQuery.customerName || '',
      };
      if (this.hasRouteDate(routeQuery.deadlineStartDate) && this.hasRouteDate(routeQuery.deadlineEndDate)) {
        this.query.deadlineRange = [
          this.normalizeRouteDate(routeQuery.deadlineStartDate),
          this.normalizeRouteDate(routeQuery.deadlineEndDate),
        ];
      } else if (!routeQuery.deadlineStartDate && !routeQuery.deadlineEndDate) {
        this.query.deadlineRange = [];
      }
    },
    reload() {
      this.loadSummary();
      this.loadData();
    },
    loadData() {
      this.loading = true;
      getOverduePaymentPage(this.page.currentPage, this.page.pageSize, this.buildQueryParams())
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
      getPaymentSummary({ ...this.buildQueryParams(), payStatus: '2' }).then(res => {
        this.summary = res.data.data || {};
      });
    },
    loadBuildingOptions() {
      getPaymentNoticeBuildings({}).then(res => {
        this.buildingOptions = res.data.data || [];
      });
    },
    buildQueryParams() {
      const params = {
        feeType: this.query.feeType,
        customerName: this.query.customerName,
        buildingNameQuery: this.query.buildingNameQuery,
        settleStatus: this.query.settleStatus,
      };
      if (this.query.periodStartDate) {
        params.periodStartBegin = this.toDayStart(this.query.periodStartDate);
        params.periodStartEnd = this.toDayEnd(this.query.periodStartDate);
      }
      if (this.query.periodEndDate) {
        params.periodEndBegin = this.toDayStart(this.query.periodEndDate);
        params.periodEndEnd = this.toDayEnd(this.query.periodEndDate);
      }
      if (Array.isArray(this.query.createRange) && this.query.createRange.length === 2) {
        params.createStartDate = this.toDayStart(this.query.createRange[0]);
        params.createEndDate = this.toDayEnd(this.query.createRange[1]);
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
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {
        billSource: '',
        feeType: '',
        periodStartDate: '',
        periodEndDate: '',
        createRange: [],
        settleStatus: '',
        deadlineRange: [],
        projectName: '',
        buildingNameQuery: '',
        customerName: '',
        companyName: '',
      };
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadData();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadData();
    },
    selectionChange(selection) {
      this.selectionList = selection;
    },
    batchRemind() {
      const rows = this.selectionList || [];
      if (!rows.length) return;
      Promise.all(rows.map(row => remindOverduePayment(row.paymentId))).then(() => {
        this.$message.success('批量催缴已记录');
        this.reload();
      });
    },
    handleExport() {
      this.$message.info('导出接口预留中，后续接入逾期账单导出服务');
    },
    overdueDays(row) {
      if (!row || !row.payDeadline || row.payStatus === '1') return 0;
      const deadline = new Date(row.payDeadline).getTime();
      if (Number.isNaN(deadline) || deadline >= Date.now()) return 0;
      return Math.ceil((Date.now() - deadline) / 86400000);
    },
    unpaidAmount(row) {
      const due = Number((row && row.amountDue) || 0);
      const paid = Number((row && row.amountPaid) || 0);
      return Math.max(due - paid, 0);
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
.overdue-bills-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.overdue-summary {
  min-height: 96px;
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.overdue-summary__item {
  position: relative;
  min-width: 0;
  padding: 18px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.overdue-summary__item + .overdue-summary__item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 28px;
  bottom: 28px;
  width: 1px;
  background: #dcdfe6;
}

.overdue-summary__item strong {
  color: #2f8cff;
  font-size: 24px;
  line-height: 30px;
  font-weight: 500;
  white-space: nowrap;
}

.overdue-summary__item span {
  margin-top: 4px;
  color: #606266;
  font-size: 13px;
}

.overdue-search-card,
.overdue-table-card {
  background: #fff;
  border-radius: 10px;
  border: 1px solid #ebeef5;
}

.overdue-search-card {
  padding: 20px 20px 16px;
}

.overdue-search-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(190px, 1fr));
  column-gap: 20px;
  row-gap: 16px;
  align-items: end;
}

.overdue-search-actions {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  min-height: 32px;
  padding-bottom: 18px;
}

.overdue-table-card {
  overflow: hidden;
}

.overdue-toolbar {
  min-height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}

.overdue-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.overdue-table {
  width: 100%;
}

.link-like {
  color: #2f8cff;
  white-space: nowrap;
}

.overdue-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
}

.overdue-bills-page :deep(.el-form-item) {
  margin-bottom: 0;
}

.overdue-bills-page :deep(.el-form-item__label) {
  color: #606266;
  line-height: 20px;
  margin-bottom: 8px;
  padding: 0;
}

.overdue-bills-page :deep(.el-input),
.overdue-bills-page :deep(.el-select),
.overdue-bills-page :deep(.el-date-editor) {
  width: 100%;
}

.overdue-bills-page :deep(.el-button),
.overdue-bills-page :deep(.el-input__wrapper),
.overdue-bills-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

.overdue-bills-page :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #606266;
  font-weight: 600;
}

.overdue-bills-page :deep(.el-table .cell) {
  white-space: nowrap;
}

@media (max-width: 1300px) {
  .overdue-search-grid {
    grid-template-columns: repeat(2, minmax(190px, 1fr));
  }
}
</style>
