<template>
  <basic-container>
    <div class="overdue-notice-page">
      <section class="notice-header">
        <div>
          <h2>我的消息</h2>
          <span>公司内部逾期处置提醒，统一记录 PC 和小程序消息</span>
        </div>
        <el-tag type="primary" effect="plain">内部通知</el-tag>
      </section>

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
        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="recordKey"
          scrollbar-always-on
          class="notice-table"
        >
          <el-table-column label="消息类型" width="110" align="center">
            <template #default><el-tag type="primary" effect="plain">内部提醒</el-tag></template>
          </el-table-column>
          <el-table-column prop="noticeTitle" label="消息标题" min-width="170" align="center" show-overflow-tooltip />
          <el-table-column
            prop="customerName"
            label="租客名称"
            :min-width="customerNameColumnWidth"
            align="center"
            class-name="notice-customer-column"
          >
            <template #default="{ row }">
              <el-button text type="primary" class="customer-link" @click="openNotice(row)">
                {{ row.customerName || '-' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column prop="contractNo" label="合同编号" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column label="房源信息" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.roomName || row.buildingName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="feeName" label="费用类型" width="100" align="center" />
          <el-table-column label="未缴金额" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="116" align="center" />
          <el-table-column prop="recipientRoles" label="接收职责" min-width="130" align="center" show-overflow-tooltip />
          <el-table-column prop="createBy" label="发送人" width="110" align="center" show-overflow-tooltip />
          <el-table-column label="消息端" width="112" align="center">PC / 小程序</el-table-column>
          <el-table-column prop="createTime" label="发送时间" width="168" align="center" />
          <el-table-column label="状态" width="86" align="center">
            <template #default="{ row }">
              <el-tag :type="recordStatusType(row)" effect="plain">
                {{ recordStatusText(row) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="156" align="center" fixed="right">
            <template #default="{ row }">
              <div class="notice-actions">
                <el-button v-if="row.readStatus !== '1'" text type="primary" @click="markRead(row)">标记已读</el-button>
                <el-button text type="primary" @click="openNotice(row)">查看通知</el-button>
              </div>
            </template>
          </el-table-column>
          <template #append>
            <div class="table-scroll-gutter" aria-hidden="true"></div>
          </template>
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

      <el-drawer v-model="noticeDetailVisible" title="内部处置提醒详情" size="520px" append-to-body>
        <div class="notice-detail">
          <section class="notice-detail__header">
            <div>
              <span>消息标题</span>
              <strong>{{ noticeDetail.noticeTitle || '-' }}</strong>
            </div>
            <el-tag :type="recordStatusType(noticeDetail)" effect="plain">
              {{ recordStatusText(noticeDetail) }}
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
            <span>发送时间</span>
            <strong>{{ noticeDetail.createTime || '-' }}</strong>
            <span>发送人 / 接收职责</span>
            <strong>{{ noticeDetail.createBy || '-' }} / {{ noticeDetail.recipientRoles || '-' }}</strong>
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
        recordType: 'notice',
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
      return [
        { key: 'total', label: '全部内部消息', value: this.summary.total },
        { key: 'unread', label: '未读消息', value: this.summary.unread },
        { key: 'read', label: '已读消息', value: this.summary.read },
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
      const base = this.cleanParams({ customerName: this.query.customerName, recordType: 'notice' });
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
        recordType: 'notice',
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
    recordStatusType(row) {
      return row && row.readStatus === '1' ? 'success' : 'warning';
    },
    recordStatusText(row) {
      return row && row.readStatus === '1' ? '已读' : '未读';
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

.notice-header,
.notice-summary,
.notice-search,
.notice-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.notice-header h2 {
  margin: 0 0 4px;
  color: #1f2937;
  font-size: 20px;
}

.notice-header span {
  color: #909399;
  font-size: 13px;
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

.notice-table :deep(.el-table__header th),
.notice-table :deep(.el-table__cell),
.notice-table :deep(.cell) {
  text-align: center;
}

.notice-table :deep(.notice-customer-column .cell) {
  padding: 0 12px;
  white-space: nowrap;
}

.notice-table :deep(.el-scrollbar__bar.is-horizontal) {
  right: 8px;
  bottom: 5px;
  left: 8px;
  height: 12px;
  opacity: 1;
  border-radius: 6px;
  background: #eef1f5;
}

.notice-table :deep(.el-scrollbar__bar.is-horizontal .el-scrollbar__thumb) {
  min-width: 96px;
  border-radius: 6px;
  background-color: #9aa4b2;
}

.table-scroll-gutter {
  height: 32px;
}

.customer-link {
  min-width: 0;
  max-width: none;
  padding: 0;
  overflow: visible;
  font-weight: 500;
  white-space: nowrap;
}

.customer-link :deep(span) {
  white-space: nowrap;
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
