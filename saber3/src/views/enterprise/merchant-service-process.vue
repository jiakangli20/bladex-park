<template>
  <basic-container>
    <div class="merchant-service-process-page">
      <stat-cards :items="summaryCards" />

      <section class="contract-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="服务单号">
            <el-input v-model="query.orderNo" clearable placeholder="请输入服务单号" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="服务商">
            <el-input v-model="query.merchantName" clearable placeholder="请输入服务商名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="客户名称">
            <el-input v-model="query.customerName" clearable placeholder="请输入客户名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="处理状态">
            <el-select v-model="query.orderStatus" clearable placeholder="全部状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="优先级">
            <el-select v-model="query.priority" clearable placeholder="全部优先级">
              <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="service-table-card">
        <div class="contract-toolbar">
          <div class="toolbar-left">
            <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openCreate">录入服务申请</el-button>
          </div>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </div>

        <el-table
          ref="table"
          v-loading="loading"
          :data="data"
          border
          row-key="orderId"
          scrollbar-always-on
          class="contract-table"
        >
        <el-table-column prop="orderNo" label="服务单号" width="172" align="center" show-overflow-tooltip />
        <el-table-column prop="merchantName" label="服务商" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="serviceType" label="服务类型" width="116" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">{{ businessTypeText(row.serviceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="联系人" width="150" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.contactName || '-' }} {{ row.contactPhone || '' }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityTagType(row.priority)" effect="plain">{{ priorityText(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderStatus" label="处理状态" width="104" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.orderStatus)" effect="plain">{{ statusText(row.orderStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignTo" label="处理人" width="110" align="center" show-overflow-tooltip />
          <el-table-column prop="nextFollowTime" label="下次跟进" width="180" align="center">
            <template #default="{ row }"><span class="single-line-cell">{{ row.nextFollowTime || '-' }}</span></template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="180" align="center">
            <template #default="{ row }"><span class="single-line-cell">{{ row.createTime || '-' }}</span></template>
          </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="permissionList.viewBtn" type="primary" text @click="openDetail(row)">详情</el-button>
              <el-button v-if="canFollow(row)" type="primary" text @click="openFollow(row)">处理</el-button>
              <el-dropdown v-if="canDeal(row) || canClose(row) || permissionList.delBtn" trigger="click">
                <el-button type="primary" text icon="el-icon-more">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="canDeal(row)" @click="openDeal(row)">成交</el-dropdown-item>
                    <el-dropdown-item v-if="canClose(row)" @click="openClose(row)">关闭</el-dropdown-item>
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
      v-model="createVisible"
      title="录入服务申请"
      width="760px"
      append-to-body
      :before-close="closeCreate"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="104px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="服务商" prop="merchantId">
              <el-select v-model="createForm.merchantId" filterable clearable placeholder="请选择服务商" style="width: 100%" @change="handleMerchantChange">
                <el-option
                  v-for="item in merchantOptions"
                  :key="item.merchantId"
                  :label="`${item.merchantName} / ${businessTypeText(item.businessType)}`"
                  :value="item.merchantId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="createForm.priority" style="width: 100%">
                <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="customerName">
              <el-input v-model="createForm.customerName" maxlength="200" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="园区ID">
              <el-input-number v-model="createForm.parkId" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="createForm.contactName" maxlength="100" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="createForm.contactPhone" maxlength="50" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="服务范围">
          <el-input v-model="createForm.serviceScope" maxlength="500" placeholder="选择服务商后自动带出，可手动调整" />
        </el-form-item>
        <el-form-item label="需求描述" prop="demandDesc">
          <el-input v-model="createForm.demandDesc" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入服务需求" />
        </el-form-item>
        <el-form-item label="需求图片">
          <el-input v-model="createForm.demandImages" maxlength="1000" placeholder="小程序可传图片地址，多个地址用英文逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeCreate">取消</el-button>
        <el-button type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="followVisible"
      title="处理服务单"
      width="640px"
      append-to-body
      :before-close="closeFollow"
    >
      <el-form ref="followFormRef" :model="followForm" :rules="followRules" label-width="104px">
        <el-form-item label="服务单号">
          <el-input v-model="followForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="处理人" prop="assignTo">
          <assignee-select v-model="followForm.assignTo" :users="userOptions" />
        </el-form-item>
        <el-form-item label="处理进展" prop="processContent">
          <el-input v-model="followForm.processContent" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请输入处理进展，小程序端会同步展示" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="followForm.nextFollowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择下次跟进时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeFollow">取消</el-button>
        <el-button type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitFollow">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dealVisible"
      title="服务成交"
      width="560px"
      append-to-body
      :before-close="closeDeal"
    >
      <el-form ref="dealFormRef" :model="dealForm" :rules="dealRules" label-width="104px">
        <el-form-item label="服务单号">
          <el-input v-model="dealForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="成交金额">
          <el-input-number v-model="dealForm.dealAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="成交说明" prop="processContent">
          <el-input v-model="dealForm.processContent" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="请输入成交说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeDeal">取消</el-button>
        <el-button type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitDeal">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="closeVisible"
      title="关闭服务单"
      width="560px"
      append-to-body
      :before-close="closeClose"
    >
      <el-form ref="closeFormRef" :model="closeForm" :rules="closeRules" label-width="104px">
        <el-form-item label="服务单号">
          <el-input v-model="closeForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="关闭原因" prop="closeReason">
          <el-input v-model="closeForm.closeReason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入关闭原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeClose">取消</el-button>
        <el-button type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitClose">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="服务处理详情" size="720px" append-to-body>
      <template v-if="currentOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="服务单号">{{ currentOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentOrder.orderStatus)" effect="plain">{{ statusText(currentOrder.orderStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务商">{{ currentOrder.merchantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务类型">{{ businessTypeText(currentOrder.serviceType) }}</el-descriptions-item>
          <el-descriptions-item label="客户名称">{{ currentOrder.customerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentOrder.contactName || '-' }} {{ currentOrder.contactPhone || '' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ currentOrder.assignTo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下次跟进">{{ currentOrder.nextFollowTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="成交金额">{{ currentOrder.dealAmount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="成交时间">{{ currentOrder.dealTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="服务范围" :span="2">{{ currentOrder.serviceScope || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求描述" :span="2">{{ currentOrder.demandDesc || '-' }}</el-descriptions-item>
          <el-descriptions-item label="需求图片" :span="2">{{ currentOrder.demandImages || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理进展" :span="2">{{ currentOrder.processContent || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关闭原因" :span="2">{{ currentOrder.closeReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="log-title">处理进展</div>
        <el-table v-loading="logLoading" :data="logs" border row-key="logId" scrollbar-always-on class="service-log-table">
          <el-table-column prop="operateTime" label="时间" width="180" align="center" />
          <el-table-column prop="action" label="操作" width="110" align="center">
            <template #default="{ row }">{{ actionText(row.action) }}</template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="120" align="center" />
          <el-table-column prop="actionDesc" label="说明" min-width="220" align="center" show-overflow-tooltip />
        </el-table>
      </template>
    </el-drawer>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { getList as getUserList } from '@/api/system/user';
import AssigneeSelect from './components/assignee-select.vue';
import { getMerchantList } from '@/api/business/merchant';
import {
  addServiceOrder,
  closeServiceOrder,
  dealServiceOrder,
  followServiceOrder,
  getServiceOrderDetail,
  getServiceOrderLog,
  getServiceOrderPage,
  getServiceOrderStatistics,
  removeServiceOrder,
} from '@/api/business/merchant-service-order';

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
  { value: '0', label: '待受理', type: 'warning' },
  { value: '1', label: '跟进中', type: 'primary' },
  { value: '2', label: '已成交', type: 'success' },
  { value: '3', label: '已关闭', type: 'info' },
];

const priorityOptions = [
  { value: '0', label: '紧急', type: 'danger' },
  { value: '1', label: '普通', type: 'primary' },
  { value: '2', label: '低', type: 'info' },
];

const defaultCreateForm = () => ({
  merchantId: null,
  merchantName: '',
  serviceType: '',
  serviceScope: '',
  customerName: '',
  customerId: null,
  contactName: '',
  contactPhone: '',
  parkId: null,
  demandDesc: '',
  demandImages: '',
  priority: '1',
});

export default {
  name: 'EnterpriseMerchantServiceProcess',
  components: {
    AssigneeSelect,
  },
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
      summaryCards: [
        { key: 'totalCount', label: '服务单总数', value: 0 },
        { key: 'pendingCount', label: '待受理', value: 0 },
        { key: 'processingCount', label: '跟进中', value: 0 },
        { key: 'dealCount', label: '已成交', value: 0 },
        { key: 'closedCount', label: '已关闭', value: 0 },
      ],
      businessTypeOptions,
      statusOptions,
      priorityOptions,
      merchantOptions: [],
      userOptions: [],
      createVisible: false,
      createForm: defaultCreateForm(),
      createRules: {
        merchantId: [{ required: true, message: '请选择服务商', trigger: 'change' }],
        customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
        contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        demandDesc: [{ required: true, message: '请输入需求描述', trigger: 'blur' }],
        priority: [{ required: true, message: '请选择优先级', trigger: 'change' }],
      },
      followVisible: false,
      followForm: {},
      followRules: {
        assignTo: [{ required: true, message: '请选择处理人', trigger: 'change' }],
        processContent: [{ required: true, message: '请输入处理进展', trigger: 'blur' }],
      },
      dealVisible: false,
      dealForm: {},
      dealRules: {
        processContent: [{ required: true, message: '请输入成交说明', trigger: 'blur' }],
      },
      closeVisible: false,
      closeForm: {},
      closeRules: {
        closeReason: [{ required: true, message: '请输入关闭原因', trigger: 'blur' }],
      },
      detailVisible: false,
      currentOrder: null,
      logs: [],
      logLoading: false,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.merchant_service_process_add, false),
        editBtn: this.validData(this.permission.merchant_service_process_edit, false),
        delBtn: this.validData(this.permission.merchant_service_process_delete, false),
        viewBtn: this.validData(this.permission.merchant_service_process_view, false),
        assignBtn: this.validData(this.permission.merchant_service_process_assign, false),
        followBtn: this.validData(this.permission.merchant_service_process_follow, false),
        dealBtn: this.validData(this.permission.merchant_service_process_deal, false),
        closeBtn: this.validData(this.permission.merchant_service_process_close, false),
      };
    },
  },
  created() {
    this.loadMerchantOptions();
    this.loadUserOptions();
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
    priorityText(value) {
      const item = this.priorityOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    priorityTagType(value) {
      const item = this.priorityOptions.find(option => option.value === value);
      return item ? item.type : '';
    },
    actionText(value) {
      return {
        create: '创建',
        assign: '指派',
        follow: '跟进',
        deal: '成交',
        close: '关闭',
      }[value] || value || '-';
    },
    canFollow(row) {
      return this.permissionList.followBtn && row.orderStatus !== '2' && row.orderStatus !== '3';
    },
    canDeal(row) {
      return this.permissionList.dealBtn && row.orderStatus !== '2' && row.orderStatus !== '3';
    },
    canClose(row) {
      return this.permissionList.closeBtn && row.orderStatus !== '2' && row.orderStatus !== '3';
    },
    loadMerchantOptions() {
      getMerchantList({ status: '0' }).then(res => {
        this.merchantOptions = res.data.data || [];
      });
    },
    loadUserOptions() {
      getUserList(1, 999, {}).then(res => {
        const payload = res.data.data || {};
        this.userOptions = payload.records || [];
      });
    },
    loadStatistics() {
      getServiceOrderStatistics({ ...this.query }).then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getServiceOrderPage(page.currentPage, page.pageSize, {
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
    handleMerchantChange(merchantId) {
      const merchant = this.merchantOptions.find(item => item.merchantId === merchantId);
      if (!merchant) return;
      this.createForm.merchantName = merchant.merchantName;
      this.createForm.serviceType = merchant.businessType;
      this.createForm.serviceScope = merchant.serviceScope;
      this.createForm.parkId = merchant.parkId;
    },
    openCreate() {
      this.createForm = defaultCreateForm();
      this.createVisible = true;
    },
    closeCreate() {
      this.createVisible = false;
      this.submitLoading = false;
      this.createForm = defaultCreateForm();
    },
    submitCreate() {
      this.$refs.createFormRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        addServiceOrder(this.createForm)
          .then(() => {
            this.$message.success('服务申请已提交');
            this.closeCreate();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    openFollow(row) {
      this.followForm = {
        orderId: row.orderId,
        orderNo: row.orderNo,
        assignTo: row.assignTo || '',
        processContent: '',
        nextFollowTime: row.nextFollowTime || '',
      };
      this.followVisible = true;
    },
    closeFollow() {
      this.followVisible = false;
      this.submitLoading = false;
      this.followForm = {};
    },
    submitFollow() {
      this.$refs.followFormRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        followServiceOrder(this.followForm)
          .then(() => {
            this.$message.success('处理进展已同步');
            this.closeFollow();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    openDeal(row) {
      this.dealForm = {
        orderId: row.orderId,
        orderNo: row.orderNo,
        dealAmount: row.dealAmount || 0,
        processContent: '服务已成交',
      };
      this.dealVisible = true;
    },
    closeDeal() {
      this.dealVisible = false;
      this.submitLoading = false;
      this.dealForm = {};
    },
    submitDeal() {
      this.$refs.dealFormRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        dealServiceOrder(this.dealForm)
          .then(() => {
            this.$message.success('服务单已成交');
            this.closeDeal();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    openClose(row) {
      this.closeForm = {
        orderId: row.orderId,
        orderNo: row.orderNo,
        closeReason: '',
      };
      this.closeVisible = true;
    },
    closeClose() {
      this.closeVisible = false;
      this.submitLoading = false;
      this.closeForm = {};
    },
    submitClose() {
      this.$refs.closeFormRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        closeServiceOrder(this.closeForm)
          .then(() => {
            this.$message.success('服务单已关闭');
            this.closeClose();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    removeRow(row) {
      this.$confirm('确定删除该服务单?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        removeServiceOrder(String(row.orderId)).then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
      });
    },
    openDetail(row) {
      this.detailVisible = true;
      this.currentOrder = null;
      this.logs = [];
      getServiceOrderDetail(row.orderId).then(res => {
        this.currentOrder = res.data.data || {};
      });
      this.logLoading = true;
      getServiceOrderLog(row.orderId)
        .then(res => {
          this.logs = res.data.data || [];
        })
        .finally(() => {
          this.logLoading = false;
        });
    },
  },
};
</script>

<style scoped>
.merchant-service-process-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contract-search,
.service-table-card {
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
}

.service-table-card {
  overflow: hidden;
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

.contract-table :deep(.el-table__header th),
.contract-table :deep(.el-table__cell),
.contract-table :deep(.cell),
.service-log-table :deep(.el-table__header th),
.service-log-table :deep(.el-table__cell),
.service-log-table :deep(.cell) {
  text-align: center;
}

.contract-table :deep(.el-scrollbar__bar.is-horizontal),
.service-log-table :deep(.el-scrollbar__bar.is-horizontal) {
  bottom: 2px;
  height: 8px;
  opacity: 1;
}

.contract-table :deep(.el-scrollbar__bar.is-horizontal .el-scrollbar__thumb),
.service-log-table :deep(.el-scrollbar__bar.is-horizontal .el-scrollbar__thumb) {
  background-color: #aeb5c2;
}

.single-line-cell {
  display: inline-block;
  white-space: nowrap;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 16px;
}

.log-title {
  margin: 18px 0 10px;
  color: #111827;
  font-size: 15px;
  font-weight: 600;
}

.merchant-service-process-page :deep(.el-button),
.merchant-service-process-page :deep(.el-input__wrapper),
.merchant-service-process-page :deep(.el-select__wrapper),
.merchant-service-process-page :deep(.el-input-number .el-input__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1180px) {
  .toolbar-left {
    flex-wrap: wrap;
  }
}
</style>
