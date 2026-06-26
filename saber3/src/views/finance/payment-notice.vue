<template>
  <basic-container>
    <div class="payment-notice-page">
      <section class="notice-title">
        <h2>收款通知</h2>
      </section>

      <section class="notice-summary">
        <div v-for="item in summaryCards" :key="item.key" class="notice-summary__item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="notice-search">
        <el-form :model="query" label-position="top">
          <div class="notice-search-grid">
            <el-form-item label="客商名称">
              <el-input v-model="query.customerName" clearable placeholder="请输入名称" @keyup.enter="searchChange" />
            </el-form-item>
            <el-form-item label="账单编号">
              <el-input v-model="query.paymentNo" clearable placeholder="请输入账单编号" @keyup.enter="searchChange" />
            </el-form-item>
            <el-form-item label="短信发送状态">
              <el-select v-model="query.smsStatus" clearable placeholder="请选择短信发送状态">
                <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="邮箱发送状态">
              <el-select v-model="query.emailStatus" clearable placeholder="请选择邮箱发送状态">
                <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="站内信发送状态">
              <el-select v-model="query.inboxStatus" clearable placeholder="请选择站内信发送状态">
                <el-option v-for="item in noticeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="创建时间">
              <el-date-picker
                v-model="query.createRange"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                range-separator="-"
                clearable
              />
            </el-form-item>
            <el-form-item label="楼宇名称">
              <el-select v-model="query.buildingName" clearable filterable placeholder="请选择楼宇名称">
                <el-option v-for="item in buildingOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <div class="notice-search-actions">
              <el-button icon="el-icon-refresh-left" @click="searchReset">重置</el-button>
              <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            </div>
          </div>
        </el-form>
      </section>

      <section class="notice-toolbar">
        <div></div>
        <el-button type="primary" plain icon="el-icon-printer" :disabled="selectionList.length === 0" @click="printSelected">
          打印
        </el-button>
      </section>

      <section class="notice-table-wrap">
        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="paymentId"
          class="notice-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="48" align="center" />
          <el-table-column prop="customerName" label="客商名称" min-width="140" align="center" show-overflow-tooltip />
          <el-table-column prop="buildingName" label="楼宇" width="120" align="center" show-overflow-tooltip />
          <el-table-column label="楼号/房号" min-width="160" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.roomName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="paymentNo" label="账单编号" min-width="160" align="center" show-overflow-tooltip />
          <el-table-column prop="smsStatus" label="短信发送状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="noticeStatusType(row.smsStatus)" effect="plain">{{ noticeStatusText(row.smsStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="emailStatus" label="邮箱发送状态" width="130" align="center">
            <template #default="{ row }">
              <el-tag :type="noticeStatusType(row.emailStatus)" effect="plain">{{ noticeStatusText(row.emailStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inboxStatus" label="站内信发送状态" width="140" align="center">
            <template #default="{ row }">
              <el-tag :type="noticeStatusType(row.inboxStatus)" effect="plain">{{ noticeStatusText(row.inboxStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="generatedDate" label="生成日期" width="120" align="center" />
          <el-table-column label="操作" width="180" align="center" fixed="right">
            <template #default="{ row }">
              <div class="notice-actions">
                <el-button text type="primary" @click="handleResend(row)">重新发送</el-button>
                <el-button text type="primary" @click="handleDownload(row)">下载</el-button>
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
    </div>
  </basic-container>
</template>

<script>
import {
  generatePaymentNotice,
  getPaymentNoticeBuildings,
  getPaymentNoticePage,
  getPaymentNoticeSummary,
  resendPaymentNotice,
} from '@/api/ics/payment';
import { downloadBlob } from '@/api/common';
import { downloadFile } from '@/utils/util';

export default {
  name: 'FinancePaymentNotice',
  data() {
    return {
      query: {},
      summary: {},
      buildingOptions: [],
      loading: false,
      data: [],
      selectionList: [],
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      noticeStatusOptions: [
        { label: '未发送', value: 'pending' },
        { label: '发送成功', value: 'success' },
        { label: '发送失败', value: 'failed' },
      ],
    };
  },
  computed: {
    summaryCards() {
      return [
        { key: 'generated', label: '已生成', value: this.summary.generatedCount || 0 },
        { key: 'sms', label: '短信发送成功', value: this.summary.smsSuccessCount || 0 },
        { key: 'email', label: '邮箱发送成功', value: this.summary.emailSuccessCount || 0 },
      ];
    },
  },
  mounted() {
    this.reload();
    this.loadBuildingOptions();
  },
  methods: {
    reload() {
      this.loadSummary();
      this.loadPage();
    },
    loadPage() {
      this.loading = true;
      getPaymentNoticePage(this.page.currentPage, this.page.pageSize, this.buildQueryParams())
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
      getPaymentNoticeSummary(this.buildQueryParams()).then(res => {
        this.summary = res.data.data || {};
      });
    },
    loadBuildingOptions() {
      getPaymentNoticeBuildings(this.buildQueryParams()).then(res => {
        this.buildingOptions = res.data.data || [];
      });
    },
    buildQueryParams() {
      const params = { ...this.query };
      if (Array.isArray(this.query.createRange) && this.query.createRange.length === 2) {
        params.createStartDate = this.query.createRange[0];
        params.createEndDate = this.query.createRange[1];
      }
      delete params.createRange;
      return params;
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
      this.loadBuildingOptions();
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.reload();
      this.loadBuildingOptions();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadPage();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadPage();
    },
    selectionChange(selection) {
      this.selectionList = selection;
    },
    handleResend(row) {
      this.$confirm('确定重新发送该收款通知吗？', '提示', {
        type: 'warning',
      })
        .then(() => resendPaymentNotice(row.paymentId))
        .then(() => {
          this.$message.success('收款通知已重新发送');
          this.reload();
        })
        .catch(() => {});
    },
    handleDownload(row) {
      const directUrl = `/blade-contract/print/payment-notice/${row.paymentId}`;
      const fallbackName = `${row.paymentNo || row.paymentId || '收款通知'}-付款通知单.docx`;
      downloadBlob(directUrl).then(res => {
        const disposition = res.headers && res.headers['content-disposition'];
        const filename = this.resolveDownloadFilename(disposition, fallbackName);
        const contentType = (res.headers && res.headers['content-type']) || 'application/octet-stream';
        downloadFile(res.data, filename, contentType);
        if (!row.fileUrl) {
          generatePaymentNotice(row.paymentId).then(() => this.reload());
        }
      });
    },
    printSelected() {
      if (this.selectionList.length === 0) return;
      this.selectionList.forEach(row => this.handleDownload(row));
    },
    resolveDownloadFilename(disposition, fallbackName) {
      if (!disposition) return fallbackName;
      const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
      if (utf8Match && utf8Match[1]) {
        return decodeURIComponent(utf8Match[1]);
      }
      const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
      return filenameMatch && filenameMatch[1] ? decodeURIComponent(filenameMatch[1]) : fallbackName;
    },
    noticeStatusText(value) {
      const item = this.noticeStatusOptions.find(option => option.value === value);
      return item ? item.label : '未发送';
    },
    noticeStatusType(value) {
      const map = {
        pending: 'info',
        success: 'success',
        failed: 'warning',
      };
      return map[value] || 'info';
    },
  },
};
</script>

<style lang="scss" scoped>
.payment-notice-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.notice-title,
.notice-summary,
.notice-search,
.notice-toolbar,
.notice-table-wrap {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.notice-title {
  display: flex;
  align-items: center;
  min-height: 72px;
  padding: 0 20px;
}

.notice-title h2 {
  margin: 0;
  color: #1f2329;
  font-size: 18px;
  font-weight: 600;
}

.notice-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  padding: 22px 18px;
}

.notice-summary__item {
  min-height: 52px;
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
  line-height: 18px;
}

.notice-summary__item strong {
  margin-top: 6px;
  color: #1f2329;
  font-size: 22px;
  line-height: 28px;
  font-weight: 500;
}

.notice-search {
  padding: 16px 18px 4px;
}

.notice-search-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(180px, 1fr));
  column-gap: 20px;
  align-items: end;
}

.notice-search :deep(.el-form-item) {
  margin-bottom: 12px;
}

.notice-search :deep(.el-input),
.notice-search :deep(.el-select),
.notice-search :deep(.el-date-editor) {
  width: 100%;
}

.notice-search-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.notice-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
}

.notice-table-wrap {
  padding: 0;
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

.notice-actions .el-button + .el-button {
  margin-left: 0;
}

.notice-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
  border-top: 1px solid #ebeef5;
}

.payment-notice-page :deep(.el-button),
.payment-notice-page :deep(.el-input__wrapper),
.payment-notice-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1200px) {
  .notice-search-grid {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}

@media (max-width: 760px) {
  .notice-summary,
  .notice-search-grid {
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
