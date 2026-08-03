<template>
  <basic-container>
    <div class="tenant-entry-page">
      <section class="summary-grid">
		<div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="tenant-entry-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="企业名称">
            <el-input
              v-model="query.enterpriseName"
              clearable
              placeholder="请输入企业名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="流程状态">
            <el-select
              v-model="query.processIsFinished"
              clearable
              placeholder="全部状态"
              @change="searchChange"
            >
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="tenant-entry-list-panel">
        <div class="tenant-entry-toolbar">
          <div class="tenant-entry-toolbar__title">
            <strong>入驻审核记录</strong>
            <span>共 {{ page.total }} 条</span>
          </div>
          <div class="toolbar-actions">
			<el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openStart">发起审核</el-button>
            <el-tooltip content="刷新" placement="top">
              <el-button icon="el-icon-refresh" circle @click="reload" />
            </el-tooltip>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="rowKey"
          class="tenant-entry-table"
        >
          <el-table-column
            prop="enterpriseName"
            label="企业名称"
            min-width="200"
            align="center"
            show-overflow-tooltip
            class-name="enterprise-name-column"
          />
          <el-table-column
            prop="processDefinitionName"
            label="审批类型"
            min-width="180"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column
            prop="taskName"
            label="当前节点"
            min-width="170"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column prop="statusLabel" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTag(row)" effect="plain">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="startUsername"
            label="发起人"
            width="120"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column prop="createTime" label="发起/到达时间" min-width="190" align="center" />
          <el-table-column label="操作" width="156" fixed="right" align="center">
            <template #default="{ row }">
              <div class="table-row-actions">
				<el-button v-if="permissionList.viewBtn && row.scope === 'todo'" text type="primary" @click="openDetail(row)"
                  >处理</el-button
                >
				<el-button v-else-if="permissionList.viewBtn" text type="primary" @click="openDetail(row)">详情</el-button>
                <el-button
				  v-if="permissionList.formBtn && canExportApprovalForm(row)"
                  text
                  type="primary"
                  @click="openApprovalForm(row)"
                  >导出审核表</el-button
                >
              </div>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无入驻审核记录" />
          </template>
        </el-table>

        <div class="tenant-entry-pagination">
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

      <el-dialog v-model="startVisible" title="发起入驻审核" width="620px" append-to-body>
        <el-form label-width="100px">
          <el-form-item label="审核流程">
            <el-select
              v-model="startForm.processDefKey"
              filterable
              :loading="processLoading"
              placeholder="请选择已部署流程"
              style="width: 100%"
            >
              <el-option
                v-for="item in processOptions"
                :key="item.id || item.key"
                :label="processOptionLabel(item)"
                :value="item.key"
              >
                <span>{{ item.name }}</span>
                <span class="option-extra">{{ item.key }} / v{{ item.version }}</span>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="商机企业">
            <el-select
              v-model="startForm.opportunityId"
              filterable
              remote
              clearable
              :remote-method="searchOpportunity"
              :loading="opportunityLoading"
				placeholder="请选择需要发起审批的商机企业"
              style="width: 100%"
            >
              <el-option
                v-for="item in opportunityOptions"
                :key="item.opportunityId"
                :label="item.enterpriseName"
                :value="item.opportunityId"
              >
                <span>{{ item.enterpriseName }}</span>
                <span class="option-extra">{{ item.contactName || item.contactPhone || '' }}</span>
              </el-option>
            </el-select>
			<div class="start-form-tip">审批必须关联商机，驳回后可从这里重新发起。</div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="startVisible = false">取消</el-button>
		  <el-button type="primary" :disabled="!startForm.processDefKey || !startForm.opportunityId" @click="goStart"
            >下一步</el-button
          >
        </template>
      </el-dialog>

      <notice-preview-dialog
        v-model="approvalPreview.visible"
        :title="approvalPreview.title"
        :html="approvalPreview.html"
        :loading="approvalPreview.loading"
        :download-url="approvalPreview.downloadUrl"
        :download-label="approvalPreview.downloadLabel"
        :preview-type="approvalPreview.previewType"
        :document-blob="approvalPreview.documentBlob"
        :pdf-blob="approvalPreview.pdfBlob"
        :pdf-file-name="approvalPreview.pdfFileName"
        :preview-error="approvalPreview.previewError"
        @download="downloadApprovalForm"
      />
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { Base64 } from 'js-base64';
import Layout from '@/page/index/index.vue';
import {
  copyList,
  doneList,
  myDoneList,
  sendList,
  todoList,
} from '@/views/plugin/workflow/api/process/process';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import {
  exportTenantEntryApprovalForm,
	getTenantEntryCandidateList,
  previewTenantEntryApprovalForm,
} from '@/api/business/opportunity';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import { downloadFile } from '@/utils/util';
import { createNoticePreviewState, resolveDownloadFilename } from '@/utils/contract-notice';

