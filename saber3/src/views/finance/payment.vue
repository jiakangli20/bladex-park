<template>
  <basic-container>
    <div class="payment-page">
      <section v-if="mode === 'notice'" class="notice-placeholder">
        <div>
          <h2>{{ notice.title || '收款通知' }}</h2>
          <p>{{ notice.message || '当前阶段仅保留入口，暂不开发通知单主流程。' }}</p>
          <span>{{ notice.nextStep || '后续按通知单主表、发送渠道和回执状态扩展。' }}</span>
        </div>
      </section>

      <template v-else>
        <section class="payment-summary">
          <div v-for="item in summaryCards" :key="item.key" class="payment-summary__item">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="payment-toolbar">
          <div class="payment-toolbar__title">{{ pageTitle }}</div>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </section>

        <avue-crud
          ref="crud"
          v-model:page="page"
          v-model:search="search"
          :option="crudOption"
          :data="data"
          :table-loading="loading"
          :permission="permissionList"
          @search-change="searchChange"
          @search-reset="searchReset"
          @current-change="currentChange"
          @size-change="sizeChange"
          @refresh-change="refreshChange"
          @on-load="onLoad"
        >
          <template #paymentId="{ row }">ZD{{ row.paymentId }}</template>
          <template #periodText="{ row }">
            {{ row.periodStart || '-' }} 至 {{ row.periodEnd || '-' }}
          </template>
          <template #amountDue="{ row }">{{ formatMoney(row.amountDue) }}</template>
          <template #amountPaid="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          <template #payStatus="{ row }">
            <el-tag :type="payStatusType(row.payStatus)" effect="plain">
              {{ payStatusText(row.payStatus) }}
            </el-tag>
          </template>
          <template #remindStatus="{ row }">
            <span v-if="row.remindStatus === '1'">{{ row.remindTime || '已催缴' }}</span>
            <span v-else class="muted">未催缴</span>
          </template>
          <template #menu="{ row }">
            <el-button v-if="permissionList.viewBtn" text type="primary" icon="el-icon-view" @click="openDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="permissionList.confirmBtn && row.payStatus !== '1'"
              text
              type="primary"
              icon="el-icon-circle-check"
              @click="openConfirm(row)"
            >
              确认
            </el-button>
            <el-button
              v-if="permissionList.remindBtn && row.payStatus !== '1'"
              text
              type="warning"
              icon="el-icon-bell"
              @click="handleRemind(row)"
            >
              催缴
            </el-button>
          </template>
        </avue-crud>
      </template>

      <el-dialog v-model="confirmVisible" title="确认缴费" width="520px" append-to-body :before-close="closeConfirm">
        <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="96px">
          <el-form-item label="账单编号">
            <el-input :model-value="currentPayment.paymentId ? `ZD${currentPayment.paymentId}` : '-'" disabled />
          </el-form-item>
          <el-form-item label="合同编号">
            <el-input :model-value="currentPayment.contractNo || '-'" disabled />
          </el-form-item>
          <el-form-item label="实收金额" prop="amountPaid">
            <el-input-number v-model="confirmForm.amountPaid" :min="0.01" :precision="2" :step="100" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="confirmForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button type="primary" icon="el-icon-circle-check" :loading="confirmLoading" @click="submitConfirm">提交</el-button>
          <el-button icon="el-icon-circle-close" @click="closeConfirm">取消</el-button>
        </template>
      </el-dialog>

      <el-drawer v-model="detailVisible" title="账单详情" size="680px" append-to-body>
        <template v-if="currentPayment.paymentId">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="账单编号">ZD{{ currentPayment.paymentId }}</el-descriptions-item>
            <el-descriptions-item label="缴费状态">
              <el-tag :type="payStatusType(currentPayment.payStatus)" effect="plain">{{ payStatusText(currentPayment.payStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="合同编号">{{ currentPayment.contractNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户名称">{{ currentPayment.customerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="费用类型">{{ currentPayment.feeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属园区">{{ currentPayment.parkId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账期">
              {{ currentPayment.periodStart || '-' }} 至 {{ currentPayment.periodEnd || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="应缴日期">{{ currentPayment.payDeadline || '-' }}</el-descriptions-item>
            <el-descriptions-item label="应收金额">{{ formatMoney(currentPayment.amountDue) }}</el-descriptions-item>
            <el-descriptions-item label="实收金额">{{ formatMoney(currentPayment.amountPaid) }}</el-descriptions-item>
            <el-descriptions-item label="实缴时间">{{ currentPayment.payTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="催缴时间">{{ currentPayment.remindTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ currentPayment.remark || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div class="log-title">合同联动日志</div>
          <el-timeline>
            <el-timeline-item v-for="item in logs" :key="item.logId" :timestamp="item.operateTime" placement="top">
              <div class="payment-log">
                <div class="payment-log__title">{{ item.actionDesc || item.action }}</div>
                <div class="payment-log__operator">{{ item.operator || '-' }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="logs.length === 0" description="暂无合同日志" />
        </template>
      </el-drawer>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import {
  confirmPayment,
  getOverduePaymentPage,
  getPaymentDetail,
  getPaymentLogs,
  getPaymentNoticePlaceholder,
  getPaymentPage,
  getPaymentSummary,
  remindPayment,
} from '@/api/ics/payment';
import { payStatusDic, tableOption } from '@/option/finance/payment';

export default {
  name: 'FinancePayment',
  props: {
    scope: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      mode: 'payment',
      query: {},
      search: {},
      loading: false,
      data: [],
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      summary: {},
      currentPayment: {},
      detailVisible: false,
      logs: [],
      confirmVisible: false,
      confirmLoading: false,
      confirmForm: {
        amountPaid: null,
        remark: '',
      },
      notice: {},
      payStatusOptions: payStatusDic,
      confirmRules: {
        amountPaid: [{ required: true, message: '请输入实收金额', trigger: 'blur' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        viewBtn: this.validData(this.permission.finance_bills_all_view || this.permission.finance_payment_view, false),
        confirmBtn: this.validData(this.permission.finance_payment_confirm, false),
        remindBtn: this.validData(this.permission.finance_payment_remind, false),
      };
    },
    summaryCards() {
      return [
        { key: 'total', label: '账单总数', value: this.summary.totalCount || 0 },
        { key: 'unpaid', label: '未缴账单', value: this.summary.unpaidCount || 0 },
        { key: 'overdue', label: '逾期账单', value: this.summary.overdueCount || 0 },
        { key: 'reminded', label: '已催缴', value: this.summary.remindedCount || 0 },
        { key: 'due', label: '应收金额', value: this.formatMoney(this.summary.amountDue) },
        { key: 'paid', label: '实收金额', value: this.formatMoney(this.summary.amountPaid) },
      ];
    },
    pageTitle() {
      return this.mode === 'overdue' ? '逾期账单' : '所有账单';
    },
    crudOption() {
      const option = {
        ...tableOption,
        column: tableOption.column.map(column => ({ ...column })),
      };
      const statusColumn = option.column.find(column => column.prop === 'payStatus');
      if (statusColumn) {
        statusColumn.searchDisabled = this.mode === 'overdue';
      }
      return option;
    },
  },
  watch: {
    scope: {
      immediate: true,
      handler(value) {
        this.applyScope(value);
      },
    },
    '$route.query': {
      handler() {
        this.applyRouteQuery();
        this.reload();
      },
      deep: true,
    },
  },
  mounted() {
    this.applyRouteQuery();
    if (this.mode === 'notice') {
      this.loadNotice();
      return;
    }
    this.reload();
  },
  methods: {
    applyScope(value) {
      const scope = value || this.$route.meta?.scope || this.$route.query?.scope || '';
      if (scope === 'all' || scope === 'overdue' || scope === 'notice') {
        this.mode = scope;
      } else {
        this.mode = 'payment';
      }
      if (this.mode === 'overdue') {
        this.query.payStatus = '2';
        this.search.payStatus = '2';
      }
    },
    applyRouteQuery() {
      const routeQuery = this.$route.query || {};
      this.query = {
        ...this.query,
        contractId: routeQuery.contractId || this.query.contractId,
        contractNo: routeQuery.contractNo || this.query.contractNo,
        customerName: routeQuery.customerName || this.query.customerName,
      };
      if (routeQuery.payStatus) {
        this.query.payStatus = routeQuery.payStatus;
        this.search.payStatus = routeQuery.payStatus;
      }
    },
    searchChange(params = {}, done) {
      this.query = { ...params };
      if (this.mode === 'overdue') {
        this.query.payStatus = '2';
        this.search.payStatus = '2';
      }
      this.page.currentPage = 1;
      this.reload();
      if (done) done();
    },
    searchReset() {
      this.query = {};
      this.search = {};
      if (this.mode === 'overdue') {
        this.query.payStatus = '2';
        this.search.payStatus = '2';
      }
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
    refreshChange() {
      this.reload();
    },
    onLoad(page) {
      this.page = page;
      this.loadPage();
      this.loadSummary();
    },
    reload() {
      if (this.mode === 'notice') {
        this.loadNotice();
        return;
      }
      this.loadSummary();
      this.loadPage();
    },
    loadPage() {
      this.loading = true;
      const params = this.buildQueryParams();
      const request = this.mode === 'overdue' ? getOverduePaymentPage : getPaymentPage;
      request(this.page.currentPage, this.page.pageSize, params)
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
    loadNotice() {
      getPaymentNoticePlaceholder().then(res => {
        this.notice = res.data.data || {};
      });
    },
    buildQueryParams() {
      const params = { ...this.query };
      if (this.mode === 'overdue') {
        params.scope = 'overdue';
        params.payStatus = '2';
      }
      return params;
    },
    openDetail(row) {
      getPaymentDetail(row.paymentId).then(res => {
        this.currentPayment = res.data.data || row;
        this.detailVisible = true;
        this.loadLogs(this.currentPayment.contractId);
      });
    },
    loadLogs(contractId) {
      if (!contractId) {
        this.logs = [];
        return;
      }
      getPaymentLogs(contractId).then(res => {
        this.logs = res.data.data || [];
      });
    },
    openConfirm(row) {
      this.currentPayment = row;
      this.confirmForm = {
        amountPaid: Number(row.amountDue || 0),
        remark: row.remark || '',
      };
      this.confirmVisible = true;
    },
    closeConfirm() {
      this.confirmVisible = false;
      this.confirmLoading = false;
      this.currentPayment = {};
      this.confirmForm = {
        amountPaid: null,
        remark: '',
      };
    },
    submitConfirm() {
      this.$refs.confirmFormRef.validate(valid => {
        if (!valid) return;
        this.confirmLoading = true;
        confirmPayment(this.currentPayment.paymentId, this.confirmForm)
          .then(() => {
            this.$message.success('操作成功!');
            this.closeConfirm();
            this.reload();
          })
          .finally(() => {
            this.confirmLoading = false;
          });
      });
    },
    handleRemind(row) {
      this.$confirm('确定记录本次催缴?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remindPayment(row.paymentId))
        .then(() => {
          this.$message.success('催缴已记录');
          this.reload();
        });
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
.payment-page {
  min-width: 0;
}

.payment-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.payment-summary__item {
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.payment-summary__item span {
  color: #606266;
  font-size: 13px;
  line-height: 18px;
}

.payment-summary__item strong {
  color: #303133;
  font-size: 21px;
  line-height: 30px;
  margin-top: 4px;
  font-weight: 600;
}

.payment-toolbar {
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  margin-bottom: 12px;
}

.payment-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.payment-toolbar__title {
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.notice-placeholder {
  min-height: 280px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 32px;
}

.notice-placeholder h2 {
  margin: 0 0 10px;
  color: #303133;
  font-size: 22px;
  font-weight: 600;
}

.notice-placeholder p {
  color: #606266;
  margin: 0 0 8px;
  line-height: 22px;
}

.notice-placeholder span,
.muted {
  color: #909399;
}

.log-title {
  margin: 18px 0 12px;
  font-weight: 600;
  color: #303133;
}

.payment-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.payment-log__title {
  color: #303133;
  font-weight: 600;
  margin-bottom: 8px;
}

.payment-log__operator {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .payment-summary {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .payment-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .payment-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
