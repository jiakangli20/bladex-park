<template>
  <basic-container>
    <div class="overdue-notice-page">
      <section class="notice-summary">
        <div v-for="item in summaryCards" :key="item.key" class="notice-summary__item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="notice-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="租客名称">
            <el-input v-model="query.customerName" clearable placeholder="请输入租客名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="阅读状态">
            <el-select v-model="query.readStatus" clearable placeholder="全部状态">
              <el-option label="未读" value="0" />
              <el-option label="已读" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="notice-table-wrap">
        <el-table v-loading="loading" :data="data" border row-key="noticeId" class="notice-table">
          <el-table-column prop="noticeTitle" label="通知标题" min-width="170" align="center" show-overflow-tooltip />
          <el-table-column prop="customerName" label="租客名称" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="contractNo" label="合同编号" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column label="房源信息" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.roomName || row.buildingName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" width="110" align="center" />
          <el-table-column label="未缴金额" width="120" align="center">
            <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="116" align="center" />
          <el-table-column prop="recipientRoles" label="通知职责" min-width="120" align="center" show-overflow-tooltip />
          <el-table-column prop="createTime" label="通知时间" width="168" align="center" />
          <el-table-column label="状态" width="86" align="center">
            <template #default="{ row }">
              <el-tag :type="row.readStatus === '1' ? 'success' : 'warning'" effect="plain">
                {{ row.readStatus === '1' ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="226" align="center" fixed="right">
            <template #default="{ row }">
              <div class="notice-actions">
                <el-button v-if="row.readStatus !== '1'" text type="primary" @click="markRead(row)">标记已读</el-button>
                <el-button text type="primary" @click="openNotice(row)">查看通知</el-button>
                <el-button v-if="permissionList.disposeBtn" text type="primary" @click="openDisposal(row)">进入处置</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="notice-pagination">
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

      <el-drawer v-model="noticeDetailVisible" title="逾期通知详情" size="520px" append-to-body>
        <div class="notice-detail">
          <section class="notice-detail__header">
            <div>
              <span>通知标题</span>
              <strong>{{ noticeDetail.noticeTitle || '-' }}</strong>
            </div>
            <el-tag :type="noticeDetail.readStatus === '1' ? 'success' : 'warning'" effect="plain">
              {{ noticeDetail.readStatus === '1' ? '已读' : '未读' }}
            </el-tag>
          </section>
          <section class="notice-detail__content">
            {{ noticeDetail.noticeContent || '-' }}
          </section>
          <section class="notice-detail__grid">
            <div v-for="item in detailItems" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>
          <section class="notice-detail__meta">
            <span>通知时间</span>
            <strong>{{ noticeDetail.createTime || '-' }}</strong>
            <span>通知职责</span>
            <strong>{{ noticeDetail.recipientRoles || '-' }}</strong>
          </section>
        </div>
        <template #footer>
          <el-button @click="noticeDetailVisible = false">关闭</el-button>
          <el-button v-if="permissionList.disposeBtn" type="primary" @click="openDisposal(noticeDetail)">进入逾期处置</el-button>
        </template>
      </el-drawer>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import {
  getOverdueInternalNoticePage,
  readOverdueInternalNotice,
} from '@/api/ics/payment';

export default {
  name: 'FinanceOverdueNotice',
  data() {
    return {
      query: {
        customerName: '',
        readStatus: '',
      },
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      loading: false,
      data: [],
      summary: {
        total: 0,
        unread: 0,
        read: 0,
      },
      noticeDetailVisible: false,
      noticeDetail: {},
    };
  },
  computed: {
    ...mapGetters(['menuAll']),
    permissionList() {
      const hasMenuCode = list => (list || []).some(item =>
        item.code === 'finance_overdue_reminder' || hasMenuCode(item.children)
      );
      return {
        disposeBtn: hasMenuCode(this.menuAll),
      };
    },
    summaryCards() {
      return [
        { key: 'total', label: '我的通知', value: this.summary.total },
        { key: 'unread', label: '未读通知', value: this.summary.unread },
        { key: 'read', label: '已读通知', value: this.summary.read },
      ];
    },
    detailItems() {
      const row = this.noticeDetail || {};
      return [
        { label: '租客名称', value: row.customerName || '-' },
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '房源信息', value: row.roomName || row.buildingName || '-' },
        { label: '费用类型', value: row.feeName || '-' },
        { label: '未缴金额', value: this.formatMoney(this.unpaidAmount(row)) },
        { label: '应缴日期', value: row.payDeadline || '-' },
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
      getOverdueInternalNoticePage(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
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
      const base = this.cleanParams({ customerName: this.query.customerName });
      Promise.all([
        getOverdueInternalNoticePage(1, 1, base),
        getOverdueInternalNoticePage(1, 1, { ...base, readStatus: '0' }),
        getOverdueInternalNoticePage(1, 1, { ...base, readStatus: '1' }),
      ]).then(([totalRes, unreadRes, readRes]) => {
        this.summary = {
          total: Number((totalRes.data.data || {}).total) || 0,
          unread: Number((unreadRes.data.data || {}).total) || 0,
          read: Number((readRes.data.data || {}).total) || 0,
        };
      });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {
        customerName: '',
        readStatus: '',
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
    markRead(row) {
      if (!row || !row.paymentId) return Promise.resolve();
      return readOverdueInternalNotice(row.paymentId).then(() => {
        this.$message.success('已标记为已读');
        this.reload();
      });
    },
    openNotice(row) {
      if (!row) return;
      this.noticeDetail = { ...row };
      this.noticeDetailVisible = true;
      if (row.readStatus === '1' || !row.paymentId) return;
      readOverdueInternalNotice(row.paymentId).then(() => {
        this.noticeDetail.readStatus = '1';
        this.reload();
      });
    },
    openDisposal(row) {
      if (!this.permissionList.disposeBtn || !row || !row.paymentId) return;
      const navigate = () => {
        this.$router.push({
          path: '/finance/overdue-reminder',
          query: {
            paymentId: row.paymentId,
            customerName: row.customerName || '',
          },
        });
      };
      if (row.readStatus === '1') {
        navigate();
        return;
      }
      readOverdueInternalNotice(row.paymentId).finally(navigate);
    },
    unpaidAmount(row) {
      return Math.max(Number(row.amountDue || 0) - Number(row.amountPaid || 0), 0);
    },
    formatMoney(value) {
      return `¥${Number(value || 0).toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      })}`;
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
.overdue-notice-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-summary,
.notice-search,
.notice-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.notice-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 20px 18px;
}

.notice-summary__item {
  min-height: 58px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1px solid #ebeef5;
}

.notice-summary__item:last-child {
  border-right: 0;
}

.notice-summary__item span {
  color: #909399;
  font-size: 13px;
}

.notice-summary__item strong {
  margin-top: 6px;
  color: #1f2937;
  font-size: 24px;
  font-weight: 600;
}

.notice-search {
  padding: 16px 18px 4px;
}

.notice-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.notice-search :deep(.el-input),
.notice-search :deep(.el-select) {
  width: 220px;
}

.notice-table-wrap {
  overflow: hidden;
}

.notice-table {
  width: 100%;
}

.notice-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.notice-actions :deep(.el-button) {
  margin-left: 0;
  padding: 0 2px;
}

.notice-pagination {
  padding: 14px 16px;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: flex-end;
}

.notice-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notice-detail__header,
.notice-detail__content,
.notice-detail__grid > div,
.notice-detail__meta {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.notice-detail__header {
  min-height: 82px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.notice-detail__header div {
  min-width: 0;
}

.notice-detail span,
.notice-detail strong {
  display: block;
}

.notice-detail span {
  color: #909399;
  font-size: 12px;
}

.notice-detail strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 14px;
  line-height: 20px;
  word-break: break-word;
}

.notice-detail__header strong {
  font-size: 17px;
  line-height: 24px;
}

.notice-detail__content {
  padding: 16px;
  color: #303133;
  font-size: 14px;
  line-height: 24px;
}

.notice-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.notice-detail__grid > div {
  min-height: 68px;
  padding: 12px;
  background: #fafafa;
}

.notice-detail__meta {
  padding: 14px 16px;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  align-items: center;
  gap: 8px 12px;
}

.notice-detail__meta strong {
  margin-top: 0;
}

@media (max-width: 900px) {
  .notice-summary {
    grid-template-columns: 1fr;
  }

  .notice-summary__item {
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
  }

  .notice-summary__item:last-child {
    border-bottom: 0;
  }
}
</style>
