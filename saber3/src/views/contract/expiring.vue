<template>
  <basic-container>
    <div class="contract-expiring-page">
      <section class="expiring-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="规则名称">
            <el-input
              v-model="query.ruleName"
              clearable
              placeholder="请输入合同到期规则名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" plain icon="el-icon-search" @click="searchChange">
              搜索
            </el-button>
            <el-button plain icon="el-icon-refresh-left" @click="searchReset">重置</el-button>
            <el-button type="primary" plain icon="el-icon-plus" @click="handleAdd">新增</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="expiring-table-wrap">
        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="ruleId"
          class="expiring-table"
        >
          <el-table-column prop="ruleName" label="名称" min-width="180" align="center" />
          <el-table-column
            prop="buildingNames"
            label="关联楼宇"
            min-width="260"
            align="center"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ row.buildingNames || '-' }}
            </template>
          </el-table-column>
          <el-table-column
            prop="remindDays"
            label="提前几天提醒(天)"
            width="180"
            align="center"
          />
          <el-table-column prop="createTime" label="创建时间" width="220" align="center" />
          <el-table-column label="操作" width="150" align="center" fixed="right">
            <template #default="{ row }">
              <div class="expiring-actions">
                <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
                <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div class="expiring-pagination">
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

      <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="560px"
        append-to-body
        @close="resetForm"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="112px"
          class="rule-form"
        >
          <el-form-item label="规则名称" prop="ruleName">
            <el-input v-model="form.ruleName" maxlength="100" placeholder="请输入规则名称" />
          </el-form-item>
          <el-form-item label="关联楼宇" prop="buildingIdList">
            <el-select
              v-model="form.buildingIdList"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择关联楼宇"
              style="width: 100%"
            >
              <el-option
                v-for="item in buildingOptions"
                :key="item.id"
                :label="item.name"
                :value="String(item.id)"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="提前提醒" prop="remindDays">
            <el-input-number
              v-model="form.remindDays"
              :min="1"
              :max="365"
              controls-position="right"
              style="width: 180px"
            />
            <span class="unit-text">天</span>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="form.remark"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="请输入备注"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { getList, remove, submit } from '@/api/contract/expiring-rule';
import { getSimpleList as getBuildingList } from '@/api/park/building';

const emptyForm = () => ({
  ruleId: '',
  ruleName: '',
  buildingIdList: [],
  buildingIds: '',
  buildingNames: '',
  remindDays: 30,
  remark: '',
});

export default {
  name: 'ContractExpiring',
  data() {
    return {
      query: {
        ruleName: '',
      },
      loading: false,
      submitLoading: false,
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      data: [],
      buildingOptions: [],
      dialogVisible: false,
      form: emptyForm(),
      rules: {
        ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        buildingIdList: [{ required: true, message: '请选择关联楼宇', trigger: 'change' }],
        remindDays: [{ required: true, message: '请输入提前提醒天数', trigger: 'change' }],
      },
    };
  },
  computed: {
    dialogTitle() {
      return this.form.ruleId ? '编辑到期提醒规则' : '新增到期提醒规则';
    },
  },
  created() {
    this.loadBuildings();
    this.onLoad();
  },
  methods: {
    loadBuildings() {
      getBuildingList().then(res => {
        this.buildingOptions = res.data.data || [];
      });
    },
    onLoad() {
      this.loading = true;
      getList(this.page.currentPage, this.page.pageSize, this.query)
        .then(res => {
          const data = res.data.data || {};
          this.data = data.records || [];
          this.page.total = data.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad();
    },
    searchReset() {
      this.query = {
        ruleName: '',
      };
      this.searchChange();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad();
    },
    handleAdd() {
      this.form = emptyForm();
      this.dialogVisible = true;
      this.clearValidate();
    },
    handleEdit(row) {
      this.form = {
        ...emptyForm(),
        ...row,
        buildingIdList: String(row.buildingIds || '')
          .split(',')
          .filter(Boolean),
      };
      this.dialogVisible = true;
      this.clearValidate();
    },
    handleDelete(row) {
      this.$confirm(`确认删除规则“${row.ruleName || ''}”吗？`, '提示', {
        type: 'warning',
      })
        .then(() => remove(row.ruleId))
        .then(() => {
          this.$message.success('删除成功');
          this.onLoad();
        })
        .catch(() => {});
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        const buildingNames = this.buildingOptions
          .filter(item => this.form.buildingIdList.includes(String(item.id)))
          .map(item => item.name)
          .join(',');
        const payload = {
          ruleId: this.form.ruleId || undefined,
          ruleName: this.form.ruleName,
          buildingIds: this.form.buildingIdList.join(','),
          buildingNames,
          remindDays: this.form.remindDays,
          remark: this.form.remark,
        };
        this.submitLoading = true;
        submit(payload)
          .then(() => {
            this.$message.success('保存成功');
            this.dialogVisible = false;
            this.onLoad();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    resetForm() {
      this.form = emptyForm();
      this.submitLoading = false;
      this.clearValidate();
    },
    clearValidate() {
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.clearValidate();
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.contract-expiring-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.expiring-search,
.expiring-table-wrap {
  border-radius: 10px;
  background: #fff;
}

.expiring-search {
  padding: 14px 20px;
}

.expiring-search :deep(.el-form-item) {
  margin-bottom: 0;
}

.expiring-search :deep(.el-input) {
  width: 240px;
}

.expiring-table-wrap {
  padding: 20px;
}

.expiring-table {
  width: 100%;
}

.expiring-table :deep(.el-table__cell) {
  height: 44px;
}

.expiring-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.expiring-actions :deep(.el-button) {
  margin-left: 0;
}

.expiring-pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}

.rule-form {
  padding-right: 8px;
}

.unit-text {
  margin-left: 8px;
  color: #606266;
}
</style>
