<template>
  <basic-container>
    <div class="contract-summary">
      <div class="contract-summary__item">
        <span>合同总数</span>
        <strong>{{ stats.totalCount || 0 }}</strong>
      </div>
      <div class="contract-summary__item">
        <span>履约中</span>
        <strong>{{ stats.activeCount || 0 }}</strong>
      </div>
      <div class="contract-summary__item">
        <span>待签约</span>
        <strong>{{ stats.pendingCount || 0 }}</strong>
      </div>
      <div class="contract-summary__item">
        <span>月租金合计</span>
        <strong>{{ formatMoney(stats.monthlyRentTotal) }}</strong>
      </div>
    </div>
    <avue-crud
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      ref="crud"
      v-model="form"
      :permission="permissionList"
      :before-open="beforeOpen"
      @row-del="rowDel"
      @row-update="rowUpdate"
      @row-save="rowSave"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @on-load="onLoad"
    >
      <template #menu-left>
        <el-button
          type="danger"
          icon="el-icon-delete"
          plain
          v-if="permission.contract_contract_delete"
          @click="handleDelete"
          >删 除
        </el-button>
        <el-button
          type="warning"
          icon="el-icon-time"
          plain
          v-if="permission.contract_contract_view"
          @click="handleExpiring"
          >到期提醒
        </el-button>
      </template>
      <template #contractStatus="{ row }">
        <el-tag :type="statusType(row.contractStatus)">
          {{ row.contractStatusName || statusText(row.contractStatus) }}
        </el-tag>
      </template>
      <template #menu="{ row }">
        <el-button type="primary" text @click="handlePayment(row)">账单</el-button>
        <el-button type="primary" text @click="handleLogs(row)">日志</el-button>
        <el-button type="primary" text @click="handleArchive(row)">归档</el-button>
        <el-button
          type="primary"
          text
          v-if="permission.contract_contract_terminate && row.contractStatus !== '4'"
          @click="handleTerminate(row)"
          >终止
        </el-button>
      </template>
    </avue-crud>

    <el-drawer v-model="paymentBox" title="缴费计划" size="760px" append-to-body>
      <el-table v-loading="paymentLoading" :data="paymentData" border>
        <el-table-column prop="feeName" label="费用" width="110" />
        <el-table-column prop="periodStart" label="账期开始" width="110" />
        <el-table-column prop="periodEnd" label="账期结束" width="110" />
        <el-table-column prop="amountDue" label="应收" width="100" align="right" />
        <el-table-column prop="amountPaid" label="实收" width="100" align="right" />
        <el-table-column prop="payDeadline" label="应缴日期" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === '1' ? 'success' : 'warning'">
              {{ paymentStatusText(row.payStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              v-if="permission.contract_payment_confirm && row.payStatus !== '1'"
              @click="handleConfirmPayment(row)"
              >确认
            </el-button>
            <el-button
              type="primary"
              text
              v-if="permission.contract_payment_remind && row.payStatus !== '1'"
              @click="handleRemind(row)"
              >催缴
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-drawer v-model="logBox" title="操作日志" size="560px" append-to-body>
      <el-timeline>
        <el-timeline-item
          v-for="item in logData"
          :key="item.logId"
          :timestamp="item.operateTime"
          placement="top"
        >
          <div class="contract-log">
            <div class="contract-log__title">{{ item.action }}</div>
            <div class="contract-log__desc">{{ item.actionDesc }}</div>
            <div class="contract-log__operator">{{ item.operator }}</div>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="logData.length === 0" description="暂无日志" />
    </el-drawer>

    <el-drawer v-model="expiringBox" title="到期提醒" size="860px" append-to-body>
      <el-table v-loading="expiringLoading" :data="expiringData" border>
        <el-table-column prop="contractNo" label="合同编号" width="160" />
        <el-table-column prop="contractName" label="合同名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户" min-width="140" show-overflow-tooltip />
        <el-table-column prop="parkName" label="园区" width="130" show-overflow-tooltip />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="renewalRemindDays" label="提醒天数" width="90" align="right" />
      </el-table>
    </el-drawer>
  </basic-container>
</template>

<script>
import {
  add,
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
  update,
} from '@/api/contract/contract';
import { paymentStatusDic, statusDic, tableOption } from '@/option/contract/contract';
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      form: {},
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      option: tableOption,
      data: [],
      stats: {},
      currentContract: {},
      paymentBox: false,
      paymentLoading: false,
      paymentData: [],
      logBox: false,
      logData: [],
      expiringBox: false,
      expiringLoading: false,
      expiringData: [],
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.contract_contract_add, false),
        viewBtn: this.validData(this.permission.contract_contract_view, false),
        delBtn: this.validData(this.permission.contract_contract_delete, false),
        editBtn: this.validData(this.permission.contract_contract_edit, false),
      };
    },
    ids() {
      const ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.contractId);
      });
      return ids.join(',');
    },
  },
  methods: {
    rowSave(row, done, loading) {
      add(row).then(
        () => {
          this.reload();
          this.$message.success('操作成功!');
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowUpdate(row, index, done, loading) {
      update(row).then(
        () => {
          this.reload();
          this.$message.success('操作成功!');
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowDel(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(row.contractId))
        .then(() => {
          this.reload();
          this.$message.success('操作成功!');
        });
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
          this.selectionClear();
        });
    },
    handleTerminate(row) {
      this.$confirm('确定终止该合同?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => terminate(row.contractId))
        .then(() => {
          this.reload();
          this.$message.success('操作成功!');
        });
    },
    handlePayment(row) {
      this.currentContract = row;
      this.paymentBox = true;
      this.loadPayment(row.contractId);
    },
    loadPayment(contractId) {
      this.paymentLoading = true;
      getPayment(contractId).then(res => {
        this.paymentData = res.data.data || [];
        this.paymentLoading = false;
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
          this.loadPayment(this.currentContract.contractId);
          this.$message.success('操作成功!');
        });
    },
    handleRemind(row) {
      remindPayment(row.paymentId).then(() => {
        this.loadPayment(this.currentContract.contractId);
        this.$message.success('操作成功!');
      });
    },
    handleLogs(row) {
      this.logBox = true;
      getLogs(row.contractId).then(res => {
        this.logData = res.data.data || [];
      });
    },
    handleArchive(row) {
      this.$router.push({
        path: '/contract/archive',
        query: {
          contractId: row.contractId,
        },
      });
    },
    handleExpiring() {
      this.expiringBox = true;
      this.expiringLoading = true;
      getExpiring(1, 100, {}).then(res => {
        const data = res.data.data;
        this.expiringData = data.records || [];
        this.expiringLoading = false;
      });
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDetail(this.form.contractId).then(res => {
          this.form = res.data.data;
        });
      }
      done();
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
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.crud.toggleSelection();
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
      this.loadStats();
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, {
        ...params,
        ...this.query,
      }).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
        this.selectionClear();
      });
    },
    loadStats() {
      getStats({}).then(res => {
        this.stats = res.data.data || {};
      });
    },
    statusText(value) {
      const item = statusDic.find(ele => ele.value === value);
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
      return map[value] || 'info';
    },
    paymentStatusText(value) {
      const item = paymentStatusDic.find(ele => ele.value === value);
      return item ? item.label : '未知';
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.contract-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.contract-summary__item {
  min-height: 72px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.contract-summary__item span {
  color: #606266;
  font-size: 13px;
  line-height: 18px;
}

.contract-summary__item strong {
  color: #303133;
  font-size: 22px;
  line-height: 30px;
  margin-top: 4px;
  font-weight: 600;
}

.contract-log__title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.contract-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.contract-log__desc {
  color: #606266;
  line-height: 20px;
}

.contract-log__operator {
  color: #909399;
  font-size: 12px;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .contract-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
