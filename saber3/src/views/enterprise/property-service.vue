<template>
  <basic-container>
    <div class="property-service-page">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="服务目录" name="service">
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
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-if="permissionList.serviceViewBtn" type="primary" text icon="el-icon-view" @click="openServiceDialog(row, true)">查看</el-button>
                <el-button v-if="permissionList.serviceEditBtn" type="primary" text icon="el-icon-edit" @click="openServiceDialog(row)">编辑</el-button>
                <el-button v-if="permissionList.workorderAddBtn && row.status === '0'" type="success" text icon="el-icon-plus" @click="openWorkorderDialog(row)">报修</el-button>
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
        </el-tab-pane>

        <el-tab-pane label="我的工单" name="mine">
          <workorder-panel
            :query="mineQuery"
            :page="minePage"
            :data="mineData"
            :loading="mineLoading"
            :statistics="mineStatistics"
            :service-options="serviceOptions"
            :status-options="statusOptions"
            :priority-options="priorityOptions"
            :permission-list="permissionList"
            mine
            @search="searchMine"
            @reset="resetMineSearch"
            @reload="loadMinePage"
            @size-change="changeMineSize"
            @current-change="changeMineCurrent"
            @create="openWorkorderDialog()"
            @view="openDetail"
            @dispose="openDisposeDialog"
            @rate="openRateDialog"
            @remove="deleteWorkorder"
          />
        </el-tab-pane>

        <el-tab-pane label="工单处理" name="process">
          <workorder-panel
            :query="processQuery"
            :page="processPage"
            :data="processData"
            :loading="processLoading"
            :statistics="processStatistics"
            :service-options="serviceOptions"
            :status-options="statusOptions"
            :priority-options="priorityOptions"
            :permission-list="permissionList"
            @search="searchProcess"
            @reset="resetProcessSearch"
            @reload="loadProcessPage"
            @size-change="changeProcessSize"
            @current-change="changeProcessCurrent"
            @create="openWorkorderDialog()"
            @view="openDetail"
            @dispose="openDisposeDialog"
            @rate="openRateDialog"
            @remove="deleteWorkorder"
          />
        </el-tab-pane>
      </el-tabs>
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

    <el-dialog
      v-model="workorderDialogVisible"
      title="创建工单"
      width="720px"
      append-to-body
      :before-close="closeWorkorderDialog"
    >
      <el-form ref="workorderFormRef" :model="workorderForm" :rules="workorderRules" label-width="104px">
        <el-form-item label="服务项" prop="serviceId">
          <el-select v-model="workorderForm.serviceId" filterable placeholder="请选择服务项" style="width: 100%" @change="handleServiceChange">
            <el-option
              v-for="item in serviceOptions"
              :key="item.serviceId"
              :label="`${item.serviceName} / ${serviceTypeText(item.serviceType)}`"
              :value="item.serviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="客户名称" prop="customerName">
          <el-input v-model="workorderForm.customerName" maxlength="200" placeholder="请输入客户名称" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="workorderForm.contactName" maxlength="100" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="workorderForm.contactPhone" maxlength="50" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="园区ID" prop="parkId">
          <el-input-number v-model="workorderForm.parkId" :min="1" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="房源信息" prop="roomInfo">
          <el-input v-model="workorderForm.roomInfo" maxlength="200" placeholder="请输入房源信息" />
        </el-form-item>
        <el-form-item label="房源ID">
          <el-input v-model="workorderForm.roomIds" maxlength="500" placeholder="多个房源ID用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="需求描述" prop="demandDesc">
          <el-input v-model="workorderForm.demandDesc" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入需求描述" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="workorderForm.priority" style="width: 100%">
            <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="初始状态" prop="orderStatus">
          <el-select v-model="workorderForm.orderStatus" style="width: 100%">
            <el-option label="待受理" value="0" />
            <el-option label="处理中" value="1" />
            <el-option label="已完成" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needsProcess(workorderForm.orderStatus)" label="指派人" prop="assignTo">
          <assignee-select v-model="workorderForm.assignTo" :users="userOptions" />
        </el-form-item>
        <el-form-item v-if="needsProcess(workorderForm.orderStatus)" label="处置内容" prop="disposalContent">
          <el-input v-model="workorderForm.disposalContent" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入处置内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" icon="el-icon-circle-check" @click="submitWorkorderForm">提交</el-button>
        <el-button icon="el-icon-circle-close" @click="closeWorkorderDialog">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="disposeDialogVisible"
      title="处置工单"
      width="600px"
      append-to-body
      :before-close="closeDisposeDialog"
    >
      <el-form ref="disposeFormRef" :model="disposeForm" :rules="disposeRules" label-width="96px">
        <el-form-item label="工单编号">
          <el-input v-model="disposeForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="disposeForm.priority" style="width: 100%">
            <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="工单状态" prop="orderStatus">
          <el-select v-model="disposeForm.orderStatus" style="width: 100%">
            <el-option label="待受理" value="0" />
            <el-option label="处理中" value="1" />
            <el-option label="已完成" value="2" />
            <el-option label="已关闭" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needsProcess(disposeForm.orderStatus)" label="指派人" prop="assignTo">
          <assignee-select v-model="disposeForm.assignTo" :users="userOptions" />
        </el-form-item>
        <el-form-item v-if="needsProcess(disposeForm.orderStatus)" label="处置内容" prop="disposalContent">
          <el-input v-model="disposeForm.disposalContent" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入处置内容" />
        </el-form-item>
        <el-form-item v-if="disposeForm.orderStatus === '4'" label="关闭说明">
          <el-input v-model="disposeForm.processRemark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入关闭说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" icon="el-icon-circle-check" @click="submitDisposeForm">提交</el-button>
        <el-button icon="el-icon-circle-close" @click="closeDisposeDialog">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rateDialogVisible" title="评价工单" width="460px" append-to-body :before-close="closeRateDialog">
      <el-form ref="rateFormRef" :model="rateForm" :rules="rateRules" label-width="80px">
        <el-form-item label="评分" prop="rating">
          <el-rate v-model="rateForm.rating" />
        </el-form-item>
        <el-form-item label="评价" prop="ratingContent">
          <el-input v-model="rateForm.ratingContent" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入评价内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" icon="el-icon-circle-check" @click="submitRateForm">提交</el-button>
        <el-button icon="el-icon-circle-close" @click="closeRateDialog">取消</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="工单详情" size="680px" append-to-body>
      <template v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentOrder.orderStatus)">{{ statusText(currentOrder.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务名称">{{ currentOrder.serviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="priorityTagType(currentOrder.priority)" effect="plain">{{ priorityText(currentOrder.priority) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="客户名称">{{ currentOrder.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentOrder.contactName || '-' }} {{ currentOrder.contactPhone || '' }}</el-descriptions-item>
          <el-descriptions-item label="园区">{{ currentOrder.parkName || currentOrder.parkId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="房源">{{ currentOrder.roomInfo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="指派人">{{ currentOrder.assignTo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ currentOrder.finishTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求描述" :span="2">{{ currentOrder.demandDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处置内容" :span="2">{{ currentOrder.disposalContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评分">
            <template v-if="currentOrder.rating">
              <el-rate :model-value="currentOrder.rating" disabled />
            </template>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="评价内容">{{ currentOrder.ratingContent || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="log-title">操作日志</div>
        <el-table v-loading="logLoading" :data="logs" border row-key="logId">
          <el-table-column prop="operateTime" label="时间" width="180" />
          <el-table-column prop="action" label="操作" width="110" />
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="actionDesc" label="说明" min-width="180" show-overflow-tooltip />
        </el-table>
      </template>
    </el-drawer>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getList as getUserList } from '@/api/system/user';
import WorkorderPanel from './components/workorder-panel.vue';
import AssigneeSelect from './components/assignee-select.vue';
import {
  closeWorkorder,
  getServiceList,
  getServicePage,
  getWorkorderDetail,
  getWorkorderLog,
  getWorkorderPage,
  getWorkorderStatistics,
  rateWorkorder,
  removeService,
  removeWorkorder,
  submitService,
  submitWorkorder,
  updateService,
  updateWorkorder,
} from '@/api/business/property-service';

export default {
  name: 'EnterprisePropertyService',
  components: {
    WorkorderPanel,
    AssigneeSelect,
  },
  data() {
    return {
      activeTab: 'service',
      serviceTypeOptions: [
        { value: 'repair', label: '维修服务' },
        { value: 'clean', label: '保洁服务' },
        { value: 'security', label: '安保服务' },
        { value: 'green', label: '绿化服务' },
        { value: 'parking', label: '车位服务' },
        { value: 'utility', label: '水电服务' },
        { value: 'other', label: '其他' },
      ],
      statusOptions: [
        { value: '0', label: '待受理' },
        { value: '1', label: '处理中' },
        { value: '2', label: '已完成' },
        { value: '3', label: '已评价' },
        { value: '4', label: '已关闭' },
      ],
      priorityOptions: [
        { value: '0', label: '紧急' },
        { value: '1', label: '普通' },
        { value: '2', label: '低' },
      ],
      serviceQuery: {},
      servicePage: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      serviceData: [],
      serviceOptions: [],
      serviceLoading: false,
      serviceDialogVisible: false,
      serviceView: false,
      serviceForm: {},
      serviceRules: {
        serviceName: [{ required: true, message: '请输入服务名称', trigger: 'blur' }],
        serviceType: [{ required: true, message: '请选择服务类型', trigger: 'change' }],
      },
      mineQuery: { mine: true },
      minePage: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      mineData: [],
      mineLoading: false,
      mineStatistics: {},
      processQuery: {},
      processPage: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      processData: [],
      processLoading: false,
      processStatistics: {},
      workorderDialogVisible: false,
      workorderForm: {},
      workorderRules: {
        serviceId: [{ required: true, message: '请选择服务项', trigger: 'change' }],
        customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
        contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        parkId: [{ required: true, message: '请输入园区ID', trigger: 'change' }],
        roomInfo: [{ required: true, message: '请输入房源信息', trigger: 'blur' }],
        demandDesc: [{ required: true, message: '请输入需求描述', trigger: 'blur' }],
        assignTo: [{ required: true, message: '请选择指派人', trigger: 'change' }],
        disposalContent: [{ required: true, message: '请输入处置内容', trigger: 'blur' }],
      },
      disposeDialogVisible: false,
      disposeForm: {},
      disposeRules: {
        priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
        orderStatus: [{ required: true, message: '请选择工单状态', trigger: 'change' }],
        assignTo: [{ required: true, message: '请选择指派人', trigger: 'change' }],
        disposalContent: [{ required: true, message: '请输入处置内容', trigger: 'blur' }],
      },
      rateDialogVisible: false,
      rateForm: {},
      rateRules: {
        rating: [{ required: true, message: '请选择评分', trigger: 'change' }],
        ratingContent: [{ required: true, message: '请输入评价内容', trigger: 'blur' }],
      },
      detailVisible: false,
      currentOrder: null,
      logs: [],
      logLoading: false,
      userOptions: [],
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
        workorderListBtn: this.validData(this.permission.property_workorder_list, false),
        workorderAddBtn: this.validData(this.permission.property_workorder_add, false),
        workorderEditBtn: this.validData(this.permission.property_workorder_edit, false),
        workorderDelBtn: this.validData(this.permission.property_workorder_delete, false),
        workorderViewBtn: this.validData(this.permission.property_workorder_view, false),
        workorderAssignBtn: this.validData(this.permission.property_workorder_assign, false),
        workorderFinishBtn: this.validData(this.permission.property_workorder_finish, false),
        workorderCloseBtn: this.validData(this.permission.property_workorder_close, false),
        workorderRateBtn: this.validData(this.permission.property_workorder_rate, false),
      };
    },
  },
  created() {
    this.loadServicePage();
    this.loadServiceOptions();
    this.loadMinePage();
    this.loadProcessPage();
    this.loadUserOptions();
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
    statusText(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    statusTagType(value) {
      return { 0: 'warning', 1: 'primary', 2: 'success', 3: 'success', 4: 'info' }[value] || 'info';
    },
    priorityText(value) {
      const item = this.priorityOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    priorityTagType(value) {
      return { 0: 'danger', 1: 'primary', 2: 'info' }[value] || 'info';
    },
    needsProcess(status) {
      return status === '1' || status === '2';
    },
    handleTabClick() {
      if (this.activeTab === 'service') {
        this.loadServicePage();
      } else if (this.activeTab === 'mine') {
        this.loadMinePage();
      } else {
        this.loadProcessPage();
      }
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
    loadServiceOptions() {
      getServiceList({ status: '0' }).then(res => {
        const data = this.responseData(res);
        this.serviceOptions = Array.isArray(data) ? data : [];
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
          this.loadServiceOptions();
        });
      });
    },
    toggleServiceStatus(row, checked) {
      updateService({ serviceId: row.serviceId, serviceName: row.serviceName, serviceType: row.serviceType, status: checked ? '0' : '1' }).then(() => {
        ElMessage.success('操作成功');
        this.loadServicePage();
        this.loadServiceOptions();
      });
    },
    deleteService(row) {
      ElMessageBox.confirm(`确定删除服务项「${row.serviceName}」吗？`, '提示', {
        type: 'warning',
      }).then(() => removeService(row.serviceId)).then(() => {
        ElMessage.success('删除成功');
        this.loadServicePage();
        this.loadServiceOptions();
      });
    },
    buildWorkorderQuery(baseQuery, mine) {
      return {
        ...baseQuery,
        mine,
      };
    },
    loadMinePage() {
      this.mineLoading = true;
      const params = this.buildWorkorderQuery(this.mineQuery, true);
      getWorkorderPage(this.minePage.currentPage, this.minePage.pageSize, params).then(res => {
        const data = this.responseData(res) || {};
        this.mineData = this.normalizeRecords(data);
        this.minePage.total = Number(data.total || 0);
      }).finally(() => {
        this.mineLoading = false;
      });
      this.loadMineStatistics();
    },
    loadMineStatistics() {
      getWorkorderStatistics(this.buildWorkorderQuery(this.mineQuery, true)).then(res => {
        this.mineStatistics = this.responseData(res) || {};
      });
    },
    loadProcessPage() {
      this.processLoading = true;
      getWorkorderPage(this.processPage.currentPage, this.processPage.pageSize, this.processQuery).then(res => {
        const data = this.responseData(res) || {};
        this.processData = this.normalizeRecords(data);
        this.processPage.total = Number(data.total || 0);
      }).finally(() => {
        this.processLoading = false;
      });
      this.loadProcessStatistics();
    },
    loadProcessStatistics() {
      getWorkorderStatistics(this.processQuery).then(res => {
        this.processStatistics = this.responseData(res) || {};
      });
    },
    searchMine() {
      this.minePage.currentPage = 1;
      this.loadMinePage();
    },
    resetMineSearch() {
      this.mineQuery = { mine: true };
      this.searchMine();
    },
    changeMineSize(size) {
      this.minePage.pageSize = size;
      this.loadMinePage();
    },
    changeMineCurrent(current) {
      this.minePage.currentPage = current;
      this.loadMinePage();
    },
    searchProcess() {
      this.processPage.currentPage = 1;
      this.loadProcessPage();
    },
    resetProcessSearch() {
      this.processQuery = {};
      this.searchProcess();
    },
    changeProcessSize(size) {
      this.processPage.pageSize = size;
      this.loadProcessPage();
    },
    changeProcessCurrent(current) {
      this.processPage.currentPage = current;
      this.loadProcessPage();
    },
    openWorkorderDialog(service) {
      const selected = service || {};
      this.workorderForm = {
        serviceId: selected.serviceId,
        parkId: selected.parkId || 1,
        priority: '1',
        orderStatus: '0',
      };
      this.workorderDialogVisible = true;
      if (!this.serviceOptions.length) {
        this.loadServiceOptions();
      }
    },
    closeWorkorderDialog() {
      this.workorderDialogVisible = false;
      this.workorderForm = {};
      this.$nextTick(() => this.$refs.workorderFormRef && this.$refs.workorderFormRef.clearValidate());
    },
    handleServiceChange(serviceId) {
      const service = this.serviceOptions.find(item => String(item.serviceId) === String(serviceId));
      if (service && service.parkId) {
        this.workorderForm.parkId = service.parkId;
      }
    },
    submitWorkorderForm() {
      this.$refs.workorderFormRef.validate(valid => {
        if (!valid) return;
        submitWorkorder(this.workorderForm).then(() => {
          ElMessage.success('工单创建成功');
          this.closeWorkorderDialog();
          this.reloadWorkorders();
        });
      });
    },
    openDisposeDialog(row) {
      this.disposeForm = {
        orderId: row.orderId,
        orderNo: row.orderNo,
        priority: row.priority || '1',
        orderStatus: row.orderStatus || '0',
        assignTo: row.assignTo || '',
        disposalContent: row.disposalContent || '',
      };
      this.disposeDialogVisible = true;
    },
    closeDisposeDialog() {
      this.disposeDialogVisible = false;
      this.disposeForm = {};
      this.$nextTick(() => this.$refs.disposeFormRef && this.$refs.disposeFormRef.clearValidate());
    },
    submitDisposeForm() {
      this.$refs.disposeFormRef.validate(valid => {
        if (!valid) return;
        const api = this.disposeForm.orderStatus === '4' ? closeWorkorder : updateWorkorder;
        api(this.disposeForm).then(() => {
          ElMessage.success('处置成功');
          this.closeDisposeDialog();
          this.reloadWorkorders();
        });
      });
    },
    openRateDialog(row) {
      this.rateForm = {
        orderId: row.orderId,
        rating: row.rating || 5,
        ratingContent: row.ratingContent || '',
      };
      this.rateDialogVisible = true;
    },
    closeRateDialog() {
      this.rateDialogVisible = false;
      this.rateForm = {};
      this.$nextTick(() => this.$refs.rateFormRef && this.$refs.rateFormRef.clearValidate());
    },
    submitRateForm() {
      this.$refs.rateFormRef.validate(valid => {
        if (!valid) return;
        rateWorkorder(this.rateForm).then(() => {
          ElMessage.success('评价成功');
          this.closeRateDialog();
          this.reloadWorkorders();
        });
      });
    },
    openDetail(row) {
      this.detailVisible = true;
      this.currentOrder = row;
      this.logs = [];
      this.logLoading = true;
      getWorkorderDetail(row.orderId).then(res => {
        this.currentOrder = this.responseData(res) || row;
        return getWorkorderLog(row.orderId);
      }).then(res => {
        const data = this.responseData(res);
        this.logs = Array.isArray(data) ? data : [];
      }).finally(() => {
        this.logLoading = false;
      });
    },
    deleteWorkorder(row) {
      ElMessageBox.confirm(`确定删除工单「${row.orderNo}」吗？`, '提示', {
        type: 'warning',
      }).then(() => removeWorkorder(row.orderId)).then(() => {
        ElMessage.success('删除成功');
        if (this.currentOrder && String(this.currentOrder.orderId) === String(row.orderId)) {
          this.detailVisible = false;
          this.currentOrder = null;
        }
        this.reloadWorkorders();
      });
    },
    reloadWorkorders() {
      this.loadMinePage();
      this.loadProcessPage();
    },
    loadUserOptions() {
      getUserList(1, 500, { status: 1 }).then(res => {
        const data = this.responseData(res) || {};
        this.userOptions = Array.isArray(data.records) ? data.records : [];
      }).catch(() => {
        this.userOptions = [];
      });
    },
  },
};
</script>

<style scoped>
.property-service-page {
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

.workorder-stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(110px, 1fr));
  gap: 12px;
  margin: 4px 0 14px;
}

.stat-card {
  border: 1px solid #edf0f5;
  border-radius: 6px;
  padding: 12px 14px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.stat-card span {
  display: block;
  color: #687386;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  color: #1f2d3d;
  font-size: 24px;
  font-weight: 600;
}

.stat-total {
  border-top: 3px solid #2f6fed;
}

.stat-pending {
  border-top: 3px solid #e6a23c;
}

.stat-processing {
  border-top: 3px solid #409eff;
}

.stat-finished {
  border-top: 3px solid #67c23a;
}

.stat-rated {
  border-top: 3px solid #8e7cc3;
}

.stat-closed {
  border-top: 3px solid #909399;
}

.log-title {
  margin: 18px 0 10px;
  color: #1f2d3d;
  font-size: 15px;
  font-weight: 600;
}

@media (max-width: 1180px) {
  .toolbar-row {
    display: block;
  }

  .toolbar-actions {
    justify-content: flex-end;
    margin-top: 8px;
  }

  .workorder-stats {
    grid-template-columns: repeat(3, minmax(110px, 1fr));
  }
}

@media (max-width: 720px) {
  .workorder-stats {
    grid-template-columns: repeat(2, minmax(110px, 1fr));
  }
}
</style>