const DEFAULT_PROCESS_KEY = 'tenant_entry-1';
const TENANT_ENTRY_BUSINESS_TYPE = 'tenant_entry';
const COPY_FORM_KEYS = ['wf_ex_TenantEntry', 'wf_ex_入驻'];

export default {
  name: 'SettlementProjectApproval',
  components: { NoticePreviewDialog },
  data() {
    return {
      loading: false,
      data: [],
      query: {
        enterpriseName: '',
        processIsFinished: '',
      },
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      summaryCards: [
        { key: 'todo', label: '待处理', value: 0 },
        { key: 'send', label: '已发起', value: 0 },
        { key: 'done', label: '已办结', value: 0 },
        { key: 'copy', label: '抄送我', value: 0 },
      ],
      scopeOptions: [
        { value: 'todo', label: '待处理' },
        { value: 'send', label: '我发起的' },
        { value: 'myDone', label: '我的已办' },
        { value: 'done', label: '已办结' },
        { value: 'copy', label: '抄送我' },
      ],
      statusOptions: [
        { value: 'unfinished', label: '审批中' },
        { value: 'finished', label: '已完成' },
        { value: 'reject', label: '被驳回' },
        { value: 'recall', label: '已撤回' },
      ],
      startVisible: false,
      startForm: {
        processDefKey: DEFAULT_PROCESS_KEY,
        opportunityId: '',
      },
      processLoading: false,
      processOptions: [],
      opportunityLoading: false,
      opportunityOptions: [],
      approvalPreview: createNoticePreviewState(),
      approvalPreviewRow: null,
    };
  },
	computed: {
	  ...mapGetters(['permission']),
	  permissionList() {
		return {
		  addBtn: Boolean(this.permission.settlement_project_approval_add),
		  viewBtn: Boolean(this.permission.settlement_project_approval_view),
		  formBtn: Boolean(this.permission.settlement_project_approval_form),
		};
	  },
  },
  created() {
    this.applyRouteQuery();
    this.onLoad();
  },
  activated() {
    this.applyRouteQuery();
    this.onLoad();
  },
  methods: {
    applyRouteQuery() {
      const { processIsFinished, processStatus } = this.$route.query || {};
      const routeStatus = processIsFinished || processStatus;
      if (routeStatus && this.statusOptions.some(item => item.value === routeStatus)) {
        this.query.processIsFinished = routeStatus;
      }
    },
    requestByScope(
      scope,
      current = this.page.currentPage,
      size = this.page.pageSize,
      extraParams = {}
    ) {
	  const formSearch = ['businessType:equal:tenant_entry'];
	  if (this.query.enterpriseName) {
		const keyword = this.query.enterpriseName.replace(/[:,]/g, ' ').trim();
		if (keyword) formSearch.push(`enterpriseName:like:${keyword}`);
	  }
	  const params =
		scope === 'copy'
		  ? { title: this.query.enterpriseName || '入驻' }
		  : { processDefinitionName: '入驻', formSearch: formSearch.join(',') };
      const apiMap = {
        todo: todoList,
        send: sendList,
        myDone: myDoneList,
        done: doneList,
        copy: copyList,
      };
      return apiMap[scope](current, size, { ...params, ...extraParams });
    },
	async requestAllByScope(scope) {
	  const size = 100;
	  const firstResponse = await this.requestByScope(scope, 1, size);
	  const firstResult = firstResponse.data.data || {};
	  const records = [...(firstResult.records || [])];
	  const total = Number(firstResult.total) || records.length;
	  const pageCount = Math.ceil(total / size);
	  if (pageCount > 1) {
		const remainingResponses = await Promise.all(
		  Array.from({ length: pageCount - 1 }, (_, index) => this.requestByScope(scope, index + 2, size))
		);
		remainingResponses.forEach(res => {
		  records.push(...((res.data.data || {}).records || []));
		});
	  }
	  return this.filterTenantEntryRecords(records, scope);
	},
    async loadAllVisibleRecords() {
	  const scopes = this.scopeOptions.map(item => item.value);
	  const scopedRecordLists = await Promise.all(scopes.map(scope => this.requestAllByScope(scope)));
	  const records = [];
	  const totals = {};
	  scopedRecordLists.forEach((scopedRecords, index) => {
		const scope = scopes[index];
		totals[scope] = scopedRecords.length;
		scopedRecords.forEach(item => {
		  records.push(this.normalizeRow(item, scope));
		});
	  });
	  this.summaryCards = this.summaryCards.map(card => ({
		...card,
		value: Number(totals[card.key]) || 0,
	  }));
	  return this.deduplicateRecords(records);
	},
	deduplicateRecords(records) {
	  const scopePriority = { todo: 5, send: 4, myDone: 3, done: 2, copy: 1 };
	  const recordMap = new Map();
	  records.forEach(row => {
		const key = row.processInstanceId || row.processId || row.taskId || row.id || row.rowKey;
		const existing = recordMap.get(key);
		if (!existing || (scopePriority[row.scope] || 0) > (scopePriority[existing.scope] || 0)) {
		  recordMap.set(key, row);
		}
	  });
	  return [...recordMap.values()].sort((left, right) => {
		return this.recordTimestamp(right) - this.recordTimestamp(left);
	  });
	},
	recordTimestamp(row) {
	  const value = row.createTime || row.startTime || row.endTime || row.arriveTime;
	  const timestamp = value ? new Date(value).getTime() : 0;
	  return Number.isNaN(timestamp) ? 0 : timestamp;
	},
	async onLoad() {
      this.loading = true;
	  try {
		const records = (await this.loadAllVisibleRecords()).filter(row => this.matchesStatus(row));
		this.page.total = records.length;
		const start = (this.page.currentPage - 1) * this.page.pageSize;
		this.data = records.slice(start, start + this.page.pageSize);
	  } finally {
		this.loading = false;
	  }
    },
    reload() {
      this.onLoad();
    },
    normalizeRow(row, scope) {
      const vars = row.variables || {};
      const processInstanceId = row.processInstanceId || row.processId || row.processInsId;
      return {
        ...row,
        rowKey: row.taskId || row.id || processInstanceId,
		scope,
        processInstanceId,
        enterpriseName:
          vars.enterpriseName ||
          row.enterpriseName ||
          this.titleEnterpriseName(row.title) ||
          row.processDefinitionName ||
          '-',
        opportunityId: vars.opportunityId || row.businessId || row.businessKey,
        taskName: this.realCurrentNode(row),
        statusLabel: this.realStatusText(row),
        startUsername: row.startUsername || row.initiator || row.assigneeName || '-',
      };
    },
    filterTenantEntryRecords(records, scope) {
      if (scope !== 'copy') {
		return records;
      }
      return records.filter(item => {
        const title = item.title || '';
        const formKey = item.formKey || '';
        return title.includes('入驻') || COPY_FORM_KEYS.includes(formKey);
      });
    },
    titleEnterpriseName(title = '') {
      const match = title.match(/^(.+?)\s*入驻审核/);
      return match ? match[1] : '';
    },
    realStatusValue(row = {}) {
      const value = row.processIsFinished ?? row.processStatus ?? row.state;
      if (value === undefined || value === null || value === '') {
        if (row.scope === 'done') return 'finished';
        if (row.scope === 'todo') return 'unfinished';
        return '';
      }
      return `${value}`;
    },
    realStatusText(row = {}) {
      const value = this.realStatusValue(row);
      const map = {
        1: '审批中',
        unfinished: '审批中',
        running: '审批中',
        active: '审批中',
        2: '被驳回',
        99: '已完成',
        finished: '已完成',
        finish: '已完成',
        completed: '已完成',
        complete: '已完成',
        approved: '已完成',
        done: '已完成',
        3: '已撤回',
        recall: '已撤回',
        withdraw: '已撤销',
        97: '已撤销',
        98: '已终结',
        terminate: '已终结',
        terminated: '已终结',
        96: '已删除',
        deleted: '已删除',
        reject: '被驳回',
        rejected: '被驳回',
      };
      return map[value] || row.statusLabel || '审批中';
    },
    realCurrentNode(row = {}) {
      const status = this.realStatusValue(row);
      if (
        ['99', 'finished', 'finish', 'completed', 'complete', 'approved', 'done'].includes(status)
      ) {
        return '流程结束';
      }
      return row.taskName || row.currentNodeName || row.title || row.processDefinitionName || '-';
    },
    statusTag(row) {
      const value = this.realStatusValue(row);
      if (['1', 'unfinished', 'running', 'active'].includes(value)) return 'warning';
      if (['99', 'finished', 'finish', 'completed', 'complete', 'approved', 'done'].includes(value))
        return 'success';
      if (['2', 'reject', 'rejected'].includes(value)) return 'danger';
      if (
        [
          '3',
          'recall',
          'withdraw',
          '97',
          '98',
          'terminate',
          'terminated',
          '96',
          'deleted',
        ].includes(value)
      )
        return 'info';
      if (row.scope === 'copy') return 'info';
      return 'primary';
    },
    matchesStatus(row) {
      if (!this.query.processIsFinished) return true;
      const value = this.realStatusValue(row);
      const statusMap = {
        unfinished: ['1', 'unfinished', 'running', 'active'],
        finished: ['99', 'finished', 'finish', 'completed', 'complete', 'approved', 'done'],
        reject: ['2', 'reject', 'rejected'],
        recall: ['3', 'recall'],
      };
      return (statusMap[this.query.processIsFinished] || []).includes(value);
    },
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad();
    },
		searchReset() {
		  this.query = { enterpriseName: '', processIsFinished: '' };
		  this.page.currentPage = 1;
		  this.onLoad();
		},
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.onLoad();
    },
    openStart() {
      this.startVisible = true;
      this.loadProcessOptions();
      if (!this.opportunityOptions.length) this.searchOpportunity('');
    },
    loadProcessOptions() {
      this.processLoading = true;
      getDeploymentList(1, -1, {
        status: 1,
      })
        .then(res => {
          const records = (res.data.data || {}).records || [];
          this.processOptions = records.filter(item => this.isTenantEntryProcess(item));
          if (
            !this.startForm.processDefKey ||
            !this.processOptions.some(item => item.key === this.startForm.processDefKey)
          ) {
            const preferred =
              this.processOptions.find(item => item.key === DEFAULT_PROCESS_KEY) ||
              this.processOptions.find(item => item.name && item.name.includes('入驻')) ||
              this.processOptions[0];
            this.startForm.processDefKey = preferred ? preferred.key : DEFAULT_PROCESS_KEY;
          }
        })
        .finally(() => {
          this.processLoading = false;
        });
    },
		isTenantEntryProcess(item = {}) {
		  const key = `${item.key || ''}`.toLowerCase();
		  return key === TENANT_ENTRY_BUSINESS_TYPE || key.startsWith(`${TENANT_ENTRY_BUSINESS_TYPE}-`);
		},
    processOptionLabel(item = {}) {
      return `${item.name || item.key}${item.version ? ` v${item.version}` : ''}`;
    },
    searchOpportunity(keyword) {
      this.opportunityLoading = true;
	  getTenantEntryCandidateList(1, 20, {
        keyword,
        tenantEntryCandidate: true,
      })
        .then(res => {
          this.opportunityOptions = (res.data.data || {}).records || [];
        })
        .finally(() => {
          this.opportunityLoading = false;
        });
    },
		goStart() {
		  if (!this.startForm.opportunityId) {
			this.$message.warning('请选择需要发起审批的商机企业');
			return;
		  }
      const formParams = {
        processDefKey: this.startForm.processDefKey,
        businessType: TENANT_ENTRY_BUSINESS_TYPE,
      };
      if (this.startForm.opportunityId) {
        formParams.opportunityId = this.startForm.opportunityId;
      }
      const params = {
        processDefKey: this.startForm.processDefKey,
        params: formParams,
      };
      this.pushExternal('start', params);
      this.startVisible = false;
    },
    openDetail(row) {
      this.pushExternal('detail', {
        taskId: row.taskId,
        processInsId: row.processInstanceId || row.processId,
      });
    },
    pushExternal(type, payload) {
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      const routeName = type === 'start' ? '发起流程TenantEntry' : '流程详情TenantEntry';
      if (!this.$router.hasRoute(routeName)) {
        this.$router.addRoute({
          path: '/plugin/workflow/pages/process/external',
          component: Layout,
          children: [
            {
              path: `TenantEntry/${type}`,
              name: routeName,
              component: () =>
                import(`@/views/plugin/workflow/pages/external/TenantEntry/${type}.vue`),
            },
          ],
        });
      }
      this.$router.push(
        `/plugin/workflow/pages/process/external/TenantEntry/${type}?p=${encodedParam}`
      );
    },
    canExportApprovalForm(row) {
      return row.opportunityId && ['done', 'send', 'myDone'].includes(row.scope);
    },
    openApprovalForm(row) {
      const processInsId = row.processInstanceId || row.processId;
      this.approvalPreviewRow = row;
      this.approvalPreview.visible = true;
      this.approvalPreview.loading = true;
      this.approvalPreview.title = '企业入驻审批表预览';
      this.approvalPreview.html = '';
	  this.approvalPreview.downloadUrl = `/blade-park/tenant-entry/approval-form/${row.opportunityId}`;
      this.approvalPreview.downloadLabel = '下载Word';
      previewTenantEntryApprovalForm(row.opportunityId, processInsId)
        .then(res => {
          const data = res.data.data || {};
          this.approvalPreview.title = data.noticeName || '企业入驻审批表预览';
          this.approvalPreview.html = data.html || '';
          this.approvalPreview.fallbackName =
            data.fileName || `企业入驻审批表-${row.enterpriseName || row.opportunityId}.docx`;
        })
        .catch(error => {
          this.approvalPreview.visible = false;
          throw error;
        })
        .finally(() => {
          this.approvalPreview.loading = false;
        });
    },
    downloadApprovalForm() {
      const row = this.approvalPreviewRow;
      if (!row) return;
      exportTenantEntryApprovalForm(row.opportunityId, row.processInstanceId || row.processId).then(
        res => {
          const disposition = res.headers && res.headers['content-disposition'];
          const filename = resolveDownloadFilename(
            disposition,
            this.approvalPreview.fallbackName ||
              `企业入驻审批表-${row.enterpriseName || row.opportunityId}.docx`
          );
          const contentType =
            (res.headers && res.headers['content-type']) || 'application/octet-stream';
          downloadFile(res.data, filename, contentType);
          this.$message.success('导出成功');
        }
      );
    },
  },
};
</script>

