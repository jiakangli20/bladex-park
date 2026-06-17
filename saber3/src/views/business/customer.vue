<template>
  <basic-container>
    <div class="customer-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="customer-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" clearable placeholder="企业/联系人/电话" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="所属园区">
            <el-select v-model="query.parkId" clearable placeholder="全部园区">
              <el-option v-for="park in parkList" :key="park.id" :label="park.name" :value="park.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="入驻状态">
            <el-select v-model="query.settlementStatus" clearable placeholder="全部状态">
              <el-option v-for="item in settlementOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="客户状态">
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

      <section class="customer-toolbar">
        <span>客户管理</span>
        <div>
          <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openAdd">新增</el-button>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </div>
      </section>

      <el-table v-loading="loading" :data="data" border row-key="customerId" class="customer-table">
        <el-table-column prop="enterpriseName" label="企业名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button text type="primary" @click="openDetail(row)">{{ row.enterpriseName || '-' }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="parkId" label="所属园区" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ parkName(row.parkId) }}</template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="settlementStatus" label="入驻状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="settlementType(row.settlementStatus)" effect="plain">
              {{ settlementText(row.settlementStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="客户状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="riskType(row.riskLevel)" effect="plain">{{ riskText(row.riskLevel) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="客户标签" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag
                v-for="tag in row.tags || []"
                :key="tag.tagId"
                size="small"
                effect="plain"
                :style="{ borderColor: tag.tagColor || '#1059C6', color: tag.tagColor || '#1059C6' }"
              >
                {{ tag.tagName }}
              </el-tag>
              <span v-if="!row.tags || row.tags.length === 0" class="muted">未设置</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="340" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="permissionList.viewBtn" text type="primary" icon="el-icon-view" @click="openDetail(row)">
              查看
            </el-button>
            <el-button v-if="permissionList.editBtn" text type="primary" icon="el-icon-edit" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-if="permissionList.tagBtn" text type="primary" icon="el-icon-collection-tag" @click="openTags(row)">
              标签
            </el-button>
            <el-dropdown v-if="permissionList.statusBtn || permissionList.checkBtn || permissionList.delBtn" trigger="click">
              <el-button text type="primary">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="permissionList.checkBtn" @click="checkRow(row)">核验</el-dropdown-item>
                  <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '0'" @click="changeStatus(row, '0')">
                    启用
                  </el-dropdown-item>
                  <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '1'" @click="changeStatus(row, '1')">
                    停用
                  </el-dropdown-item>
                  <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '2'" @click="changeStatus(row, '2')">
                    归档
                  </el-dropdown-item>
                  <el-dropdown-item v-if="permissionList.delBtn" divided @click="removeRow(row)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="customer-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-sizes="[10, 20, 30, 50, 100]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>

      <el-dialog v-model="formVisible" :title="formTitle" width="820px" append-to-body>
        <el-form ref="customerForm" :model="form" :rules="rules" label-width="110px">
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="企业名称" prop="enterpriseName">
                <el-input v-model="form.enterpriseName" maxlength="200" placeholder="请输入企业名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="统一信用代码">
                <el-input v-model="form.creditCode" maxlength="50" placeholder="请输入统一信用代码" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="所属园区" prop="parkId">
                <el-select v-model="form.parkId" placeholder="请选择所属园区">
                  <el-option v-for="park in parkList" :key="park.id" :label="park.name" :value="park.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="行业类型">
                <el-input v-model="form.industry" maxlength="50" placeholder="请输入行业类型" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactName">
                <el-input v-model="form.contactName" maxlength="100" placeholder="请输入联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="form.contactPhone" maxlength="20" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="入驻状态">
                <el-select v-model="form.settlementStatus" placeholder="请选择入驻状态">
                  <el-option v-for="item in settlementOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="客户状态">
                <el-select v-model="form.status" placeholder="请选择客户状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="企业规模">
                <el-input v-model="form.scale" maxlength="20" placeholder="请输入企业规模" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合作等级">
                <el-select v-model="form.cooperationLevel" placeholder="请选择合作等级">
                  <el-option v-for="item in cooperationOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="注册地址">
                <el-input v-model="form.registeredAddress" maxlength="500" placeholder="请输入注册地址" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="经营范围">
                <el-input v-model="form.businessScope" type="textarea" :rows="3" maxlength="2000" placeholder="请输入经营范围" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="客户标签">
                <customer-tag-selector v-model="form.tagIds" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" placeholder="请输入备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <template #footer>
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
        </template>
      </el-dialog>

      <el-drawer v-model="detailVisible" :title="detailTitle" size="68%" append-to-body>
        <div v-loading="detailLoading" class="customer-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="企业名称">{{ current.enterpriseName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属园区">{{ parkName(current.parkId) }}</el-descriptions-item>
            <el-descriptions-item label="统一信用代码">{{ current.creditCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业类型">{{ current.industry || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ current.contactName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ current.contactPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="入驻状态">{{ settlementText(current.settlementStatus) }}</el-descriptions-item>
            <el-descriptions-item label="客户状态">{{ statusText(current.status) }}</el-descriptions-item>
            <el-descriptions-item label="基础核验">{{ verifyText(current.verifyStatus) }}</el-descriptions-item>
            <el-descriptions-item label="行业准入">{{ accessText(current.industryAccessResult) }}</el-descriptions-item>
            <el-descriptions-item label="风险等级">{{ riskText(current.riskLevel) }}</el-descriptions-item>
            <el-descriptions-item label="合作等级">{{ cooperationText(current.cooperationLevel) }}</el-descriptions-item>
            <el-descriptions-item label="经营范围" :span="2">{{ current.businessScope || '-' }}</el-descriptions-item>
            <el-descriptions-item label="核验说明" :span="2">{{ current.verifyMessage || '-' }}</el-descriptions-item>
            <el-descriptions-item label="准入说明" :span="2">{{ current.industryAccessReason || '-' }}</el-descriptions-item>
            <el-descriptions-item label="风险摘要" :span="2">{{ current.riskSummary || '-' }}</el-descriptions-item>
          </el-descriptions>
          <section class="detail-tags">
            <h4>客户标签</h4>
            <div class="tag-list">
              <el-tag
                v-for="tag in current.tags || []"
                :key="tag.tagId"
                effect="plain"
                :style="{ borderColor: tag.tagColor || '#1059C6', color: tag.tagColor || '#1059C6' }"
              >
                {{ tag.tagName }}
              </el-tag>
              <el-empty v-if="!current.tags || current.tags.length === 0" description="暂无客户标签" />
            </div>
          </section>
        </div>
      </el-drawer>

      <el-dialog v-model="tagVisible" title="设置客户标签" width="720px" append-to-body>
        <customer-tag-selector v-model="tagForm.tagIds" />
        <template #footer>
          <el-button @click="tagVisible = false">取消</el-button>
          <el-button type="primary" :loading="tagLoading" @click="submitTags">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import {
  addCustomer,
  changeCustomerStatus,
  checkCustomer,
  getCustomerDetail,
  getCustomerList,
  getCustomerStatistics,
  removeCustomer,
  setCustomerTags,
  updateCustomer,
} from '@/api/business/customer';
import { getList as getParkList } from '@/api/park/park';
import CustomerTagSelector from '@/views/business/modules/customer-tag-selector.vue';

const statusOptions = [
  { value: '0', label: '正常', type: 'success' },
  { value: '1', label: '停用', type: 'warning' },
  { value: '2', label: '归档', type: 'info' },
];

const settlementOptions = [
  { value: 0, label: '未入驻', type: 'info' },
  { value: 1, label: '意向', type: 'warning' },
  { value: 2, label: '签约', type: 'primary' },
  { value: 3, label: '入驻', type: 'success' },
];

const cooperationOptions = [
  { value: 1, label: '普通' },
  { value: 2, label: 'VIP' },
  { value: 3, label: '战略' },
];

const createDefaultForm = () => ({
  customerId: null,
  enterpriseName: '',
  creditCode: '',
  parkId: undefined,
  industry: '',
  contactName: '',
  contactPhone: '',
  settlementStatus: 0,
  status: '0',
  cooperationLevel: 1,
  scale: '',
  registeredAddress: '',
  businessScope: '',
  remark: '',
  tagIds: [],
});

export default {
  name: 'BusinessCustomer',
  components: {
    CustomerTagSelector,
  },
  data() {
    return {
      loading: false,
      detailLoading: false,
      submitLoading: false,
      tagLoading: false,
      data: [],
      parkList: [],
      query: {},
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      summaryCards: [
        { key: 'totalCount', label: '客户总数', value: 0 },
        { key: 'settledCount', label: '已入驻', value: 0 },
        { key: 'normalCount', label: '正常客户', value: 0 },
        { key: 'monthNewCount', label: '本月新增', value: 0 },
      ],
      formVisible: false,
      detailVisible: false,
      tagVisible: false,
      current: {},
      form: createDefaultForm(),
      tagForm: {
        customerId: null,
        tagIds: [],
      },
      rules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        parkId: [{ required: true, message: '请选择所属园区', trigger: 'change' }],
        contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '联系电话必须为合法手机号', trigger: 'blur' },
        ],
      },
      statusOptions,
      settlementOptions,
      cooperationOptions,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.settlement_customer_add, false),
        editBtn: this.validData(this.permission.settlement_customer_edit, false),
        delBtn: this.validData(this.permission.settlement_customer_delete, false),
        viewBtn: this.validData(this.permission.settlement_customer_view, false),
        statusBtn: this.validData(this.permission.settlement_customer_status, false),
        tagBtn: this.validData(this.permission.settlement_customer_tag, false),
        checkBtn: this.validData(this.permission.settlement_customer_check, false),
      };
    },
    formTitle() {
      return this.form.customerId ? '编辑客户' : '新增客户';
    },
    detailTitle() {
      return this.current.enterpriseName ? `客户详情 - ${this.current.enterpriseName}` : '客户详情';
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
    defaultForm() {
      return createDefaultForm();
    },
    loadParkList() {
      getParkList(1, 999, {}).then(res => {
        const result = res.data.data || {};
        this.parkList = result.records || [];
      });
    },
    loadStatistics() {
      getCustomerStatistics({ ...this.query }).then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getCustomerList(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = Number(result.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    reload() {
      this.onLoad(this.page);
      this.loadStatistics();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    openAdd() {
      this.form = this.defaultForm();
      this.formVisible = true;
      this.$nextTick(() => {
        if (this.$refs.customerForm) {
          this.$refs.customerForm.clearValidate();
        }
      });
    },
    openEdit(row) {
      getCustomerDetail(row.customerId).then(res => {
        const detail = res.data.data || {};
        this.form = {
          ...this.defaultForm(),
          ...detail,
          tagIds: detail.tagIds || [],
        };
        this.formVisible = true;
        this.$nextTick(() => {
          if (this.$refs.customerForm) {
            this.$refs.customerForm.clearValidate();
          }
        });
      });
    },
    openDetail(row) {
      this.detailVisible = true;
      this.detailLoading = true;
      getCustomerDetail(row.customerId)
        .then(res => {
          this.current = res.data.data || {};
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    openTags(row) {
      getCustomerDetail(row.customerId).then(res => {
        const detail = res.data.data || {};
        this.tagForm = {
          customerId: detail.customerId,
          tagIds: detail.tagIds || [],
        };
        this.tagVisible = true;
      });
    },
    submitForm() {
      this.$refs.customerForm.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const action = this.form.customerId ? updateCustomer : addCustomer;
        action(this.normalizePayload(this.form))
          .then(() => {
            this.$message.success('保存成功');
            this.formVisible = false;
            this.reload();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    submitTags() {
      if (!this.tagForm.customerId) return;
      this.tagLoading = true;
      setCustomerTags(this.tagForm.customerId, this.tagForm.tagIds)
        .then(() => {
          this.$message.success('标签已更新');
          this.tagVisible = false;
          this.reload();
        })
        .finally(() => {
          this.tagLoading = false;
        });
    },
    removeRow(row) {
      this.$confirm(`确定删除客户「${row.enterpriseName}」？`, '提示', { type: 'warning' })
        .then(() => removeCustomer(row.customerId))
        .then(() => {
          this.$message.success('删除成功');
          this.reload();
        });
    },
    changeStatus(row, status) {
      this.$confirm(`确定将客户状态改为「${this.statusText(status)}」？`, '提示', { type: 'warning' })
        .then(() => changeCustomerStatus(row.customerId, status))
        .then(() => {
          this.$message.success('状态已更新');
          this.reload();
        });
    },
    checkRow(row) {
      checkCustomer(row.customerId).then(() => {
        this.$message.success('核验完成');
        this.reload();
      });
    },
    normalizePayload(row) {
      return {
        ...row,
        creditCode: row.creditCode || null,
      };
    },
    parkName(parkId) {
      const park = this.parkList.find(item => String(item.id) === String(parkId));
      return park ? park.name : parkId || '-';
    },
    settlementText(value) {
      const item = this.settlementOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '-';
    },
    settlementType(value) {
      const item = this.settlementOptions.find(option => String(option.value) === String(value));
      return item ? item.type : 'info';
    },
    statusText(value) {
      const item = this.statusOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '-';
    },
    statusType(value) {
      const item = this.statusOptions.find(option => String(option.value) === String(value));
      return item ? item.type : 'info';
    },
    cooperationText(value) {
      const item = this.cooperationOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '-';
    },
    verifyText(value) {
      const map = {
        0: '未核验',
        1: '通过',
        2: '不通过',
      };
      return map[String(value)] || '-';
    },
    accessText(value) {
      const map = {
        0: '未判定',
        1: '通过',
        2: '限制',
        3: '禁入',
      };
      return map[String(value)] || '-';
    },
    riskText(value) {
      const map = {
        0: '未排查',
        1: '低',
        2: '中',
        3: '高',
      };
      return map[String(value)] || '-';
    },
    riskType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'danger',
      };
      return map[String(value)] || 'info';
    },
  },
};
</script>

<style lang="scss" scoped>
.customer-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
}

.summary-card {
  display: flex;
  min-height: 74px;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.summary-card span {
  color: #909399;
  font-size: 13px;
}

.summary-card strong {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.customer-search,
.customer-toolbar {
  background: #fff;
}

.customer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.customer-table {
  width: 100%;
}

.customer-pagination {
  display: flex;
  justify-content: flex-end;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.muted {
  color: #909399;
}

.customer-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-tags h4 {
  margin: 0 0 10px;
  color: #303133;
  font-size: 15px;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(120px, 1fr));
  }
}
</style>
