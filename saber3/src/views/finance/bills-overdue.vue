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
            <el-form-item label="楼宇名称">
              <el-select v-model="query.buildingNameQuery" clearable filterable placeholder="请选择楼宇名称">
                <el-option v-for="item in buildingOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="租客名称">
              <el-input v-model="query.customerName" clearable placeholder="请填写租客名称" @keyup.enter="searchChange" />
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
          <el-table-column type="selection" width="44" align="center" />
          <el-table-column prop="customerName" label="对方名称" min-width="180" align="center">
            <template #default="{ row }">
              <el-button text type="primary" class="customer-link" @click="openBillDrawer(row)">
                {{ row.customerName || '-' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="buildingName" label="楼宇名称" width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="payStatus" label="结清状态" width="110" align="center">
            <template #default="{ row }">
              <span>{{ row.payStatus === '1' ? '已结清' : '逾期' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" width="120" align="center" show-overflow-tooltip />
          <el-table-column prop="amountDue" label="应收金额" width="130" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="实收金额" width="130" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column label="需收金额" width="130" align="center">
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
          <el-table-column label="逾期天数" width="110" align="center">
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

      <el-drawer
        v-model="drawerVisible"
        size="82%"
        append-to-body
        class="overdue-bill-drawer"
        :with-header="false"
        @closed="closeBillDrawer"
      >
        <div v-if="drawerRow" v-loading="drawerLoading" class="tenant-drawer">
          <section class="tenant-drawer-header">
            <div class="tenant-drawer-title">
              <el-button text class="tenant-drawer-back" @click="drawerVisible = false">
                <i class="el-icon-arrow-left"></i>
              </el-button>
              <strong>{{ drawerTenantName }}</strong>
            </div>
            <div class="tenant-drawer-actions">
              <el-button type="primary" plain :disabled="!drawerRow.contractId" @click="openContract(drawerRow)">编辑</el-button>
              <el-button type="warning" plain :disabled="!drawerRow.paymentId" @click="handleRemind(drawerRow)">记录催缴</el-button>
              <el-button type="primary" :disabled="!drawerRow.paymentId" @click="goOverdueReminder(drawerRow)">逾期提醒</el-button>
            </div>
          </section>

          <section class="tenant-profile">
            <div v-for="item in drawerTenantItems" :key="item.label" class="tenant-profile-item">
              <span>{{ item.label }}：</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>

          <section class="tenant-tab-card">
            <el-tabs v-model="drawerActiveTab" class="tenant-tabs">
              <el-tab-pane label="合同" name="contract">
                <el-table :data="drawerContracts" class="tenant-table" border empty-text="暂无合同">
                  <el-table-column prop="contractNo" label="合同编号" min-width="180" align="center" show-overflow-tooltip>
                    <template #default="{ row }">
                      <el-button text type="primary" class="tenant-link" @click="openContract(row)">
                        {{ row.contractNo || '-' }}
                      </el-button>
                    </template>
                  </el-table-column>
                  <el-table-column prop="contractStatus" label="合同状态" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag :type="contractStatusType(row.contractStatus)" effect="light">
                        {{ contractStatusText(row.contractStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="合同类型" width="130" align="center">
                    <template #default="{ row }">{{ row.contractType || '普通合同' }}</template>
                  </el-table-column>
                  <el-table-column label="租赁单价" min-width="150" align="center">
                    <template #default="{ row }">{{ rentPriceText(row) }}</template>
                  </el-table-column>
                  <el-table-column prop="signDate" label="签订日期" width="140" align="center">
                    <template #default="{ row }">{{ dateOnly(row.signDate) || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="合同来源" width="130" align="center">
                    <template #default="{ row }">{{ row.parentContractId ? '续签合同' : '新建合同' }}</template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="账单" name="bill">
                <div class="tenant-bill-summary">
                  <div v-for="item in drawerBillSummary" :key="item.key" class="tenant-bill-summary__item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
                <el-table :data="drawerBills" class="tenant-table" border empty-text="暂无账单">
                  <el-table-column prop="contractNo" label="合同编号" min-width="170" align="center" show-overflow-tooltip />
                  <el-table-column prop="feeName" label="费用类型" width="120" align="center" show-overflow-tooltip />
                  <el-table-column label="账期" min-width="190" align="center">
                    <template #default="{ row }">{{ row.periodStart || '--' }} 至 {{ row.periodEnd || '--' }}</template>
                  </el-table-column>
                  <el-table-column prop="payDeadline" label="应收日期" width="130" align="center" />
                  <el-table-column label="应收金额" width="130" align="center">
                    <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
                  </el-table-column>
                  <el-table-column label="需收金额" width="130" align="center">
                    <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
                  </el-table-column>
                  <el-table-column label="逾期状态" width="110" align="center">
                    <template #default="{ row }">
                      <el-tag :type="overdueStatusType(row)" effect="plain">{{ overdueStatusText(row) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="结清状态" width="110" align="center">
                    <template #default="{ row }">
                      <el-tag :type="settleStatusType(row)" effect="plain">{{ settleStatusText(row) }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="附件" name="attachment">
                <div class="tenant-attachment-toolbar">
                  <el-upload
                    ref="attachmentUploadRef"
                    action="/api/blade-resource/oss/endpoint/put-file"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :disabled="!attachmentUploadPayment || attachmentUploadLoading"
                    accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.pdf,.jpg,.jpeg,.png"
                    :before-upload="beforeAttachmentUpload"
                    :on-success="handleAttachmentUploadSuccess"
                    :on-error="handleAttachmentUploadError"
                  >
                    <el-button type="primary" icon="el-icon-upload" :loading="attachmentUploadLoading" :disabled="!attachmentUploadPayment">
                      上传附件
                    </el-button>
                  </el-upload>
                </div>
                <el-table :data="drawerAttachmentRows" class="tenant-table" border empty-text="暂无附件">
                  <el-table-column prop="fileName" label="文件名称" min-width="220" align="center" show-overflow-tooltip />
                  <el-table-column prop="source" label="来源" width="140" align="center" />
                  <el-table-column prop="businessNo" label="关联编号" min-width="180" align="center" show-overflow-tooltip />
                  <el-table-column label="操作" width="156" align="center" fixed="right">
                    <template #default="{ row }">
                      <div class="tenant-table-actions">
                        <el-button text type="primary" @click="openAttachment(row)">查看</el-button>
                        <el-button text type="primary" @click="downloadAttachment(row)">下载</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>
            </el-tabs>
          </section>

          <section v-if="permissionList.logBtn" class="drawer-section drawer-log-inline">
            <div class="drawer-section-title">最近联动日志</div>
            <div v-loading="drawerLogLoading" class="drawer-log-list">
              <el-timeline v-if="drawerLogs.length">
                <el-timeline-item
                  v-for="item in drawerLogs.slice(0, 4)"
                  :key="item.logId"
                  :timestamp="item.operateTime"
                  placement="top"
                >
                  <div class="drawer-log-item">
                    <div class="drawer-log-title">{{ item.actionDesc || item.action }}</div>
                    <div class="drawer-log-operator">{{ item.operator || '-' }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无合同联动日志" :image-size="64" />
            </div>
          </section>
        </div>
        <el-empty v-else description="请选择逾期账单" />
      </el-drawer>

      <el-dialog v-model="logDialogVisible" title="合同联动日志" width="680px" append-to-body>
        <div v-loading="drawerLogLoading" class="drawer-log-list">
          <el-timeline v-if="drawerLogs.length">
            <el-timeline-item
              v-for="item in drawerLogs"
              :key="item.logId"
              :timestamp="item.operateTime"
              placement="top"
            >
              <div class="drawer-log-item">
                <div class="drawer-log-title">{{ item.actionDesc || item.action }}</div>
                <div class="drawer-log-operator">{{ item.operator || '-' }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无合同联动日志" />
        </div>
      </el-dialog>

      <notice-preview-dialog
        v-model="noticePreview.visible"
        :title="noticePreview.title"
        :html="noticePreview.html"
        :loading="noticePreview.loading"
        :download-url="noticePreview.downloadUrl"
        :download-label="noticePreview.downloadLabel"
        @download="downloadPreviewFile"
      />
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import {
  getPaymentByContract,
  getOverduePaymentPage,
  getPaymentSummary,
  getPaymentNoticeBuildings,
  getOverduePaymentLogs,
  remindOverduePayment,
  updatePaymentAttachment,
} from '@/api/ics/payment';
import { getList as getContractList } from '@/api/contract/contract';
import { feeTypeDic } from '@/option/finance/payment';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import { createNoticePreviewState, downloadNoticeFile, openAttachmentPreview } from '@/utils/contract-notice';
import { getToken } from '@/utils/auth';

export default {
  name: 'FinanceBillsOverdue',
  components: {
    NoticePreviewDialog,
  },
  data() {
    return {
      query: {
        billSource: '',
        feeType: '',
        periodStartDate: '',
        periodEndDate: '',
        settleStatus: '',
        deadlineRange: [],
        buildingNameQuery: '',
        customerName: '',
      },
      summary: {},
      buildingOptions: [],
      loading: false,
      data: [],
      selectionList: [],
      drawerVisible: false,
      drawerRow: null,
      drawerLoading: false,
      drawerActiveTab: 'contract',
      drawerContracts: [],
      drawerBills: [],
      drawerLogs: [],
      drawerLogLoading: false,
      attachmentUploadLoading: false,
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      noticePreview: createNoticePreviewState(),
      logDialogVisible: false,
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        logBtn: this.validData(this.permission.finance_overdue_reminder_log, false),
      };
    },
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
    drawerTenantName() {
      const row = this.drawerRow || {};
      return row.customerName || '-';
    },
    drawerTenantItems() {
      const row = this.drawerRow || {};
      return [
        { label: '联系人', value: row.customerName || '--' },
        { label: '行业分类', value: row.industryCategory || '--' },
        { label: '租客标签', value: row.customerTag || '--' },
        { label: '邮箱', value: row.email || row.contactEmail || '--' },
        { label: '租客编码', value: row.customerCode || '--' },
      ];
    },
    drawerBillSummary() {
      const bills = this.drawerBills || [];
      const overdueBills = bills.filter(item => item.payStatus !== '1' && this.overdueDays(item) > 0);
      const amountDue = bills.reduce((total, item) => total + Number(item.amountDue || 0), 0);
      const pending = bills.reduce((total, item) => total + this.unpaidAmount(item), 0);
      return [
        { key: 'total', label: '账单数', value: bills.length },
        { key: 'overdue', label: '逾期账单', value: overdueBills.length },
        { key: 'amount', label: '应收金额', value: this.formatMoney(amountDue) },
        { key: 'pending', label: '需收金额', value: this.formatMoney(pending) },
      ];
    },
    drawerAttachmentRows() {
      const rows = [];
      (this.drawerContracts || []).forEach(contract => {
        if (contract.contractFileUrl) {
          rows.push({
            fileName: `${contract.contractNo || '合同'}正文`,
            source: '合同',
            businessNo: contract.contractNo || '-',
            url: contract.contractFileUrl,
            fileUrl: contract.contractFileUrl,
          });
        }
      });
      (this.drawerBills || []).forEach(bill => {
        if (bill.attachmentUrl) {
          rows.push({
            fileName: bill.attachmentName || `${bill.feeName || '账单'}附件`,
            source: '账单',
            businessNo: bill.paymentId ? `ZD${bill.paymentId}` : bill.contractNo || '-',
            url: bill.attachmentUrl,
            fileUrl: bill.attachmentUrl,
            paymentId: bill.paymentId,
          });
        }
      });
      return rows;
    },
    attachmentUploadPayment() {
      if (this.drawerRow && this.drawerRow.paymentId) {
        return this.drawerRow;
      }
      return (this.drawerBills || []).find(item => item.paymentId) || null;
    },
    contractIdList() {
      const ids = new Set();
      (this.drawerContracts || []).forEach(item => {
        if (item.contractId) ids.add(String(item.contractId));
      });
      if (this.drawerRow && this.drawerRow.contractId) {
        ids.add(String(this.drawerRow.contractId));
      }
      return Array.from(ids);
    },
    contractNoMap() {
      return (this.drawerContracts || []).reduce((result, item) => {
        if (item.contractId) {
          result[String(item.contractId)] = item.contractNo || '';
        }
        return result;
      }, {});
    },
    contractStatusOptions() {
      return [
        { value: '0', label: '待审批', type: 'warning' },
        { value: '1', label: '正常执行中', type: 'success' },
        { value: '2', label: '已到期', type: 'danger' },
        { value: '3', label: '已续签', type: 'success' },
        { value: '4', label: '已退租', type: 'info' },
        { value: '5', label: '待盖章', type: 'warning' },
        { value: '6', label: '退租中', type: 'warning' },
        { value: '7', label: '退租交接中', type: 'warning' },
        { value: '8', label: '房屋验收中', type: 'warning' },
      ];
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
        settleStatus: '',
        deadlineRange: [],
        buildingNameQuery: '',
        customerName: '',
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
    openBillDrawer(row) {
      this.drawerRow = { ...(row || {}) };
      this.drawerActiveTab = 'contract';
      this.drawerVisible = true;
      this.loadTenantDrawerData(row);
    },
    closeBillDrawer() {
      this.drawerRow = null;
      this.drawerLoading = false;
      this.drawerActiveTab = 'contract';
      this.drawerContracts = [];
      this.drawerBills = [];
      this.drawerLogs = [];
      this.attachmentUploadLoading = false;
    },
    loadTenantDrawerData(row) {
      if (!row) return;
      this.drawerLoading = true;
      this.drawerContracts = [];
      this.drawerBills = [];
      this.drawerLogs = [];
      this.loadDrawerContracts(row)
        .then(() => this.loadDrawerBills(row))
        .then(() => {
          if (this.permissionList.logBtn) {
            return this.loadDrawerLogs(row);
          }
          return null;
        })
        .finally(() => {
          this.drawerLoading = false;
        });
    },
    loadDrawerContracts(row) {
      const customerName = row && row.customerName;
      if (!customerName) {
        this.drawerContracts = this.fallbackContractRows(row);
        return Promise.resolve();
      }
      return getContractList(1, 50, { customerName })
        .then(res => {
          const result = res.data.data || {};
          const records = result.records || [];
          this.drawerContracts = records.length ? records : this.fallbackContractRows(row);
        })
        .catch(() => {
          this.drawerContracts = this.fallbackContractRows(row);
        });
    },
    loadDrawerBills(row) {
      const ids = this.contractIdList;
      if (!ids.length) {
        this.drawerBills = row && row.paymentId ? [{ ...row }] : [];
        return Promise.resolve();
      }
      return Promise.all(ids.map(contractId => getPaymentByContract(contractId).catch(() => ({ data: { data: [] } }))))
        .then(results => {
          const bills = [];
          results.forEach(res => {
            const list = (res.data && res.data.data) || [];
            list.forEach(item => {
              bills.push({
                ...item,
                contractNo: item.contractNo || this.contractNoMap[String(item.contractId)] || row.contractNo,
                customerName: item.customerName || row.customerName,
              });
            });
          });
          if (!bills.length && row && row.paymentId) {
            bills.push({ ...row });
          }
          this.drawerBills = bills.sort((left, right) => this.billSortValue(right) - this.billSortValue(left));
        });
    },
    fallbackContractRows(row) {
      if (!row || !row.contractId) return [];
      return [
        {
          contractId: row.contractId,
          contractNo: row.contractNo,
          contractName: row.contractName,
          customerName: row.customerName,
          buildingName: row.buildingName,
          roomName: row.roomName,
          contractStatus: row.contractStatus,
          contractStatusName: row.contractStatusName,
        },
      ];
    },
    billSortValue(row) {
      const overdueWeight = row && row.payStatus !== '1' && this.overdueDays(row) > 0 ? 10000000000000 : 0;
      const deadline = row && row.payDeadline ? new Date(row.payDeadline).getTime() : 0;
      return overdueWeight + (Number.isNaN(deadline) ? 0 : deadline);
    },
    openLogDialog(row) {
      if (!this.permissionList.logBtn) return;
      this.logDialogVisible = true;
      this.loadDrawerLogs(row);
    },
    loadDrawerLogs(row) {
      if (!row || !row.contractId) {
        this.drawerLogs = [];
        return Promise.resolve();
      }
      this.drawerLogLoading = true;
      return getOverduePaymentLogs(row.contractId)
        .then(res => {
          this.drawerLogs = res.data.data || [];
        })
        .finally(() => {
          this.drawerLogLoading = false;
        });
    },
    handleRemind(row) {
      if (!row || !row.paymentId) return;
      this.$confirm(`确定记录“${row.customerName || '该租客'}”本次催缴吗？`, '记录催缴', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remindOverduePayment(row.paymentId))
        .then(() => {
          this.$message.success('催缴提醒已记录');
          this.drawerRow = {
            ...this.drawerRow,
            remindStatus: '1',
            remindTime: this.formatDate(new Date()),
          };
          this.drawerBills = this.drawerBills.map(item => (item.paymentId === row.paymentId
            ? { ...item, remindStatus: '1', remindTime: this.formatDate(new Date()) }
            : item));
          if (this.permissionList.logBtn) {
            this.loadDrawerLogs(row);
          }
          this.reload();
        })
        .catch(() => {});
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
    goOverdueReminder(row) {
      this.$router.push({
        path: '/finance/overdue-reminder',
        query: {
          customerName: row.customerName || '',
          contractNo: row.contractNo || '',
        },
      });
    },
    contractStatusText(value) {
      const item = this.contractStatusOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '未知';
    },
    contractStatusType(value) {
      const item = this.contractStatusOptions.find(option => String(option.value) === String(value));
      return item ? item.type : 'info';
    },
    overdueStatusText(row) {
      return this.overdueDays(row) > 0 && row.payStatus !== '1' ? '逾期' : '正常';
    },
    overdueStatusType(row) {
      return this.overdueDays(row) > 0 && row.payStatus !== '1' ? 'danger' : 'info';
    },
    settleStatusText(row) {
      if (!row) return '-';
      if (row.payStatus === '1') return '已支付';
      if (row.payStatus === '3') return '部分支付';
      return '未支付';
    },
    settleStatusType(row) {
      if (!row) return 'info';
      if (row.payStatus === '1') return 'primary';
      if (row.payStatus === '3') return 'warning';
      return 'danger';
    },
    rentPriceText(row) {
      const price = Number((row && row.rentPrice) || 0);
      if (!price) return '-';
      const unit = row.rentUnit || row.rentIncreaseUnit || '元/㎡·天';
      return `${this.formatMoney(price)}${unit}`;
    },
    beforeAttachmentUpload(file) {
      const allowed = /\.(doc|docx|xls|xlsx|ppt|pptx|txt|pdf|jpg|jpeg|png)$/i.test(file.name || '');
      const underLimit = file.size / 1024 / 1024 <= 10;
      if (!allowed) this.$message.error('仅支持 doc、xls、ppt、txt、pdf、图片格式文件');
      if (!underLimit) this.$message.error('文件大小不能超过 10MB');
      if (!this.attachmentUploadPayment) this.$message.error('当前没有可关联的账单');
      if (allowed && underLimit && this.attachmentUploadPayment) {
        this.attachmentUploadLoading = true;
      }
      return allowed && underLimit && !!this.attachmentUploadPayment;
    },
    handleAttachmentUploadSuccess(response, file) {
      const fileInfo = this.resolveUploadFile(response, file);
      if (!fileInfo.url) {
        this.attachmentUploadLoading = false;
        this.$message.error('附件上传成功，但未返回文件地址');
        return;
      }
      const payment = this.attachmentUploadPayment;
      updatePaymentAttachment(payment.paymentId, {
        attachmentName: fileInfo.name,
        attachmentUrl: fileInfo.url,
      })
        .then(res => {
          const updated = res.data.data || {
            ...payment,
            attachmentName: fileInfo.name,
            attachmentUrl: fileInfo.url,
          };
          this.applyUpdatedPaymentAttachment(updated);
          this.$message.success('附件已上传');
        })
        .finally(() => {
          this.attachmentUploadLoading = false;
          if (this.$refs.attachmentUploadRef && this.$refs.attachmentUploadRef.clearFiles) {
            this.$refs.attachmentUploadRef.clearFiles();
          }
        });
    },
    handleAttachmentUploadError(error) {
      this.attachmentUploadLoading = false;
      this.$message.error((error && error.message) || '附件上传失败');
    },
    resolveUploadFile(response, file) {
      if (!response || response.success === false) {
        this.$message.error((response && response.msg) || '附件上传失败');
        return {};
      }
      const data = response.data || {};
      const url = typeof data === 'string'
        ? data
        : data.link || data.url || data.path || response.link || response.url || '';
      return {
        name: (file && file.name) || data.originalName || data.name || '账单附件',
        url,
      };
    },
    applyUpdatedPaymentAttachment(updated) {
      const paymentId = updated.paymentId;
      this.drawerBills = this.drawerBills.map(item => (item.paymentId === paymentId ? { ...item, ...updated } : item));
      this.data = this.data.map(item => (item.paymentId === paymentId ? { ...item, ...updated } : item));
      if (this.drawerRow && this.drawerRow.paymentId === paymentId) {
        this.drawerRow = { ...this.drawerRow, ...updated };
      }
      if (this.permissionList.logBtn) {
        this.loadDrawerLogs(updated);
      }
    },
    openAttachment(row) {
      if (!row || !row.url) {
        this.$message.warning('附件地址不存在');
        return;
      }
      openAttachmentPreview(
        this.noticePreview,
        {
          fileUrl: row.url,
          fileName: row.fileName,
          sourceName: `${row.source || '附件'} ${row.businessNo || ''}`.trim(),
        },
        '附件预览'
      );
    },
    downloadAttachment(row) {
      if (!row || !row.url) {
        this.$message.warning('附件地址不存在');
        return;
      }
      downloadNoticeFile(row.url, row.fileName || '附件').catch(() => {
        this.$message.error('附件下载失败，请稍后重试');
      });
    },
    downloadPreviewFile() {
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName || '附件').catch(() => {
        this.$message.error('附件下载失败，请稍后重试');
      });
    },
    batchRemind() {
      const rows = this.selectionList || [];
      if (!rows.length) return;
      this.$confirm(`确定对已选 ${rows.length} 条逾期账单记录催缴吗？`, '批量催缴', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => Promise.all(rows.map(row => remindOverduePayment(row.paymentId))))
        .then(() => {
          this.$message.success('批量催缴已记录');
          this.reload();
        })
        .catch(() => {});
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
    formatDate(date) {
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      return `${y}-${m}-${d}`;
    },
    dateOnly(value) {
      return value ? String(value).slice(0, 10) : '';
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

.overdue-table :deep(.el-table__cell) {
  text-align: center;
}

.link-like,
.customer-link {
  color: #2f8cff;
  white-space: nowrap;
}

.customer-link {
  min-width: 0;
  padding: 0;
  font-weight: 500;
}

.overdue-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px;
}

.tenant-drawer {
  min-height: 100%;
  background: #f4f4f6;
}

.tenant-drawer-header {
  min-height: 64px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: #fff;
}

.tenant-drawer-title,
.tenant-drawer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.tenant-drawer-title {
  min-width: 0;
}

.tenant-drawer-title strong {
  min-width: 0;
  color: #303133;
  font-size: 20px;
  line-height: 28px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-drawer-back {
  width: 24px;
  height: 32px;
  padding: 0;
  color: #606266;
}

.tenant-profile {
  min-height: 76px;
  padding: 0 18px 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px 28px;
  background: #fff;
}

.tenant-profile-item {
  min-width: 0;
  display: flex;
  align-items: center;
  color: #303133;
  font-size: 14px;
  line-height: 22px;
}

.tenant-profile-item span {
  flex: 0 0 auto;
  color: #303133;
}

.tenant-profile-item strong {
  min-width: 0;
  color: #303133;
  font-weight: 400;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-tab-card {
  margin-top: 14px;
  padding: 0 18px 20px;
  background: #fff;
}

.tenant-tabs :deep(.el-tabs__header) {
  height: 72px;
  margin: 0;
  display: flex;
  align-items: center;
}

.tenant-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}

.tenant-tabs :deep(.el-tabs__item) {
  height: 72px;
  padding: 0 16px;
  color: #303133;
  font-size: 15px;
  line-height: 72px;
}

.tenant-tabs :deep(.el-tabs__item.is-active) {
  color: #1677ff;
}

.tenant-table {
  width: 100%;
  border-radius: 0;
}

.tenant-attachment-toolbar {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.tenant-table :deep(.el-table__cell) {
  height: 53px;
}

.tenant-link {
  padding: 0;
}

.tenant-bill-summary {
  margin-bottom: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.tenant-bill-summary__item {
  min-height: 64px;
  padding: 10px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: #fafafa;
}

.tenant-bill-summary__item + .tenant-bill-summary__item {
  border-left: 1px solid #ebeef5;
}

.tenant-bill-summary__item span {
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.tenant-bill-summary__item strong {
  margin-top: 4px;
  color: #303133;
  font-size: 18px;
  line-height: 24px;
  font-weight: 600;
}

.tenant-table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.tenant-table-actions :deep(.el-button) {
  margin-left: 0;
}

.drawer-section {
  margin: 14px 18px 20px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.drawer-section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.drawer-log-list {
  min-height: 120px;
}

.drawer-log-list :deep(.el-timeline) {
  padding-left: 4px;
}

.drawer-log-item {
  padding: 2px 0 8px;
}

.drawer-log-title {
  color: #1f2937;
  font-size: 14px;
  line-height: 20px;
}

.drawer-log-operator {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
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

.overdue-bill-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f4f4f6;
}

@media (max-width: 1300px) {
  .overdue-search-grid {
    grid-template-columns: repeat(2, minmax(190px, 1fr));
  }

  .tenant-profile,
  .tenant-bill-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