<style lang="scss" scoped>
.tenant-entry-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tenant-entry-search,
.tenant-entry-list-panel {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.tenant-entry-search {
  padding: 16px 18px 4px;
}

.tenant-entry-search :deep(.el-form-item) {
  margin: 0 22px 12px 0;
}

.tenant-entry-search :deep(.el-form-item__label) {
  height: 36px;
  line-height: 36px;
  color: #303133;
}

.tenant-entry-search :deep(.el-input),
.tenant-entry-search :deep(.el-select) {
  width: 168px;
}

.tenant-entry-search :deep(.el-input__wrapper),
.tenant-entry-search :deep(.el-select__wrapper) {
  min-height: 36px;
}

.tenant-entry-search :deep(.el-button) {
  height: 36px;
  padding: 0 18px;
}

.tenant-entry-toolbar {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.tenant-entry-list-panel {
  overflow: hidden;
  padding: 0 18px 18px;
}

.tenant-entry-toolbar__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.tenant-entry-toolbar__title strong {
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.tenant-entry-toolbar__title span {
  color: #909399;
  font-size: 13px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tenant-entry-table {
  width: 100%;
}

.tenant-entry-table :deep(.el-table__cell) {
  text-align: center;
}

.tenant-entry-table :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}

.tenant-entry-table :deep(.enterprise-name-column .cell) {
  overflow: hidden;
  flex-wrap: nowrap;
  white-space: nowrap;
  word-break: keep-all;
}

.tenant-entry-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.option-extra {
  float: right;
  color: #909399;
  font-size: 12px;
}

.start-form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.approval-html {
  max-height: 66vh;
  overflow: auto;
}

@media (max-width: 1200px) {
  .tenant-entry-search :deep(.el-form-item) {
    margin-right: 14px;
  }
}
</style>
