<template>
  <basic-container>
    <div class="contract-page">
      <el-alert
        v-if="expiringData.length"
        type="warning"
        show-icon
        :closable="false"
        :description="expiringSummary"
        class="contract-alert"
      >
        <template #title>
          合同到期提醒：当前有 {{ expiringData.length }} 份合同进入续签提醒周期
        </template>
      </el-alert>

      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="contract-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input v-model="query.contractNo" clearable placeholder="请输入合同编号" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="客户名称">
            <el-input v-model="query.customerName" clearable placeholder="请输入客户名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="合同状态">
            <el-select v-model="query.contractStatus" clearable placeholder="请选择">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="contract-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建合同</el-button>
          <el-button
            v-if="permission.contract_contract_delete"
            type="danger"
            plain
            icon="el-icon-delete"
            :disabled="selectionList.length === 0"
            @click="handleDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <el-table
        v-loading="loading"
        :data="data"
        border
        row-key="contractId"
        class="contract-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="customerName" label="租客名称" min-width="150" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">{{ row.customerName || '-' }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="contractNo" label="合同编号" width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="contractStatus" label="合同状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.contractStatus)" effect="plain">
              {{ row.contractStatusName || statusText(row.contractStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="parkName" label="园区名称" width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="roomName" label="房源信息" min-width="160" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">{{ row.roomName || '-' }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日" width="115" align="center" />
        <el-table-column prop="endDate" label="结束日" width="145" align="center">
          <template #default="{ row }">
            <span>{{ row.endDate || '-' }}</span>
            <el-tag v-if="isExpiringSoon(row)" type="warning" effect="plain" class="inline-tag">即将到期</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rentPrice" label="租赁单价" width="120" align="center">
          <template #default="{ row }">{{ formatUnitPrice(row.rentPrice) }}</template>
        </el-table-column>
        <el-table-column prop="signDate" label="签订日期" width="120" align="center">
          <template #default="{ row }">{{ row.signDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" @click="openDetail(row)">详情</el-button>
              <el-button text type="primary" @click="handlePayment(row)">账单</el-button>
              <el-button text type="primary" @click="handleArchive(row)">归档</el-button>
              <el-button
                v-if="permission.contract_contract_terminate && row.contractStatus !== '4'"
                text
                type="danger"
                @click="handleTerminate(row)"
              >
                终止
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="contract-pagination">
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

      <el-drawer v-model="detailVisible" :title="detailTitle" size="980px" append-to-body>
        <section class="detail-head">
          <div>
            <h3>{{ detailContract.contractName || '合同详情' }}</h3>
            <span>{{ detailContract.contractNo || '-' }}</span>
          </div>
          <div class="detail-actions">
            <el-button type="primary" plain @click="handleArchive(detailContract)">合同归档</el-button>
            <el-button
              v-if="permission.contract_contract_terminate && detailContract.contractStatus !== '4'"
              type="danger"
              @click="handleTerminate(detailContract)"
            >
              作废
            </el-button>
          </div>
        </section>

        <el-alert class="detail-alert" type="info" show-icon :closable="false" :title="contractSummary" />

        <el-tabs v-model="detailTab">
          <el-tab-pane label="合同信息" name="info">
            <section class="detail-section">
              <div class="detail-section-title">基本信息</div>
              <div class="contract-field-grid">
                <div v-for="item in basicDetailItems" :key="item.label" class="contract-field">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-title">房源信息</div>
              <div class="contract-field-grid">
                <div class="contract-field">
                  <span>房源信息</span>
                  <strong>{{ detailContract.roomName || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>面积</span>
                  <strong>{{ formatAreaValue(detailContract.rentArea) }}</strong>
                </div>
                <div class="contract-field">
                  <span>建筑名称</span>
                  <strong>{{ detailContract.buildingName || '-' }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-title">租客信息</div>
              <div class="contract-field-grid">
                <div class="contract-field">
                  <span>租客</span>
                  <strong>{{ detailContract.customerName || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>跟进人</span>
                  <strong>{{ detailContract.followUser || detailContract.createBy || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>备注</span>
                  <strong>{{ detailContract.remark || '-' }}</strong>
                </div>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="账单列表" name="payment">
            <el-table v-loading="paymentLoading" :data="paymentData" border>
              <el-table-column prop="feeName" label="费用类型" width="110" align="center" />
              <el-table-column label="费用期间" min-width="210" align="center">
                <template #default="{ row }">{{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}</template>
              </el-table-column>
              <el-table-column prop="amountDue" label="应缴金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
              </el-table-column>
              <el-table-column prop="amountPaid" label="已缴金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
              </el-table-column>
              <el-table-column prop="payDeadline" label="到期日" width="120" align="center" />
              <el-table-column prop="payStatus" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="paymentTagType(row)" effect="plain">{{ paymentStatusText(row.payStatus) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="130" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.payStatus !== '1'" text type="primary" @click="handleConfirmPayment(row)">确认缴费</el-button>
                  <el-button v-if="row.payStatus !== '1'" text type="primary" @click="handleRemind(row)">催缴</el-button>
                  <span v-else class="muted">已完成</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="操作日志" name="log">
            <el-timeline>
              <el-timeline-item
                v-for="item in logData"
                :key="item.logId"
                :timestamp="item.operateTime"
                placement="top"
              >
                <div class="contract-log">
                  <div class="contract-log__title">{{ item.actionDesc || item.action }}</div>
                  <div class="contract-log__operator">{{ item.operator }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="logData.length === 0" description="暂无操作记录" />
          </el-tab-pane>
        </el-tabs>
      </el-drawer>

      <el-drawer v-model="paymentBox" title="缴费计划" size="760px" append-to-body>
        <el-table v-loading="paymentLoading" :data="paymentData" border>
          <el-table-column prop="feeName" label="费用" width="110" />
          <el-table-column prop="periodStart" label="账期开始" width="110" />
          <el-table-column prop="periodEnd" label="账期结束" width="110" />
          <el-table-column prop="amountDue" label="应收" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="实收" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="paymentTagType(row)" effect="plain">{{ paymentStatusText(row.payStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.payStatus !== '1'" type="primary" text @click="handleConfirmPayment(row)">确认</el-button>
              <el-button v-if="row.payStatus !== '1'" type="primary" text @click="handleRemind(row)">催缴</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-drawer>
    </div>
  </basic-container>
</template>

<script>
import {
  confirmPayment,
  getDetail,
  getExpiring,
  getList,
  getLogs,
  getPayment,
  getStats,
  remindPayment,
  remove,
  terminate,
} from '@/api/contract/contract';
import { paymentStatusDic, statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';

export default {
  name: 'ContractList',
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        contractStatus: '',
      },
      loading: false,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      data: [],
      stats: {},
      expiringData: [],
      detailVisible: false,
      detailTab: 'info',
      detailContract: {},
      paymentBox: false,
      paymentLoading: false,
      paymentData: [],
      logData: [],
      routeDetailOpenedContractId: '',
    };
  },
  computed: {
    ...mapGetters(['permission']),
    statusOptions() {
      return statusDic;
    },
    summaryCards() {
      return [
        { key: 'total', label: '合同总数', value: this.stats.totalCount || 0 },
        { key: 'active', label: '履约中', value: this.stats.activeCount || 0 },
        { key: 'pending', label: '待签约', value: this.stats.pendingCount || 0 },
        { key: 'rent', label: '月租金合计', value: this.formatMoney(this.stats.monthlyRentTotal) },
      ];
    },
    ids() {
      return this.selectionList.map(item => item.contractId).join(',');
    },
    expiringSummary() {
      return this.expiringData
        .slice(0, 3)
        .map(item => `${item.customerName || item.contractName || '-'}：${item.endDate || '-'}`)
        .join('；');
    },
    detailTitle() {
      return this.detailContract.contractName || '合同详情';
    },
    contractSummary() {
      const contract = this.detailContract || {};
      return `合同摘要：起租日 ${contract.startDate || '--'}，租赁面积 ${this.formatAreaValue(contract.rentArea)}，租金单价 ${this.formatUnitPrice(contract.rentPrice)}，月租金 ${this.formatMoney(contract.monthlyRent)}。`;
    },
    basicDetailItems() {
      const contract = this.detailContract || {};
      return [
        { label: '合同编号', value: contract.contractNo || '-' },
        { label: '合同状态', value: contract.contractStatusName || this.statusText(contract.contractStatus) },
        { label: '所属园区', value: contract.parkName || '-' },
        { label: '签订日', value: contract.signDate || '-' },
        { label: '合同开始日', value: contract.startDate || '-' },
        { label: '合同结束日', value: contract.endDate || '-' },
        { label: '租赁面积', value: this.formatAreaValue(contract.rentArea) },
        { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
        { label: '租金单价', value: this.formatUnitPrice(contract.rentPrice) },
        { label: '物业费', value: this.formatUnitPrice(contract.propertyFee) },
        { label: '押金', value: this.formatMoney(contract.deposit) },
        { label: '滞纳金', value: this.lateFeeText(contract) },
        { label: '租金递增', value: contract.rentIncreaseNode || '-' },
        { label: '续签提醒', value: contract.renewalRemindDays ? `${contract.renewalRemindDays}天` : '-' },
      ];
    },
  },
  created() {
    this.applyRouteQuery();
    this.reload();
  },
  watch: {
    '$route.query'() {
      this.applyRouteQuery();
      this.reload();
    },
  },
  methods: {
    applyRouteQuery() {
      const routeQuery = this.$route.query || {};
      if (routeQuery.customerId) {
        this.query.customerId = routeQuery.customerId;
      }
      if (routeQuery.customerName) {
        this.query.customerName = routeQuery.customerName;
      }
      if (routeQuery.contractId) {
        this.query.contractId = routeQuery.contractId;
        this.routeDetailOpenedContractId = '';
      } else {
        delete this.query.contractId;
        this.routeDetailOpenedContractId = '';
      }
    },
    handleAdd() {
      this.$router.push({ path: '/contract/create-template', query: { mode: 'create' } });
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(this.ids))
        .then(() => {
          this.reload();
          this.$message.success('操作成功!');
        });
    },
    handleTerminate(row) {
      if (!row || !row.contractId) return;
      this.$confirm('确定终止该合同?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => terminate(row.contractId))
        .then(() => {
          this.reload();
          this.detailVisible = false;
          this.$message.success('操作成功!');
        });
    },
    openDetail(row) {
      if (!row || !row.contractId) return;
      this.detailVisible = true;
      this.detailTab = 'info';
      getDetail(row.contractId).then(res => {
        this.detailContract = res.data.data || {};
      });
      this.loadPayment(row.contractId);
      this.loadLogs(row.contractId);
    },
    handlePayment(row) {
      this.detailContract = row || {};
      this.paymentBox = true;
      this.loadPayment(row.contractId);
    },
    loadPayment(contractId) {
      if (!contractId) return;
      this.paymentLoading = true;
      getPayment(contractId)
        .then(res => {
          this.paymentData = res.data.data || [];
        })
        .finally(() => {
          this.paymentLoading = false;
        });
    },
    loadLogs(contractId) {
      if (!contractId) return;
      getLogs(contractId).then(res => {
        this.logData = res.data.data || [];
      });
    },
    handleConfirmPayment(row) {
      this.$prompt('请输入实收金额', '确认缴费', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: row.amountDue,
      })
        .then(({ value }) =>
          confirmPayment(row.paymentId, {
            amountPaid: value,
          })
        )
        .then(() => {
          this.loadPayment(this.currentPaymentContractId(row));
          this.$message.success('操作成功!');
        });
    },
    handleRemind(row) {
      remindPayment(row.paymentId).then(() => {
        this.$message.success('操作成功!');
      });
    },
    handleArchive(row) {
      if (!row || !row.contractId) return;
      this.$router.push({
        path: '/contract/archive',
        query: {
          contractId: row.contractId,
        },
      });
    },
    searchReset() {
      this.query = {
        contractNo: '',
        customerName: '',
        contractStatus: '',
      };
      this.page.currentPage = 1;
      this.reload();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    selectionChange(list) {
      this.selectionList = list;
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
    reload() {
      this.loadData();
      this.loadStats();
      this.loadExpiring();
    },
    loadData() {
      this.loading = true;
      getList(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          this.data = result.records || [];
          this.openRouteContractDetail();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadStats() {
      getStats({}).then(res => {
        this.stats = res.data.data || {};
      });
    },
    loadExpiring() {
      getExpiring(1, 20, {}).then(res => {
        const data = res.data.data || {};
        this.expiringData = data.records || [];
      });
    },
    openRouteContractDetail() {
      if (!this.query.contractId) return;
      if (String(this.routeDetailOpenedContractId) === String(this.query.contractId)) return;
      const target = this.data.find(item => String(item.contractId) === String(this.query.contractId));
      if (target) {
        this.routeDetailOpenedContractId = String(this.query.contractId);
        this.openDetail(target);
      }
    },
    currentPaymentContractId(row) {
      return row.contractId || this.detailContract.contractId || (this.paymentData[0] && this.paymentData[0].contractId);
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
    isExpiringSoon(row) {
      if (!row || !row.endDate) return false;
      const end = new Date(row.endDate).getTime();
      if (Number.isNaN(end)) return false;
      const days = Math.ceil((end - Date.now()) / 86400000);
      const remindDays = Number(row.renewalRemindDays || 30);
      return days >= 0 && days <= remindDays;
    },
    statusText(value) {
      const item = statusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '未知';
    },
    statusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'primary',
        4: 'danger',
      };
      return map[String(value)] || 'info';
    },
    paymentStatusText(value) {
      const item = paymentStatusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '未知';
    },
    paymentTagType(row) {
      if (row.payStatus === '1') return 'success';
      if (this.isPaymentOverdue(row)) return 'danger';
      return 'warning';
    },
    isPaymentOverdue(row) {
      if (!row.payDeadline || row.payStatus === '1') return false;
      return new Date(row.payDeadline).getTime() < Date.now();
    },
    lateFeeText(contract) {
      if (!contract || !contract.lateFeeRatio) return '-';
      const unitMap = {
        percent_day: '%/天',
        yuan_day: '元/天',
        percentPerDay: '%/日',
        percentPerMonth: '%/月',
        amountPerDay: '元/日',
      };
      return `${contract.lateFeeRatio}${unitMap[contract.lateFeeUnit] || contract.lateFeeUnit || ''}`;
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    formatUnitPrice(value) {
      if (value === null || value === undefined || value === '') return '-';
      return `${this.formatMoney(value)}元/㎡/月`;
    },
    formatAreaValue(value) {
      const number = Number(value || 0);
      return `${number.toLocaleString('zh-CN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      })}㎡`;
    },
  },
};
</script>

<style lang="scss" scoped>
.contract-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contract-alert,
.contract-search,
.contract-toolbar {
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

.contract-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
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

.inline-tag {
  margin-left: 6px;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.detail-head span {
  display: block;
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.detail-actions {
  display: flex;
  gap: 8px;
}

.detail-alert {
  margin-bottom: 12px;
}

.detail-section {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-weight: 600;
}

.contract-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.contract-field {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}

.contract-field span {
  flex: 0 0 104px;
  color: #8c98aa;
}

.contract-field strong {
  min-width: 0;
  color: #303133;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.contract-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.contract-log__title {
  color: #303133;
  font-weight: 600;
}

.contract-log__operator,
.muted {
  color: #909399;
  font-size: 12px;
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

.contract-page :deep(.el-button),
.contract-page :deep(.el-input__wrapper),
.contract-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .contract-field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
