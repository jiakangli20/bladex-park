<template>
  <basic-container>
    <div class="background-page">
      <section class="background-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="企业关键词">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="企业名称/信用代码/联系人"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="所属园区">
            <el-select v-model="query.parkId" clearable placeholder="全部园区">
              <el-option
                v-for="park in parkList"
                :key="park.id"
                :label="park.name"
                :value="park.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="风险等级">
            <el-select v-model="query.riskLevel" clearable placeholder="全部等级">
              <el-option
                v-for="item in riskOptions"
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

      <section class="background-list-panel">
        <div class="background-toolbar">
          <div class="background-toolbar__title">
            <strong>企业背景调查</strong>
            <span>共 {{ page.total }} 家企业</span>
          </div>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </div>

        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="customerId"
          class="background-table"
        >
          <el-table-column type="index" label="序号" width="70" align="center">
            <template #default="scope">{{ rowIndex(scope.$index) }}</template>
          </el-table-column>
          <el-table-column
            prop="enterpriseName"
            label="企业名称"
            min-width="220"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column
            prop="creditCode"
            label="统一社会信用代码"
            min-width="180"
            align="center"
            show-overflow-tooltip
          >
            <template #default="{ row }">{{ valueText(row.creditCode) }}</template>
          </el-table-column>
          <el-table-column label="所属园区" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ parkName(row.parkId) }}</template>
          </el-table-column>
          <el-table-column prop="contactName" label="联系人" width="110" align="center">
            <template #default="{ row }">{{ valueText(row.contactName) }}</template>
          </el-table-column>
          <el-table-column prop="contactPhone" label="联系电话" width="140" align="center">
            <template #default="{ row }">{{ valueText(row.contactPhone) }}</template>
          </el-table-column>
          <el-table-column prop="verifyStatus" label="核验状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="verifyType(row.verifyStatus)" effect="plain">{{
                verifyText(row.verifyStatus)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskLevel" label="风险等级" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="riskType(row.riskLevel)" effect="plain">{{
                riskText(row.riskLevel)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="verifyTime" label="最近核验时间" width="170" align="center">
            <template #default="{ row }">{{ valueText(row.verifyTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button text type="primary" icon="el-icon-search" @click="openInvestigation(row)">
                查看调查
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无企业数据" />
          </template>
        </el-table>

        <div class="background-pagination">
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

      <background-investigation-dialog
        v-model="dialogVisible"
        :enterprise-name="currentEnterpriseName"
      />
    </div>
  </basic-container>
</template>

<script>
import { getCustomerList } from '@/api/business/customer';
import { getList as getParkList } from '@/api/park/park';
import BackgroundInvestigationDialog from '@/views/business/modules/background-investigation-dialog.vue';

const riskOptions = [
  { value: '0', label: '未排查', type: 'info' },
  { value: '1', label: '低风险', type: 'success' },
  { value: '2', label: '中风险', type: 'warning' },
  { value: '3', label: '高风险', type: 'danger' },
];

export default {
  name: 'SettlementBackgroundInvestigation',
  components: {
    BackgroundInvestigationDialog,
  },
  data() {
    return {
      loading: false,
      data: [],
      parkList: [],
      query: {},
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      dialogVisible: false,
      currentEnterpriseName: '',
      riskOptions,
    };
  },
  created() {
    this.loadParkList();
    this.onLoad();
  },
  methods: {
    onLoad() {
      this.loading = true;
      getCustomerList(this.page.currentPage, this.page.pageSize, this.query)
        .then(res => {
          const result = res && res.data ? res.data.data || {} : {};
          this.data = Array.isArray(result.records) ? result.records : [];
          this.page.total = Number(result.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadParkList() {
      getParkList(1, 999, {}).then(res => {
        const result = res && res.data ? res.data.data || {} : {};
        this.parkList = Array.isArray(result.records) ? result.records : [];
      });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad();
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.onLoad();
    },
    reload() {
      this.onLoad();
    },
    sizeChange(size) {
      this.page.pageSize = size;
      this.page.currentPage = 1;
      this.onLoad();
    },
    currentChange(current) {
      this.page.currentPage = current;
      this.onLoad();
    },
    rowIndex(index) {
      return (this.page.currentPage - 1) * this.page.pageSize + index + 1;
    },
    openInvestigation(row) {
      this.currentEnterpriseName = row.enterpriseName || '';
      this.dialogVisible = true;
    },
    parkName(parkId) {
      const park = this.parkList.find(item => String(item.id) === String(parkId));
      return park ? park.name : '-';
    },
    valueText(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    riskText(value) {
      const option = this.riskOptions.find(item => String(item.value) === String(value));
      return option ? option.label : '未排查';
    },
    riskType(value) {
      const option = this.riskOptions.find(item => String(item.value) === String(value));
      return option ? option.type : 'info';
    },
    verifyText(value) {
      const map = {
        0: '未核验',
        1: '通过',
        2: '不通过',
      };
      return map[String(value)] || '未核验';
    },
    verifyType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'danger',
      };
      return map[String(value)] || 'info';
    },
  },
};
</script>

<style lang="scss" scoped>
.background-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.background-search,
.background-list-panel {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.background-search {
  padding: 16px 18px 4px;
}

.background-search :deep(.el-form-item) {
  margin: 0 22px 12px 0;
}

.background-search :deep(.el-form-item__label) {
  height: 36px;
  line-height: 36px;
  color: #303133;
}

.background-search :deep(.el-input),
.background-search :deep(.el-select) {
  width: 210px;
}

.background-search :deep(.el-input__wrapper),
.background-search :deep(.el-select__wrapper) {
  min-height: 36px;
}

.background-list-panel {
  overflow: hidden;
  padding: 0 18px 18px;
}

.background-toolbar {
  display: flex;
  min-height: 58px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.background-toolbar__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.background-toolbar__title strong {
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.background-toolbar__title span {
  color: #909399;
  font-size: 13px;
}

.background-table {
  width: 100%;
}

.background-table :deep(.el-table__cell) {
  text-align: center;
}

.background-table :deep(.cell) {
  justify-content: center;
}

.background-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 900px) {
  .background-search :deep(.el-input),
  .background-search :deep(.el-select) {
    width: 180px;
  }
}
</style>
