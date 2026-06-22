<template>
  <el-dialog
    v-model="visible"
    title="行业准入规则配置"
    width="860px"
    append-to-body
    class="industry-rule-dialog"
    @closed="handleClosed"
  >
    <section class="rule-filter">
      <el-input
        v-model="query.industryKeyword"
        clearable
        placeholder="行业关键词"
        @keyup.enter="loadRules"
      />
      <el-select v-model="query.accessType" clearable placeholder="准入类型">
        <el-option label="限制" value="2" />
        <el-option label="禁入" value="3" />
      </el-select>
      <el-button type="primary" icon="el-icon-search" @click="loadRules">查询</el-button>
      <el-button icon="el-icon-delete" @click="resetQuery">清空</el-button>
      <el-button type="primary" plain icon="el-icon-plus" @click="handleAdd">新增规则</el-button>
    </section>

    <el-table
      v-loading="loading"
      :data="ruleList"
      border
      row-key="ruleId"
      class="rule-table"
      empty-text="暂无行业准入规则"
    >
      <el-table-column prop="industryKeyword" label="行业关键词" min-width="150" align="center" />
      <el-table-column prop="accessType" label="准入类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="String(row.accessType) === '3' ? 'danger' : 'warning'" effect="plain">
            {{ accessTypeText(row.accessType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="规则说明" min-width="220" align="center" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" align="center" />
      <el-table-column label="操作" width="116" fixed="right" align="center">
        <template #default="{ row }">
          <el-button text type="primary" class="table-action-btn" @click="handleEdit(row)">编辑</el-button>
          <el-button text type="danger" class="table-action-btn" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>

    <el-dialog
      v-model="formVisible"
      :title="form.ruleId ? '编辑规则' : '新增规则'"
      width="520px"
      append-to-body
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="关键词" prop="industryKeyword">
          <el-input v-model="form.industryKeyword" maxlength="100" placeholder="例如：金融、化工" />
        </el-form-item>
        <el-form-item label="准入类型" prop="accessType">
          <el-select v-model="form.accessType" placeholder="请选择准入类型" style="width: 100%">
            <el-option label="限制" value="2" />
            <el-option label="禁入" value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则说明" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script>
import {
  addIndustryRule,
  delIndustryRule,
  listIndustryRule,
  updateIndustryRule,
} from '@/api/business/customer';

const defaultForm = () => ({
  ruleId: undefined,
  industryKeyword: '',
  accessType: '2',
  reason: '',
});

export default {
  name: 'IndustryRuleDialog',
  emits: ['ok'],
  data() {
    return {
      visible: false,
      formVisible: false,
      loading: false,
      submitLoading: false,
      query: {},
      ruleList: [],
      form: defaultForm(),
      rules: {
        industryKeyword: [{ required: true, message: '请输入关键词', trigger: 'blur' }],
        accessType: [{ required: true, message: '请选择准入类型', trigger: 'change' }],
      },
    };
  },
  methods: {
    open() {
      this.visible = true;
      this.loadRules();
    },
    loadRules() {
      this.loading = true;
      listIndustryRule(this.query)
        .then(res => {
          this.ruleList = res.data.data || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    resetQuery() {
      this.query = {};
      this.loadRules();
    },
    handleAdd() {
      this.form = defaultForm();
      this.formVisible = true;
    },
    handleEdit(row) {
      this.form = {
        ...defaultForm(),
        ...row,
      };
      this.formVisible = true;
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const action = this.form.ruleId ? updateIndustryRule : addIndustryRule;
        action(this.form)
          .then(() => {
            this.$message.success('保存成功');
            this.formVisible = false;
            this.loadRules();
            this.$emit('ok');
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    handleDelete(row) {
      this.$confirm(`确认删除规则【${row.industryKeyword}】吗？`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => delIndustryRule(row.ruleId))
        .then(() => {
          this.$message.success('删除成功');
          this.loadRules();
          this.$emit('ok');
        });
    },
    resetForm() {
      this.form = defaultForm();
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    handleClosed() {
      this.query = {};
      this.ruleList = [];
    },
    accessTypeText(value) {
      return String(value) === '3' ? '禁入' : '限制';
    },
  },
};
</script>

<style scoped>
.rule-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.rule-filter .el-input {
  width: 220px;
}

.rule-filter .el-select {
  width: 140px;
}

.rule-table {
  width: 100%;
}

.rule-table :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.table-action-btn {
  padding: 0 4px;
  font-weight: 400;
}
</style>
