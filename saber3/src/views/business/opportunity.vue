<template>
  <basic-container>
    <div class="opportunity-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="opportunity-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="企业名/联系人"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input
              v-model="query.contactKeyword"
              clearable
              placeholder="联系人/电话"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.opportunityStatus" clearable placeholder="全部状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="提交审核">
            <el-select v-model="query.submittedAuditFlag" clearable placeholder="全部">
              <el-option label="未提交" value="0" />
              <el-option label="已提交" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item label="招商渠道">
            <el-select v-model="query.channel" clearable placeholder="全部渠道">
              <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="租期">
            <el-select v-model="query.leaseTermLabel" clearable placeholder="全部租期">
              <el-option v-for="item in leaseTermOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="opportunity-toolbar">
        <div>
          <el-button
            v-if="permissionList.addBtn"
            type="primary"
            icon="el-icon-plus"
            @click="handleAdd"
          >
            新增商机
          </el-button>
          <el-button
            v-if="permissionList.delBtn"
            type="danger"
            plain
            icon="el-icon-delete"
            :disabled="selectionList.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="onLoad(page)" />
        </el-tooltip>
      </section>

      <el-table
        v-loading="loading"
        :data="data"
        border
        row-key="opportunityId"
        class="opportunity-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="opportunityNo" label="商机编号" width="160" />
        <el-table-column prop="enterpriseName" label="企业名称" min-width="210" show-overflow-tooltip />
        <el-table-column prop="contactName" label="联系人" width="110" />
        <el-table-column prop="contactPhone" label="联系电话" width="140" />
        <el-table-column prop="channel" label="渠道" width="110" />
        <el-table-column prop="carrierTypes" label="意向载体" width="150">
          <template #default="{ row }">{{ formatCarrierTypes(row.carrierTypes) }}</template>
        </el-table-column>
        <el-table-column prop="intentArea" label="面积(㎡)" width="110" align="right" />
        <el-table-column prop="leaseTermLabel" label="租期" width="110" />
        <el-table-column prop="opportunityStatus" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.opportunityStatus] || ''" effect="plain">
              {{ statusTextMap[row.opportunityStatus] || row.opportunityStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submittedAuditFlag" label="审核" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.submittedAuditFlag === '1' ? 'success' : 'info'" effect="plain">
              {{ row.submittedAuditFlag === '1' ? '已提交' : '未提交' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="followUser" label="跟进人" width="110" />
        <el-table-column prop="lastFollowTime" label="最后跟进" width="170" />
        <el-table-column label="操作" width="340" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="permissionList.viewBtn"
              text
              type="primary"
              icon="el-icon-view"
              @click="handleView(row)"
            >
              查看
            </el-button>
            <el-button
              v-if="permissionList.editBtn && row.submittedAuditFlag !== '1'"
              text
              type="primary"
              icon="el-icon-edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="permissionList.followBtn"
              text
              type="primary"
              icon="el-icon-chat-line-square"
              @click="openFollow(row)"
            >
              跟进
            </el-button>
            <el-button
              v-if="permissionList.auditBtn && row.submittedAuditFlag !== '1'"
              text
              type="warning"
              icon="el-icon-upload"
              @click="handleSubmitAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="permissionList.delBtn"
              text
              type="danger"
              icon="el-icon-delete"
              @click="rowDel(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="opportunity-pagination">
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

      <el-drawer
        v-model="formDrawerVisible"
        :title="formTitle"
        size="76%"
        append-to-body
        :before-close="closeFormDrawer"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :disabled="view"
          label-width="118px"
          class="opportunity-form"
        >
          <section class="form-section">
            <header>跟进信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="跟进人" prop="followUser">
                  <el-input v-model="form.followUser" placeholder="请输入跟进人" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商机状态" prop="opportunityStatus">
                  <el-select v-model="form.opportunityStatus" style="width: 100%">
                    <el-option
                      v-for="item in statusOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </section>

          <section class="form-section">
            <header>企业基本信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="企业名称" prop="enterpriseName">
                  <el-input v-model="form.enterpriseName" placeholder="请输入企业名称">
                    <template #append>
                      <el-button :loading="backgroundLoading" @click.stop="handleBackgroundCheck">
                        背调
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="统一信用代码" prop="creditCode">
                  <el-input v-model="form.creditCode" placeholder="请输入统一信用代码" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="成立日期">
                  <el-date-picker
                    v-model="form.establishDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="注册资本">
                  <el-input-number v-model="form.registeredCapital" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="企业类型">
                  <el-input v-model="form.enterpriseType" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="行业类型">
                  <el-input v-model="form.industryType" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="经营范围">
              <el-input v-model="form.businessScope" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="注册地址">
              <el-input v-model="form.registeredAddress" />
            </el-form-item>
            <el-form-item label="客户标签">
              <customer-tag-selector v-model="form.tagIds" :disabled="view" />
            </el-form-item>
          </section>

          <section class="form-section">
            <header>经营状况信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="主营业务">
                  <el-input v-model="form.mainBusiness" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上年度营收">
                  <el-input-number v-model="form.lastYearRevenue" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="主要合作客户">
              <el-input v-model="form.majorClients" type="textarea" :rows="2" />
            </el-form-item>
            <el-row :gutter="18">
              <el-col :span="8">
                <el-form-item label="违法违规">
                  <el-radio-group v-model="form.majorIllegalFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="失信记录">
                  <el-radio-group v-model="form.dishonestFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="行业处罚">
                  <el-radio-group v-model="form.industryPenaltyFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </section>

          <section class="form-section">
            <header>入驻需求信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="意向载体" prop="carrierTypeArray">
                  <el-select v-model="form.carrierTypeArray" multiple style="width: 100%">
                    <el-option v-for="item in carrierTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="意向面积" prop="intentArea">
                  <el-input-number v-model="form.intentArea" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="使用用途" prop="usagePurpose">
                  <el-input v-model="form.usagePurpose" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租赁期限" prop="leaseTermYears">
                  <el-input-number
                    v-model="form.leaseTermYears"
                    :min="0"
                    :precision="2"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="装修要求">
              <el-input v-model="form.decorationRequirement" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="配套需求">
              <el-input v-model="form.supportingNeeds" type="textarea" :rows="2" />
            </el-form-item>
          </section>

          <section class="form-section">
            <header>联系人与招商信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="负责人姓名" prop="contactName">
                  <el-input v-model="form.contactName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话" prop="contactPhone">
                  <el-input v-model="form.contactPhone" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.contactEmail" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职务">
                  <el-input v-model="form.contactPosition" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="招商渠道" prop="channel">
                  <el-select v-model="form.channel" style="width: 100%">
                    <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第三方渠道">
                  <el-input v-model="form.thirdPartyChannelName" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </section>

          <section v-if="view" class="form-section">
            <header>跟进记录</header>
            <el-timeline v-if="followList.length">
              <el-timeline-item
                v-for="item in followList"
                :key="item.followId || item.createTime"
                :timestamp="item.followTime"
              >
                <div class="timeline-title">
                  {{ item.followUser || '-' }} ·
                  {{ statusTextMap[item.opportunityStatus] || item.opportunityStatus || '-' }}
                </div>
                <div class="timeline-text">{{ item.followContent || '-' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无跟进记录" />
          </section>

          <section v-if="view" class="form-section">
            <header>附件</header>
            <el-upload
              v-if="permissionList.fileUploadBtn"
              :show-file-list="false"
              :http-request="handleUploadFile"
            >
              <el-button icon="el-icon-upload" type="primary" plain>上传附件</el-button>
            </el-upload>
            <div v-if="fileList.length" class="file-list">
              <a v-for="item in fileList" :key="item.fileId" :href="item.fileUrl" target="_blank">
                {{ item.fileName || item.fileUrl }}
              </a>
            </div>
            <el-empty v-else description="暂无附件" />
          </section>
        </el-form>

        <template #footer>
          <div class="drawer-footer">
            <el-button @click="closeFormDrawer">关闭</el-button>
            <el-button v-if="!view" type="primary" :loading="submitLoading" @click="handleSubmit">
              保存
            </el-button>
          </div>
        </template>
      </el-drawer>

      <el-dialog
        v-model="followDialogVisible"
        title="商机跟进"
        width="520px"
        append-to-body
        :before-close="closeFollowDialog"
      >
        <el-form ref="followFormRef" :model="followForm" :rules="followRules" label-width="104px">
          <el-form-item label="商机状态" prop="opportunityStatus">
            <el-select v-model="followForm.opportunityStatus" style="width: 100%">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="跟进内容" prop="followContent">
            <el-input v-model="followForm.followContent" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item v-if="followForm.opportunityStatus !== 'DEAL'" label="下次跟进">
            <el-date-picker
              v-model="followForm.nextFollowTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="closeFollowDialog">取消</el-button>
          <el-button type="primary" :loading="followLoading" @click="handleSaveFollow">提交</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="backgroundVisible" title="背景调查" width="880px" append-to-body>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="企业名称">
            {{ backgroundData.enterpriseName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="是否命中">
            {{ backgroundData.found ? '是' : '否' }}
          </el-descriptions-item>
        </el-descriptions>
        <el-tabs v-model="backgroundActiveTab" class="background-tabs">
          <el-tab-pane label="涉诉信息" name="litigationList">
            <el-empty :description="emptyBackgroundText('litigationList')" />
          </el-tab-pane>
          <el-tab-pane label="被执行人" name="executorList">
            <el-empty :description="emptyBackgroundText('executorList')" />
          </el-tab-pane>
          <el-tab-pane label="处罚记录" name="penaltyList">
            <el-empty :description="emptyBackgroundText('penaltyList')" />
          </el-tab-pane>
          <el-tab-pane label="关联风险" name="relatedRiskList">
            <el-empty :description="emptyBackgroundText('relatedRiskList')" />
          </el-tab-pane>
        </el-tabs>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import {
  addOpportunity,
  addOpportunityFollow,
  getOpportunityBackgroundByName,
  getOpportunityDetail,
  getOpportunityFileList,
  getOpportunityFollowList,
  getOpportunityList,
  getOpportunityStatistics,
  removeOpportunity,
  submitOpportunityAudit,
  updateOpportunity,
  uploadOpportunityFile,
} from '@/api/business/opportunity';
import CustomerTagSelector from './modules/customer-tag-selector.vue';
import { mapGetters } from 'vuex';

const createDefaultForm = () => ({
  opportunityStatus: 'INITIAL',
  majorIllegalFlag: '0',
  dishonestFlag: '0',
  industryPenaltyFlag: '0',
  carrierTypeArray: [],
  tagIds: [],
});

export default {
  name: 'BusinessOpportunity',
  components: { CustomerTagSelector },
  data() {
    return {
      loading: false,
      submitLoading: false,
      followLoading: false,
      backgroundLoading: false,
      data: [],
      selectionList: [],
      query: {},
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      summaryCards: [
        { key: 'totalCount', label: '商机总数', value: 0 },
        { key: 'followCount', label: '跟进中', value: 0 },
        { key: 'dealCount', label: '已成交', value: 0 },
        { key: 'lostCount', label: '已流失', value: 0 },
      ],
      formDrawerVisible: false,
      formMode: 'add',
      form: createDefaultForm(),
      followList: [],
      fileList: [],
      currentRecord: null,
      followDialogVisible: false,
      followForm: {
        opportunityStatus: 'INITIAL',
        followContent: '',
        nextFollowTime: '',
      },
      backgroundVisible: false,
      backgroundActiveTab: 'litigationList',
      backgroundData: {},
      statusOptions: [
        { value: 'LEAD', label: '潜在线索' },
        { value: 'INITIAL', label: '初步沟通' },
        { value: 'DEEP', label: '深入洽谈' },
        { value: 'DEAL', label: '成交' },
        { value: 'LOST', label: '流失' },
      ],
      statusTextMap: {
        DRAFT: '初步沟通',
        AUDIT: '初步沟通',
        LEAD: '潜在线索',
        INITIAL: '初步沟通',
        DEEP: '深入洽谈',
        DEAL: '成交',
        LOST: '流失',
      },
      statusTypeMap: {
        LEAD: '',
        INITIAL: 'info',
        DEEP: 'warning',
        DEAL: 'success',
        LOST: 'danger',
      },
      channelOptions: ['自主招商', '中介推荐', '政府推荐', '第三方渠道', '线上咨询', '其他'],
      carrierTypeOptions: ['办公', '研发', '生产', '仓储', '商业', '配套'],
      leaseTermOptions: ['1年以内', '1-3年', '3-5年', '5年以上'],
      rules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        creditCode: [{ required: true, message: '请输入统一信用代码', trigger: 'blur' }],
        contactName: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }],
        contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        channel: [{ required: true, message: '请选择招商渠道', trigger: 'change' }],
        opportunityStatus: [{ required: true, message: '请选择商机状态', trigger: 'change' }],
      },
      followRules: {
        opportunityStatus: [{ required: true, message: '请选择商机状态', trigger: 'change' }],
        followContent: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    view() {
      return this.formMode === 'view';
    },
    formTitle() {
      const titleMap = {
        add: '新增商机',
        edit: '编辑商机',
        view: '商机详情',
      };
      return titleMap[this.formMode] || '商机';
    },
    permissionList() {
      return {
        addBtn: this.validData(this.permission.business_opportunity_add, false),
        editBtn: this.validData(this.permission.business_opportunity_edit, false),
        delBtn: this.validData(this.permission.business_opportunity_delete, false),
        viewBtn: this.validData(this.permission.business_opportunity_view, false),
        followBtn: this.validData(this.permission.business_opportunity_follow, false),
        auditBtn: this.validData(this.permission.business_opportunity_audit, false),
        fileUploadBtn: this.validData(this.permission.business_opportunity_file_upload, false),
      };
    },
    ids() {
      return this.selectionList.map(item => item.opportunityId).join(',');
    },
  },
  created() {
    this.loadStatistics();
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    loadStatistics() {
      getOpportunityStatistics().then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      const query = {
        ...this.query,
        ...params,
      };
      getOpportunityList(page.currentPage, page.pageSize, query)
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = Number(result.total) || 0;
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
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    handleAdd() {
      this.formMode = 'add';
      this.form = createDefaultForm();
      this.followList = [];
      this.fileList = [];
      this.formDrawerVisible = true;
    },
    handleEdit(row) {
      this.openForm('edit', row);
    },
    handleView(row) {
      this.openForm('view', row);
    },
    openForm(mode, row) {
      this.formMode = mode;
      this.formDrawerVisible = true;
      getOpportunityDetail(row.opportunityId).then(res => {
        const detail = res.data.data || {};
        this.form = {
          ...createDefaultForm(),
          ...detail,
          carrierTypeArray: detail.carrierTypeArray || this.splitCarrierTypes(detail.carrierTypes),
          tagIds: detail.tagIds || (detail.tags || []).map(tag => tag.tagId),
        };
        this.followList = detail.followList || [];
        this.fileList = detail.fileList || [];
      });
    },
    closeFormDrawer() {
      this.formDrawerVisible = false;
      this.form = createDefaultForm();
      this.followList = [];
      this.fileList = [];
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const action = this.form.opportunityId ? updateOpportunity : addOpportunity;
        action(this.form)
          .then(() => {
            this.$message.success('保存成功');
            this.closeFormDrawer();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    rowDel(row) {
      this.$confirm('确定删除该商机?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeOpportunity(row.opportunityId))
        .then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
    },
    handleBatchDelete() {
      if (!this.ids) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定删除选中的商机?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeOpportunity(this.ids))
        .then(() => {
          this.$message.success('删除成功');
          this.selectionList = [];
          this.loadStatistics();
          this.onLoad(this.page);
        });
    },
    openFollow(row) {
      this.currentRecord = row;
      const status = ['DRAFT', 'AUDIT'].includes(row.opportunityStatus)
        ? 'INITIAL'
        : row.opportunityStatus;
      this.followForm = {
        opportunityStatus: status || 'INITIAL',
        followContent: '',
        nextFollowTime: '',
      };
      this.followDialogVisible = true;
    },
    closeFollowDialog() {
      this.followDialogVisible = false;
      this.currentRecord = null;
      this.$nextTick(() => {
        this.$refs.followFormRef && this.$refs.followFormRef.clearValidate();
      });
    },
    handleSaveFollow() {
      this.$refs.followFormRef.validate(valid => {
        if (!valid || !this.currentRecord) return;
        this.followLoading = true;
        addOpportunityFollow(this.currentRecord.opportunityId, this.followForm)
          .then(() => {
            this.$message.success('跟进成功');
            this.closeFollowDialog();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.followLoading = false;
          });
      });
    },
    handleSubmitAudit(row) {
      this.$confirm('确定提交该商机进入入驻审核?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => submitOpportunityAudit(row.opportunityId))
        .then(() => {
          this.$message.success('提交审核成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
    },
    handleUploadFile({ file }) {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('opportunityId', this.form.opportunityId);
      return uploadOpportunityFile(formData).then(() => {
        this.$message.success('上传成功');
        return getOpportunityFileList(this.form.opportunityId).then(res => {
          this.fileList = res.data.data || [];
        });
      });
    },
    handleBackgroundCheck() {
      if (!this.form.enterpriseName) {
        this.$message.warning('请先填写企业名称');
        return;
      }
      this.backgroundLoading = true;
      getOpportunityBackgroundByName(this.form.enterpriseName)
        .then(res => {
          this.backgroundData = res.data.data || {};
          this.backgroundVisible = true;
        })
        .finally(() => {
          this.backgroundLoading = false;
        });
    },
    emptyBackgroundText(key) {
      const list = this.backgroundData[key] || [];
      return list.length ? '' : '暂无记录';
    },
    formatCarrierTypes(value) {
      return value ? String(value).split(',').filter(Boolean).join('、') : '-';
    },
    splitCarrierTypes(value) {
      return value ? String(value).split(',').filter(Boolean) : [];
    },
  },
};
</script>

<style scoped>
.opportunity-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: flex;
  min-height: 72px;
  flex-direction: column;
  justify-content: center;
  padding: 14px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.summary-card span {
  color: #6b7280;
  font-size: 13px;
}

.summary-card strong {
  margin-top: 6px;
  color: #111827;
  font-size: 24px;
  line-height: 1.2;
}

.opportunity-search,
.opportunity-toolbar {
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.opportunity-search :deep(.el-form-item) {
  margin-bottom: 8px;
}

.opportunity-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.opportunity-table {
  width: 100%;
}

.opportunity-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.opportunity-form {
  padding: 0 20px 20px;
}

.form-section {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.form-section header {
  margin-bottom: 14px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 20px;
}

.timeline-title {
  color: #1f2937;
  font-weight: 600;
}

.timeline-text {
  margin-top: 4px;
  color: #4b5563;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.background-tabs {
  margin-top: 14px;
}

@media (max-width: 900px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .opportunity-form :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
