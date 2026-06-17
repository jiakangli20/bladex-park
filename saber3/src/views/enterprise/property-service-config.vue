<template>
  <basic-container>
    <div class="property-service-config-page">
      <section class="toolbar-row">
        <el-form :inline="true" :model="serviceQuery" class="query-form">
          <el-form-item label="服务名称">
            <el-input v-model="serviceQuery.serviceName" clearable placeholder="请输入服务名称" @keyup.enter="loadServicePage" />
          </el-form-item>
          <el-form-item label="服务类型">
            <el-select v-model="serviceQuery.serviceType" clearable placeholder="全部类型">
              <el-option v-for="item in serviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="serviceQuery.status" clearable placeholder="全部状态">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchService">搜索</el-button>
            <el-button icon="el-icon-delete" @click="resetServiceSearch">清空</el-button>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button v-if="permissionList.serviceAddBtn" type="primary" icon="el-icon-plus" @click="openServiceDialog()">新增服务</el-button>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="loadServicePage" />
          </el-tooltip>
        </div>
      </section>

      <el-table v-loading="serviceLoading" :data="serviceData" border row-key="serviceId">
        <el-table-column prop="serviceName" label="服务名称" min-width="160" />
        <el-table-column prop="serviceType" label="服务类型" width="120">
          <template #default="{ row }">
            <el-tag effect="plain">{{ serviceTypeText(row.serviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chargeStandard" label="收费标准" min-width="140" show-overflow-tooltip />
        <el-table-column prop="timeLimit" label="服务时限" width="110" align="center">
          <template #default="{ row }">
            {{ row.timeLimit ? `${row.timeLimit}小时` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '0'"
              :disabled="!permissionList.serviceEditBtn"
              @change="checked => toggleServiceStatus(row, checked)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="permissionList.serviceViewBtn" type="primary" text icon="el-icon-view" @click="openServiceDialog(row, true)">查看</el-button>
            <el-button v-if="permissionList.serviceEditBtn" type="primary" text icon="el-icon-edit" @click="openServiceDialog(row)">编辑</el-button>
            <el-button v-if="permissionList.serviceDelBtn" type="danger" text icon="el-icon-delete" @click="deleteService(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          :current-page="servicePage.currentPage"
          :page-size="servicePage.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="servicePage.total"
          @size-change="size => changeServiceSize(size)"
          @current-change="page => changeServiceCurrent(page)"
        />
      </div>

      <el-dialog
        v-model="serviceDialogVisible"
        :title="serviceView ? '查看服务' : serviceForm.serviceId ? '编辑服务' : '新增服务'"
        width="620px"
        append-to-body
        :before-close="closeServiceDialog"
      >
        <el-form ref="serviceFormRef" :model="serviceForm" :rules="serviceRules" :disabled="serviceView" label-width="96px">
          <el-form-item label="服务名称" prop="serviceName">
            <el-input v-model="serviceForm.serviceName" maxlength="100" show-word-limit placeholder="请输入服务名称" />
          </el-form-item>
          <el-form-item label="服务类型" prop="serviceType">
            <el-select v-model="serviceForm.serviceType" placeholder="请选择服务类型" style="width: 100%">
              <el-option v-for="item in serviceTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="园区ID" prop="parkId">
            <el-input-number v-model="serviceForm.parkId" :min="1" :precision="0" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="收费标准">
            <el-input v-model="serviceForm.chargeStandard" maxlength="200" placeholder="请输入收费标准" />
          </el-form-item>
          <el-form-item label="服务时限">
            <el-input-number v-model="serviceForm.timeLimit" :min="0" :precision="0" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="serviceForm.sortOrder" :min="0" :precision="0" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="serviceForm.status" active-value="0" inactive-value="1" />
          </el-form-item>
          <el-form-item label="服务说明">
            <el-input v-model="serviceForm.serviceDesc" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入服务说明" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span v-if="!serviceView">
            <el-button type="primary" icon="el-icon-circle-check" @click="submitServiceForm">提交</el-button>
            <el-button icon="el-icon-circle-close" @click="closeServiceDialog">取消</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  getServicePage,
  removeService,
  submitService,
  updateService,
} from '@/api/business/property-service';

export default {
  name: 'EnterprisePropertyServiceConfig',
  data() {
    return {
      serviceTypeOptions: [
        { value: 'repair', label: '维修服务' },
        { value: 'clean', label: '保洁服务' },
        { value: 'security', label: '安保服务' },
        { value: 'green', label: '绿化服务' },
        { value: 'parking', label: '车位服务' },
        { value: 'utility', label: '水电服务' },
        { value: 'other', label: '其他' },
      ],
      serviceQuery: {},
      servicePage: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      serviceData: [],
      serviceLoading: false,
      serviceDialogVisible: false,
      serviceView: false,
      serviceForm: {},
      serviceRules: {
        serviceName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
        serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        serviceListBtn: this.validData(this.permission.property_service_list, false),
        serviceAddBtn: this.validData(this.permission.property_service_add, false),
        serviceEditBtn: this.validData(this.permission.property_service_edit, false),
        serviceDelBtn: this.validData(this.permission.property_service_delete, false),
        serviceViewBtn: this.validData(this.permission.property_service_view, false),
      };
    },
  },
  created() {
    this.loadServicePage();
  },
  methods: {
    responseData(res) {
      const body = res && res.data ? res.data : res;
      return body && body.data !== undefined ? body.data : body;
    },
    normalizeRecords(payload) {
      return Array.isArray(payload && payload.records) ? payload.records : [];
    },
    serviceTypeText(value) {
      const item = this.serviceTypeOptions.find(option => option.value === value);
      return item ? item.label : value || '-';
    },
    loadServicePage() {
      this.serviceLoading = true;
      getServicePage(this.servicePage.currentPage, this.servicePage.pageSize, this.serviceQuery).then(res => {
        const data = this.responseData(res) || {};
        this.serviceData = this.normalizeRecords(data);
        this.servicePage.total = Number(data.total || 0);
      }).finally(() => {
        this.serviceLoading = false;
      });
    },
    searchService() {
      this.servicePage.currentPage = 1;
      this.loadServicePage();
    },
    resetServiceSearch() {
      this.serviceQuery = {};
      this.searchService();
    },
    changeServiceSize(size) {
      this.servicePage.pageSize = size;
      this.loadServicePage();
    },
    changeServiceCurrent(current) {
      this.servicePage.currentPage = current;
      this.loadServicePage();
    },
    openServiceDialog(row, view = false) {
      this.serviceView = view;
      this.serviceForm = row ? { ...row } : { parkId: 1, status: '0', sortOrder: 0 };
      this.serviceDialogVisible = true;
    },
    closeServiceDialog() {
      this.serviceDialogVisible = false;
      this.serviceView = false;
      this.serviceForm = {};
      this.$nextTick(() => this.$refs.serviceFormRef && this.$refs.serviceFormRef.clearValidate());
    },
    submitServiceForm() {
      this.$refs.serviceFormRef.validate(valid => {
        if (!valid) return;
        const api = this.serviceForm.serviceId ? updateService : submitService;
        api(this.serviceForm).then(() => {
          ElMessage.success('操作成功');
          this.closeServiceDialog();
          this.loadServicePage();
        });
      });
    },
    toggleServiceStatus(row, checked) {
      updateService({ serviceId: row.serviceId, serviceName: row.serviceName, serviceType: row.serviceType, status: checked ? '0' : '1' }).then(() => {
        ElMessage.success('操作成功');
        this.loadServicePage();
      });
    },
    deleteService(row) {
      ElMessageBox.confirm(`确定删除服务项「${row.serviceName}」吗？`, '提示', {
        type: 'warning',
      }).then(() => removeService(row.serviceId)).then(() => {
        ElMessage.success('删除成功');
        this.loadServicePage();
      });
    },
  },
};
</script>

<style scoped>
.property-service-config-page {
  min-width: 0;
}

.toolbar-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.query-form {
  flex: 1;
  min-width: 0;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1180px) {
  .toolbar-row {
    display: block;
  }

  .toolbar-actions {
    justify-content: flex-end;
    margin-top: 8px;
  }
}
</style>
