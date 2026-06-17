<template>
  <div>
    <section class="toolbar-row">
      <el-form :inline="true" :model="query" class="query-form">
        <el-form-item label="工单号">
          <el-input v-model="query.orderNo" clearable placeholder="请输入工单号" @keyup.enter="$emit('search')" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="query.customerName" clearable placeholder="请输入客户名称" @keyup.enter="$emit('search')" />
        </el-form-item>
        <el-form-item v-if="showServiceFilter" label="服务项">
          <el-select v-model="query.serviceId" clearable filterable placeholder="全部服务">
            <el-option v-for="item in serviceOptions" :key="item.serviceId" :label="item.serviceName" :value="item.serviceId" />
          </el-select>
        </el-form-item>
        <el-form-item label="工单状态">
          <el-select v-model="query.orderStatus" clearable placeholder="全部状态">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="query.priority" clearable placeholder="全部优先级">
            <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="$emit('search')">搜索</el-button>
          <el-button icon="el-icon-delete" @click="$emit('reset')">清空</el-button>
        </el-form-item>
      </el-form>
    </section>

    <div class="workorder-stats" :class="{ 'workorder-stats--compact': !showTotalStat }">
      <div v-if="showTotalStat" class="stat-card stat-total"><span>全部</span><strong>{{ statistics.totalCount || 0 }}</strong></div>
      <div class="stat-card stat-pending"><span>待受理</span><strong>{{ statistics.pendingCount || 0 }}</strong></div>
      <div class="stat-card stat-processing"><span>处理中</span><strong>{{ statistics.processingCount || 0 }}</strong></div>
      <div class="stat-card stat-finished"><span>已完成</span><strong>{{ statistics.finishedCount || 0 }}</strong></div>
      <div class="stat-card stat-rated"><span>已评价</span><strong>{{ statistics.ratedCount || 0 }}</strong></div>
      <div class="stat-card stat-closed"><span>已关闭</span><strong>{{ statistics.closedCount || 0 }}</strong></div>
    </div>

    <div class="action-row">
      <el-button v-if="permissionList.workorderAddBtn" type="primary" icon="el-icon-plus" @click="$emit('create')">{{ createLabel }}</el-button>
      <el-tooltip v-if="showReload" content="刷新" placement="top">
        <el-button icon="el-icon-refresh" circle @click="$emit('reload')" />
      </el-tooltip>
    </div>

    <el-table v-loading="loading" :data="data" border row-key="orderId">
      <el-table-column prop="orderNo" label="工单编号" width="180" />
      <el-table-column prop="serviceName" label="服务名称" min-width="130" show-overflow-tooltip />
      <el-table-column prop="customerName" label="客户" min-width="150" show-overflow-tooltip />
      <el-table-column prop="parkName" label="园区" min-width="120" show-overflow-tooltip />
      <el-table-column prop="roomInfo" label="房源" min-width="150" show-overflow-tooltip />
      <el-table-column prop="priority" label="优先级" width="96" align="center">
        <template #default="{ row }">
          <el-tag :type="priorityTagType(row.priority)" effect="plain">{{ priorityText(row.priority) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="orderStatus" label="状态" width="96" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.orderStatus)">{{ statusText(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="assignTo" label="指派人" width="120" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="240" fixed="right" align="center">
        <template #default="{ row }">
          <el-button v-if="permissionList.workorderViewBtn" type="primary" text icon="el-icon-view" @click="$emit('view', row)">详情</el-button>
          <el-button v-if="canDispose(row)" type="primary" text icon="el-icon-edit" @click="$emit('dispose', row)">处置</el-button>
          <el-button v-if="canRate(row)" type="success" text icon="el-icon-star-off" @click="$emit('rate', row)">评价</el-button>
          <el-button v-if="permissionList.workorderDelBtn" type="danger" text icon="el-icon-delete" @click="$emit('remove', row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        background
        :current-page="page.currentPage"
        :page-size="page.pageSize"
        :page-sizes="[10, 20, 30, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="page.total"
        @size-change="size => $emit('size-change', size)"
        @current-change="current => $emit('current-change', current)"
      />
    </div>
  </div>
</template>

<script>
export default {
  name: 'WorkorderPanel',
  props: {
    query: { type: Object, required: true },
    page: { type: Object, required: true },
    data: { type: Array, required: true },
    loading: { type: Boolean, default: false },
    statistics: { type: Object, required: true },
    serviceOptions: { type: Array, required: true },
    statusOptions: { type: Array, required: true },
    priorityOptions: { type: Array, required: true },
    permissionList: { type: Object, required: true },
    mine: { type: Boolean, default: false },
    showTotalStat: { type: Boolean, default: true },
    showServiceFilter: { type: Boolean, default: true },
    createLabel: { type: String, default: '创建工单' },
    showReload: { type: Boolean, default: true },
  },
  emits: ['search', 'reset', 'reload', 'size-change', 'current-change', 'create', 'view', 'dispose', 'rate', 'remove'],
  methods: {
    statusText(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    statusTagType(value) {
      return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'info' }[value] || 'info';
    },
    priorityText(value) {
      const item = this.priorityOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    priorityTagType(value) {
      return { 0: 'danger', 1: 'primary', 2: 'info' }[value] || 'info';
    },
    canDispose(row) {
      return this.permissionList.workorderEditBtn && row.orderStatus !== '3' && row.orderStatus !== '4';
    },
    canRate(row) {
      return this.permissionList.workorderRateBtn && row.orderStatus === '2';
    },
  },
};
</script>

<style scoped>
.toolbar-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.query-form {
  flex: 1;
  min-width: 0;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.workorder-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(110px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.workorder-stats--compact {
  grid-template-columns: repeat(5, minmax(110px, 1fr));
}

.stat-card {
  border: 1px solid #edf0f5;
  border-radius: 6px;
  padding: 12px 14px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.stat-card span {
  display: block;
  color: #687386;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  color: #1f2d3d;
  font-size: 24px;
  font-weight: 600;
}

.stat-total {
  border-top: 3px solid #2f6fed;
}

.stat-pending {
  border-top: 3px solid #e6a23c;
}

.stat-processing {
  border-top: 3px solid #409eff;
}

.stat-finished {
  border-top: 3px solid #67c23a;
}

.stat-rated {
  border-top: 3px solid #8e7cc3;
}

.stat-closed {
  border-top: 3px solid #909399;
}

@media (max-width: 1180px) {
  .toolbar-row {
    display: block;
  }

  .action-row {
    justify-content: flex-end;
    margin-top: 8px;
  }

  .workorder-stats {
    grid-template-columns: repeat(3, minmax(110px, 1fr));
  }
}

@media (max-width: 720px) {
  .workorder-stats {
    grid-template-columns: repeat(2, minmax(110px, 1fr));
  }
}
</style>
