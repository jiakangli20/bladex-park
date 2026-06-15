<template>
  <basic-container>
    <avue-crud
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      ref="crud"
      :permission="permissionList"
      @search-change="searchChange"
      @search-reset="searchReset"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @on-load="onLoad"
    >
      <template #contractName="{ row }">
        <el-link type="primary" :underline="false" @click="openArchive(row)">
          {{ row.contractName || row.contractNo || '-' }}
        </el-link>
      </template>
      <template #contractStatus="{ row }">
        <el-tag :type="statusType(row.contractStatus)">
          {{ row.contractStatusName || statusText(row.contractStatus) }}
        </el-tag>
      </template>
      <template #menu="{ row }">
        <el-button
          type="primary"
          text
          v-if="permission.contract_archive_detail"
          @click="openArchive(row)"
          >查看档案
        </el-button>
        <el-button
          type="primary"
          text
          v-if="permission.contract_archive_export_approval"
          @click="handleExportApproval(row)"
          >导出审批表
        </el-button>
        <el-button type="primary" text @click="backToContract(row)">返回合同</el-button>
      </template>
    </avue-crud>

    <el-drawer v-model="drawerVisible" title="合同档案详情" size="920px" append-to-body>
      <el-skeleton :loading="detailLoading" animated>
        <template #template>
          <el-skeleton-item variant="p" style="width: 90%" />
          <el-skeleton-item variant="p" style="width: 70%" />
          <el-skeleton-item variant="p" style="width: 80%" />
        </template>
        <template #default>
          <el-steps :active="archiveStep" finish-status="success" simple>
            <el-step title="合同签订" />
            <el-step title="租金收缴" />
            <el-step title="变更/退租" />
            <el-step title="档案归档" />
          </el-steps>

          <el-tabs v-model="activeTab" class="archive-tabs">
            <el-tab-pane label="合同档案" name="contract">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="合同编号">{{
                  current.contractNo || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="合同名称">{{
                  current.contractName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="客户名称">{{
                  current.customerName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="所属园区">{{
                  current.parkName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="房源">{{
                  current.roomName || current.buildingName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="租期">
                  {{ current.startDate || '-' }} 至 {{ current.endDate || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="租赁面积">
                  {{ formatArea(current.rentArea) }}
                </el-descriptions-item>
                <el-descriptions-item label="月租金">
                  {{ formatMoney(current.monthlyRent) }}
                </el-descriptions-item>
                <el-descriptions-item label="押金">
                  {{ formatMoney(current.deposit) }}
                </el-descriptions-item>
                <el-descriptions-item label="合同状态">
                  <el-tag :type="statusType(current.contractStatus)">
                    {{ current.contractStatusName || statusText(current.contractStatus) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{
                  current.remark || '-'
                }}</el-descriptions-item>
              </el-descriptions>
              <div class="archive-actions">
                <el-button
                  type="primary"
                  v-if="permission.contract_archive_export_approval"
                  @click="handleExportApproval(current)"
                  >导出合同审批表
                </el-button>
                <el-button
                  v-if="permission.contract_archive_print"
                  @click="handlePrint(current)"
                  >预览打印合同
                </el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="缴费记录" name="payment">
              <el-table :data="payments" border>
                <el-table-column prop="feeName" label="费用名称" min-width="120" />
                <el-table-column label="账期" min-width="190">
                  <template #default="{ row }">
                    {{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="amountDue" label="应收" width="110" align="right" />
                <el-table-column prop="amountPaid" label="实收" width="110" align="right" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.payStatus === '1' ? 'success' : 'warning'">
                      {{ dicText(paymentStatusDic, row.payStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="payments.length === 0" description="暂无缴费记录" />
            </el-tab-pane>

            <el-tab-pane label="变更审批" name="change">
              <el-table :data="changes" border>
                <el-table-column prop="changeNo" label="变更单号" min-width="150" />
                <el-table-column label="变更类型" width="120">
                  <template #default="{ row }">
                    {{ dicText(changeTypeDic, row.changeType) }}
                  </template>
                </el-table-column>
                <el-table-column label="审批状态" width="120">
                  <template #default="{ row }">
                    {{ dicText(changeApprovalStatusDic, row.approvalStatus) }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="approvalOpinion"
                  label="审批意见"
                  min-width="180"
                  show-overflow-tooltip
                />
              </el-table>
              <el-empty v-if="changes.length === 0" description="暂无变更审批" />
            </el-tab-pane>

            <el-tab-pane label="退租记录" name="termination">
              <el-table :data="terminations" border>
                <el-table-column prop="terminationNo" label="退租单号" min-width="150" />
                <el-table-column prop="terminationTime" label="退租时间" width="170" />
                <el-table-column label="审批状态" width="120">
                  <template #default="{ row }">
                    {{ dicText(terminationApprovalStatusDic, row.approvalStatus) }}
                  </template>
                </el-table-column>
                <el-table-column label="结算状态" width="120">
                  <template #default="{ row }">
                    {{ dicText(terminationStatusDic, row.status) }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="approvalOpinion"
                  label="审批意见"
                  min-width="180"
                  show-overflow-tooltip
                />
              </el-table>
              <el-empty v-if="terminations.length === 0" description="暂无退租记录" />
            </el-tab-pane>

            <el-tab-pane label="操作日志" name="log">
              <el-timeline>
                <el-timeline-item
                  v-for="item in logs"
                  :key="item.logId"
                  :timestamp="item.operateTime"
                  placement="top"
                >
                  <div class="archive-log">
                    <div class="archive-log__title">{{ item.actionDesc || item.action }}</div>
                    <div class="archive-log__operator">{{ item.operator || '-' }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="logs.length === 0" description="暂无日志" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-skeleton>
    </el-drawer>

    <el-dialog v-model="printVisible" :title="printTitle" width="820px" append-to-body>
      <div class="archive-print" v-html="printHtml"></div>
      <template #footer>
        <el-button @click="printVisible = false">关闭</el-button>
        <el-button type="primary" @click="doBrowserPrint">打印</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import {
  exportApproval,
  getArchiveDetail,
  getArchiveList,
  printContract,
} from '@/api/contract/archive';
import {
  changeApprovalStatusDic,
  changeTypeDic,
  paymentStatusDic,
  tableOption,
  terminationApprovalStatusDic,
  terminationStatusDic,
} from '@/option/contract/archive';
import { statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      option: tableOption,
      data: [],
      drawerVisible: false,
      detailLoading: false,
      activeTab: 'contract',
      current: {},
      payments: [],
      changes: [],
      terminations: [],
      logs: [],
      archiveStep: 0,
      printVisible: false,
      printTitle: '合同打印',
      printHtml: '',
      paymentStatusDic,
      changeTypeDic,
      changeApprovalStatusDic,
      terminationApprovalStatusDic,
      terminationStatusDic,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: false,
        viewBtn: this.validData(this.permission.contract_archive_detail, false),
        delBtn: false,
        editBtn: false,
      };
    },
  },
  created() {
    if (this.$route.query.contractId) {
      this.query.contractId = this.$route.query.contractId;
    }
  },
  methods: {
    openArchive(row) {
      const contractId = row.contractId;
      this.drawerVisible = true;
      this.detailLoading = true;
      this.activeTab = 'contract';
      getArchiveDetail(contractId)
        .then(res => {
          const data = res.data.data || {};
          this.current = data.contract || row;
          this.payments = data.payments || [];
          this.changes = data.changes || [];
          this.terminations = data.terminations || [];
          this.logs = data.logs || [];
          this.archiveStep = data.archiveStep || 0;
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    handleExportApproval(row) {
      exportApproval(row.contractId).then(res => {
        const data = res.data.data || {};
        if (!data.html) {
          this.$message.warning('暂无可导出的审批表内容');
          return;
        }
        const blob = new Blob([data.html], { type: data.contentType || 'text/html;charset=utf-8' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = data.fileName || `${row.contractNo || 'contract'}-approval.html`;
        link.click();
        URL.revokeObjectURL(link.href);
      });
    },
    handlePrint(row) {
      printContract(row.contractId).then(res => {
        const data = res.data.data || {};
        this.printTitle = `合同打印 - ${row.contractNo || ''}`;
        this.printHtml = data.html || '';
        this.printVisible = true;
      });
    },
    doBrowserPrint() {
      const win = window.open('', '_blank');
      if (!win) return;
      win.document.write(
        `<!doctype html><html><head><meta charset="utf-8"><title>${this.printTitle}</title></head><body>${this.printHtml}</body></html>`
      );
      win.document.close();
      win.print();
    },
    backToContract(row) {
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
          customerName: row.customerName,
        },
      });
    },
    searchReset() {
      this.query = {};
      this.reload();
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.reload();
    },
    reload() {
      this.onLoad(this.page, this.query);
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getArchiveList(page.currentPage, page.pageSize, {
        ...params,
        ...this.query,
      })
        .then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    statusText(value) {
      return this.dicText(statusDic, value);
    },
    statusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'primary',
        4: 'danger',
      };
      return map[value] || 'info';
    },
    dicText(dic, value) {
      const item = dic.find(ele => `${ele.value}` === `${value}`);
      return item ? item.label : '-';
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    formatArea(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      return `${value} ㎡`;
    },
  },
};
</script>

<style lang="scss" scoped>
.archive-tabs {
  margin-top: 16px;
}

.archive-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.archive-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.archive-log__title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.archive-log__operator {
  color: #909399;
  font-size: 12px;
}

.archive-print {
  max-height: 60vh;
  overflow: auto;
  border: 1px solid #ebeef5;
}
</style>
