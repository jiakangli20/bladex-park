<template>
  <basic-container>
    <div class="policy-service-page">
      <section class="policy-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="服务标题">
            <el-input v-model="query.serviceTitle" clearable placeholder="请输入服务标题" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="项目范围">
            <el-select v-model="query.projectScope" clearable placeholder="请选择投放人群">
              <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务状态">
            <el-select v-model="query.serviceStatus" clearable placeholder="请选择服务状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="policy-table-card">
        <div class="policy-toolbar">
          <div class="policy-toolbar__actions">
            <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openAdd">新增</el-button>
            <el-button icon="el-icon-download" @click="exportCsv">导出</el-button>
          </div>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="onLoad(page)" />
          </el-tooltip>
        </div>

        <el-table
          ref="table"
          v-loading="loading"
          :data="data"
          border
          row-key="policyId"
          scrollbar-always-on
          class="policy-table"
        >
          <el-table-column prop="serviceTitle" label="服务标题" min-width="240" align="center" show-overflow-tooltip />
          <el-table-column prop="serviceStatus" label="服务状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.serviceStatus)" effect="plain">{{ statusText(row.serviceStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览总数" width="120" align="center" />
          <el-table-column prop="permanentFlag" label="是否永久有效" width="150" align="center">
            <template #default="{ row }">{{ permanentText(row.permanentFlag) }}</template>
          </el-table-column>
          <el-table-column prop="validTime" label="有效期" width="190" align="center">
            <template #default="{ row }">
              <span class="single-line-cell">{{ row.permanentFlag === '0' ? '-' : row.validTime || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="onlineFlag" label="上架" width="120" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.onlineFlag === '0'"
                :disabled="!permissionList.onlineBtn"
                @change="value => changeOnline(row, value)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center">
            <template #default="{ row }"><span class="single-line-cell">{{ row.createTime || '-' }}</span></template>
          </el-table-column>
          <el-table-column label="操作" width="156" fixed="right" align="center">
            <template #default="{ row }">
              <div class="table-row-actions">
                <el-button v-if="permissionList.editBtn" type="primary" text @click="openEdit(row)">编辑</el-button>
                <el-dropdown v-if="permissionList.addBtn || permissionList.viewBtn || permissionList.delBtn" trigger="click">
                  <el-button type="primary" text icon="el-icon-more">更多</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="permissionList.addBtn" @click="copyRow(row)">复制</el-dropdown-item>
                      <el-dropdown-item v-if="permissionList.viewBtn" @click="openRecord(row)">提交记录</el-dropdown-item>
                      <el-dropdown-item v-if="permissionList.delBtn" divided @click="removeRow(row)">删除</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
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
      </section>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="form.policyId ? '编辑政策服务' : '新增政策服务'"
      width="820px"
      append-to-body
      :before-close="closeForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="116px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="服务标题" prop="serviceTitle">
              <el-input v-model="form.serviceTitle" maxlength="200" show-word-limit placeholder="请输入服务标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目范围" prop="projectScope">
              <el-select v-model="form.projectScope" placeholder="请选择投放人群" style="width: 100%">
                <el-option v-for="item in scopeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="服务状态" prop="serviceStatus">
              <el-select v-model="form.serviceStatus" placeholder="请选择服务状态" style="width: 100%">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="园区ID">
              <el-input-number v-model="form.parkId" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="有效方式" prop="permanentFlag">
              <el-radio-group v-model="form.permanentFlag">
                <el-radio label="0">永久有效</el-radio>
                <el-radio label="1">指定时间</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.permanentFlag === '1'" label="有效期" prop="validTime">
              <el-date-picker v-model="form.validTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择有效期" style="width: 100%" />
            </el-form-item>
            <el-form-item v-else label="有效期">
              <el-input disabled value="永久有效" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="上架">
              <el-switch v-model="form.onlineFlag" active-value="0" inactive-value="1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-upload
              action="/api/blade-resource/oss/endpoint/put-file-attach"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
            >
              <el-image v-if="form.coverUrl" :src="form.coverUrl" fit="cover" class="cover-preview" />
              <el-button v-else icon="el-icon-upload">上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverUrl" maxlength="500" placeholder="也可以直接填写图片地址" />
          </div>
        </el-form-item>
        <el-form-item label="政策内容">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="4000" show-word-limit placeholder="请输入小程序展示的政策内容" />
        </el-form-item>
        <el-form-item label="附件">
          <el-input v-model="form.attachmentFiles" type="textarea" :rows="2" maxlength="1000" placeholder="多个附件地址用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeForm">取消</el-button>
        <el-button type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordVisible" title="提交记录" width="560px" append-to-body>
      <el-descriptions v-if="record" :column="1" border>
        <el-descriptions-item label="服务标题">{{ record.serviceTitle }}</el-descriptions-item>
        <el-descriptions-item label="服务状态">{{ statusText(record.serviceStatus) }}</el-descriptions-item>
        <el-descriptions-item label="上架状态">{{ record.onlineFlag === '0' ? '已上架' : '已下架' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ record.createBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ record.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近提交人">{{ record.updateBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最近提交时间">{{ record.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="recordVisible = false">确定</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import {
  addPolicy,
  changePolicyOnline,
  copyPolicy,
  getPolicyDetail,
  getPolicyPage,
  getPolicySubmitRecord,
  removePolicy,
  updatePolicy,
} from '@/api/business/policy-service';

const scopeOptions = [
  { value: 'all', label: '全部企业' },
  { value: 'settled', label: '入驻企业' },
  { value: 'startup', label: '创业企业' },
  { value: 'technology', label: '科技企业' },
  { value: 'finance', label: '金融服务' },
];

const statusOptions = [
  { value: '0', label: '已发布', type: 'success' },
  { value: '1', label: '草稿', type: 'info' },
  { value: '2', label: '已下线', type: 'warning' },
];

const defaultForm = () => ({
  policyId: null,
  parkId: null,
  serviceTitle: '',
  projectScope: 'all',
  serviceStatus: '0',
  viewCount: 0,
  permanentFlag: '0',
  validTime: '',
  onlineFlag: '0',
  coverUrl: '',
  content: '',
  attachmentFiles: '',
  sortOrder: 0,
  remark: '',
});

export default {
  name: 'EnterprisePolicyService',
  data() {
    const validateValidTime = (rule, value, callback) => {
      if (this.form.permanentFlag === '1' && !value) {
        callback(new Error('请选择有效期'));
        return;
      }
      callback();
    };
    return {
      query: {},
      loading: false,
      submitLoading: false,
      data: [],
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      scopeOptions,
      statusOptions,
      formVisible: false,
      form: defaultForm(),
      recordVisible: false,
      record: null,
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      rules: {
        serviceTitle: [{ required: true, message: '请输入服务标题', trigger: 'blur' }],
        projectScope: [{ required: true, message: '请选择项目范围', trigger: 'change' }],
        serviceStatus: [{ required: true, message: '请选择服务状态', trigger: 'change' }],
        permanentFlag: [{ required: true, message: '请选择有效方式', trigger: 'change' }],
        validTime: [{ validator: validateValidTime, trigger: 'change' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.enterprise_policy_service_add, false),
        editBtn: this.validData(this.permission.enterprise_policy_service_edit, false),
        delBtn: this.validData(this.permission.enterprise_policy_service_delete, false),
        viewBtn: this.validData(this.permission.enterprise_policy_service_view, false),
        onlineBtn: this.validData(this.permission.enterprise_policy_service_online, false),
      };
    },
  },
  created() {
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    statusText(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    statusTagType(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.type : '';
    },
    permanentText(value) {
      return value === '1' ? '指定时间' : '永久有效';
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getPolicyPage(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const payload = res.data.data || {};
          this.data = payload.records || [];
          this.page.total = Number(payload.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    sizeChange(size) {
      this.page.pageSize = size;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    currentChange(current) {
      this.page.currentPage = current;
      this.onLoad(this.page);
    },
    openAdd() {
      this.form = defaultForm();
      this.formVisible = true;
    },
    openEdit(row) {
      getPolicyDetail(row.policyId).then(res => {
        this.form = {
          ...defaultForm(),
          ...(res.data.data || {}),
        };
        this.formVisible = true;
      });
    },
    closeForm() {
      this.formVisible = false;
      this.submitLoading = false;
      this.form = defaultForm();
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const payload = {
          ...this.form,
          validTime: this.form.permanentFlag === '0' ? '' : this.form.validTime,
        };
        const request = payload.policyId ? updatePolicy(payload) : addPolicy(payload);
        request
          .then(() => {
            this.$message.success('操作成功');
            this.closeForm();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    changeOnline(row, value) {
      const onlineFlag = value ? '0' : '1';
      changePolicyOnline(row.policyId, onlineFlag)
        .then(() => {
          this.$message.success(value ? '已上架' : '已下架');
          row.onlineFlag = onlineFlag;
        })
        .catch(() => {
          row.onlineFlag = value ? '1' : '0';
        });
    },
    copyRow(row) {
      copyPolicy(row.policyId).then(() => {
        this.$message.success('复制成功');
        this.onLoad(this.page);
      });
    },
    openRecord(row) {
      getPolicySubmitRecord(row.policyId).then(res => {
        this.record = res.data.data || {};
        this.recordVisible = true;
      });
    },
    removeRow(row) {
      this.$confirm('确定删除该政策服务?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        removePolicy(String(row.policyId)).then(() => {
          this.$message.success('删除成功');
          this.onLoad(this.page);
        });
      });
    },
    handleCoverSuccess(response) {
      if (!response || !response.success) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      this.form.coverUrl = data.link || data.url || '';
      this.$message.success('上传成功');
    },
    exportCsv() {
      const header = ['服务标题', '服务状态', '浏览总数', '是否永久有效', '有效期', '上架', '创建时间'];
      const rows = this.data.map(row => [
        row.serviceTitle || '',
        this.statusText(row.serviceStatus),
        row.viewCount || 0,
        this.permanentText(row.permanentFlag),
        row.permanentFlag === '0' ? '' : row.validTime || '',
        row.onlineFlag === '0' ? '上架' : '下架',
        row.createTime || '',
      ]);
      const csv = [header, ...rows].map(items => items.map(item => `"${String(item).replace(/"/g, '""')}"`).join(',')).join('\n');
      const blob = new Blob([`\ufeff${csv}`], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `政策服务_${Date.now()}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    },
  },
};
</script>

<style scoped>
.policy-service-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.policy-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.policy-search :deep(.el-form-item) {
  margin-right: 24px;
  margin-bottom: 12px;
}

.policy-search :deep(.el-input),
.policy-search :deep(.el-select) {
  width: 240px;
}

.policy-table {
  width: 100%;
  border-radius: 0;
}

.policy-table-card {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.policy-toolbar {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.policy-toolbar__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.policy-table :deep(.el-table__header th),
.policy-table :deep(.el-table__cell),
.policy-table :deep(.cell) {
  text-align: center;
}

.policy-table :deep(.el-table__header th) {
  height: 54px;
  color: #606266;
  font-weight: 500;
  background: #fff;
}

.policy-table :deep(.el-table__row) {
  height: 53px;
}

.policy-table :deep(.el-scrollbar__bar.is-horizontal) {
  bottom: 2px;
  height: 8px;
  opacity: 1;
}

.policy-table :deep(.el-scrollbar__bar.is-horizontal .el-scrollbar__thumb) {
  background-color: #aeb5c2;
}

.single-line-cell {
  display: inline-block;
  white-space: nowrap;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

.cover-upload {
  display: grid;
  grid-template-columns: 118px 1fr;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.cover-preview {
  width: 108px;
  height: 68px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.policy-service-page :deep(.el-button),
.policy-service-page :deep(.el-input__wrapper),
.policy-service-page :deep(.el-select__wrapper),
.policy-service-page :deep(.el-input-number .el-input__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1180px) {
  .policy-search :deep(.el-form-item) {
    margin-right: 12px;
  }

  .policy-search :deep(.el-input),
  .policy-search :deep(.el-select) {
    width: 190px;
  }
}
</style>
