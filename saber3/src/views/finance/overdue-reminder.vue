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

      <section class="reminder-table-wrap">
        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="paymentId"
          scrollbar-always-on
          class="reminder-table"
        >
          <el-table-column label="账单编号" width="118" align="center">
            <template #default="{ row }">ZD{{ row.paymentId }}</template>
          </el-table-column>
          <el-table-column prop="customerName" label="租客名称" :min-width="customerNameColumnWidth" align="center">
            <template #default="{ row }">
              <el-button text type="primary" class="customer-link" @click="openReminderDrawer(row)">
                {{ row.customerName || '-' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="contractNo" label="合同编号" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column label="房源信息" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.roomName || row.buildingName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" width="100" align="center" />
          <el-table-column label="账期" width="210" align="center">
            <template #default="{ row }">
              <span class="period-cell">{{ row.periodStart || '-' }} 至 {{ row.periodEnd || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="amountDue" label="应收金额" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="已缴金额" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column label="未缴金额" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
          </el-table-column>
          <el-table-column label="结清状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="isSettled(row) ? 'success' : 'danger'" effect="plain">
                {{ isSettled(row) ? '已结清' : '未结清' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="116" align="center" />
          <el-table-column label="逾期天数" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="isSettled(row) ? 'info' : 'danger'" effect="plain">
                {{ isSettled(row) ? '已结清' : `${overdueDays(row)}天` }}
              </el-tag>
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
          <el-table-column label="操作" width="96" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openReminderDrawer(row)">处理</el-button>
            </template>
          </el-table-column>
          <template #append>
            <div class="table-scroll-gutter" aria-hidden="true"></div>
          </template>
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

      <el-drawer
        v-model="drawerVisible"
        title="逾期处置"
        size="760px"
        append-to-body
        class="overdue-action-drawer"
      >
        <div v-if="drawerRow" class="drawer-body">
          <section class="drawer-profile">
            <div class="drawer-profile__main">
              <span>租客名称</span>
              <strong>{{ drawerRow.customerName || '-' }}</strong>
              <em>{{ drawerRow.contractNo || '暂无合同编号' }}</em>
            </div>
            <el-tag :type="isSettled(drawerRow) ? 'success' : 'danger'" effect="plain">
              {{ isSettled(drawerRow) ? '已结清' : '逾期未结清' }}
            </el-tag>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">账单信息</div>
            <div class="drawer-field-grid">
              <div v-for="item in drawerSummaryItems" :key="item.label" class="drawer-field">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">处置申请</div>
            <div class="drawer-action-grid">
              <el-button type="primary" plain @click="openContract(drawerRow)">查看合同</el-button>
              <el-button
                type="warning"
                plain
                :disabled="isSettled(drawerRow)"
                @click="openRecipientDialog(drawerRow)"
              >
                催缴通知
              </el-button>
              <el-button
                type="warning"
                plain
                :disabled="isSettled(drawerRow) || workflowStatus('contract_overdue_legal') === 'running'"
                @click="handleStartOverdueApproval(drawerRow)"
              >
                律师函申请
              </el-button>
              <el-button
                type="danger"
                plain
                :disabled="isSettled(drawerRow) || workflowStatus('contract_termination') === 'running'"
                @click="handleStartTermination(drawerRow)"
              >
                退租申请
              </el-button>
              <el-button
                type="primary"
                plain
                :disabled="workflowStatus('contract_room_review') === 'running' || workflowStatus('contract_room_review') === 'approved'"
                @click="handleStartRoomReview(drawerRow)"
              >
                房屋验收申请
              </el-button>
              <el-button v-if="permissionList.logBtn" plain @click="openLogDialog(drawerRow)">联动日志</el-button>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title-row">
              <div class="drawer-section-title">处置前置</div>
              <el-tag :type="isSettled(drawerRow) ? 'success' : 'warning'" effect="plain">
                {{ isSettled(drawerRow) ? '账单已结清，历史保留' : '处置进行中' }}
              </el-tag>
            </div>
            <div v-loading="disposalLoading" class="precondition-grid">
              <div
                v-for="item in disposalPrerequisiteItems"
                :key="item.key"
                :class="['precondition-item', { 'is-done': item.done }]"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">流程生成文件</div>
            <div v-if="workflowGeneratedFiles.length" class="document-list">
              <div v-for="item in workflowGeneratedFiles" :key="item.fileKey" class="document-row">
                <div class="document-row__name">
                  <strong>{{ item.fileName }}</strong>
                  <span>{{ item.sourceLabel || workflowBusinessTypeText(item.businessType) }} / {{ item.approvalTime || item.updateTime || '-' }}</span>
                </div>
                <div class="document-row__actions">
                  <el-button text type="primary" @click="previewWorkflowFile(item)">预览</el-button>
                  <el-button text type="primary" @click="downloadWorkflowFile(item)">下载</el-button>
                </div>
              </div>
            </div>
            <el-empty v-else :image-size="54" description="审批完成后，律师函申请审批表、律师函、退租审批表和房屋验收文件将在此生成" />
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">通知文书</div>
            <div class="document-list">
              <div v-for="item in drawerNoticeTypes" :key="item.value" class="document-row">
                <div class="document-row__name">
                  <strong>{{ item.label }}</strong>
                  <span>非审批 Word 文书</span>
                </div>
                <div class="document-row__actions">
                  <el-button text type="primary" @click="previewNotice(drawerRow, item.value)">预览</el-button>
                  <el-button
                    text
                    type="primary"
                    :disabled="isSettled(drawerRow)"
                    @click="handleGenerateNotice(drawerRow, item.value)"
                  >
                    生成并下载
                  </el-button>
                </div>
              </div>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">小程序发送</div>
            <div class="drawer-action-grid">
              <el-button
                v-for="item in drawerMiniAppTypes"
                :key="item.value"
                plain
                type="primary"
                :disabled="isSettled(drawerRow)"
                @click="handleSendMiniApp(drawerRow, item.value)"
              >
                {{ item.label }}
              </el-button>
            </div>
          </section>

		  <section class="drawer-section">
				<div class="drawer-section-title">首次逾期通知记录</div>
			<div class="record-list">
			  <div
				v-for="item in disposalDetail.internalNotices"
				:key="item.noticeId"
				class="record-item"
			  >
				<div>
				  <strong>{{ item.recipientName || item.recipientAccount || '-' }}</strong>
				  <span>{{ item.recipientRoles || '-' }} / {{ item.createTime || '-' }}</span>
				</div>
				<el-tag :type="item.readStatus === '1' ? 'success' : 'warning'" effect="plain">
				  {{ item.readStatus === '1' ? '已读' : '未读' }}
				</el-tag>
			  </div>
			  <el-empty v-if="!disposalDetail.internalNotices.length" description="暂无内部通知记录" />
			</div>
		  </section>

          <section class="drawer-section">
            <div class="drawer-section-title">文书生成记录</div>
            <div class="record-list">
              <div v-for="item in disposalDetail.documentRecords" :key="item.logId" class="record-item">
                <div>
                  <strong>{{ item.actionDesc || item.action }}</strong>
                  <span>{{ item.operator || '-' }} / {{ item.operateTime || '-' }}</span>
                </div>
                <el-tag effect="plain" type="success">已生成</el-tag>
              </div>
              <el-empty v-if="!disposalDetail.documentRecords.length" description="暂无生成记录" />
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">小程序发送记录</div>
            <div class="record-list">
              <div v-for="item in disposalDetail.miniAppRecords" :key="item.logId" class="record-item">
                <div>
                  <strong>{{ item.actionDesc || item.action }}</strong>
                  <span>{{ item.operator || '-' }} / {{ item.operateTime || '-' }}</span>
                </div>
                <el-tag effect="plain" type="primary">已生成</el-tag>
              </div>
              <el-empty v-if="!disposalDetail.miniAppRecords.length" description="暂无小程序发送记录" />
            </div>
          </section>

        </div>
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

      <overdue-recipient-dialog
        v-model="recipientVisible"
        :payment="recipientPayment"
        @sent="handleRecipientSent"
      />

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
import { mapGetters } from 'vuex';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import OverdueRecipientDialog from './components/overdue-recipient-dialog.vue';
import { noticePrintUrl } from '@/api/contract/print';
import {
  generateContractNotice,
  getOverdueDisposalDetail,
  getOverduePaymentLogs,
  getOverdueReminderPage,
  getOverdueReminderSummary,
	readOverdueInternalNotice,
  remindOverduePayment,
  sendMiniAppNotice,
} from '@/api/ics/payment';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { feeTypeDic } from '@/option/finance/payment';
import {
  createNoticePreviewState,
  downloadNoticeFile,
  openNoticePreview,
} from '@/utils/contract-notice';

const OVERDUE_WORKFLOW = 'contract_overdue_legal';
const TERMINATION_WORKFLOW = 'contract_termination';
const ROOM_REVIEW_WORKFLOW = 'contract_room_review';
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
  [ROOM_REVIEW_WORKFLOW]: {
    title: '发起房屋验收流程',
    summaryTitle: '退租交接信息',
    processPlaceholder: '请选择已部署的房屋验收流程',
    formKeys: ['return'],
    defaultKeys: ['roomreview'],
    nameKeywords: ['房屋验收', '交接验收', '归还载体'],
  },
};

export default {
  name: 'FinanceOverdueReminder',
  components: {
    NoticePreviewDialog,
    OverdueRecipientDialog,
  },
  data() {
    return {
      query: {
        paymentId: '',
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
      drawerVisible: false,
      drawerRow: null,
      drawerLogs: [],
      drawerLogLoading: false,
      logDialogVisible: false,
      disposalLoading: false,
      disposalDetail: {
        paymentNotice: null,
        documentRecords: [],
        miniAppRecords: [],
        workflowRecords: [],
		internalNotices: [],
      },
      workflowVisible: false,
      workflowLoading: false,
      workflowType: OVERDUE_WORKFLOW,
      workflowRow: {},
      workflowProcessOptions: [],
      workflowForm: {
        processDefKey: '',
      },
      noticePreview: createNoticePreviewState(),
      recipientVisible: false,
      recipientPayment: {},
      routePaymentId: '',
      routeDrawerOpened: false,
    };
  },
  computed: {
    ...mapGetters(['permission', 'userInfo']),
    permissionList() {
      return {
        logBtn: this.validData(this.permission.finance_overdue_reminder_log, false),
      };
    },
    feeTypeOptions() {
      return feeTypeDic;
    },
    customerNameColumnWidth() {
      const longestUnits = this.data.reduce((maxUnits, row) => {
        const name = String(row.customerName || '-');
        const units = Array.from(name).reduce(
          (total, character) => total + (/^[\u0000-\u00ff]$/.test(character) ? 0.6 : 1),
          0
        );
        return Math.max(maxUnits, units);
      }, 0);
      return Math.max(180, Math.ceil(longestUnits * 15 + 48));
    },
    summaryCards() {
      const running = this.data.filter(item => item.overdueApprovalStatus === 'running').length;
      const approved = this.data.filter(item => item.overdueApprovalStatus === 'approved').length;
      return [
        { key: 'overdue', label: '逾期处置记录', value: this.summary.totalCount || this.page.total || 0 },
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
    drawerSummaryItems() {
      const row = this.drawerRow || {};
      if (!row.paymentId) return [];
      return [
        { label: '账单编号', value: `ZD${row.paymentId}` },
        { label: '房源信息', value: row.roomName || row.buildingName || '-' },
        { label: '费用类型', value: row.feeName || '-' },
        { label: '账期', value: `${row.periodStart || '--'} 至 ${row.periodEnd || '--'}` },
        { label: '应收金额', value: this.formatMoney(row.amountDue) },
        { label: '已缴金额', value: this.formatMoney(row.amountPaid) },
        { label: '未缴金额', value: this.formatMoney(this.unpaidAmount(row)) },
        { label: '应缴日期', value: row.payDeadline || '-' },
        { label: '逾期天数', value: `${this.overdueDays(row)}天` },
        { label: '催缴状态', value: row.remindStatus === '1' ? '已催缴' : '未催缴' },
        { label: '律师函审批', value: this.approvalStatusText(row.overdueApprovalStatus) },
        { label: '退租审批', value: this.approvalStatusText(row.terminationApprovalStatus) },
      ];
    },
    drawerNoticeTypes() {
      return [
        { label: '催款通知书', value: 'reminder-notice' },
        { label: '逾期处理通知书', value: 'overdue-notice' },
        { label: '限期搬离通知书', value: 'move-out-notice' },
      ];
    },
    drawerMiniAppTypes() {
      return [
        { label: '催款通知书', value: 'reminder-notice' },
        { label: '逾期处理通知书', value: 'overdue-notice' },
        { label: '限期搬离通知书', value: 'move-out-notice' },
      ];
    },
    disposalPrerequisiteItems() {
      const row = this.drawerRow || {};
      return [
        {
          key: 'reminder',
          label: '催缴通知',
          value: row.remindStatus === '1' || this.disposalDetail.internalNotices.length ? '已完成' : '待发送',
          done: row.remindStatus === '1' || this.disposalDetail.internalNotices.length > 0,
        },
        {
          key: OVERDUE_WORKFLOW,
          label: '律师函审批',
          value: this.approvalStatusText(this.workflowStatus(OVERDUE_WORKFLOW)),
          done: this.workflowStatus(OVERDUE_WORKFLOW) === 'approved',
        },
        {
          key: TERMINATION_WORKFLOW,
          label: '退租审批',
          value: this.approvalStatusText(this.workflowStatus(TERMINATION_WORKFLOW)),
          done: this.workflowStatus(TERMINATION_WORKFLOW) === 'approved',
        },
        {
          key: ROOM_REVIEW_WORKFLOW,
          label: '房屋验收流程',
          value: this.approvalStatusText(this.workflowStatus(ROOM_REVIEW_WORKFLOW)),
          done: this.workflowStatus(ROOM_REVIEW_WORKFLOW) === 'approved',
        },
        {
          key: 'settled',
          label: '账单结清',
          value: this.isSettled(row) ? '已完成' : '待补缴',
          done: this.isSettled(row),
        },
      ];
    },
    workflowGeneratedFiles() {
      return [OVERDUE_WORKFLOW, TERMINATION_WORKFLOW, ROOM_REVIEW_WORKFLOW]
        .map(type => (this.disposalDetail.workflowRecords || []).find(
          item => item.businessType === type && item.processStatus === 'approved'
        ))
        .filter(Boolean)
        .flatMap(item => {
          const generatedFile = {
            ...item,
            fileKey: `${item.recordId}-${this.workflowTemplateKey(item)}`,
            fileName: this.workflowFileName(item),
            fileUrl: this.workflowFileUrl(item),
          };
          if (item.businessType !== OVERDUE_WORKFLOW) return [generatedFile];
          const approvalTemplateKey = 'project-approval';
          return [
            {
              ...item,
              fileKey: `${item.recordId}-${approvalTemplateKey}`,
              templateKey: approvalTemplateKey,
              printFileUrl: '',
              fileName: '律师函申请项目审批表.docx',
              fileUrl: noticePrintUrl(approvalTemplateKey, {
                paymentId: item.paymentId || this.drawerRow?.paymentId,
                contractId: item.contractId || this.drawerRow?.contractId,
              }),
              sourceLabel: '律师函申请审批',
            },
            {
              ...generatedFile,
              sourceLabel: '律师函审批结果',
            },
          ];
        })
        .filter(item => item.fileUrl);
    },
  },
  created() {
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
      this.routePaymentId = routeQuery.paymentId || '';
      this.routeDrawerOpened = false;
      this.query = {
        ...this.query,
        paymentId: routeQuery.paymentId || '',
        contractNo: routeQuery.contractNo || '',
        customerName: routeQuery.customerName || '',
        feeType: routeQuery.feeType || '',
      };
    },
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
          this.openRoutePaymentDrawer();
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
        paymentId: '',
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
    openReminderDrawer(row) {
      this.drawerRow = { ...(row || {}) };
      this.drawerVisible = true;
	  readOverdueInternalNotice(row.paymentId)
		.catch(() => {})
		.finally(() => this.loadDisposalDetail(row));
    },
    openRoutePaymentDrawer() {
      if (!this.routePaymentId || this.routeDrawerOpened) return;
      const row = this.data.find(item => String(item.paymentId) === String(this.routePaymentId));
      if (!row) return;
      this.routeDrawerOpened = true;
      this.openReminderDrawer(row);
    },
    loadDisposalDetail(row) {
      if (!row || !row.paymentId) {
        this.resetDisposalDetail();
        return Promise.resolve();
      }
      this.disposalLoading = true;
      return getOverdueDisposalDetail(row.paymentId)
        .then(res => {
          const data = res.data.data || {};
          this.disposalDetail = {
            paymentNotice: data.paymentNotice || null,
            documentRecords: data.documentRecords || [],
            miniAppRecords: data.miniAppRecords || [],
            workflowRecords: data.workflowRecords || [],
			internalNotices: data.internalNotices || [],
          };
        })
        .finally(() => {
          this.disposalLoading = false;
        });
    },
    resetDisposalDetail() {
      this.disposalDetail = {
        paymentNotice: null,
        documentRecords: [],
        miniAppRecords: [],
        workflowRecords: [],
		internalNotices: [],
      };
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
    openRecipientDialog(row) {
      if (!row || !row.paymentId) return;
      if (this.isSettled(row)) {
        this.$message.warning('该账单已结清，仅保留历史处置记录');
        return;
      }
      this.recipientPayment = { ...row };
      this.recipientVisible = true;
    },
    handleRecipientSent({ payment }) {
      remindOverduePayment(payment.paymentId, 'overdue_reminder').finally(() => {
        this.loadDisposalDetail(payment);
        if (this.logDialogVisible && this.permissionList.logBtn) {
          this.loadDrawerLogs(payment);
        }
        this.reload();
      });
    },
    previewNotice(row, noticeType) {
      if (!row || !row.paymentId) return;
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          paymentId: row.paymentId,
          contractId: row.contractId,
        },
        noticePrintUrl(noticeType, {
          paymentId: row.paymentId,
          contractId: row.contractId,
        }),
        this.noticeFallbackName(row, noticeType),
        '审批表预览'
      );
    },
    handleGenerateNotice(row, noticeType) {
      if (!row || !row.paymentId) return;
      if (this.isSettled(row)) {
        this.$message.warning('该账单已结清，不能继续生成催缴文书');
        return;
      }
      const params = {
        noticeType,
        paymentId: row.paymentId,
        contractId: row.contractId,
      };
      generateContractNotice(params).then(res => {
        const file = res.data.data || {};
        const url =
          file.fileUrl ||
          noticePrintUrl(noticeType, {
            paymentId: row.paymentId,
            contractId: row.contractId,
          });
        const fallbackName = this.noticeFallbackName(row, noticeType, file.noticeName);
        return this.downloadNoticeFile(url, fallbackName).then(() => {
          this.$message.success(`${file.noticeName || '通知文件'}已生成`);
          this.loadDisposalDetail(row);
        });
      });
    },
    handleSendMiniApp(row, noticeType = 'overdue-notice') {
      if (!row || !row.paymentId) return;
      const messageMap = {
        'reminder-notice': '催款通知书',
        'overdue-notice': '租金逾期处理通知书',
        'legal-letter': '律师函',
        'move-out-notice': '限期搬离通知书',
      };
      sendMiniAppNotice({
        noticeType,
        paymentId: row.paymentId,
        contractId: row.contractId,
      }).then(() => {
        const refresh = noticeType === 'move-out-notice'
          ? Promise.resolve()
          : remindOverduePayment(row.paymentId, 'overdue_reminder');
        refresh.finally(() => {
          this.loadDisposalDetail(row);
          this.reload();
        });
        this.$message.success(`${messageMap[noticeType] || '通知文件'}的小程序发送数据已生成，已预留给小程序接口`);
      });
    },
    handleStartOverdueApproval(row) {
      if (!row || !row.paymentId) return;
      if (this.isSettled(row)) {
        this.$message.warning('该账单已结清，不能继续发起律师函审批');
        return;
      }
      if (this.workflowStatus(OVERDUE_WORKFLOW) === 'running') {
        this.$message.warning('该账单律师函审批正在进行中');
        return;
      }
      this.openWorkflowDialog(OVERDUE_WORKFLOW, row);
    },
    handleStartTermination(row) {
      if (!row || !row.contractId) return;
      if (this.isSettled(row)) {
        this.$message.warning('该账单已结清，不能继续发起退租审批');
        return;
      }
      if (this.workflowStatus(TERMINATION_WORKFLOW) === 'running') {
        this.$message.warning('该合同退租审批正在进行中');
        return;
      }
      this.openWorkflowDialog(TERMINATION_WORKFLOW, row);
    },
    handleStartRoomReview(row) {
      if (!row || !row.contractId) return;
      const terminationStatus = this.workflowStatus(TERMINATION_WORKFLOW);
      const roomReviewStatus = this.workflowStatus(ROOM_REVIEW_WORKFLOW);
      if (terminationStatus !== 'approved') {
        this.$alert('房屋验收需在退租审批完成后发起。当前前置状态已在上方“处置前置”中展示。', '前置条件未完成', {
          confirmButtonText: '知道了',
          type: 'warning',
        });
        return;
      }
      if (roomReviewStatus === 'running') {
        this.$message.warning('该合同房屋验收流程正在进行中');
        return;
      }
      if (roomReviewStatus === 'approved') {
        this.$message.success('该合同房屋验收流程已完成');
        return;
      }
      this.openWorkflowDialog(ROOM_REVIEW_WORKFLOW, row);
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
      if (this.workflowType === ROOM_REVIEW_WORKFLOW) {
        const terminationRecord = this.latestWorkflowRecord(TERMINATION_WORKFLOW) || {};
        return {
          processDefKey: this.workflowForm.processDefKey,
          params: {
            ...commonParams,
            businessType: ROOM_REVIEW_WORKFLOW,
            businessTable: 'biz_contract',
            businessKey: String(row.contractId || ''),
            contractId: row.contractId,
            sourceTerminationRecordId: terminationRecord.recordId,
            templateKey: 'room-review',
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
    latestWorkflowRecord(businessType) {
      return (this.disposalDetail.workflowRecords || []).find(item => item.businessType === businessType) || null;
    },
    workflowStatus(businessType) {
      const record = this.latestWorkflowRecord(businessType);
      if (record) return record.processStatus || '';
      const row = this.drawerRow || {};
      if (businessType === OVERDUE_WORKFLOW) return row.overdueApprovalStatus || '';
      if (businessType === TERMINATION_WORKFLOW) return row.terminationApprovalStatus || '';
      return '';
    },
    workflowTemplateKey(record = {}) {
      const templateMap = {
        [OVERDUE_WORKFLOW]: 'legal-letter',
        [TERMINATION_WORKFLOW]: 'termination-approval',
        [ROOM_REVIEW_WORKFLOW]: 'room-review',
      };
      return record.templateKey || templateMap[record.businessType] || '';
    },
    workflowFileName(record = {}) {
      if (record.fileName) return record.fileName;
      const nameMap = {
        [OVERDUE_WORKFLOW]: '律师函.docx',
        [TERMINATION_WORKFLOW]: '退租审批表.docx',
        [ROOM_REVIEW_WORKFLOW]: '房屋退租交接验收单.docx',
      };
      return nameMap[record.businessType] || '流程文件.docx';
    },
    workflowFileUrl(record = {}) {
      if (record.processStatus !== 'approved') return '';
      if (record.fileUrl) return record.fileUrl;
      if (record.printFileUrl) return record.printFileUrl;
      const templateKey = this.workflowTemplateKey(record);
      return noticePrintUrl(templateKey, {
        paymentId: record.paymentId || this.drawerRow?.paymentId,
        contractId: record.contractId || this.drawerRow?.contractId,
      });
    },
    previewWorkflowFile(record) {
      const templateKey = this.workflowTemplateKey(record);
      const fileUrl = this.workflowFileUrl(record);
      if (!templateKey || !fileUrl) {
        this.$message.warning('流程完成后才可预览生成文件');
        return;
      }
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType: templateKey,
          paymentId: record.paymentId || this.drawerRow?.paymentId,
          contractId: record.contractId || this.drawerRow?.contractId,
        },
        fileUrl,
        this.workflowFileName(record),
        `${this.workflowBusinessTypeText(record.businessType)}预览`
      );
    },
    downloadWorkflowFile(record) {
      const fileUrl = this.workflowFileUrl(record);
      if (!fileUrl) {
        this.$message.warning('流程完成后才可下载生成文件');
        return;
      }
      this.downloadNoticeFile(fileUrl, this.workflowFileName(record));
    },
    noticeFallbackName(row, noticeType, noticeName) {
      const typeNameMap = {
        'project-approval': '项目审批表',
        'payment-notice': '付款通知单',
        'reminder-notice': '催款通知书',
        'overdue-notice': '租金逾期处理通知书',
        'legal-letter': '律师函',
        'move-out-notice': '限期搬离通知书',
      };
      const baseName = noticeName || typeNameMap[noticeType] || '通知文件';
      const contractNo = row && row.contractNo ? `${row.contractNo}-` : '';
      return `${contractNo}${baseName}.docx`;
    },
    downloadNoticeFile(url, fallbackName) {
      return Promise.resolve().then(() => downloadNoticeFile(url, fallbackName));
    },
    downloadNoticePreviewFile() {
      if (!this.noticePreview.downloadUrl) return;
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName);
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
    workflowBusinessTypeText(value) {
      const map = {
        contract_payment: '付款审批',
        contract_overdue_legal: '逾期律师函',
        contract_termination: '退租审批',
        contract_room_review: '房屋验收',
      };
      return map[String(value || '')] || value || '-';
    },
    overdueDays(row) {
      if (!row || !row.payDeadline || row.payStatus === '1') return 0;
      const deadline = new Date(row.payDeadline).getTime();
      if (Number.isNaN(deadline) || deadline >= Date.now()) return 0;
      return Math.ceil((Date.now() - deadline) / 86400000);
    },
    isSettled(row) {
      return String((row && row.payStatus) || '') === '1';
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

.reminder-table-wrap {
  padding: 0;
  overflow: hidden;
}

.reminder-table {
  width: 100%;
}

.reminder-table :deep(.el-table__header th),
.reminder-table :deep(.el-table__cell),
.reminder-table :deep(.cell) {
  text-align: center;
}

.reminder-table :deep(.el-scrollbar__bar.is-horizontal) {
  right: 8px;
  bottom: 5px;
  left: 8px;
  height: 12px;
  opacity: 1;
  border-radius: 6px;
  background: #eef1f5;
}

.reminder-table :deep(.el-scrollbar__bar.is-horizontal .el-scrollbar__thumb) {
  min-width: 96px;
  border-radius: 6px;
  background-color: #9aa4b2;
}

.table-scroll-gutter {
  height: 32px;
}

.period-cell {
  display: inline-block;
  white-space: nowrap;
}

.customer-link {
  padding: 0;
  min-width: 0;
  max-width: none;
  font-weight: 500;
  white-space: nowrap;
  overflow: visible;
}

.customer-link :deep(span) {
  white-space: nowrap;
}

.reminder-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-profile,
.drawer-section {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.drawer-profile {
  min-height: 96px;
  padding: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.drawer-profile__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.drawer-profile__main span,
.drawer-field span {
  color: #6b7280;
  font-size: 12px;
}

.drawer-profile__main strong {
  color: #111827;
  font-size: 20px;
  line-height: 26px;
}

.drawer-profile__main em {
  color: #909399;
  font-style: normal;
  font-size: 13px;
}

.drawer-section {
  padding: 16px;
}

.drawer-section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.drawer-section-title-row {
  min-height: 28px;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.drawer-section-title-row .drawer-section-title {
  margin-bottom: 0;
}

.drawer-field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.drawer-field {
  min-height: 66px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}

.drawer-field strong {
  display: block;
  margin-top: 6px;
  color: #111827;
  font-size: 14px;
  line-height: 20px;
  word-break: break-all;
}

.drawer-action-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.drawer-action-grid :deep(.el-button) {
  width: 100%;
  min-height: 40px;
  margin-left: 0;
}

.precondition-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.precondition-item {
  min-height: 62px;
  padding: 12px;
  border: 1px solid #f3c78e;
  border-radius: 8px;
  background: #fff8ef;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.precondition-item span {
  color: #606266;
  font-size: 13px;
}

.precondition-item strong {
  color: #d97706;
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.precondition-item.is-done {
  border-color: #b7dfaa;
  background: #f2faef;
}

.precondition-item.is-done strong {
  color: #3b9b2f;
}

.document-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.document-row {
  min-height: 58px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.document-row__name {
  min-width: 0;
}

.document-row__name strong,
.document-row__name span {
  display: block;
}

.document-row__name strong {
  color: #1f2937;
  font-size: 14px;
  line-height: 20px;
}

.document-row__name span {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
}

.document-row__actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.document-row__actions :deep(.el-button) {
  margin-left: 0;
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

.record-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  min-height: 58px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-item div {
  min-width: 0;
}

.record-item strong {
  display: block;
  color: #1f2937;
  font-size: 14px;
  line-height: 20px;
}

.record-item span {
  display: block;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

  .drawer-field-grid,
  .drawer-action-grid,
  .precondition-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}
</style>
