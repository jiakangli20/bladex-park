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
            <el-input v-model="query.enterpriseName" clearable placeholder="请输入企业名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="流程状态">
            <el-select v-model="query.scope" placeholder="请选择状态" @change="searchChange">
              <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="tenant-entry-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" icon="el-icon-plus" @click="openStart">发起审核</el-button>
        </div>
      </section>

      <el-table v-loading="loading" :data="data" border row-key="rowKey" class="tenant-entry-table">
        <el-table-column
          prop="enterpriseName"
          label="企业名称"
          width="180"
          align="center"
          show-overflow-tooltip
          class-name="enterprise-name-column"
        />
        <el-table-column prop="processDefinitionName" label="审批类型" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column prop="taskName" label="当前节点" min-width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="statusLabel" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row)" effect="plain">{{ row.statusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startUsername" label="发起人" width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="createTime" label="发起/到达时间" min-width="210" align="center" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="operation-actions">
              <el-button v-if="row.scope === 'todo'" text type="primary" @click="openDetail(row)">处理</el-button>
              <el-button v-else text type="primary" @click="openDetail(row)">详情</el-button>
              <el-button v-if="canExportApprovalForm(row)" text type="primary" @click="openApprovalForm(row)">导出审核表</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="tenant-entry-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-sizes="[10, 20, 30, 50]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>

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
          <el-form-item label="入驻商机">
            <el-select
              v-model="startForm.opportunityId"
              filterable
              remote
              clearable
              :remote-method="searchOpportunity"
              :loading="opportunityLoading"
              placeholder="请选择待审核商机"
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
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="startVisible = false">取消</el-button>
          <el-button type="primary" :disabled="!startForm.processDefKey || !startForm.opportunityId" @click="goStart">下一步</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="approvalVisible" title="企业入驻审批表" width="860px" append-to-body>
        <div class="approval-html" v-html="approvalHtml"></div>
        <template #footer>
          <el-button @click="approvalVisible = false">关闭</el-button>
          <el-button type="primary" icon="el-icon-printer" @click="printApprovalForm">打印 / 另存为 PDF</el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import Layout from '@/page/index/index.vue';
import {
  copyList,
  doneList,
  myDoneList,
  sendList,
  todoList,
} from '@/views/plugin/workflow/api/process/process';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { exportTenantEntryApprovalForm, getOpportunityList } from '@/api/business/opportunity';

const DEFAULT_PROCESS_KEY = 'tenant_entry-1';
const TENANT_ENTRY_BUSINESS_TYPE = 'tenant_entry';
const COPY_FORM_KEYS = ['wf_ex_TenantEntry', 'wf_ex_入驻'];

