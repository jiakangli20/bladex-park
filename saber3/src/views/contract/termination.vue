<template>
  <basic-container>
    <div class="termination-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="termination-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input
              v-model="query.contractNo"
              clearable
              placeholder="请输入合同编号"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="租客名称">
            <el-input
              v-model="query.customerName"
              clearable
              placeholder="请输入租客名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="流程状态">
            <el-select v-model="query.processStatus" clearable placeholder="请选择">
              <el-option
                v-for="item in approvalStatusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <el-table
        v-loading="loading"
        :data="data"
        border
        row-key="recordId"
        class="termination-table"
      >
        <el-table-column
          prop="contractNo"
          label="合同编号"
          min-width="150"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          prop="customerName"
          label="租客名称"
          min-width="150"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          prop="roomName"
          label="房源信息"
          min-width="150"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            {{ row.roomName || row.buildingName || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="processName"
          label="流程名称"
          min-width="140"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column prop="processStatus" label="审批状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="approvalStatusType(row.processStatus)" effect="plain">
              {{ approvalStatusText(row.processStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退租阶段" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="terminationStageType(row)" effect="plain">{{
              terminationStageText(row)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="currentNode"
          label="当前节点"
          min-width="130"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column prop="contractStatus" label="合同状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="contractStatusType(row.contractStatus)" effect="plain">
              {{ row.contractStatusName || statusText(row.contractStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deposit" label="保证金" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.deposit) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" width="170" align="center" />
        <el-table-column prop="approvalTime" label="完成时间" width="170" align="center">
          <template #default="{ row }">{{ row.approvalTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="360" align="center" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" @click="openContract(row)">合同详情</el-button>
              <el-button
                v-if="canStartRoomReview(row)"
                text
                type="primary"
                @click="handleStartRoomReview(row)"
              >
                房屋验收
              </el-button>
              <el-button
                v-if="canStartDepositRefund(row)"
                text
                type="primary"
                @click="handleStartDepositRefund(row)"
              >
                押金退还
              </el-button>
              <el-button
                v-if="row.printFileUrl"
                text
                type="primary"
                @click="openWorkflowFile(row.printFileUrl, row)"
              >
                审批表
              </el-button>
              <el-button
                v-if="terminationAgreementUrl(row)"
                text
                type="primary"
                @click="openWorkflowFile(terminationAgreementUrl(row), row)"
              >
                解除协议
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="termination-pagination">
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

      <el-dialog
        v-model="workflowVisible"
        :title="workflowDialogTitle"
        width="720px"
        append-to-body
        @close="resetWorkflowDialog"
      >
        <div class="workflow-dialog">
          <section class="detail-section">
            <div class="detail-section-title">{{ workflowSummaryTitle }}</div>
            <div class="field-grid">
              <div v-for="item in workflowSummaryItems" :key="item.label" class="field-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>
          <section class="detail-section">
            <div class="detail-section-title">审批配置</div>
            <el-form :model="workflowForm" label-width="108px">
              <el-form-item label="审批流程" required>
                <el-select
                  v-model="workflowForm.processDefKey"
                  filterable
                  :loading="workflowLoading"
                  :placeholder="workflowProcessPlaceholder"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in workflowProcessOptions"
                    :key="item.id || item.key"
                    :label="workflowProcessLabel(item)"
                    :value="item.key"
                  >
                    <div class="workflow-option">
                      <span>{{ item.name || item.key }}</span>
                      <em
                        >{{ item.key
                        }}<template v-if="item.version"> / v{{ item.version }}</template></em
                      >
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-form>
          </section>
        </div>
        <template #footer>
          <el-button @click="workflowVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!workflowForm.processDefKey || workflowLoading"
            @click="goWorkflow"
          >
            下一步
          </el-button>
        </template>
      </el-dialog>

      <notice-preview-dialog
        v-model="noticePreview.visible"
        :title="noticePreview.title"
        :html="noticePreview.html"
        :loading="noticePreview.loading"
        :download-url="noticePreview.downloadUrl"
        @download="downloadNoticePreviewFile"
      />
    </div>
  </basic-container>
</template>

<script>
import { Base64 } from 'js-base64';
import {
  ensureDepositRefundPayment,
  getDepositRefundPayment,
  getWorkflowRecordPage,
} from '@/api/contract/contract';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { approvalStatusDic } from '@/option/contract/archive';
import { statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import { noticePrintUrl } from '@/api/contract/print';
import {
  createNoticePreviewState,
  downloadNoticeFile,
  openNoticePreview,
} from '@/utils/contract-notice';

const TERMINATION_BUSINESS_TYPE = 'contract_termination';
const ROOM_REVIEW_BUSINESS_TYPE = 'contract_room_review';
const PAYMENT_BUSINESS_TYPE = 'contract_payment';
const TERMINATION_AGREEMENT_KEY = 'termination-agreement';
const WORKFLOW_CONFIGS = {
  [ROOM_REVIEW_BUSINESS_TYPE]: {
    title: '发起房屋验收',
    summaryTitle: '退租交接信息',
    placeholder: '请选择已部署的房屋验收流程',
    formKeys: ['return'],
    defaultKeys: ['roomreview'],
    nameKeywords: ['房屋验收', '交接验收', '归还载体'],
  },
  [PAYMENT_BUSINESS_TYPE]: {
    title: '发起押金退还',
    summaryTitle: '押金退还信息',
    placeholder: '请选择已部署的付款审批流程',
    formKeys: ['pay', 'invoice'],
    defaultKeys: ['pay'],
    nameKeywords: ['付款', '缴费', '押金退还', '押金退款'],
  },
};

export default {
  name: 'ContractTermination',
  components: {
    NoticePreviewDialog,
  },
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        processStatus: '',
      },
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      loading: false,
      data: [],
      workflowVisible: false,
      workflowLoading: false,
      workflowType: ROOM_REVIEW_BUSINESS_TYPE,
      workflowRecord: {},
      workflowPayment: {},
      workflowProcessOptions: [],
      workflowForm: {
        processDefKey: '',
      },
      noticePreview: createNoticePreviewState(),
    };
  },
  computed: {
    ...mapGetters(['userInfo']),
    approvalStatusOptions() {
      return approvalStatusDic.filter(item => item.value !== '');
    },
    workflowConfig() {
      return WORKFLOW_CONFIGS[this.workflowType] || WORKFLOW_CONFIGS[ROOM_REVIEW_BUSINESS_TYPE];
    },
    workflowDialogTitle() {
      return this.workflowConfig.title;
    },
    workflowSummaryTitle() {
      return this.workflowConfig.summaryTitle;
    },
    workflowProcessPlaceholder() {
      return this.workflowConfig.placeholder;
    },
    workflowSummaryItems() {
      const row = this.workflowRecord || {};
      const payment = this.workflowPayment || {};
      if (this.workflowType === PAYMENT_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: row.contractNo || payment.contractNo || '-' },
          { label: '企业名称', value: row.customerName || payment.customerName || '-' },
          { label: '退款事项', value: payment.feeName || '押金退还' },
          { label: '退款金额', value: this.formatMoney(payment.amountDue || row.deposit) },
          {
            label: '合同状态',
            value: row.contractStatusName || this.statusText(row.contractStatus),
          },
          {
            label: '审批状态',
            value: payment.paymentApprovalStatus
              ? this.approvalStatusText(payment.paymentApprovalStatus)
              : '未发起',
          },
        ];
      }
      return [
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '企业名称', value: row.customerName || '-' },
        { label: '交接房源', value: row.roomName || row.buildingName || '-' },
        { label: '合同期限', value: `${row.startDate || '--'} 至 ${row.endDate || '--'}` },
        { label: '保证金', value: this.formatMoney(row.deposit) },
        { label: '当前状态', value: row.contractStatusName || this.statusText(row.contractStatus) },
      ];
    },
    summaryCards() {
      const running = this.data.filter(item => item.processStatus === 'running').length;
      const approved = this.data.filter(item => item.processStatus === 'approved').length;
      const handover = this.data.filter(item => String(item.contractStatus) === '7').length;
      return [
        { key: 'total', label: '退租申请', value: this.page.total || 0 },
        { key: 'running', label: '审批中', value: running },
        { key: 'approved', label: '已通过', value: approved },
        { key: 'handover', label: '待交接', value: handover },
      ];
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    searchChange() {
      this.page.currentPage = 1;
      this.loadData();
    },
    searchReset() {
      this.query = {
        contractNo: '',
        customerName: '',
        processStatus: '',
      };
      this.page.currentPage = 1;
      this.loadData();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadData();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadData();
    },
    loadData() {
      this.loading = true;
      getWorkflowRecordPage(
        this.page.currentPage,
        this.page.pageSize,
        this.cleanParams({
          ...this.query,
          businessType: TERMINATION_BUSINESS_TYPE,
        })
      )
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          return this.enrichTerminationRecords(result.records || []);
        })
        .then(records => {
          this.data = records || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    enrichTerminationRecords(records = []) {
      const targets = records.filter(item => String(item.contractStatus) === '4' && item.contractId);
      if (!targets.length) {
        return Promise.resolve(records);
      }
      return Promise.all(
        targets.map(item =>
          getDepositRefundPayment(item.contractId)
            .then(res => {
              const payment = res.data.data || {};
              return {
                contractId: item.contractId,
                depositRefundPayStatus: String(payment.payStatus || ''),
                depositRefundPaymentId: payment.paymentId || '',
                depositRefundApprovalStatus: payment.paymentApprovalStatus || '',
              };
            })
            .catch(() => ({
              contractId: item.contractId,
              depositRefundPayStatus: '',
              depositRefundPaymentId: '',
              depositRefundApprovalStatus: '',
            }))
        )
      ).then(paymentStates => {
        const paymentStateMap = paymentStates.reduce((result, item) => {
          result[item.contractId] = item;
          return result;
        }, {});
        return records.map(item => ({
          ...item,
          ...(paymentStateMap[item.contractId] || {}),
        }));
      });
    },
    cleanParams(params) {
      return Object.keys(params || {}).reduce((result, key) => {
        const value = params[key];
        if (value !== '' && value !== undefined && value !== null) {
          result[key] = value;
        }
        return result;
      }, {});
    },
    openContract(row) {
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
          customerName: row.customerName,
        },
      });
    },
    handleStartRoomReview(row) {
      if (!this.canStartRoomReview(row)) {
        this.$message.warning('退租审批通过并进入待交接后才可以发起房屋验收');
        return;
      }
      this.workflowType = ROOM_REVIEW_BUSINESS_TYPE;
      this.workflowRecord = { ...(row || {}) };
      this.workflowPayment = {};
      this.workflowForm.processDefKey = '';
      this.workflowVisible = true;
      this.loadWorkflowProcessOptions();
    },
    handleStartDepositRefund(row) {
      if (!this.canStartDepositRefund(row)) {
        this.$message.warning('房屋验收完成后才可以发起押金退还');
        return;
      }
      ensureDepositRefundPayment(row.contractId).then(res => {
        const payment = res.data.data || {};
        if (payment.paymentApprovalStatus === 'running') {
          this.$message.warning('该押金退还付款审批正在进行中');
          return;
        }
        if (payment.payStatus === '1') {
          this.$message.warning('该押金退还已完成');
          return;
        }
        this.workflowType = PAYMENT_BUSINESS_TYPE;
        this.workflowRecord = { ...(row || {}) };
        this.workflowPayment = payment;
        this.workflowForm.processDefKey = '';
        this.workflowVisible = true;
        this.loadWorkflowProcessOptions();
      });
    },
    loadWorkflowProcessOptions() {
      this.workflowLoading = true;
      getDeploymentList(1, -1, { status: 1 })
        .then(res => {
          const records = (res.data.data || {}).records || [];
          this.workflowProcessOptions = records
            .filter(item => this.isWorkflowProcess(item))
            .sort((a, b) => {
              const versionDiff = Number(b.version || 0) - Number(a.version || 0);
              if (versionDiff !== 0) return versionDiff;
              const timeA = new Date(a.deployTime || a.createTime || 0).getTime();
              const timeB = new Date(b.deployTime || b.createTime || 0).getTime();
              return timeB - timeA;
            });
          this.workflowForm.processDefKey = this.resolveWorkflowProcessKey(
            this.workflowProcessOptions
          );
          if (this.workflowProcessOptions.length === 0) {
            this.$message.warning(
              `未找到可用的${this.workflowDialogTitle.replace('发起', '')}，请先在部署管理激活流程`
            );
          }
        })
        .finally(() => {
          this.workflowLoading = false;
        });
    },
    isWorkflowProcess(item = {}) {
      const name = item.name || '';
      const key = item.key || '';
      const formKey = item.formKey || '';
      const config = this.workflowConfig;
      return (
        (config.formKeys || []).includes(formKey) ||
        (config.defaultKeys || []).includes(key) ||
        (config.nameKeywords || []).some(keyword => name.includes(keyword) || key.includes(keyword))
      );
    },
    resolveWorkflowProcessKey(processOptions = []) {
      const config = this.workflowConfig;
      const preferred =
        processOptions.find(item => (config.formKeys || []).includes(item.formKey)) ||
        processOptions.find(item => (config.defaultKeys || []).includes(item.key)) ||
        processOptions.find(item =>
          (config.nameKeywords || []).some(keyword => (item.name || '').includes(keyword))
        ) ||
        processOptions[0];
      return preferred ? preferred.key : '';
    },
    workflowProcessLabel(item = {}) {
      const name = item.name || item.key || '-';
      const key = item.key ? ` / ${item.key}` : '';
      const version = item.version ? ` v${item.version}` : '';
      return `${name}${version}${key}`;
    },
    canStartRoomReview(row) {
      return Boolean(
        row &&
          row.contractId &&
          row.processStatus === 'approved' &&
          String(row.contractStatus) === '7'
      );
    },
    canStartDepositRefund(row) {
      return Boolean(
        row &&
          row.contractId &&
          row.processStatus === 'approved' &&
          String(row.contractStatus) === '4' &&
          !this.isDepositRefundCompleted(row)
      );
    },
    isDepositRefundCompleted(row) {
      return String(row?.depositRefundPayStatus || '') === '1';
    },
    goWorkflow() {
      if (!this.workflowRecord.contractId) {
        this.$message.warning('请选择退租记录');
        return;
      }
      if (this.workflowType === PAYMENT_BUSINESS_TYPE && !this.workflowPayment.paymentId) {
        this.$message.warning('请先生成押金退还付款单');
        return;
      }
      if (!this.workflowForm.processDefKey) {
        this.$message.warning('请选择审批流程');
        return;
      }
      const payload =
        this.workflowType === PAYMENT_BUSINESS_TYPE
          ? this.buildDepositRefundPayload()
          : this.buildRoomReviewPayload();
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      this.workflowVisible = false;
      this.$router.push(`/plugin/workflow/pages/process/form/start/${encodedParam}`);
    },
    buildRoomReviewPayload() {
      const row = this.workflowRecord || {};
      return {
        processDefKey: this.workflowForm.processDefKey,
        params: {
          processDefKey: this.workflowForm.processDefKey,
          businessType: ROOM_REVIEW_BUSINESS_TYPE,
          businessTable: 'biz_contract',
          businessKey: String(row.contractId || ''),
          contractId: row.contractId,
          contractNo: row.contractNo,
          contractName: row.contractName,
          customerId: row.customerId,
          customerName: row.customerName,
          roomIds: row.roomIds,
          roomName: row.roomName,
          buildingName: row.buildingName,
          parkId: row.parkId,
          monthlyRent: row.monthlyRent,
          deposit: row.deposit,
          startDate: row.startDate,
          endDate: row.endDate,
          sourceTerminationRecordId: row.recordId,
          templateKey: 'room-review',
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    buildDepositRefundPayload() {
      const row = this.workflowRecord || {};
      const payment = this.workflowPayment || {};
      const processKey = this.workflowForm.processDefKey || '';
      const isInvoiceWorkflow = processKey.includes('invoice');
      return {
        processDefKey: this.workflowForm.processDefKey,
        params: {
          processDefKey: this.workflowForm.processDefKey,
          businessType: PAYMENT_BUSINESS_TYPE,
          businessTable: 'biz_contract_payment',
          businessKey: String(payment.paymentId || ''),
          contractId: row.contractId || payment.contractId,
          paymentId: payment.paymentId,
          contractNo: row.contractNo || payment.contractNo,
          contractName: row.contractName || payment.contractName,
          customerId: row.customerId,
          customerName: row.customerName || payment.customerName,
          roomIds: row.roomIds,
          roomName: row.roomName,
          buildingName: row.buildingName,
          parkId: row.parkId || payment.parkId,
          feeType: payment.feeType || 'deposit_refund',
          feeName: payment.feeName || '押金退还',
          periodStart: payment.periodStart,
          periodEnd: payment.periodEnd,
          amountDue: payment.amountDue || row.deposit,
          amountPaid: payment.amountPaid,
          payDeadline: payment.payDeadline,
          templateKey: isInvoiceWorkflow ? 'invoice-apply' : 'payment-notice',
          refundType: 'deposit_refund',
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    resetWorkflowDialog() {
      this.workflowType = ROOM_REVIEW_BUSINESS_TYPE;
      this.workflowRecord = {};
      this.workflowPayment = {};
      this.workflowProcessOptions = [];
      this.workflowForm.processDefKey = '';
    },
    openWorkflowFile(url, row) {
      if (!url || !row) return;
      const noticeType = this.noticeTypeByUrl(url, row);
      if (!noticeType) {
        downloadNoticeFile(url, '退租流程文件');
        return;
      }
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          contractId: row.contractId,
          formDataJson: row.formDataJson || '',
        },
        noticePrintUrl(noticeType, { contractId: row.contractId }),
        '退租流程文件',
        '审批表预览'
      );
    },
    noticeTypeByUrl(url, row) {
      if (url === this.terminationAgreementUrl(row) || String(url).includes('termination-agreement')) {
        return 'termination-agreement';
      }
      if (String(url).includes('room-review')) {
        return 'room-review';
      }
      if (String(url).includes('payment-notice')) {
        return 'payment-notice';
      }
      if (String(url).includes('invoice-apply')) {
        return 'invoice-apply';
      }
      if (row.businessType === PAYMENT_BUSINESS_TYPE) {
        return row.processDefKey && String(row.processDefKey).includes('invoice')
          ? 'invoice-apply'
          : 'payment-notice';
      }
      return 'termination-approval';
    },
    downloadNoticePreviewFile() {
      if (!this.noticePreview.downloadUrl) return;
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName);
    },
    terminationAgreementUrl(row) {
      return this.attachmentUrl(row, TERMINATION_AGREEMENT_KEY);
    },
    attachmentUrl(row, key) {
      if (!row || !row.attachmentJson || !key) return '';
      try {
        const attachment = JSON.parse(row.attachmentJson);
        return attachment && attachment[key] ? attachment[key] : '';
      } catch (error) {
        return '';
      }
    },
    terminationStageText(row) {
      if (!row) return '-';
      if (row.processStatus === 'running') return '退租审批中';
      if (row.processStatus === 'rejected') return '退租驳回';
      if (row.processStatus === 'canceled') return '退租取消';
      if (String(row.contractStatus) === '8') return '房屋验收中';
      if (this.isDepositRefundCompleted(row)) return '退租已完成';
      if (String(row.contractStatus) === '4') return '待押金退还';
      if (String(row.contractStatus) === '7') return '待房屋验收';
      if (row.processStatus === 'approved') return '退租已通过';
      return this.approvalStatusText(row.processStatus);
    },
    terminationStageType(row) {
      if (!row) return 'info';
      if (row.processStatus === 'rejected') return 'danger';
      if (row.processStatus === 'running' || String(row.contractStatus) === '8') return 'warning';
      if (this.isDepositRefundCompleted(row)) return 'success';
      if (String(row.contractStatus) === '4') return 'success';
      if (String(row.contractStatus) === '7') return 'primary';
      return 'info';
    },
    statusText(value) {
      const item = statusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '-';
    },
    contractStatusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'primary',
        4: 'danger',
        5: 'warning',
        6: 'warning',
        7: 'primary',
        8: 'warning',
      };
      return map[String(value || '')] || 'info';
    },
    approvalStatusText(value) {
      const item = approvalStatusDic.find(ele => String(ele.value) === String(value || ''));
      return item ? item.label : '-';
    },
    approvalStatusType(value) {
      const map = {
        running: 'warning',
        approved: 'success',
        rejected: 'danger',
        canceled: 'info',
        deleted: 'info',
      };
      return map[String(value || '')] || 'info';
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
      const m = date.getMonth() + 1;
      const d = date.getDate();
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.termination-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-card span {
  color: #606266;
  font-size: 13px;
}

.summary-card strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

.termination-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.termination-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.termination-search :deep(.el-input),
.termination-search :deep(.el-select) {
  width: 190px;
}

.termination-table {
  width: 100%;
  border-radius: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  min-width: 40px;
  padding: 0 3px;
  margin-left: 0;
}

.workflow-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-section {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-weight: 600;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.field-item {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}

.field-item span {
  flex: 0 0 104px;
  color: #8c98aa;
}

.field-item strong {
  min-width: 0;
  color: #303133;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.workflow-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.workflow-option em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.termination-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.termination-page :deep(.el-button),
.termination-page :deep(.el-input__wrapper),
.termination-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
