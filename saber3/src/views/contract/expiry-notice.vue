<template>
  <basic-container>
    <div class="expiry-notice-page">
      <section class="expiry-summary">
        <div v-for="item in summaryCards" :key="item.key" class="expiry-summary__item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="expiry-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input v-model="query.contractNo" clearable placeholder="请输入合同编号" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="租客名称">
            <el-input v-model="query.customerName" clearable placeholder="请输入租客名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="楼宇名称">
            <el-input v-model="query.buildingName" clearable placeholder="请输入楼宇名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="expiry-toolbar">
        <strong>合同到期提醒</strong>
        <div class="expiry-toolbar__actions">
          <el-button type="primary" plain icon="el-icon-setting" @click="openSettings">到期设置</el-button>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </div>
      </section>

      <section class="expiry-table-wrap">
        <el-table v-loading="loading" :data="data" border row-key="contractId" class="expiry-table">
          <el-table-column prop="contractNo" label="合同编号" min-width="170" align="center" show-overflow-tooltip />
          <el-table-column prop="customerName" label="租客名称" :min-width="customerNameColumnWidth" align="center" />
          <el-table-column prop="buildingName" label="楼宇名称" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="roomName" label="房源信息" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="endDate" label="合同到期日" width="120" align="center" />
          <el-table-column label="剩余天数" width="112" align="center">
            <template #default="{ row }">
              <el-tag :type="expiryTagType(row)" effect="plain">{{ remainingDaysText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提醒规则" width="126" align="center">
            <template #default="{ row }">提前 {{ row.renewalRemindDays || 0 }} 天</template>
          </el-table-column>
          <el-table-column prop="followUser" label="跟进人" width="120" align="center">
            <template #default="{ row }">{{ row.followUser || '-' }}</template>
          </el-table-column>
          <el-table-column label="合同状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="success" effect="plain">{{ row.contractStatusName || '生效' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="166" align="center" fixed="right">
            <template #default="{ row }">
              <div class="expiry-actions">
                <el-button text type="primary" @click="openNotice(row)">查看提醒</el-button>
                <el-button text type="primary" @click="openContract(row)">查看合同</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="expiry-pagination">
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
      </section>

      <el-drawer v-model="detailVisible" title="合同到期提醒详情" size="560px" append-to-body>
        <div class="expiry-detail">
          <section class="expiry-detail__header">
            <div>
              <span>租客名称</span>
              <strong>{{ detailRow.customerName || '-' }}</strong>
              <em>{{ detailRow.contractNo || '-' }}</em>
            </div>
            <el-tag :type="expiryTagType(detailRow)" effect="plain">{{ remainingDaysText(detailRow) }}</el-tag>
          </section>
          <section class="expiry-detail__notice">
            合同将于 {{ detailRow.endDate || '-' }} 到期，请及时联系租客确认续租或退租安排。
          </section>
          <section class="expiry-detail__grid">
            <div v-for="item in detailItems" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>
        </div>
        <template #footer>
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="openContract(detailRow)">查看合同</el-button>
        </template>
      </el-drawer>
    </div>
  </basic-container>
</template>

<script>
import { getExpiring, getExpiringSummary } from '@/api/contract/contract';

export default {
  name: 'ContractExpiryNotice',
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        buildingName: '',
      },
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      summary: {
        totalCount: 0,
        within30Days: 0,
        within7Days: 0,
      },
      data: [],
      loading: false,
      detailVisible: false,
      detailRow: {},
    };
  },
  computed: {
    summaryCards() {
      return [
        { key: 'total', label: '进入提醒周期', value: this.summary.totalCount || 0 },
        { key: 'within30', label: '30 天内到期', value: this.summary.within30Days || 0 },
        { key: 'within7', label: '7 天内到期', value: this.summary.within7Days || 0 },
      ];
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
    detailItems() {
      const row = this.detailRow || {};
      return [
        { label: '合同名称', value: row.contractName || '-' },
        { label: '园区名称', value: row.parkName || '-' },
        { label: '楼宇名称', value: row.buildingName || '-' },
        { label: '房源信息', value: row.roomName || '-' },
        { label: '合同开始日', value: row.startDate || '-' },
        { label: '合同到期日', value: row.endDate || '-' },
        { label: '提醒规则', value: `提前 ${row.renewalRemindDays || 0} 天` },
        { label: '跟进人', value: row.followUser || '-' },
      ];
    },
  },
  created() {
    this.reload();
  },
  methods: {
    reload() {
      this.loadData();
      this.loadSummary();
    },
    loadData() {
      this.loading = true;
      getExpiring(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = result.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadSummary() {
      getExpiringSummary(this.cleanParams(this.query)).then(res => {
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
        buildingName: '',
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
    openNotice(row) {
      this.detailRow = { ...(row || {}) };
      this.detailVisible = true;
    },
    openContract(row) {
      if (!row || !row.contractId) return;
      this.detailVisible = false;
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
        },
      });
    },
    openSettings() {
      this.$router.push('/contract/expiring');
    },
    remainingDays(row) {
      if (!row || !row.endDate) return null;
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const endDate = new Date(`${row.endDate}T00:00:00`);
      return Math.max(Math.ceil((endDate.getTime() - today.getTime()) / 86400000), 0);
    },
    remainingDaysText(row) {
      const days = this.remainingDays(row);
      if (days === null) return '-';
      return days === 0 ? '今天到期' : `${days}天`;
    },
    expiryTagType(row) {
      const days = this.remainingDays(row);
      if (days === null) return 'info';
      if (days <= 7) return 'danger';
      if (days <= 30) return 'warning';
      return 'primary';
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
.expiry-notice-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.expiry-summary,
.expiry-search,
.expiry-toolbar,
.expiry-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.expiry-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 20px 18px;
}

.expiry-summary__item {
  min-height: 58px;
  border-right: 1px solid #ebeef5;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.expiry-summary__item:last-child {
  border-right: 0;
}

.expiry-summary__item span {
  color: #909399;
  font-size: 13px;
}

.expiry-summary__item strong {
  margin-top: 6px;
  color: #1f2937;
  font-size: 24px;
  font-weight: 600;
}

.expiry-search {
  padding: 16px 18px 4px;
}

.expiry-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.expiry-search :deep(.el-input) {
  width: 200px;
}

.expiry-toolbar {
  min-height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.expiry-toolbar > strong {
  color: #1f2937;
  font-size: 15px;
}

.expiry-toolbar__actions,
.expiry-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.expiry-toolbar__actions :deep(.el-button),
.expiry-actions :deep(.el-button) {
  margin-left: 0;
}

.expiry-table-wrap {
  overflow: hidden;
}

.expiry-table {
  width: 100%;
}

.expiry-pagination {
  padding: 14px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
}

.expiry-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.expiry-detail__header,
.expiry-detail__notice,
.expiry-detail__grid > div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.expiry-detail__header {
  min-height: 92px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.expiry-detail__header div {
  min-width: 0;
}

.expiry-detail__header span,
.expiry-detail__header strong,
.expiry-detail__header em,
.expiry-detail__grid span,
.expiry-detail__grid strong {
  display: block;
}

.expiry-detail__header span,
.expiry-detail__grid span {
  color: #909399;
  font-size: 12px;
}

.expiry-detail__header strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 18px;
  line-height: 24px;
}

.expiry-detail__header em {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.expiry-detail__notice {
  padding: 16px;
  color: #303133;
  font-size: 14px;
  line-height: 24px;
}

.expiry-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.expiry-detail__grid > div {
  min-height: 68px;
  padding: 12px;
  background: #fafafa;
}

.expiry-detail__grid strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 14px;
  line-height: 20px;
  word-break: break-word;
}

@media (max-width: 900px) {
  .expiry-summary {
    grid-template-columns: 1fr;
  }

  .expiry-summary__item {
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
  }

  .expiry-summary__item:last-child {
    border-bottom: 0;
  }
}
</style>
