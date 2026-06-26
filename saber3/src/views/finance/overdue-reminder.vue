<template>
  <basic-container>
    <div class="overdue-reminder-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="reminder-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input v-model="query.contractNo" clearable placeholder="请输入合同编号" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="租客名称">
            <el-input v-model="query.customerName" clearable placeholder="请输入租客名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="费用类型">
            <el-select v-model="query.feeType" clearable placeholder="全部类型">
              <el-option v-for="item in feeTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="reminder-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" plain icon="el-icon-document" :disabled="!singleSelected" @click="handleGenerateNotice(singleSelected, 'overdue-notice')">
            生成逾期通知
          </el-button>
          <el-button type="warning" plain icon="el-icon-s-promotion" :disabled="!singleSelected" @click="handleStartOverdueApproval(singleSelected)">
            发起律师函审批
          </el-button>
          <el-button type="danger" plain icon="el-icon-position" :disabled="!singleSelected" @click="handleStartTermination(singleSelected)">
            发起退租审批
          </el-button>
          <el-button type="danger" plain icon="el-icon-warning" :disabled="!singleSelected" @click="handleRefusalDispose(singleSelected)">
            拒不返还处理
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <section class="reminder-table-wrap">
        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="paymentId"
          class="reminder-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="48" align="center" />
          <el-table-column label="账单编号" width="118" align="center">
            <template #default="{ row }">ZD{{ row.paymentId }}</template>
          </el-table-column>
          <el-table-column prop="customerName" label="租客名称" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="contractNo" label="合同编号" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column label="房源信息" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.roomName || row.buildingName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" width="100" align="center" />
          <el-table-column label="账期" min-width="180" align="center">
            <template #default="{ row }">{{ row.periodStart || '-' }} 至 {{ row.periodEnd || '-' }}</template>
          </el-table-column>
          <el-table-column prop="amountDue" label="应收金额" width="116" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="已缴金额" width="116" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column label="未缴金额" width="116" align="right">
            <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="116" align="center" />
          <el-table-column label="逾期天数" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="danger" effect="plain">{{ overdueDays(row) }}天</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remindStatus" label="催缴状态" width="116" align="center">
            <template #default="{ row }">
              <el-tag :type="row.remindStatus === '1' ? 'success' : 'info'" effect="plain">
                {{ row.remindStatus === '1' ? '已催缴' : '未催缴' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="overdueApprovalStatus" label="律师函审批" width="126" align="center">
            <template #default="{ row }">
              <el-tag :type="approvalStatusType(row.overdueApprovalStatus)" effect="plain">
                {{ approvalStatusText(row.overdueApprovalStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="340" align="center" fixed="right">
            <template #default="{ row }">
              <div class="table-actions">
                <el-button text type="primary" @click="openContract(row)">合同</el-button>
                <el-button text type="warning" :disabled="row.overdueApprovalStatus === 'running'" @click="handleStartOverdueApproval(row)">
                  律师函审批
                </el-button>
                <el-dropdown trigger="click" @command="command => handleNoticeCommand(row, command)">
                  <el-button text type="primary">通知文件</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="project-approval">项目审批表</el-dropdown-item>
                      <el-dropdown-item command="payment-notice">付款通知单</el-dropdown-item>
                      <el-dropdown-item command="reminder-notice">催款通知书</el-dropdown-item>
                      <el-dropdown-item command="overdue-notice">逾期处理通知书</el-dropdown-item>
                      <el-dropdown-item command="legal-letter">律师函</el-dropdown-item>
                      <el-dropdown-item command="move-out-notice">限期搬离通知书</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
                <el-button text type="primary" @click="handleSendMiniApp(row)">发小程序</el-button>
                <el-button text type="danger" :disabled="row.terminationApprovalStatus === 'running'" @click="handleRefusalDispose(row)">
                  拒不返还
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="reminder-pagination">
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

      <el-dialog v-model="workflowVisible" :title="workflowDialogTitle" width="760px" append-to-body @close="resetWorkflowDialog">
        <section class="workflow-section">
          <div class="workflow-section-title">{{ workflowSummaryTitle }}</div>
          <div class="workflow-field-grid">
            <div v-for="item in workflowSummaryItems" :key="item.label" class="workflow-field">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </section>
        <section class="workflow-section">
          <div class="workflow-section-title">审批配置</div>
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
                    <em>{{ item.key }}<template v-if="item.version"> / v{{ item.version }}</template></em>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-form>
        </section>
        <template #footer>
          <el-button @click="workflowVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!workflowForm.processDefKey || workflowLoading" @click="goWorkflow">
            下一步
          </el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { Base64 } from 'js-base64';
import { mapGetters } from 'vuex';
import {
  generateContractNotice,
  getOverdueReminderPage,
  getOverdueReminderSummary,
  remindOverduePayment,
  sendMiniAppNotice,
} from '@/api/ics/payment';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { feeTypeDic } from '@/option/finance/payment';
import { baseUrl } from '@/config/env';

const OVERDUE_WORKFLOW = 'contract_overdue_legal';
const TERMINATION_WORKFLOW = 'contract_termination';
const WORKFLOW_CONFIG = {
  [OVERDUE_WORKFLOW]: {
    title: '发起逾期律师函审批',
    summaryTitle: '逾期账单信息',
    processPlaceholder: '请选择已部署的逾期律师函流程',
    formKeys: ['project-approval-law'],
    defaultKeys: ['law'],
    nameKeywords: ['律师函', '逾期', '项目审批'],
  },
  [TERMINATION_WORKFLOW]: {
    title: '发起退租审批',
    summaryTitle: '退租信息',
    processPlaceholder: '请选择已部署的退租审批流程',
    formKeys: ['terminate'],
    defaultKeys: ['termination'],
    nameKeywords: ['退租审批', '退租'],
  },
};

export default {
  name: 'FinanceOverdueReminder',
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        feeType: '',
      },
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      loading: false,
      data: [],
      summary: {},
      selectionList: [],
      workflowVisible: false,
      workflowLoading: false,
      workflowType: OVERDUE_WORKFLOW,
      workflowRow: {},
      workflowProcessOptions: [],
      workflowForm: {
        processDefKey: '',
      },
    };
  },
  computed: {
    ...mapGetters(['userInfo']),
    feeTypeOptions() {
      return feeTypeDic;
    },
    singleSelected() {
      return this.selectionList.length === 1 ? this.selectionList[0] : null;
    },
    summaryCards() {
      const running = this.data.filter(item => item.overdueApprovalStatus === 'running').length;
      const approved = this.data.filter(item => item.overdueApprovalStatus === 'approved').length;
      return [
        { key: 'overdue', label: '逾期账单', value: this.summary.overdueCount || this.page.total || 0 },
        { key: 'pending', label: '未收金额', value: this.formatMoney(this.summary.amountPending) },
        { key: 'reminded', label: '已催缴', value: this.summary.remindedCount || 0 },
        { key: 'running', label: '律师函审批中', value: running },
        { key: 'approved', label: '审批通过', value: approved },
      ];
    },
    workflowConfig() {
      return WORKFLOW_CONFIG[this.workflowType] || WORKFLOW_CONFIG[OVERDUE_WORKFLOW];
    },
    workflowDialogTitle() {
      return this.workflowConfig.title;
    },
    workflowSummaryTitle() {
      return this.workflowConfig.summaryTitle;
    },
    workflowProcessPlaceholder() {
      return this.workflowConfig.processPlaceholder;
    },
    workflowSummaryItems() {
      const row = this.workflowRow || {};
      return [
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '租客名称', value: row.customerName || '-' },
        { label: '房源信息', value: row.roomName || row.buildingName || '-' },
        { label: '费用类型', value: row.feeName || '-' },
        { label: '账期', value: `${row.periodStart || '--'} 至 ${row.periodEnd || '--'}` },
        { label: '未缴金额', value: this.formatMoney(this.unpaidAmount(row)) },
      ];
    },
  },
  created() {
    this.reload();
  },
  methods: {
    reload() {
      this.loadSummary();
      this.loadData();
    },
    loadData() {
      this.loading = true;
      getOverdueReminderPage(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          this.data = result.records || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadSummary() {
      getOverdueReminderSummary(this.cleanParams(this.query)).then(res => {
        this.summary = res.data.data || {};
      });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {
        contractNo: '',
        customerName: '',
        feeType: '',
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
    selectionChange(list) {
      this.selectionList = list;
    },
    handleNoticeCommand(row, noticeType) {
      this.handleGenerateNotice(row, noticeType);
    },
    handleGenerateNotice(row, noticeType) {
      if (!row || !row.paymentId) return;
      const params = {
        noticeType,
        paymentId: row.paymentId,
        contractId: row.contractId,
      };
      generateContractNotice(params).then(res => {
        const file = res.data.data || {};
        const url = file.fileUrl || this.printUrl(row, noticeType);
        window.open(url, '_blank');
        this.$message.success(`${file.noticeName || '通知文件'}已生成`);
      });
    },
    handleSendMiniApp(row) {
      if (!row || !row.paymentId) return;
      sendMiniAppNotice({
        noticeType: 'overdue-notice',
        paymentId: row.paymentId,
        contractId: row.contractId,
      }).then(() => {
        remindOverduePayment(row.paymentId).finally(() => this.reload());
        this.$message.success('小程序发送数据已生成，已预留给小程序接口');
      });
    },
    handleStartOverdueApproval(row) {
      if (!row || !row.paymentId) return;
      if (row.overdueApprovalStatus === 'running') {
        this.$message.warning('该账单律师函审批正在进行中');
        return;
      }
      this.openWorkflowDialog(OVERDUE_WORKFLOW, row);
    },
    handleStartTermination(row) {
      if (!row || !row.contractId) return;
      if (row.terminationApprovalStatus === 'running') {
        this.$message.warning('该合同退租审批正在进行中');
        return;
      }
      this.openWorkflowDialog(TERMINATION_WORKFLOW, row);
    },
    handleRefusalDispose(row) {
      if (!row || !row.paymentId || !row.contractId) return;
      if (row.terminationApprovalStatus === 'running') {
        this.$message.warning('该合同退租审批正在进行中');
        return;
      }
      this.$confirm('将生成《限期搬离通知书》，并进入退租审批流程，是否继续?', {
        confirmButtonText: '继续',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() =>
          generateContractNotice({
            noticeType: 'move-out-notice',
            paymentId: row.paymentId,
            contractId: row.contractId,
          })
        )
        .then(res => {
          const file = res.data.data || {};
          const url = file.fileUrl || this.printUrl(row, 'move-out-notice');
          if (url) {
            window.open(url, '_blank');
          }
          this.$message.success('限期搬离通知书已生成，请继续发起退租审批');
          this.openWorkflowDialog(TERMINATION_WORKFLOW, row);
        });
    },
    openWorkflowDialog(type, row) {
      this.workflowType = type;
      this.workflowRow = { ...(row || {}) };
      this.workflowForm.processDefKey = '';
      this.workflowVisible = true;
      this.loadWorkflowProcessOptions();
    },
    loadWorkflowProcessOptions() {
      this.workflowLoading = true;
      getDeploymentList(1, -1, { status: 1 })
        .then(res => {
          const records = (res.data.data || {}).records || [];
          this.workflowProcessOptions = records
            .filter(item => this.isWorkflowProcess(item))
            .sort((a, b) => Number(b.version || 0) - Number(a.version || 0));
          this.workflowForm.processDefKey = this.resolveWorkflowProcessKey(this.workflowProcessOptions);
          if (this.workflowProcessOptions.length === 0) {
            this.$message.warning(`未找到可用的${this.workflowDialogTitle.replace('发起', '')}，请先在部署管理激活流程`);
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
      return (config.formKeys || []).includes(formKey)
        || (config.defaultKeys || []).includes(key)
        || (config.nameKeywords || []).some(keyword => name.includes(keyword) || key.includes(keyword));
    },
    resolveWorkflowProcessKey(processOptions = []) {
      const config = this.workflowConfig;
      const preferred =
        processOptions.find(item => (config.formKeys || []).includes(item.formKey))
        || processOptions.find(item => (config.defaultKeys || []).includes(item.key))
        || processOptions.find(item => (config.nameKeywords || []).some(keyword => (item.name || '').includes(keyword)))
        || processOptions[0];
      return preferred ? preferred.key : '';
    },
    workflowProcessLabel(item = {}) {
      const name = item.name || item.key || '-';
      const key = item.key ? ` / ${item.key}` : '';
      const version = item.version ? ` v${item.version}` : '';
      return `${name}${version}${key}`;
    },
    goWorkflow() {
      if (!this.workflowForm.processDefKey) {
        this.$message.warning('请选择审批流程');
        return;
      }
      const payload = this.buildWorkflowPayload();
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      this.workflowVisible = false;
      this.$router.push(`/plugin/workflow/pages/process/form/start/${encodedParam}`);
    },
    buildWorkflowPayload() {
      const row = this.workflowRow || {};
      const commonParams = {
        processDefKey: this.workflowForm.processDefKey,
        contractId: row.contractId,
        paymentId: row.paymentId,
        contractNo: row.contractNo,
        contractName: row.contractName,
        customerName: row.customerName,
        roomName: row.roomName,
        buildingName: row.buildingName,
        parkId: row.parkId,
        feeName: row.feeName,
        periodStart: row.periodStart,
        periodEnd: row.periodEnd,
        amountDue: row.amountDue,
        amountPaid: row.amountPaid,
        unpaidAmount: this.unpaidAmount(row),
        payDeadline: row.payDeadline,
        overdueDays: this.overdueDays(row),
        applicant: this.userInfo.nick_name,
        applicantDept: this.userInfo.dept_name,
        applyTime: this.formatDate(new Date()),
      };
      if (this.workflowType === TERMINATION_WORKFLOW) {
        return {
          processDefKey: this.workflowForm.processDefKey,
          params: {
            ...commonParams,
            businessType: TERMINATION_WORKFLOW,
            businessTable: 'biz_contract',
            businessKey: String(row.contractId || ''),
            templateKey: 'termination-approval',
            expectedTerminationDate: this.formatDate(new Date()),
          },
        };
      }
      return {
        processDefKey: this.workflowForm.processDefKey,
        params: {
          ...commonParams,
          businessType: OVERDUE_WORKFLOW,
          businessTable: 'biz_contract_payment',
          businessKey: String(row.paymentId || ''),
          paymentId: row.paymentId,
          templateKey: 'legal-letter',
        },
      };
    },
    resetWorkflowDialog() {
      this.workflowType = OVERDUE_WORKFLOW;
      this.workflowRow = {};
      this.workflowForm.processDefKey = '';
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
    printUrl(row, noticeType) {
      const typeMap = {
        'project-approval': `${baseUrl}/blade-contract/print/project-approval/${row.paymentId}`,
        'payment-notice': `${baseUrl}/blade-contract/print/payment-notice/${row.paymentId}`,
        'reminder-notice': `${baseUrl}/blade-contract/print/reminder-notice/${row.paymentId}`,
        'overdue-notice': `${baseUrl}/blade-contract/print/overdue-notice/${row.paymentId}`,
        'legal-letter': `${baseUrl}/blade-contract/print/legal-letter/${row.paymentId}`,
        'move-out-notice': `${baseUrl}/blade-contract/print/move-out-notice/payment/${row.paymentId}`,
      };
      return typeMap[noticeType] || typeMap['overdue-notice'];
    },
    approvalStatusText(value) {
      const map = {
        running: '审批中',
        approved: '已通过',
        rejected: '已驳回',
        canceled: '已取消',
        deleted: '已删除',
      };
      return map[String(value || '')] || '未发起';
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
    cleanParams(params) {
      return Object.keys(params || {}).reduce((result, key) => {
        const value = params[key];
        if (value !== '' && value !== undefined && value !== null) {
          result[key] = value;
        }
        return result;
      }, {});
    },
  },
};
</script>

<style lang="scss" scoped>
.overdue-reminder-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 84px;
  padding: 16px 18px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.summary-card span {
  color: #6b7280;
  font-size: 13px;
}

.summary-card strong {
  color: #111827;
  font-size: 24px;
  font-weight: 600;
}

.reminder-search,
.reminder-toolbar,
.reminder-table-wrap {
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.reminder-search {
  padding: 16px 18px 4px;
}

.reminder-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.reminder-search :deep(.el-input),
.reminder-search :deep(.el-select) {
  width: 190px;
}

.reminder-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}

.toolbar-left,
.table-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reminder-table-wrap {
  padding: 0;
  overflow: hidden;
}

.reminder-table {
  width: 100%;
}

.reminder-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

.workflow-section {
  margin-bottom: 16px;
}

.workflow-section-title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.workflow-field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.workflow-field {
  min-height: 68px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #fafafa;
}

.workflow-field span {
  display: block;
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 12px;
}

.workflow-field strong {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  word-break: break-all;
}

.workflow-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workflow-option em {
  color: #909399;
  font-style: normal;
  font-size: 12px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
