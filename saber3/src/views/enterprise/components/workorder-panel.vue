<template>
  <div class="workorder-panel">
    <stat-cards v-if="showStatistics" :items="statItems" />

    <section v-if="showSearch" class="contract-search">
      <el-form :inline="true" :model="query">
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

    <section class="workorder-table-card">
      <div class="contract-toolbar">
        <div class="toolbar-left">
          <el-button v-if="permissionList.workorderAddBtn" type="primary" icon="el-icon-plus" @click="$emit('create')">{{ createLabel }}</el-button>
        </div>
        <el-tooltip v-if="showReload" content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="$emit('reload')" />
        </el-tooltip>
      </div>

      <el-table v-loading="loading" :data="data" border row-key="orderId" class="contract-table">
        <el-table-column prop="orderNo" label="工单编号" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="serviceName" label="服务名称" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="parkName" label="园区" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="roomInfo" label="房源" min-width="140" align="center" show-overflow-tooltip />
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
        <el-table-column prop="assignTo" label="指派人" width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column v-if="showActions" label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="permissionList.workorderViewBtn" type="primary" text @click="$emit('view', row)">详情</el-button>
              <el-button v-if="canDispose(row)" type="primary" text @click="$emit('dispose', row)">处置</el-button>
              <el-button v-if="canRate(row)" type="success" text @click="$emit('rate', row)">评价</el-button>
              <el-button v-if="permissionList.workorderDelBtn" type="danger" text @click="$emit('remove', row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="contract-pagination">
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
    </section>
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
    showActions: { type: Boolean, default: true },
    showStatistics: { type: Boolean, default: true },
    showSearch: { type: Boolean, default: true },
  },
  emits: ['search', 'reset', 'reload', 'size-change', 'current-change', 'create', 'view', 'dispose', 'rate', 'remove'],
  computed: {
    statItems() {
      const items = [];
      if (this.showTotalStat) {
        items.push({ key: 'total', label: '工单总数', value: this.statistics.totalCount || 0 });
      }
      items.push(
        { key: 'pending', label: '待受理', value: this.statistics.pendingCount || 0 },
        { key: 'processing', label: '处理中', value: this.statistics.processingCount || 0 },
        { key: 'finished', label: '已完成', value: this.statistics.finishedCount || 0 },
        { key: 'rated', label: '已评价', value: this.statistics.ratedCount || 0 },
        { key: 'closed', label: '已关闭', value: this.statistics.closedCount || 0 },
      );
      return items;
    },
  },
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
.workorder-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contract-search,
.workorder-table-card {
  border-radius: 10px;
}

.contract-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.contract-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.contract-search :deep(.el-input),
.contract-search :deep(.el-select) {
  width: 190px;
}

.workorder-table-card {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.contract-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.contract-table {
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

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 14px;
}

.workorder-panel :deep(.el-button),
.workorder-panel :deep(.el-input__wrapper),
.workorder-panel :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1180px) {
  .toolbar-left {
    flex-wrap: wrap;
  }
}
</style>
