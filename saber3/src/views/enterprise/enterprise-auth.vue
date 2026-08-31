<template>
  <basic-container>
    <div class="auth-page">
      <business-page-intro
        title="企业认证审核"
        subtitle="处理小程序游客提交的企业认证及园区权限申请"
      />

      <div class="toolbar">
        <el-select v-model="status" class="status-select" @change="load">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>

      <el-table :data="rows" border v-loading="loading" empty-text="暂无认证申请">
        <el-table-column prop="applicationType" label="申请事项" width="110" align="center">
          <template #default="{ row }">{{ applicationTypeText(row.applicationType) }}</template>
        </el-table-column>
        <el-table-column prop="subjectType" label="认证类型" width="100" align="center">
          <template #default="{ row }">{{ subjectTypeText(row.subjectType) }}</template>
        </el-table-column>
        <el-table-column
          prop="enterpriseName"
          label="企业名称"
          min-width="200"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column label="申请园区" min-width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ parkNamesText(row.parkNames) }}</template>
        </el-table-column>
        <el-table-column
          prop="creditCode"
          label="统一社会信用代码"
          min-width="190"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">{{ valueText(row.creditCode) }}</template>
        </el-table-column>
        <el-table-column prop="legalRepresentative" label="法定代表人" width="120" align="center">
          <template #default="{ row }">{{ valueText(row.legalRepresentative) }}</template>
        </el-table-column>
        <el-table-column prop="registeredCapital" label="注册资本（万）" width="140" align="center">
          <template #default="{ row }">{{ valueText(row.registeredCapital) }}</template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="120" align="center" />
        <el-table-column prop="contactPhone" label="认证手机号" width="150" align="center" />
        <el-table-column
          prop="contactEmail"
          label="联系邮箱"
          min-width="180"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="156" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions" v-if="row.status === 'PENDING'">
              <el-button type="primary" text @click="review(row, 'APPROVE')">通过</el-button>
              <el-button type="danger" text @click="review(row, 'REJECT')">驳回</el-button>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </basic-container>
</template>

<script>
import {
  getEnterpriseCertificationList,
  reviewEnterpriseCertification,
} from '@/api/business/enterprise-auth';

export default {
  name: 'EnterpriseAuth',
  data: () => ({
    rows: [],
    loading: false,
    status: 'PENDING',
  }),
  created() {
    this.load();
  },
  methods: {
    async load() {
      this.loading = true;
      try {
        const res = await getEnterpriseCertificationList({ status: this.status });
        this.rows = (res.data && res.data.data) || [];
      } finally {
        this.loading = false;
      }
    },
    subjectTypeText(value) {
      return value === 'PERSONAL' ? '个人' : '企业';
    },
    applicationTypeText(value) {
      return value === 'ADD_PARK' ? '新增园区' : '企业认证';
    },
    valueText(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    parkNamesText(value) {
      return Array.isArray(value) && value.length ? value.join('、') : '-';
    },
    statusText(value) {
      return {
        PENDING: '待审核',
        APPROVED: '已通过',
        REJECTED: '已驳回',
      }[value] || value;
    },
    tagType(value) {
      if (value === 'APPROVED') return 'success';
      if (value === 'REJECTED') return 'danger';
      return 'primary';
    },
    review(row, action) {
      const subject = row.applicationType === 'ADD_PARK' ? '新增园区申请' : '企业认证';
      const message = action === 'APPROVE' ? `确认通过该${subject}？` : `确认驳回该${subject}？`;
      this.$confirm(message, '提示')
        .then(() => reviewEnterpriseCertification(row.id, { action }))
        .then(() => {
          this.$message.success('处理成功');
          this.load();
        })
        .catch(() => {});
    },
  },
};
</script>

<style scoped>
.auth-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.status-select {
  width: 180px;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
