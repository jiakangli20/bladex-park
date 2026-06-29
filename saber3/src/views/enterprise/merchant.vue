<template>
  <basic-container>
    <div class="merchant-page">
      <stat-cards :items="summaryCards" />

      <section class="contract-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="服务商名称">
            <el-input v-model="query.merchantName" clearable placeholder="请输入服务商名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="服务类型">
            <el-select v-model="query.businessType" clearable placeholder="全部类型">
              <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务区域">
            <el-input v-model="query.serviceArea" clearable placeholder="请输入服务区域" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="contract-toolbar">
        <div class="toolbar-left">
          <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openAdd">新增商户</el-button>
          <el-button
            v-if="permissionList.delBtn"
            type="danger"
            icon="el-icon-delete"
            plain
            :disabled="selectionList.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <el-table
        ref="table"
        v-loading="loading"
        :data="data"
        border
        row-key="merchantId"
        class="contract-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="merchantName" label="服务商名称" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column prop="businessType" label="服务类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ businessTypeText(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="serviceScope" label="服务范围" min-width="180" align="center" show-overflow-tooltip />
        <el-table-column prop="serviceArea" label="服务区域" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="110" align="center" show-overflow-tooltip />
        <el-table-column prop="contactPhone" label="联系电话" width="130" align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="小程序展示" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="permissionList.viewBtn" type="primary" text @click="openView(row)">查看</el-button>
              <el-button v-if="permissionList.editBtn" type="primary" text @click="openEdit(row)">编辑</el-button>
              <el-dropdown v-if="permissionList.statusBtn || permissionList.delBtn" trigger="click">
                <el-button type="primary" text>更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '0'" @click="changeStatus(row, '0')">上架</el-dropdown-item>
                    <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '1'" @click="changeStatus(row, '1')">下架</el-dropdown-item>
                    <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '2'" @click="changeStatus(row, '2')">暂停展示</el-dropdown-item>
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
          :page-sizes="[10, 20, 30, 50]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="viewMode ? '查看商户' : form.merchantId ? '编辑商户' : '新增商户'"
      width="760px"
      append-to-body
      :before-close="closeForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" :disabled="viewMode" label-width="104px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="服务商名称" prop="merchantName">
              <el-input v-model="form.merchantName" maxlength="200" show-word-limit placeholder="请输入服务商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务类型" prop="businessType">
              <el-select v-model="form.businessType" placeholder="请选择服务类型" style="width: 100%">
                <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="园区ID" prop="parkId">
              <el-select v-model="form.parkId" clearable filterable placeholder="请选择园区" style="width: 100%">
                <el-option v-for="item in parkList" :key="item.id" :label="item.parkName || item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="服务范围">
          <el-input v-model="form.serviceScope" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入服务范围" />
        </el-form-item>
        <el-form-item label="服务区域">
          <el-input v-model="form.serviceArea" maxlength="500" placeholder="多个区域可用逗号分隔" />
        </el-form-item>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" maxlength="50" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系方式">
              <el-input v-model="form.contactPhone" maxlength="20" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" maxlength="300" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="资质附件">
          <el-upload
            action="/api/blade-resource/oss/endpoint/put-file-attach"
            :headers="uploadHeaders"
            :file-list="qualificationFileList"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            :before-remove="beforeUploadRemove"
            :disabled="viewMode"
            multiple
          >
            <el-button v-if="!viewMode" icon="el-icon-upload">上传附件</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeForm">取消</el-button>
        <el-button v-if="!viewMode" type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import { getList as getParkList } from '@/api/park/park';
import {
  addMerchant,
  changeMerchantStatus,
  getMerchantDetail,
  getMerchantPage,
  getMerchantStatistics,
  removeMerchant,
  updateMerchant,
} from '@/api/business/merchant';

const businessTypeOptions = [
  { value: 'value_added', label: '增值服务' },
  { value: 'IT', label: 'IT服务' },
  { value: 'clean', label: '保洁服务' },
  { value: 'security', label: '安保服务' },
  { value: 'catering', label: '餐饮服务' },
  { value: 'repair', label: '设备维修' },
  { value: 'green', label: '绿化服务' },
  { value: 'other', label: '其他' },
];

const statusOptions = [
  { value: '0', label: '已上架', type: 'success' },
  { value: '1', label: '已下架', type: 'info' },
  { value: '2', label: '暂停展示', type: 'warning' },
];

const defaultForm = () => ({
  merchantId: null,
  parkId: null,
  merchantName: '',
  businessType: '',
  serviceScope: '',
  serviceArea: '',
  contactName: '',
  contactPhone: '',
  address: '',
  qualificationFiles: '',
  remark: '',
  status: '0',
});

export default {
  name: 'EnterpriseMerchant',
  data() {
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
      selectionList: [],
      summaryCards: [
        { key: 'totalCount', label: '商户总数', value: 0 },
        { key: 'normalCount', label: '小程序展示', value: 0 },
        { key: 'disabledCount', label: '已下架', value: 0 },
        { key: 'suspendedCount', label: '暂停展示', value: 0 },
      ],
      businessTypeOptions,
      statusOptions,
      parkList: [],
      formVisible: false,
      viewMode: false,
      form: defaultForm(),
      qualificationFileList: [],
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      rules: {
        merchantName: [{ required: true, message: '请输入服务商名称', trigger: 'blur' }],
        businessType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.merchant_service_merchant_add, false),
        editBtn: this.validData(this.permission.merchant_service_merchant_edit, false),
        delBtn: this.validData(this.permission.merchant_service_merchant_delete, false),
        viewBtn: this.validData(this.permission.merchant_service_merchant_view, false),
        statusBtn: this.validData(this.permission.merchant_service_merchant_status, false),
      };
    },
    ids() {
      return this.selectionList.map(item => item.merchantId).join(',');
    },
  },
  created() {
    this.loadParkList();
    this.loadStatistics();
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    businessTypeText(value) {
      const item = this.businessTypeOptions.find(option => option.value === value);
      return item ? item.label : value || '-';
    },
    statusText(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    statusTagType(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.type : '';
    },
    loadParkList() {
      getParkList(1, 999, {}).then(res => {
        const payload = res.data.data || {};
        this.parkList = payload.records || [];
      });
    },
    loadStatistics() {
      getMerchantStatistics({ ...this.query }).then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getMerchantPage(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const payload = res.data.data || {};
          this.data = payload.records || [];
          this.page.total = Number(payload.total) || 0;
          this.selectionClear();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.loadStatistics();
      this.onLoad(this.page);
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.loadStatistics();
      this.onLoad(this.page);
    },
    reload() {
      this.loadStatistics();
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
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      if (this.$refs.table) {
        this.$refs.table.clearSelection();
      }
    },
    openAdd() {
      this.viewMode = false;
      this.form = defaultForm();
      this.qualificationFileList = [];
      this.formVisible = true;
    },
    openEdit(row) {
      this.viewMode = false;
      this.loadDetail(row.merchantId);
    },
    openView(row) {
      this.viewMode = true;
      this.loadDetail(row.merchantId);
    },
    loadDetail(merchantId) {
      getMerchantDetail(merchantId).then(res => {
        this.form = {
          ...defaultForm(),
          ...(res.data.data || {}),
        };
        this.qualificationFileList = this.parseQualificationFiles(this.form.qualificationFiles);
        this.formVisible = true;
      });
    },
    closeForm() {
      this.formVisible = false;
      this.viewMode = false;
      this.submitLoading = false;
      this.form = defaultForm();
      this.qualificationFileList = [];
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const payload = {
          ...this.form,
          qualificationFiles: JSON.stringify(
            this.qualificationFileList
              .map(file => ({
                name: file.name,
                url: file.url,
              }))
              .filter(file => file.url)
          ),
        };
        const request = payload.merchantId ? updateMerchant(payload) : addMerchant(payload);
        request
          .then(() => {
            this.$message.success('操作成功');
            this.closeForm();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    removeRow(row) {
      this.$confirm('确定删除该商户?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        removeMerchant(String(row.merchantId)).then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
      });
    },
    handleBatchDelete() {
      if (!this.ids) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定删除选中的商户?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        removeMerchant(this.ids).then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
      });
    },
    changeStatus(row, status) {
      changeMerchantStatus(row.merchantId, status).then(() => {
        this.$message.success('状态已更新');
        this.loadStatistics();
        this.onLoad(this.page);
      });
    },
    parseQualificationFiles(value) {
      if (!value) return [];
      try {
        const list = typeof value === 'string' ? JSON.parse(value) : value;
        return (Array.isArray(list) ? list : []).map((item, index) => {
          const url = typeof item === 'string' ? item : item.url || item.link || '';
          return {
            uid: `${Date.now()}-${index}`,
            name: typeof item === 'string' ? item.split('/').pop() : item.name || item.originalName || `附件${index + 1}`,
            url,
            status: 'success',
          };
        });
      } catch (error) {
        return [];
      }
    },
    handleUploadSuccess(response, file) {
      if (!response || !response.success) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      this.qualificationFileList.push({
        uid: file.uid,
        name: data.originalName || data.name || file.name,
        url: data.link || data.url || '',
        status: 'success',
      });
      this.$message.success('上传成功');
    },
    handleUploadRemove(file) {
      this.qualificationFileList = this.qualificationFileList.filter(item => item.uid !== file.uid);
    },
    beforeUploadRemove() {
      return !this.viewMode;
    },
  },
};
</script>

<style scoped>
.merchant-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

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

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.merchant-page :deep(.el-button),
.merchant-page :deep(.el-input__wrapper),
.merchant-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 960px) {
  .toolbar-left {
    flex-wrap: wrap;
  }
}
</style>
