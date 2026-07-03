<template>
  <basic-container>
    <div class="approval-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="approval-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="项目名称">
            <el-input v-model="query.projectName" clearable placeholder="请输入项目名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="企业名称">
            <el-input v-model="query.enterpriseName" clearable placeholder="请输入企业名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item v-if="!isSettlementMode" label="审批类型">
            <el-select v-model="query.businessType" clearable placeholder="全部类型">
              <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.processStatus" clearable placeholder="全部状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="approval-toolbar">
        <div class="toolbar-left">
          <el-button
            v-if="isSettlementMode && permissionList.addBtn"
            type="primary"
            icon="el-icon-plus"
            @click="openCreateDialog"
          >
            发起审核
          </el-button>
          <el-button
            v-if="isSettlementMode && permissionList.delBtn"
            type="danger"
            plain
            icon="el-icon-delete"
            :disabled="selectionList.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <section class="approval-table-card">
        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="projectId"
          class="approval-table"
          @selection-change="selectionChange"
        >
          <el-table-column v-if="isSettlementMode" type="selection" width="48" align="center" />
          <el-table-column prop="projectName" label="项目名称" min-width="210" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button class="table-link" text type="primary" @click="openDetail(row)">{{ row.projectName || '-' }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="enterpriseName" label="企业名称" min-width="190" align="center" show-overflow-tooltip />
          <el-table-column prop="businessType" label="审批类型" width="130" align="center">
            <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
          </el-table-column>
          <el-table-column prop="currentNodeName" label="当前节点" width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="processStatus" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.processStatus)" effect="plain">
                {{ statusText(row.processStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="materialCount" label="资料" width="90" align="center">
            <template #default="{ row }">{{ row.materialCount || 0 }} 份</template>
          </el-table-column>
          <el-table-column prop="logCount" label="日志" width="90" align="center">
            <template #default="{ row }">{{ row.logCount || 0 }} 条</template>
          </el-table-column>
          <el-table-column prop="applicant" label="发起人" width="120" align="center" />
          <el-table-column prop="applicantTime" label="发起时间" width="170" align="center" />
          <el-table-column label="操作" width="200" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="permissionList.viewBtn" text type="primary" icon="el-icon-view" @click="openDetail(row)">
                详情
              </el-button>
              <el-button v-if="permissionList.formBtn" text type="primary" icon="el-icon-document" @click="openForm(row)">
                审批表
              </el-button>
              <el-button
                v-if="permissionList.approveBtn && canCurrentUserAct(row)"
                text
                type="success"
                icon="el-icon-check"
                @click="openAction(row, 'approve')"
              >
                通过
              </el-button>
              <el-button
                v-if="permissionList.rejectBtn && canCurrentUserAct(row)"
                text
                type="danger"
                icon="el-icon-close"
                @click="openAction(row, 'reject')"
              >
                驳回
              </el-button>
              <el-dropdown v-if="permissionList.transferBtn || permissionList.archiveBtn || permissionList.resubmitBtn" trigger="click">
                <el-button text type="primary">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-if="permissionList.transferBtn && canCurrentUserAct(row)"
                      @click="openAction(row, 'transfer')"
                    >
                      转审
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="permissionList.resubmitBtn && row.processStatus === '3'"
                      @click="openAction(row, 'resubmit')"
                    >
                      重新提交
                    </el-dropdown-item>
                    <el-dropdown-item
                      v-if="permissionList.archiveBtn && ['2', '3'].includes(row.processStatus)"
                      @click="archiveProject(row)"
                    >
                      归档
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <div class="approval-pagination">
          <el-pagination
            background
            :current-page="page.currentPage"
            :page-sizes="[10, 20, 30, 50, 100]"
            :page-size="page.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="page.total"
            @size-change="sizeChange"
            @current-change="currentChange"
          />
        </div>
      </section>

      <el-dialog
        v-model="createVisible"
        title="发起审核"
        width="640px"
        append-to-body
        class="create-approval-dialog"
        @closed="resetCreateForm"
      >
        <el-form
          ref="createFormRef"
          :model="createForm"
          :rules="createRules"
          label-width="112px"
          class="create-approval-form"
        >
          <el-form-item label="入驻客户" prop="customerId">
            <el-select
              v-model="createForm.customerId"
              filterable
              remote
              clearable
              :remote-method="remoteCustomerSearch"
              :loading="customerLoading"
              placeholder="请选择客户"
              style="width: 100%"
              @change="handleCustomerChange"
            >
              <el-option
                v-for="item in customerOptions"
                :key="item.customerId"
                :label="item.enterpriseName"
                :value="item.customerId"
              >
                <span>{{ item.enterpriseName }}</span>
                <span class="customer-option-extra">{{ item.contactName || item.contactPhone || '' }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="审批流程" prop="flowId">
            <el-select
              v-model="createForm.flowId"
              :loading="flowLoading"
              placeholder="审批流程暂未配置"
              style="width: 100%"
            >
              <el-option
                v-for="item in flowOptions"
                :key="item.flowId"
                :label="flowLabel(item)"
                :value="item.flowId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="项目名称">
            <el-input v-model="createForm.projectName" placeholder="留空时自动按客户生成" />
          </el-form-item>
          <el-form-item label="发起部门">
            <el-input v-model="createForm.applicantDept" disabled placeholder="自动带出当前部门" />
          </el-form-item>
          <el-form-item label="租赁楼层">
            <el-input v-model="createForm.leaseFloor" placeholder="如：1号楼3层" />
          </el-form-item>
          <el-form-item label="租赁面积">
            <el-input v-model="createForm.leaseArea" placeholder="请输入租赁面积" />
          </el-form-item>
          <el-form-item label="单价（元）">
            <el-input v-model="createForm.rentPrice" placeholder="请输入租金单价" />
          </el-form-item>
          <el-form-item label="保证金（元）">
            <el-input v-model="createForm.deposit" placeholder="请输入保证金" />
          </el-form-item>
          <el-form-item label="租赁周期">
            <el-date-picker
              v-model="createForm.leaseRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="Start date"
              end-placeholder="End date"
              range-separator="~"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="附件">
            <el-upload
              action="#"
              :auto-upload="false"
              :file-list="attachmentList"
              :on-change="handleAttachmentChange"
              :on-remove="handleAttachmentRemove"
              multiple
            >
              <el-button icon="el-icon-upload">上传附件</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="createVisible = false">取消</el-button>
          <el-button type="primary" :loading="createLoading" @click="submitCreateApproval">确定</el-button>
        </template>
      </el-dialog>

      <el-drawer v-model="detailVisible" :title="detailTitle" size="76%" append-to-body>
        <div v-loading="detailLoading" class="approval-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ current.projectName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="企业名称">{{ current.enterpriseName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="统一信用代码">{{ current.creditCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审批类型">{{ businessTypeText(current.businessType) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(current.processStatus)" effect="plain">
                {{ statusText(current.processStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="当前节点">{{ current.currentNodeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发起人">{{ current.applicant || current.createBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="发起时间">{{ current.applicantTime || current.createTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="租赁面积">{{ formatArea(current.leaseArea) }}</el-descriptions-item>
            <el-descriptions-item label="租金单价">{{ formatMoney(current.rentPrice) }}</el-descriptions-item>
            <el-descriptions-item label="审批事项" :span="2">{{ current.approvalMatter || current.summary || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-tabs v-model="activeTab" class="approval-tabs">
            <el-tab-pane label="审批进度" name="progress">
              <el-steps :active="stepActive" finish-status="success" process-status="process" align-center>
                <el-step v-for="item in flowNodes" :key="item.nodeOrder || item.nodeName" :title="item.nodeName">
                  <template #description>
                    <span>{{ item.approverName || item.approverLogin || '-' }}</span>
                  </template>
                </el-step>
              </el-steps>
              <el-empty v-if="flowNodes.length === 0" description="暂无流程节点" />
            </el-tab-pane>
            <el-tab-pane label="审批日志" name="logs">
              <el-timeline v-if="logs.length">
                <el-timeline-item
                  v-for="log in logs"
                  :key="log.logId"
                  :timestamp="log.operateTime || log.createTime"
                  :type="timelineType(log.actionType)"
                >
                  <div class="timeline-card">
                    <strong>{{ log.nodeName || '-' }}</strong>
                    <el-tag size="small" effect="plain">{{ actionText(log.actionType) }}</el-tag>
                    <span>{{ log.operatorName || '-' }}</span>
                    <p v-if="log.transferTo">转审至：{{ log.transferTo }}</p>
                    <p>{{ log.opinion || '-' }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无审批日志" />
            </el-tab-pane>
            <el-tab-pane label="审批资料" name="materials">
              <el-table :data="materials" border>
                <el-table-column prop="materialType" label="资料类型" width="140" />
                <el-table-column prop="fileName" label="文件名称" min-width="220" show-overflow-tooltip />
                <el-table-column prop="fileSize" label="大小" width="110">
                  <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center">
                  <template #default="{ row }">
                    <el-button text type="primary" @click="openFile(row.fileUrl)">预览</el-button>
                    <el-button text type="primary" @click="openFile(row.fileUrl)">下载</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-drawer>

      <el-dialog v-model="formVisible" title="审批表" width="860px" append-to-body>
        <div class="approval-form-preview">
          <div class="approval-form-preview__doc" v-html="approvalHtml"></div>
        </div>
        <template #footer>
          <el-button @click="formVisible = false">关闭</el-button>
          <el-button icon="el-icon-printer" @click="printApprovalForm">打印 / 另存为 PDF</el-button>
          <el-button type="primary" icon="el-icon-download" @click="downloadApprovalForm">导出 HTML</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="actionVisible" :title="actionTitle" width="520px" append-to-body>
        <el-form :model="actionForm" label-width="90px">
          <el-form-item v-if="actionType === 'transfer'" label="转审人">
            <el-input v-model="actionForm.transferTo" placeholder="请输入转审人账号" />
          </el-form-item>
          <el-form-item label="审批意见">
            <el-input v-model="actionForm.opinion" type="textarea" :rows="4" placeholder="请输入审批意见" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="actionVisible = false">取消</el-button>
          <el-button type="primary" :loading="actionLoading" @click="submitAction">提交</el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { downloadBlob } from '@/api/common';
import {
  approveApprovalProject,
  archiveApprovalProject,
  exportApprovalForm,
  getApprovalForm,
  getApprovalFlowAll,
  getApprovalProjectList,
  getApprovalProjectStatistics,
  removeApprovalProject,
  rejectApprovalProject,
  resubmitApprovalProject,
  saveApprovalProject,
  submitApprovalProject,
  transferApprovalProject,
} from '@/api/approval/approval';
import { getCustomerDetail, getCustomerList } from '@/api/business/customer';
import { downloadFile } from '@/utils/util';
import { printHtml } from '@/utils/print-html';

const statusOptions = [
  { value: '0', label: '草稿', type: 'info' },
  { value: '1', label: '审批中', type: 'warning' },
  { value: '2', label: '已通过', type: 'success' },
  { value: '3', label: '已驳回', type: 'danger' },
  { value: '4', label: '已撤回', type: 'info' },
  { value: '5', label: '已归档', type: 'primary' },
];

const businessTypeOptions = [
  { value: 'tenant_entry', label: '入驻审批' },
  { value: 'contract_renewal', label: '合同续签' },
  { value: 'termination', label: '退租审批' },
];

const createDefaultForm = () => ({
  customerId: '',
  flowId: '',
  projectName: '',
  enterpriseName: '',
  creditCode: '',
  applicantDept: '',
  shareholderInfo: '',
  businessScope: '',
  responsiblePerson: '',
  contactPhone: '',
  leaseFloor: '',
  leaseArea: '',
  rentPrice: '',
  deposit: '',
  leaseRange: [],
});

export default {
  name: 'ApprovalMyFlow',
  props: {
    mode: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      loading: false,
      detailLoading: false,
      actionLoading: false,
      data: [],
      query: {},
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      summaryCards: [
        { key: 'pending', label: '审批中', value: 0 },
        { key: 'approved', label: '已通过', value: 0 },
        { key: 'rejected', label: '已驳回', value: 0 },
        { key: 'archived', label: '已归档', value: 0 },
      ],
      current: {},
      materials: [],
      logs: [],
      flowNodes: [],
      activeTab: 'progress',
      detailVisible: false,
      formVisible: false,
      formData: {},
      approvalHtml: '',
      actionVisible: false,
      actionType: 'approve',
      actionForm: {
        opinion: '',
        transferTo: '',
      },
      selectionList: [],
      createVisible: false,
      createLoading: false,
      flowLoading: false,
      customerLoading: false,
      customerOptions: [],
      flowOptions: [],
      attachmentList: [],
      createForm: createDefaultForm(),
      createRules: {
        customerId: [{ required: true, message: '请选择入驻客户', trigger: 'change' }],
        flowId: [{ required: true, message: '请选择审批流程', trigger: 'change' }],
      },
      statusOptions,
      businessTypeOptions,
    };
  },
  computed: {
    ...mapGetters(['permission', 'userInfo']),
    isSettlementMode() {
      return this.mode === 'settlement' || this.$route.path === '/settlement/project-approval';
    },
    approvalMode() {
      if (this.isSettlementMode) return 'settlement';
      if (['todo', 'done', 'cc'].includes(this.mode)) return this.mode;
      if (this.$route.path === '/approval/todo') return 'todo';
      if (this.$route.path === '/approval/done') return 'done';
      if (this.$route.path === '/approval/cc') return 'cc';
      return 'mine';
    },
    pageTitle() {
      const titles = {
        settlement: '入驻审核',
        mine: '我的审批',
        todo: '待办任务',
        done: '已办任务',
        cc: '抄送我的',
      };
      return titles[this.approvalMode] || '我的审批';
    },
    permissionList() {
      const prefixMap = {
        settlement: 'settlement_project_approval',
        mine: 'approval_my_flow',
        todo: 'approval_todo_task',
        done: 'approval_done_task',
        cc: 'approval_cc_task',
      };
      const prefix = prefixMap[this.approvalMode] || 'approval_my_flow';
      const fallback = key => this.validData(this.permission[`${prefix}_${key}`], this.permission[`approval_my_flow_${key}`]);
      const settlementEntryMode = this.approvalMode === 'settlement';
      return {
        addBtn: this.validData(fallback('add'), settlementEntryMode),
        delBtn: this.validData(fallback('delete'), settlementEntryMode),
        viewBtn: this.validData(fallback('view'), false),
        formBtn: this.validData(fallback('form'), false),
        approveBtn: settlementEntryMode ? false : this.validData(fallback('approve'), false),
        rejectBtn: settlementEntryMode ? false : this.validData(fallback('reject'), false),
        transferBtn: settlementEntryMode ? false : this.validData(fallback('transfer'), false),
        archiveBtn: settlementEntryMode ? false : this.validData(fallback('archive'), false),
        resubmitBtn: settlementEntryMode ? false : this.validData(fallback('resubmit'), false),
      };
    },
    detailTitle() {
      return this.current.projectName ? `审批详情 - ${this.current.projectName}` : '审批详情';
    },
    actionTitle() {
      const titles = {
        approve: '审批通过',
        reject: '审批驳回',
        transfer: '转审',
        resubmit: '重新提交',
      };
      return titles[this.actionType] || '审批处理';
    },
    stepActive() {
      const index = this.flowNodes.findIndex(item => item.nodeName === this.current.currentNodeName);
      if (index >= 0) return index;
      if (this.current.processStatus === '2' || this.current.processStatus === '5') return this.flowNodes.length;
      return 0;
    },
    ids() {
      return this.selectionList.map(item => item.projectId).filter(Boolean).join(',');
    },
  },
  created() {
    this.query = this.defaultQuery();
    this.loadStatistics();
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    defaultQuery() {
      if (this.approvalMode === 'settlement') {
        return {
          scope: 'settlement',
          businessType: 'tenant_entry',
          excludeProcessStatus: '0',
        };
      }
      if (this.approvalMode === 'todo') {
        return {
          scope: 'todo',
        };
      }
      if (this.approvalMode === 'done') {
        return {
          scope: 'done',
        };
      }
      if (this.approvalMode === 'cc') {
        return {
          scope: 'cc',
        };
      }
      return {
        scope: 'mine',
      };
    },
    loadStatistics() {
      getApprovalProjectStatistics({ ...this.query }).then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getApprovalProjectList(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = Number(result.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    reload() {
      this.onLoad(this.page);
      this.loadStatistics();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = this.defaultQuery();
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    loadFlowOptions() {
      this.flowLoading = true;
      getApprovalFlowAll({
        businessType: 'tenant_entry',
        status: '1',
        includeGlobal: true,
      })
        .then(res => {
          this.flowOptions = res.data.data || [];
          if (!this.createForm.flowId && this.flowOptions.length) {
            this.createForm.flowId = this.flowOptions[0].flowId;
          }
        })
        .finally(() => {
          this.flowLoading = false;
        });
    },
    loadCustomerOptions(keyword = '') {
      this.customerLoading = true;
      getCustomerList(1, 20, {
        keyword,
      })
        .then(res => {
          const result = res.data.data || {};
          this.customerOptions = result.records || [];
        })
        .finally(() => {
          this.customerLoading = false;
        });
    },
    remoteCustomerSearch(keyword) {
      this.loadCustomerOptions(keyword);
    },
    openCreateDialog() {
      this.createForm = {
        ...createDefaultForm(),
        applicantDept: this.currentDeptName(),
      };
      this.attachmentList = [];
      this.createVisible = true;
      this.loadFlowOptions();
      this.loadCustomerOptions();
      this.$nextTick(() => {
        if (this.$refs.createFormRef) {
          this.$refs.createFormRef.clearValidate();
        }
      });
    },
    resetCreateForm() {
      this.createForm = createDefaultForm();
      this.attachmentList = [];
      this.createLoading = false;
    },
    handleCustomerChange(customerId) {
      if (!customerId) {
        this.createForm = {
          ...this.createForm,
          enterpriseName: '',
          creditCode: '',
          businessScope: '',
          shareholderInfo: '',
          responsiblePerson: '',
          contactPhone: '',
        };
        return;
      }
      const cached = this.customerOptions.find(item => `${item.customerId}` === `${customerId}`);
      if (cached) {
        this.fillCreateCustomer(cached);
      }
      getCustomerDetail(customerId).then(res => {
        const detail = res.data.data || cached || {};
        this.fillCreateCustomer(detail);
      });
    },
    fillCreateCustomer(customer = {}) {
      const enterpriseName = customer.enterpriseName || '';
      this.createForm = {
        ...this.createForm,
        customerId: customer.customerId || this.createForm.customerId,
        enterpriseName,
        creditCode: customer.creditCode || '',
        businessScope: customer.businessScope || customer.mainBusiness || '',
        shareholderInfo: customer.equityStructure || '',
        responsiblePerson: customer.contactName || '',
        contactPhone: customer.contactPhone || '',
        projectName: this.createForm.projectName || (enterpriseName ? `${enterpriseName} 入驻审核` : ''),
      };
    },
    handleAttachmentChange(file, fileList) {
      this.attachmentList = fileList;
    },
    handleAttachmentRemove(file, fileList) {
      this.attachmentList = fileList;
    },
    submitCreateApproval() {
      if (!this.flowOptions.length) {
        this.$message.warning('审批流程暂未配置，请先在协同办公配置入驻审批流程');
        return;
      }
      this.$refs.createFormRef.validate(valid => {
        if (!valid) return;
        const payload = this.buildCreatePayload();
        this.createLoading = true;
        saveApprovalProject(payload)
          .then(res => {
            const project = res.data.data || {};
            if (!project.projectId) {
              throw new Error('审批项目创建失败');
            }
            return submitApprovalProject(project.projectId);
          })
          .then(() => {
            this.$message.success('发起成功，已提交入驻审批申请');
            this.createVisible = false;
            this.selectionList = [];
            this.reload();
          })
          .finally(() => {
            this.createLoading = false;
          });
      });
    },
    buildCreatePayload() {
      const [leaseStartDate, leaseEndDate] = this.createForm.leaseRange || [];
      const projectName = this.createForm.projectName || `${this.createForm.enterpriseName || ''} 入驻审核`;
      return {
        businessType: 'tenant_entry',
        processStatus: '0',
        customerId: this.createForm.customerId,
        businessId: this.createForm.customerId,
        flowId: this.createForm.flowId,
        projectName,
        enterpriseName: this.createForm.enterpriseName,
        creditCode: this.createForm.creditCode,
        applicantDept: this.createForm.applicantDept,
        shareholderInfo: this.createForm.shareholderInfo,
        businessScope: this.createForm.businessScope,
        responsiblePerson: this.createForm.responsiblePerson,
        contactPhone: this.createForm.contactPhone,
        leaseFloor: this.createForm.leaseFloor,
        leaseArea: this.createForm.leaseArea,
        rentPrice: this.createForm.rentPrice,
        deposit: this.createForm.deposit,
        leaseStartDate,
        leaseEndDate,
        summary: projectName,
        approvalMatter: `${this.createForm.enterpriseName || projectName} 入驻审批申请`,
      };
    },
    handleBatchDelete() {
      if (!this.ids) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定删除选中的审批项目?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeApprovalProject(this.ids))
        .then(() => {
          this.$message.success('删除成功');
          this.selectionList = [];
          this.reload();
        });
    },
    openDetail(row) {
      this.detailVisible = true;
      this.activeTab = 'progress';
      this.detailLoading = true;
      getApprovalForm(row.projectId)
        .then(res => {
          const data = res.data.data || {};
          this.setDetailData(data, row);
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    openForm(row) {
      getApprovalForm(row.projectId).then(res => {
        const data = res.data.data || {};
        this.setDetailData(data, row);
        this.approvalHtml = this.buildApprovalContent(data);
        this.formVisible = true;
      });
    },
    setDetailData(data, row) {
      this.formData = data;
      this.current = data.project || row || {};
      this.materials = data.materials || [];
      this.logs = data.logs || [];
      this.flowNodes = data.flowNodes || [];
    },
    openAction(row, type) {
      this.current = row;
      this.actionType = type;
      this.actionForm = {
        opinion: type === 'approve' ? '同意' : '',
        transferTo: '',
      };
      this.actionVisible = true;
    },
    submitAction() {
      const actions = {
        approve: approveApprovalProject,
        reject: rejectApprovalProject,
        transfer: transferApprovalProject,
        resubmit: resubmitApprovalProject,
      };
      const action = actions[this.actionType];
      if (!action || !this.current.projectId) return;
      if (this.actionType === 'transfer' && !this.actionForm.transferTo) {
        this.$message.warning('请输入转审人账号');
        return;
      }
      this.actionLoading = true;
      action(this.current.projectId, this.actionForm)
        .then(() => {
          this.$message.success('操作成功');
          this.actionVisible = false;
          this.reload();
        })
        .finally(() => {
          this.actionLoading = false;
        });
    },
    archiveProject(row) {
      this.$confirm('确定归档该审批项目？', '提示', { type: 'warning' })
        .then(() => archiveApprovalProject(row.projectId))
        .then(() => {
          this.$message.success('归档成功');
          this.reload();
        });
    },
    downloadApprovalForm() {
      exportApprovalForm(this.current.projectId).then(res => {
        const data = res.data.data || this.formData;
        const html = this.buildApprovalHtml(data);
        const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = `${this.current.projectName || 'approval'}-approval.html`;
        link.click();
        URL.revokeObjectURL(link.href);
      });
    },
    printApprovalForm() {
      printHtml(this.buildApprovalHtml(this.formData), '审批表');
    },
    buildApprovalRows(data) {
      const project = data.project || {};
      const logs = data.logs || [];
      const materials = data.materials || [];
      const rows = [
        ['项目名称', project.projectName],
        ['企业名称', project.enterpriseName],
        ['统一信用代码', project.creditCode],
        ['审批类型', this.businessTypeText(project.businessType)],
        ['流程状态', this.statusText(project.processStatus)],
        ['当前节点', project.currentNodeName],
        ['发起人', project.applicant || project.createBy],
        ['发起时间', project.applicantTime || project.createTime],
        ['审批事项', project.approvalMatter || project.summary],
      ];
      return { rows, logs, materials };
    },
    buildApprovalContent(data) {
      const { rows, logs, materials } = this.buildApprovalRows(data);
      return `<h2>${this.escapeHtml(data.templateName || '项目审批表')}</h2>
        <table>${rows.map(row => `<tr><th style="width:140px">${this.escapeHtml(row[0])}</th><td>${this.escapeHtml(row[1] || '-')}</td></tr>`).join('')}</table>
        <h3>审批日志</h3>
        <table><tr><th>节点</th><th>动作</th><th>操作人</th><th>意见</th><th>时间</th></tr>
        ${logs.map(log => `<tr><td>${this.escapeHtml(log.nodeName || '-')}</td><td>${this.escapeHtml(this.actionText(log.actionType))}</td><td>${this.escapeHtml(log.operatorName || '-')}</td><td>${this.escapeHtml(log.opinion || '-')}</td><td>${this.escapeHtml(log.operateTime || log.createTime || '-')}</td></tr>`).join('')}
        </table><h3>审批资料</h3><p class="muted">共 ${materials.length} 份资料</p>`;
    },
    buildApprovalHtml(data) {
      return `<!doctype html><html><head><meta charset="utf-8"><title>审批表</title>
        <style>body{font-family:Arial,"Microsoft YaHei",sans-serif;padding:24px;color:#303133}h2{text-align:center}table{width:100%;border-collapse:collapse;margin:14px 0}td,th{border:1px solid #dcdfe6;padding:8px;text-align:left}.muted{color:#909399}</style>
        </head><body>${this.buildApprovalContent(data)}</body></html>`;
    },
    escapeHtml(value) {
      return `${value}`.replace(/[&<>"']/g, char => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
      }[char]));
    },
    openFile(url) {
      if (!url) {
        this.$message.warning('暂无文件地址');
        return;
      }
      downloadBlob(url).then(res => {
        const disposition = res.headers && res.headers['content-disposition'];
        const filename = this.resolveDownloadFilename(disposition, '审批附件');
        const contentType = (res.headers && res.headers['content-type']) || 'application/octet-stream';
        downloadFile(res.data, filename, contentType);
      });
    },
    resolveDownloadFilename(disposition, fallbackName) {
      if (!disposition) return fallbackName;
      const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
      if (utf8Match && utf8Match[1]) {
        return decodeURIComponent(utf8Match[1]);
      }
      const filenameMatch = disposition.match(/filename=\"?([^\";]+)\"?/i);
      return filenameMatch && filenameMatch[1] ? decodeURIComponent(filenameMatch[1]) : fallbackName;
    },
    canCurrentUserAct(row) {
      if (row.processStatus !== '1') return false;
      const authority = this.userInfo.authority || '';
      if (authority.includes('administrator') || authority.includes('admin')) return true;
      const account = this.userInfo.account || this.userInfo.userName || this.userInfo.user_name;
      return `${row.currentNode || ''}`.split(',').map(item => item.trim()).includes(account);
    },
    businessTypeText(value) {
      const item = this.businessTypeOptions.find(option => option.value === value);
      return item ? item.label : value || '-';
    },
    flowLabel(flow) {
      return `${flow.flowName || '入驻审批'}${flow.flowVersion ? ` v${flow.flowVersion}` : ''}`;
    },
    currentDeptName() {
      return this.userInfo.dept_name || this.userInfo.deptName || this.userInfo.dept_name_full || '';
    },
    statusText(value) {
      const item = this.statusOptions.find(option => option.value === `${value}`);
      return item ? item.label : '-';
    },
    statusType(value) {
      const item = this.statusOptions.find(option => option.value === `${value}`);
      return item ? item.type : 'info';
    },
    timelineType(actionType) {
      const map = {
        APPROVE: 'success',
        REJECT: 'danger',
        TRANSFER: 'warning',
        ARCHIVE: 'primary',
      };
      return map[actionType] || 'info';
    },
    actionText(actionType) {
      const map = {
        SUBMIT: '提交',
        APPROVE: '通过',
        REJECT: '驳回',
        TRANSFER: '转审',
        RESUBMIT: '重新提交',
        WITHDRAW: '撤回',
        ARCHIVE: '归档',
      };
      return map[actionType] || actionType || '-';
    },
    formatArea(value) {
      return value === null || value === undefined || value === '' ? '-' : `${value} ㎡`;
    },
    formatMoney(value) {
      if (value === null || value === undefined || value === '') return '-';
      return Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    formatSize(value) {
      const size = Number(value || 0);
      if (size <= 0) return '-';
      if (size < 1024) return `${size} B`;
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
      return `${(size / 1024 / 1024).toFixed(1)} MB`;
    },
  },
};
</script>

<style lang="scss" scoped>
.approval-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.approval-search {
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.approval-search :deep(.el-form-item) {
  margin: 0 22px 12px 0;
}

.approval-search :deep(.el-form-item__label) {
  height: 36px;
  line-height: 36px;
  color: #303133;
}

.approval-search :deep(.el-input),
.approval-search :deep(.el-select) {
  width: 168px;
}

.approval-search :deep(.el-input__wrapper),
.approval-search :deep(.el-select__wrapper) {
  min-height: 36px;
}

.approval-search :deep(.el-button) {
  height: 36px;
  padding: 0 18px;
}

.approval-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0;
  border: 0;
  background: transparent;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.approval-table {
  width: 100%;
}

.approval-table-card {
  padding: 0;
  border: 0;
  background: transparent;
}

.approval-table :deep(.el-table__cell) {
  text-align: center;
}

.approval-table :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}

.approval-table :deep(.el-button + .el-button) {
  margin-left: 4px;
}

.approval-table :deep(.el-dropdown) {
  margin-left: 4px;
}

.table-link {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  padding: 0;
  font-weight: 400;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.approval-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.create-approval-form {
  padding: 8px 34px 0 26px;
}

.create-approval-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.create-approval-form :deep(.el-input__wrapper),
.create-approval-form :deep(.el-select__wrapper) {
  min-height: 32px;
  border-radius: 0;
}

.create-approval-form :deep(.el-date-editor) {
  width: 100%;
}

.customer-option-extra {
  float: right;
  margin-left: 16px;
  color: #909399;
  font-size: 12px;
}

.approval-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.approval-tabs {
  margin-top: 14px;
}

.timeline-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  line-height: 1.6;
}

.timeline-card p {
  flex-basis: 100%;
  margin: 0;
  color: #606266;
}

.approval-form-preview {
  max-height: 62vh;
  overflow: auto;
  border: 1px solid #ebeef5;
}

.approval-form-preview__doc {
  padding: 24px;
  color: #303133;
  font-family: Arial, "Microsoft YaHei", sans-serif;
}

.approval-form-preview__doc :deep(h2) {
  margin: 0 0 14px;
  text-align: center;
}

.approval-form-preview__doc :deep(h3) {
  margin: 18px 0 8px;
}

.approval-form-preview__doc :deep(table) {
  width: 100%;
  margin: 14px 0;
  border-collapse: collapse;
}

.approval-form-preview__doc :deep(td),
.approval-form-preview__doc :deep(th) {
  padding: 8px;
  border: 1px solid #dcdfe6;
  text-align: left;
}

.approval-form-preview__doc :deep(.muted) {
  color: #909399;
}

</style>
