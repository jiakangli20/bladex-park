<template>
  <el-dialog
    :model-value="modelValue"
    title="发送内部逾期处置提醒"
    width="820px"
    append-to-body
    @close="closeDialog"
    @closed="resetDialog"
  >
    <div class="recipient-dialog">
      <el-alert
        title="该提醒发送给公司内部人员，并同步记录到 PC“我的消息”和内部小程序。"
        type="info"
        :closable="false"
        show-icon
      />
      <div class="recipient-summary">
        <div>
          <span>租客名称</span>
          <strong>{{ tenantSummary }}</strong>
        </div>
        <div>
          <span>合同编号</span>
          <strong>{{ contractSummary }}</strong>
        </div>
        <el-input
          v-model="keyword"
          clearable
          prefix-icon="el-icon-search"
          placeholder="搜索姓名、账号、部门或角色"
        />
      </div>

      <el-table
        ref="recipientTable"
        v-loading="loading"
        :data="filteredCandidates"
        row-key="userId"
        border
        max-height="420"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          type="selection"
          width="44"
          align="center"
          reserve-selection
          :selectable="recipientSelectable"
        />
        <el-table-column prop="userName" label="用户姓名" min-width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="account" label="登录账号" min-width="110" align="center" show-overflow-tooltip />
        <el-table-column prop="deptName" label="所属部门" min-width="130" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deptName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="roleNames" label="所属角色" min-width="140" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.roleNames || '-' }}</template>
        </el-table-column>
        <el-table-column label="推荐职责" min-width="126" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.suggestedRoles" type="warning" effect="plain">{{ row.suggestedRoles }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="86" align="center">
          <template #default="{ row }">
            <el-tag :type="row.alreadySent ? 'success' : 'info'" effect="plain">
              {{ row.alreadySent ? '已发送' : '待发送' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <span class="recipient-selected-count">已选择 {{ selection.length }} 人</span>
      <el-button @click="closeDialog">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="selection.length === 0"
        @click="submit"
      >
        确认发送
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import {
  getOverdueNoticeRecipients,
  sendOverdueInternalNotice,
} from '@/api/ics/payment';

export default {
  name: 'OverdueRecipientDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    payment: {
      type: Object,
      default: () => ({}),
    },
    payments: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['update:modelValue', 'sent'],
  data() {
    return {
      loading: false,
      submitting: false,
      keyword: '',
      candidates: [],
      selection: [],
    };
  },
  computed: {
    targetPayments() {
      if (this.payments.length) return this.payments.filter(item => item && item.paymentId);
      return this.payment && this.payment.paymentId ? [this.payment] : [];
    },
    tenantSummary() {
      if (this.targetPayments.length > 1) return `已选 ${this.targetPayments.length} 条账单`;
      return (this.targetPayments[0] && this.targetPayments[0].customerName) || '-';
    },
    contractSummary() {
      if (this.targetPayments.length > 1) return '多个合同/账单';
      return (this.targetPayments[0] && this.targetPayments[0].contractNo) || '-';
    },
    filteredCandidates() {
      const keyword = (this.keyword || '').trim().toLowerCase();
      if (!keyword) return this.candidates;
      return this.candidates.filter(item =>
        [item.userName, item.account, item.deptName, item.roleNames, item.suggestedRoles]
          .some(value => String(value || '').toLowerCase().includes(keyword))
      );
    },
  },
  watch: {
    modelValue(value) {
      if (value) this.loadCandidates();
    },
  },
  methods: {
    loadCandidates() {
      if (!this.targetPayments.length) return;
      this.loading = true;
      this.keyword = '';
      this.candidates = [];
      this.selection = [];
      Promise.all(this.targetPayments.map(item => getOverdueNoticeRecipients(item.paymentId)))
        .then(results => {
          const candidateMap = new Map();
          results.forEach(res => {
            (res.data.data || []).forEach(item => {
              const current = candidateMap.get(item.userId);
              if (!current) {
                candidateMap.set(item.userId, {
                  ...item,
                  suggestedRoleSet: new Set(item.suggestedRoles ? item.suggestedRoles.split('、') : []),
                });
                return;
              }
              current.defaultSelected = current.defaultSelected || item.defaultSelected;
              current.alreadySent = current.alreadySent && item.alreadySent;
              if (item.suggestedRoles) {
                item.suggestedRoles.split('、').filter(Boolean).forEach(role => current.suggestedRoleSet.add(role));
              }
            });
          });
          this.candidates = Array.from(candidateMap.values()).map(item => ({
            ...item,
            suggestedRoles: Array.from(item.suggestedRoleSet).join('、'),
            suggestedRoleSet: undefined,
          }));
          this.$nextTick(() => {
            const table = this.$refs.recipientTable;
            if (!table) return;
            table.clearSelection();
            this.candidates
              .filter(item => item.defaultSelected && !item.alreadySent)
              .forEach(item => table.toggleRowSelection(item, true));
          });
        })
        .finally(() => {
          this.loading = false;
        });
    },
    recipientSelectable(row) {
      return !row.alreadySent;
    },
    handleSelectionChange(selection) {
      this.selection = selection || [];
    },
    submit() {
      if (!this.targetPayments.length || this.selection.length === 0) return;
      this.submitting = true;
      const recipientUserIds = this.selection.map(item => item.userId);
      Promise.all(this.targetPayments.map(payment =>
        sendOverdueInternalNotice({
          paymentId: payment.paymentId,
          recipientUserIds,
        }).then(res => ({
          payment,
          inserted: Number(res.data.data) || 0,
        }))
      ))
        .then(results => {
          const inserted = results.reduce((total, item) => total + item.inserted, 0);
          const sentPayments = results.filter(item => item.inserted > 0).map(item => item.payment);
          if (inserted > 0) {
            this.$message.success(`已向 ${inserted} 名内部人员发送处置提醒`);
            this.$emit('sent', {
              inserted,
              payment: { ...(sentPayments[0] || this.targetPayments[0]) },
              payments: sentPayments.map(item => ({ ...item })),
              recipients: this.selection.map(item => ({ ...item })),
            });
          } else {
            this.$message.warning('所选账号均已收到该账单的内部处置提醒');
          }
          this.closeDialog();
        })
        .finally(() => {
          this.submitting = false;
        });
    },
    closeDialog() {
      this.$emit('update:modelValue', false);
    },
    resetDialog() {
      this.keyword = '';
      this.candidates = [];
      this.selection = [];
    },
  },
};
</script>

<style lang="scss" scoped>
.recipient-dialog {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.recipient-summary {
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(150px, 1fr) 240px;
  align-items: end;
  gap: 12px;
}

.recipient-summary > div {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.recipient-summary span,
.recipient-summary strong {
  display: block;
}

.recipient-summary span {
  color: #909399;
  font-size: 12px;
}

.recipient-summary strong {
  margin-top: 4px;
  overflow: hidden;
  color: #1f2937;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipient-selected-count {
  float: left;
  color: #606266;
  font-size: 13px;
  line-height: 32px;
}

@media (max-width: 900px) {
  .recipient-summary {
    grid-template-columns: 1fr;
  }

  .recipient-summary :deep(.el-input) {
    width: 100%;
  }
}
</style>
