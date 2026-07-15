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
            <el-dropdown trigger="click" @command="handleCreateCommand">
              <el-button type="primary" icon="el-icon-plus">
                创建账单
                <i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="receivable">添加收款账单</el-dropdown-item>
                  <el-dropdown-item command="payable">添加付款账单</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
          <el-table-column prop="customerName" label="对方名称" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button text type="primary" class="bill-link" @click="openBillDetail(row)">
                {{ row.customerName || '-' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="buildingName" label="楼宇名称" width="130" align="center" show-overflow-tooltip />
          <el-table-column prop="payStatus" label="账单状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.payStatus)" effect="plain">{{ payStatusText(row.payStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" min-width="130" align="center" show-overflow-tooltip />
          <el-table-column :label="amountDueLabel" prop="amountDue" width="130" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column :label="amountPaidLabel" prop="amountPaid" width="130" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column :label="amountPendingLabel" width="130" align="center">
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

      <bill-create-drawer
        v-model="createVisible"
        :direction="createDirection"
        @saved="handleBillCreated"
      />

      <el-drawer
        v-model="detailVisible"
        size="76%"
        append-to-body
        class="bill-detail-drawer"
        @closed="closeBillDetail"
      >
        <template #header>
          <div class="bill-drawer-header">
            <div class="bill-drawer-title">
              <el-button text class="bill-drawer-back" @click="detailVisible = false">
                <i class="el-icon-arrow-left"></i>
              </el-button>
              <span>账单详情</span>
              <el-tag :type="payStatusType(detailRow.payStatus)" effect="plain">
                {{ payStatusText(detailRow.payStatus) }}
              </el-tag>
            </div>
            <div class="bill-drawer-actions">
              <el-button :disabled="!detailRow.contractId" @click="openContract(detailRow)">查看合同</el-button>
              <el-button type="primary" plain :disabled="!detailRow.paymentId" @click="handleGeneratePaymentNotice(detailRow)">
                生成缴费通知单
              </el-button>
            </div>
          </div>
        </template>
        <div v-loading="detailLoading" class="bill-detail-body">
          <template v-if="detailRow && detailRow.paymentId">
            <section class="bill-detail-hero">
              <h2>{{ detailPartyTitle }}</h2>
              <div class="bill-status-strip">
                <div v-for="item in detailMetricItems" :key="item.key" class="bill-status-cell">
                  <span>{{ item.label }}</span>
                  <strong v-if="item.status" :class="['bill-status-value', `is-${item.type || 'info'}`]">{{ item.value }}</strong>
                  <el-date-picker
                    v-else-if="item.editableDate"
                    v-model="detailDeadline"
                    :disabled="deadlineSaving"
                    type="date"
                    value-format="YYYY-MM-DD"
                    class="bill-status-date"
                    @change="handleDeadlineChange"
                  />
                  <strong v-else>{{ item.value }}</strong>
                </div>
              </div>
            </section>

            <div class="bill-detail-two-col">
              <section class="bill-detail-section">
                <div class="bill-detail-section__title">账单信息</div>
                <div class="bill-detail-info-grid">
                  <div v-for="item in detailBillItems" :key="item.label" class="bill-detail-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </section>

              <section class="bill-detail-section">
                <div class="bill-detail-section__title">房源信息</div>
                <el-table :data="detailRoomRows" border class="bill-detail-table">
                  <el-table-column prop="buildingName" label="所属楼宇" min-width="120" align="center" />
                  <el-table-column prop="roomName" label="楼层/房号" min-width="120" align="center" show-overflow-tooltip />
                  <el-table-column prop="area" label="面积" min-width="120" align="center" />
                </el-table>
              </section>
            </div>

            <section class="bill-detail-section bill-detail-lines">
              <div class="bill-detail-section__title">账单明细</div>
              <el-table :data="detailLineRows" border class="bill-detail-table">
                <el-table-column prop="feeName" label="费用类型" min-width="140" align="center" />
                <el-table-column prop="amountDue" :label="amountDueLabel" min-width="150" align="center" />
                <el-table-column prop="taxRate" label="税率" width="110" align="center" />
                <el-table-column prop="taxAmount" label="税额" width="110" align="center" />
                <el-table-column prop="periodStart" label="开始日期" min-width="130" align="center" />
                <el-table-column prop="periodEnd" label="结束日期" min-width="130" align="center" />
                <el-table-column prop="remark" label="账单备注" min-width="160" align="center" show-overflow-tooltip />
              </el-table>
            </section>
          </template>
          <el-empty v-else description="暂无账单详情" />
        </div>
      </el-drawer>
    </div>
  </basic-container>
</template>

<script>
import { noticePrintUrl } from '@/api/contract/print';
import { generateContractNotice, getPaymentDetail, getPaymentPage, getPaymentSummary, updatePaymentDeadline } from '@/api/ics/payment';
import { payStatusDic } from '@/option/finance/payment';
import { downloadNoticeFile } from '@/utils/contract-notice';
import BillCreateDrawer from './components/bill-create-drawer.vue';

export default {
  name: 'FinanceBillsAll',
  components: {
    BillCreateDrawer,
  },
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
      detailLoading: false,
      deadlineSaving: false,
      detailVisible: false,
      createVisible: false,
      createDirection: 'receivable',
      detailDeadline: '',
      detailRow: {},
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
    payerTitle() {
      return this.isPayable ? '收款方' : '付款方';
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
    detailBillItems() {
      const row = this.detailRow || {};
      return [
        { label: '费用类型', value: row.feeName || '-' },
        { label: '计费周期', value: `${row.periodStart || '--'}~${row.periodEnd || '--'}` },
        { label: '账单金额（元）', value: this.formatMoney(row.amountDue) },
        { label: '创建时间', value: row.createTime || '-' },
        { label: this.payerTitle, value: row.customerName || '-' },
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '账单编号', value: row.paymentId ? `ZD${row.paymentId}` : '-' },
        { label: '起算天数', value: '--' },
        { label: '滞纳金比例', value: '--' },
        { label: '滞纳金上限', value: '--' },
        { label: '经办人', value: row.createBy || '-' },
        { label: '账单备注', value: row.remark || '-' },
      ];
    },
    detailPartyTitle() {
      const row = this.detailRow || {};
      const label = this.payerTitle || '';
      const name = row.customerName || '-';
      return label ? `${label}：${name}` : name;
    },
    detailMetricItems() {
      const row = this.detailRow || {};
      return [
        {
          key: 'status',
          label: '账单状态',
          value: this.payStatusText(row.payStatus),
          status: true,
          type: this.payStatusType(row.payStatus),
        },
        { key: 'amountDue', label: `${this.amountDueLabel}（元）`, value: this.formatMoney(row.amountDue) },
        { key: 'amountPending', label: `${this.amountPendingLabel}（元）`, value: this.formatMoney(this.pendingAmount(row)) },
        { key: 'deadline', label: this.deadlineLabel, editableDate: true },
      ];
    },
    detailRoomRows() {
      const row = this.detailRow || {};
      return [
        {
          buildingName: row.buildingName || '-',
          roomName: row.roomName || '-',
          area: row.rentArea ? `${row.rentArea}㎡` : '-',
        },
      ];
    },
    detailLineRows() {
      const row = this.detailRow || {};
      return [
        {
          feeName: row.feeName || '-',
          amountDue: this.formatMoney(row.amountDue),
          taxRate: '0.00',
          taxAmount: '0.00',
          periodStart: row.periodStart || '-',
          periodEnd: row.periodEnd || '-',
          remark: row.remark || '-',
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
    handleCreateCommand(direction) {
      this.createDirection = direction === 'payable' ? 'payable' : 'receivable';
      this.createVisible = true;
    },
    handleBillCreated(payload = {}) {
      this.direction = payload.direction === 'payable' ? 'payable' : 'receivable';
      this.page.currentPage = 1;
      this.reload();
    },
    openBillDetail(row) {
      if (!row || !row.paymentId) return;
      this.detailVisible = true;
      this.detailLoading = true;
      this.detailDeadline = this.dateOnly(row.payDeadline);
      getPaymentDetail(row.paymentId)
        .then(res => {
          this.detailRow = res.data.data || { ...row };
          this.detailDeadline = this.dateOnly(this.detailRow.payDeadline);
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    closeBillDetail() {
      this.detailRow = {};
      this.detailLoading = false;
      this.deadlineSaving = false;
      this.detailDeadline = '';
    },
    handleDeadlineChange(value) {
      const paymentId = this.detailRow && this.detailRow.paymentId;
      if (!paymentId) return;
      if (!value) {
        this.detailDeadline = this.dateOnly(this.detailRow.payDeadline);
        return;
      }
      this.deadlineSaving = true;
      updatePaymentDeadline(paymentId, value)
        .then(() => {
          this.detailRow = {
            ...this.detailRow,
            payDeadline: value,
          };
          this.data = this.data.map(item => (item.paymentId === paymentId ? { ...item, payDeadline: value } : item));
          this.loadSummary();
          this.$message.success(`${this.deadlineLabel}已更新`);
        })
        .catch(() => {
          this.detailDeadline = this.dateOnly(this.detailRow.payDeadline);
        })
        .finally(() => {
          this.deadlineSaving = false;
        });
    },
    openContract(row) {
      if (!row || !row.contractId) return;
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
        },
      });
    },
    handleGeneratePaymentNotice(row) {
      if (!row || !row.paymentId) return;
      const noticeType = 'payment-notice';
      generateContractNotice({
        noticeType,
        paymentId: row.paymentId,
        contractId: row.contractId,
      }).then(res => {
        const file = res.data.data || {};
        const url =
          file.fileUrl ||
          noticePrintUrl(noticeType, {
            paymentId: row.paymentId,
            contractId: row.contractId,
          });
        const fallbackName = file.fileName || `${row.contractNo ? `${row.contractNo}-` : ''}缴费通知单.docx`;
        return downloadNoticeFile(url, fallbackName).then(() => {
          this.$message.success('缴费通知单已生成');
        });
      });
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
    dateOnly(value) {
      if (!value) return '';
      return String(value).slice(0, 10);
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
  gap: 14px;
  flex-wrap: wrap;
}

.bill-tools :deep(.el-button + .el-button) {
  margin-left: 0;
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

.bill-table :deep(.el-table__cell) {
  text-align: center;
}

.bill-link {
  max-width: 100%;
  padding: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
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

.bill-detail-body {
  min-height: 280px;
  padding: 0 0 24px;
  background: #f4f4f6;
}

.bill-drawer-header {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.bill-drawer-title,
.bill-drawer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bill-drawer-title {
  min-width: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.bill-drawer-back {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #606266;
}

.bill-drawer-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.bill-detail-hero {
  min-height: 128px;
  padding: 18px 24px 22px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.bill-detail-hero h2 {
  margin: 0 0 22px;
  color: #303133;
  font-size: 22px;
  line-height: 30px;
  font-weight: 600;
}

.bill-status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 168px));
  gap: 30px;
  align-items: end;
  max-width: 760px;
}

.bill-status-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bill-status-cell span,
.bill-detail-item span {
  color: #909399;
  font-size: 13px;
  line-height: 18px;
}

.bill-status-cell strong {
  color: #303133;
  font-size: 22px;
  line-height: 28px;
  font-weight: 500;
  white-space: nowrap;
}

.bill-status-value {
  display: inline-flex;
  align-items: center;
  width: fit-content;
}

.bill-status-value.is-success {
  color: #16a34a;
}

.bill-status-value.is-danger {
  color: #dc2626;
}

.bill-status-value.is-warning {
  color: #d97706;
}

.bill-status-value.is-info {
  color: #303133;
}

.bill-status-date {
  width: 150px;
}

.bill-status-date :deep(.el-input__wrapper) {
  min-height: 30px;
  padding: 0;
  box-shadow: none;
  background: transparent;
}

.bill-status-date :deep(.el-input__inner) {
  color: #303133;
  font-size: 22px;
  line-height: 28px;
  font-weight: 500;
}

.bill-status-date :deep(.el-input__prefix) {
  order: 2;
  margin-left: 6px;
  margin-right: 0;
}

.bill-detail-two-col {
  padding: 18px 20px 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.bill-detail-section {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.bill-detail-section {
  min-height: 260px;
}

.bill-detail-section__title {
  height: 58px;
  margin: 0;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
  color: #303133;
  font-size: 16px;
  line-height: 58px;
  font-weight: 600;
}

.bill-detail-info-grid {
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 24px;
  row-gap: 18px;
}

.bill-detail-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bill-detail-item strong {
  color: #303133;
  font-size: 14px;
  line-height: 22px;
  font-weight: 500;
  word-break: break-word;
}

.bill-detail-table {
  width: calc(100% - 40px);
  margin: 20px;
}

.bill-detail-lines {
  margin: 18px 20px 0;
  min-height: 0;
}

.bill-detail-drawer :deep(.el-drawer__header) {
  margin: 0;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.bill-detail-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f4f4f6;
}

.bill-detail-table :deep(.el-table__cell) {
  text-align: center;
}

.bill-detail-table :deep(.el-table__header-wrapper col),
.bill-detail-table :deep(.el-table__body-wrapper col) {
  width: auto;
}

@media (max-width: 1100px) {
  .bill-detail-two-col,
  .bill-status-strip {
    grid-template-columns: 1fr;
  }

  .bill-detail-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