export default {
  name: 'SettlementProjectApproval',
  data() {
    return {
      loading: false,
      data: [],
      query: {
        enterpriseName: '',
        scope: 'send',
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
      startVisible: false,
      startForm: {
        processDefKey: DEFAULT_PROCESS_KEY,
        opportunityId: '',
      },
      processLoading: false,
      processOptions: [],
      opportunityLoading: false,
      opportunityOptions: [],
      approvalVisible: false,
      approvalHtml: '',
    };
  },
  created() {
    this.applyRouteQuery();
    this.loadSummary();
    this.onLoad();
  },
  activated() {
    this.applyRouteQuery();
    this.loadSummary();
    this.onLoad();
  },
  methods: {
    applyRouteQuery() {
      const { scope } = this.$route.query || {};
      if (scope && this.scopeOptions.some(item => item.value === scope)) {
        this.query.scope = scope;
      }
    },
    requestByScope(scope, current = this.page.currentPage, size = this.page.pageSize) {
      const params = scope === 'copy'
        ? { title: '入驻' }
        : { processDefinitionName: '入驻' };
      const apiMap = {
        todo: todoList,
        send: sendList,
        myDone: myDoneList,
        done: doneList,
        copy: copyList,
      };
      return apiMap[scope](current, size, params);
    },
    loadSummary() {
      this.summaryCards.forEach(card => {
        this.requestByScope(card.key, 1, 1).then(res => {
          const result = res.data.data || {};
          card.value = Number(result.total) || 0;
        });
      });
    },
    onLoad() {
      this.loading = true;
      this.requestByScope(this.query.scope)
        .then(res => {
          const result = res.data.data || {};
          const records = this.filterTenantEntryRecords(result.records || [], this.query.scope);
          this.data = records
            .map(item => this.normalizeRow(item))
            .filter(item => !this.query.enterpriseName || item.enterpriseName.includes(this.query.enterpriseName));
          this.page.total = Number(result.total) || this.data.length;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    normalizeRow(row) {
      const vars = row.variables || {};
      const processInstanceId = row.processInstanceId || row.processId || row.processInsId;
      return {
        ...row,
        rowKey: row.taskId || row.id || processInstanceId,
        scope: this.query.scope,
        processInstanceId,
        enterpriseName: vars.enterpriseName || row.enterpriseName || this.titleEnterpriseName(row.title) || row.processDefinitionName || '-',
        opportunityId: vars.opportunityId || row.businessId || row.businessKey,
        taskName: row.taskName || row.title || row.processDefinitionName || '-',
        statusLabel: this.scopeText(this.query.scope),
        startUsername: row.startUsername || row.initiator || row.assigneeName || '-',
      };
    },
    filterTenantEntryRecords(records, scope) {
      if (scope !== 'copy') {
        return records.filter(item => {
          const vars = item.variables || {};
          const title = item.title || item.processDefinitionName || '';
          const processKey = item.processDefinitionKey || '';
          return vars.businessType === TENANT_ENTRY_BUSINESS_TYPE
            || processKey.includes('tenant_entry')
            || title.includes('入驻');
        });
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
    scopeText(scope) {
      const item = this.scopeOptions.find(option => option.value === scope);
      return item ? item.label : '审批中';
    },
    statusTag(row) {
      if (row.scope === 'todo') return 'warning';
      if (row.scope === 'done') return 'success';
      if (row.scope === 'copy') return 'info';
      return 'primary';
    },
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad();
    },
    searchReset() {
      this.query = { enterpriseName: '', scope: 'send' };
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
      }).then(res => {
        const records = (res.data.data || {}).records || [];
        this.processOptions = records.filter(item => this.isTenantEntryProcess(item));
        if (!this.startForm.processDefKey || !this.processOptions.some(item => item.key === this.startForm.processDefKey)) {
          const preferred = this.processOptions.find(item => item.key === DEFAULT_PROCESS_KEY)
            || this.processOptions.find(item => item.name && item.name.includes('入驻'))
            || this.processOptions[0];
          this.startForm.processDefKey = preferred ? preferred.key : DEFAULT_PROCESS_KEY;
        }
      }).finally(() => {
        this.processLoading = false;
      });
    },
    isTenantEntryProcess(item = {}) {
      const name = item.name || '';
      const key = item.key || '';
      const formKey = item.formKey || '';
      return name.includes('入驻') || key.includes('tenant_entry') || COPY_FORM_KEYS.includes(formKey);
    },
    processOptionLabel(item = {}) {
      return `${item.name || item.key}${item.version ? ` v${item.version}` : ''}`;
    },
    searchOpportunity(keyword) {
      this.opportunityLoading = true;
      getOpportunityList(1, 20, {
        keyword,
        tenantEntryCandidate: true,
      }).then(res => {
        this.opportunityOptions = (res.data.data || {}).records || [];
      }).finally(() => {
        this.opportunityLoading = false;
      });
    },
    goStart() {
      const params = {
        processDefKey: this.startForm.processDefKey,
        params: {
          opportunityId: this.startForm.opportunityId,
          processDefKey: this.startForm.processDefKey,
          businessType: TENANT_ENTRY_BUSINESS_TYPE,
        },
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
      const encodedParam = encodeURIComponent(window.btoa(JSON.stringify(payload)));
      const routeName = type === 'start' ? '发起流程TenantEntry' : '流程详情TenantEntry';
      if (!this.$router.hasRoute(routeName)) {
        this.$router.addRoute({
          path: '/plugin/workflow/pages/process/external',
          component: Layout,
          children: [
            {
              path: `TenantEntry/${type}`,
              name: routeName,
              component: () => import(`@/views/plugin/workflow/pages/external/TenantEntry/${type}.vue`),
            },
          ],
        });
      }
      this.$router.push(`/plugin/workflow/pages/process/external/TenantEntry/${type}?p=${encodedParam}`);
    },
    canExportApprovalForm(row) {
      return row.opportunityId && ['done', 'send', 'myDone'].includes(row.scope);
    },
    openApprovalForm(row) {
      exportTenantEntryApprovalForm(row.opportunityId, row.processInstanceId).then(res => {
        this.approvalHtml = (res.data.data || {}).html || '';
        this.approvalVisible = true;
      });
    },
    printApprovalForm() {
      const win = window.open('', '_blank');
      win.document.write(this.approvalHtml);
      win.document.close();
      win.focus();
      win.print();
    },
  },
};
</script>

<style lang="scss" scoped>
.tenant-entry-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tenant-entry-search {
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
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
  align-items: center;
  justify-content: space-between;
  padding: 0;
  border: 0;
  background: transparent;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
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

.tenant-entry-table :deep(.operation-actions) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.tenant-entry-table :deep(.operation-actions .el-button) {
  margin-left: 0;
}

.tenant-entry-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.option-extra {
  float: right;
  color: #909399;
  font-size: 12px;
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
