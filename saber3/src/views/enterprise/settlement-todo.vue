<template>
  <basic-container>
    <div class="settlement-todo-page">
      <business-page-intro title="入驻意向待办" subtitle="处理小程序提交的企业入驻申请" />

      <section class="summary-grid">
        <div v-for="item in summaryItems" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="todo-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="企业信息">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="企业、联系人或手机号"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="所属园区">
            <el-select v-model="query.parkId" clearable filterable placeholder="全部园区">
              <el-option v-for="park in parks" :key="park.id" :label="park.name" :value="park.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="待办状态">
            <el-select v-model="query.todoStatus" clearable placeholder="全部状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
            <el-button icon="el-icon-delete" @click="reset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="todo-table-card">
        <div class="table-toolbar">
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </div>

        <el-table
          v-loading="loading"
          :data="rows"
          border
          row-key="todoId"
          class="todo-table"
          empty-text="暂无招商待办"
        >
          <el-table-column
            prop="todoNo"
            label="待办编号"
            width="176"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column label="所属园区" min-width="130" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ parkName(row.parkId) }}</template>
          </el-table-column>
          <el-table-column
            prop="enterpriseName"
            label="企业名称"
            min-width="220"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column
            prop="contactName"
            label="联系人"
            width="110"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column prop="contactPhone" label="联系电话" width="132" align="center" />
          <el-table-column
            prop="industryType"
            label="行业类型"
            min-width="130"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column prop="intentArea" label="意向面积" width="112" align="center">
            <template #default="{ row }">{{ valueWithUnit(row.intentArea, 'm²') }}</template>
          </el-table-column>
          <el-table-column prop="expectedEntryDate" label="预计入驻" width="120" align="center" />
          <el-table-column prop="todoStatus" label="状态" width="96" align="center">
            <template #default="{ row }">
              <el-tag :type="statusMeta(row.todoStatus).type" effect="plain">
                {{ statusMeta(row.todoStatus).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="170" align="center" />
          <el-table-column
            v-if="permissionList.viewBtn || permissionList.processBtn"
            label="操作"
            :width="permissionList.processBtn ? 156 : 96"
            fixed="right"
            align="center"
          >
            <template #default="{ row }">
              <div class="table-row-actions">
                <el-button
                  v-if="permissionList.viewBtn"
                  type="primary"
                  text
                  @click="openDetail(row)"
                  >详情</el-button
                >
                <el-button
                  v-if="permissionList.processBtn && !isFinished(row.todoStatus)"
                  type="primary"
                  text
                  @click="openProcess(row)"
                >
                  处理
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="todo-pagination">
          <el-pagination
            v-model:current-page="page.currentPage"
            v-model:page-size="page.pageSize"
            background
            :page-sizes="[10, 20, 30, 40, 50, 100]"
            :total="page.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="changeSize"
            @current-change="loadList"
          />
        </div>
      </section>
    </div>

    <el-drawer
      v-model="detail.visible"
      title="招商待办详情"
      size="680px"
      append-to-body
      destroy-on-close
    >
      <div v-loading="detail.loading">
        <el-descriptions v-if="detail.row" :column="2" border>
          <el-descriptions-item label="待办编号">{{ detail.row.todoNo }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusMeta(detail.row.todoStatus).type" effect="plain">
              {{ statusMeta(detail.row.todoStatus).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属园区">{{
            parkName(detail.row.parkId)
          }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{
            detail.row.createTime || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="企业名称" :span="2">{{
            detail.row.enterpriseName || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="信用代码" :span="2">{{
            detail.row.creditCode || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{
            detail.row.contactName || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{
            detail.row.contactPhone || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="行业类型">{{
            detail.row.industryType || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="企业规模">{{
            detail.row.enterpriseScale || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="意向面积">{{
            valueWithUnit(detail.row.intentArea, 'm²')
          }}</el-descriptions-item>
          <el-descriptions-item label="预计入驻">{{
            detail.row.expectedEntryDate || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="需求说明" :span="2">{{
            detail.row.demandDesc || '-'
          }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.row" class="detail-section-title">处理信息</div>
        <el-descriptions v-if="detail.row" :column="2" border>
          <el-descriptions-item label="处理人">{{
            detail.row.assigneeName || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="处理时间">{{
            detail.row.processedTime || '-'
          }}</el-descriptions-item>
          <el-descriptions-item label="处理说明" :span="2">{{
            detail.row.processRemark || '-'
          }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.row.rejectReason" label="驳回原因" :span="2">
            {{ detail.row.rejectReason }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <el-dialog
      v-model="process.visible"
      title="处理招商待办"
      width="600px"
      append-to-body
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form ref="processFormRef" :model="process.form" :rules="processRules" label-width="96px">
        <el-form-item label="企业名称">
          <el-input :model-value="process.row?.enterpriseName || '-'" disabled />
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="statusMeta(process.row?.todoStatus).type" effect="plain">
            {{ statusMeta(process.row?.todoStatus).label }}
          </el-tag>
        </el-form-item>
        <el-form-item label="处理动作" prop="action">
          <el-select v-model="process.form.action" placeholder="请选择处理动作" style="width: 100%">
            <el-option
              v-for="item in actionOptions(process.row?.todoStatus)"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明" prop="content">
          <el-input
            v-model="process.form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入本次受理或跟进说明"
          />
        </el-form-item>
        <el-form-item v-if="process.form.action === 'REJECT'" label="驳回原因" prop="reason">
          <el-input
            v-model="process.form.reason"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="process.visible = false">取消</el-button>
        <el-button type="primary" :loading="process.submitting" @click="submitProcess"
          >确认处理</el-button
        >
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage } from 'element-plus';
import { getDetail, getList, getStatistics, processTodo } from '@/api/business/settlement-todo';
import { getList as getParkList } from '@/api/park/park';

const statusOptions = [
  { value: '0', label: '待受理', type: 'primary' },
  { value: '1', label: '已受理', type: 'primary' },
  { value: '2', label: '跟进中', type: 'warning' },
  { value: '3', label: '已完成', type: 'success' },
  { value: '4', label: '已驳回', type: 'danger' },
];

const defaultProcessForm = () => ({ action: '', content: '', reason: '' });

export default {
  name: 'EnterpriseSettlementTodo',
  data() {
    return {
      loading: false,
      query: { keyword: '', parkId: '', todoStatus: '' },
      statusOptions,
      statistics: {},
      parks: [],
      rows: [],
      page: { currentPage: 1, pageSize: 10, total: 0 },
      detail: { visible: false, loading: false, row: null },
      process: { visible: false, submitting: false, row: null, form: defaultProcessForm() },
      processRules: {
        action: [{ required: true, message: '请选择处理动作', trigger: 'change' }],
        content: [{ required: true, message: '请输入处理说明', trigger: 'blur' }],
        reason: [
          {
            validator: (rule, value, callback) => {
              if (this.process.form.action === 'REJECT' && !String(value || '').trim()) {
                callback(new Error('请输入驳回原因'));
                return;
              }
              callback();
            },
            trigger: 'blur',
          },
        ],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        viewBtn: this.validData(this.permission.settlement_todo_view, false),
        processBtn: this.validData(this.permission.settlement_todo_process, false),
      };
    },
    summaryItems() {
      return [
        { key: 'total', label: '待办总数', value: this.statistics.totalCount || 0 },
        { key: 'pending', label: '待受理', value: this.statistics.pendingCount || 0 },
        { key: 'processing', label: '处理中', value: this.statistics.processingCount || 0 },
        { key: 'finished', label: '已完成', value: this.statistics.finishedCount || 0 },
      ];
    },
  },
  created() {
    this.loadParks();
    this.reload();
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    async loadList() {
      this.loading = true;
      try {
        const res = await getList(this.page.currentPage, this.page.pageSize, this.query);
        const data = res.data.data || {};
        this.rows = data.records || [];
        this.page.total = Number(data.total) || 0;
      } finally {
        this.loading = false;
      }
    },
    async loadStatistics() {
      const { todoStatus, ...params } = this.query;
      const res = await getStatistics(params);
      this.statistics = res.data.data || {};
    },
    async loadParks() {
      const res = await getParkList(1, 999, {});
      this.parks = res.data.data?.records || [];
    },
    reload() {
      return Promise.all([this.loadList(), this.loadStatistics()]);
    },
    search() {
      this.page.currentPage = 1;
      this.reload();
    },
    reset() {
      this.query = { keyword: '', parkId: '', todoStatus: '' };
      this.page.currentPage = 1;
      this.reload();
    },
    changeSize() {
      this.page.currentPage = 1;
      this.loadList();
    },
    parkName(id) {
      return this.parks.find(item => String(item.id) === String(id))?.name || '-';
    },
    statusMeta(value) {
      return (
        statusOptions.find(item => item.value === String(value)) || { label: '-', type: 'info' }
      );
    },
    isFinished(value) {
      return ['3', '4'].includes(String(value));
    },
    valueWithUnit(value, unit) {
      return value === null || value === undefined || value === '' ? '-' : `${value}${unit}`;
    },
    actionOptions(status) {
      if (String(status) === '0') {
        return [
          { value: 'ACCEPT', label: '受理' },
          { value: 'REJECT', label: '驳回' },
        ];
      }
      return [
        { value: 'FOLLOW', label: '更新跟进' },
        { value: 'COMPLETE', label: '完成' },
        { value: 'REJECT', label: '驳回' },
      ];
    },
    async fetchDetail(row) {
      const res = await getDetail(row.todoId);
      return res.data.data;
    },
    async openDetail(row) {
      this.detail.visible = true;
      this.detail.loading = true;
      this.detail.row = null;
      try {
        this.detail.row = await this.fetchDetail(row);
      } finally {
        this.detail.loading = false;
      }
    },
    async openProcess(row) {
      const current = await this.fetchDetail(row);
      if (this.isFinished(current.todoStatus)) {
        ElMessage.warning('该招商待办已处理结束');
        this.reload();
        return;
      }
      this.process.row = current;
      this.process.form = defaultProcessForm();
      this.process.visible = true;
      this.$nextTick(() => this.$refs.processFormRef?.clearValidate());
    },
    submitProcess() {
      this.$refs.processFormRef.validate(async valid => {
        if (!valid) return;
        this.process.submitting = true;
        try {
          await processTodo(this.process.row.todoId, this.process.form);
          ElMessage.success('处理成功');
          this.process.visible = false;
          await this.reload();
        } finally {
          this.process.submitting = false;
        }
      });
    },
  },
};
</script>

<style scoped>
.settlement-todo-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.todo-search,
.todo-table-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.todo-search {
  padding: 16px 18px 4px;
}

.todo-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.todo-search :deep(.el-input),
.todo-search :deep(.el-select) {
  width: 200px;
}

.todo-table-card {
  overflow: hidden;
}

.table-toolbar {
  min-height: 40px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.todo-table {
  width: 100%;
  border-radius: 0;
}

.table-row-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.todo-pagination {
  padding: 12px 16px 14px;
  display: flex;
  justify-content: flex-end;
}

.detail-section-title {
  margin: 22px 0 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.settlement-todo-page :deep(.el-button),
.settlement-todo-page :deep(.el-input__wrapper),
.settlement-todo-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1180px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .todo-search :deep(.el-form-item),
  .todo-search :deep(.el-input),
  .todo-search :deep(.el-select) {
    width: 100%;
    margin-right: 0;
  }
}
</style>
