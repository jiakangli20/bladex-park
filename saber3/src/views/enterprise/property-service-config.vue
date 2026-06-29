<template>
  <basic-container>
    <div class="property-service-config-page">
      <stat-cards :items="serviceSummaryCards" />

      <section class="contract-search">
        <el-form :inline="true" :model="serviceQuery">
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
              <el-option label="已上架" value="0" />
              <el-option label="已下架" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchService">搜索</el-button>
            <el-button icon="el-icon-delete" @click="resetServiceSearch">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="contract-toolbar">
        <div class="toolbar-left">
          <el-button v-if="permissionList.serviceAddBtn" type="primary" icon="el-icon-plus" @click="openServiceDialog()">新增服务</el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reloadService" />
        </el-tooltip>
      </section>

      <el-table v-loading="serviceLoading" :data="serviceData" border row-key="serviceId" class="contract-table">
        <el-table-column prop="serviceName" label="服务名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="serviceType" label="服务类型" width="130" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ serviceTypeText(row.serviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chargeStandard" label="收费标准" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="requiredMaterials" label="申请材料" min-width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.requiredMaterials || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="serviceFlow" label="办理流程" min-width="200" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.serviceFlow || '小程序申请-工单受理-处置完成-用户评价' }}
          </template>
        </el-table-column>
        <el-table-column prop="timeLimit" label="处理时限" width="110" align="center">
          <template #default="{ row }">
            {{ row.timeLimit ? `${row.timeLimit}小时` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="小程序上架" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '0'"
              :disabled="!permissionList.serviceEditBtn"
              active-text="上架"
              inactive-text="下架"
              inline-prompt
              @change="checked => toggleServiceStatus(row, checked)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="permissionList.serviceViewBtn" type="primary" text @click="openServiceDialog(row, true)">查看</el-button>
              <el-button v-if="permissionList.serviceEditBtn" type="primary" text @click="openServiceDialog(row)">编辑</el-button>
              <el-button v-if="permissionList.serviceDelBtn" type="danger" text @click="deleteService(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="contract-pagination">
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
        width="760px"
        append-to-body
        :before-close="closeServiceDialog"
      >
        <el-form ref="serviceFormRef" :model="serviceForm" :rules="serviceRules" :disabled="serviceView" label-width="104px">
          <div class="form-grid">
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
              <el-input v-model="serviceForm.chargeStandard" maxlength="200" placeholder="如：物业公司代收物业费" />
            </el-form-item>
            <el-form-item label="处理时限">
              <el-input-number v-model="serviceForm.timeLimit" :min="0" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="serviceForm.sortOrder" :min="0" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="小程序上架">
              <el-switch v-model="serviceForm.status" active-text="上架" inactive-text="下架" active-value="0" inactive-value="1" inline-prompt />
            </el-form-item>
          </div>
          <el-form-item label="申请材料">
            <el-input v-model="serviceForm.requiredMaterials" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入小程序申请时需要展示或采集的材料" />
          </el-form-item>
          <el-form-item label="办理流程">
            <el-input v-model="serviceForm.serviceFlow" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="默认：小程序申请-工单受理-处置完成-用户评价" />
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
  getServiceList,
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
        { value: 'property_fee', label: '物业代收费' },
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
      serviceSummaryData: [],
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
    serviceSummaryCards() {
      const source = this.serviceSummaryData.length ? this.serviceSummaryData : this.serviceData;
      const total = source.length || this.servicePage.total || 0;
      const enabled = source.filter(item => item.status === '0').length;
      const disabled = source.filter(item => item.status === '1').length;
      const miniapp = enabled;
      return [
        { key: 'total', label: '服务总数', value: total },
        { key: 'enabled', label: '已上架服务', value: enabled },
        { key: 'miniapp', label: '小程序可申请', value: miniapp },
        { key: 'disabled', label: '已下架服务', value: disabled },
      ];
    },
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
    this.loadServiceSummary();
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
    loadServiceSummary() {
      getServiceList({}).then(res => {
        const data = this.responseData(res);
        this.serviceSummaryData = Array.isArray(data) ? data : [];
      });
    },
    reloadService() {
      this.loadServicePage();
      this.loadServiceSummary();
    },
    searchService() {
      this.servicePage.currentPage = 1;
      this.loadServicePage();
      this.loadServiceSummary();
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
      this.serviceForm = row ? { ...row } : {
        parkId: 1,
        serviceType: 'property_fee',
        chargeStandard: '物业公司代收物业费',
        serviceFlow: '小程序申请-工单受理-处置完成-用户评价',
        status: '0',
        sortOrder: 0,
      };
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
          this.reloadService();
        });
      });
    },
    toggleServiceStatus(row, checked) {
      updateService({ serviceId: row.serviceId, serviceName: row.serviceName, serviceType: row.serviceType, status: checked ? '0' : '1' }).then(() => {
        ElMessage.success('操作成功');
        this.reloadService();
      });
    },
    deleteService(row) {
      ElMessageBox.confirm(`确定删除服务项「${row.serviceName}」吗？`, '提示', {
        type: 'warning',
      }).then(() => removeService(row.serviceId)).then(() => {
        ElMessage.success('删除成功');
        this.reloadService();
      });
    },
  },
};
</script>

<style scoped>
.property-service-config-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
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
  align-items: center;
  gap: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.contract-table {
  width: 100%;
  border-radius: 0;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  white-space: nowrap;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.property-service-config-page :deep(.el-button),
.property-service-config-page :deep(.el-input__wrapper),
.property-service-config-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1180px) {
  .toolbar-left {
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
