<template>
  <basic-container>
    <div class="contract-create-page">
      <template v-if="pageMode === 'list'">
        <section class="template-list-card">
          <header class="list-header">
            <div>
              <h3>合同模板列表</h3>
              <span>选择模板后在一个页面完成合同创建</span>
            </div>
            <el-button type="primary" @click="handleTemplateAdd">+ 模板</el-button>
          </header>

          <el-table :data="templates" border class="template-table" row-key="key">
            <el-table-column
              prop="name"
              label="模板名称"
              min-width="220"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              prop="projectName"
              label="应用项目"
              width="180"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column prop="updateTime" label="更新时间" width="180" align="center" />
            <el-table-column prop="expireTime" label="截止时间" width="120" align="center" />
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-switch v-model="row.enabled" disabled />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="210" align="center" fixed="right">
              <template #default="{ row }">
                <el-button text type="primary" @click="startCreate(row)">创建合同</el-button>
                <el-button text type="primary" @click="previewTemplate(row)">预览</el-button>
                <el-button text type="danger" disabled>删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </template>

      <template v-else>
        <section class="create-step-bar">
          <button type="button" class="back-btn" @click="backToList">
            <el-icon><ArrowLeft /></el-icon>
          </button>
          <div class="step-item is-active">
            <span>1</span>
            <strong>基本信息</strong>
          </div>
          <div class="step-line"></div>
          <div class="step-item" :class="{ 'is-active': activeAnchor === 'fee' }">
            <span>2</span>
            <strong>费用条款</strong>
          </div>
          <div class="selected-template">当前模板：{{ currentTemplate.name }}</div>
        </section>

        <div class="create-workspace">
          <main class="form-column">
            <el-form
              ref="contractForm"
              :model="form"
              :rules="rules"
              label-width="132px"
              class="contract-form"
            >
              <section class="form-section">
                <header class="section-title">
                  <span>基本信息</span>
                  <em>合同名称、签约日期和承租企业</em>
                </header>
                <el-row :gutter="18">
                  <el-col :span="12">
                    <el-form-item label="合同模板名称">
                      <el-select
                        v-model="currentTemplateKey"
                        placeholder="请选择合同模板"
                        style="width: 100%"
                        @change="handleTemplateChange"
                      >
                        <el-option
                          v-for="item in templates"
                          :key="item.key"
                          :label="item.name"
                          :value="item.key"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="合同名称" prop="contractName">
                      <el-input
                        v-model="form.contractName"
                        maxlength="120"
                        placeholder="请输入合同名称"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="18">
                  <el-col :span="12">
                    <el-form-item label="乙方企业" prop="customerId">
                      <el-select
                        v-model="form.customerId"
                        filterable
                        remote
                        clearable
                        reserve-keyword
                        placeholder="请选择已入驻企业"
                        :remote-method="searchCustomers"
                        :loading="customerLoading"
                        style="width: 100%"
                        @change="handleCustomerChange"
                      >
                        <el-option
                          v-for="item in customerOptions"
                          :key="item.customerId"
                          :label="item.enterpriseName"
                          :value="item.customerId"
                        >
                          <div class="select-option-row">
                            <span>{{ item.enterpriseName }}</span>
                            <em>{{ item.contactName || '-' }} {{ item.contactPhone || '' }}</em>
                          </div>
                        </el-option>
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="联系人">
                      <el-input :model-value="customerContactText" disabled />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="18">
                  <el-col :span="12">
                    <el-form-item label="签约日期" prop="signDate">
                      <el-date-picker
                        v-model="form.signDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择签约日期"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="合同编号">
                      <el-input v-model="form.contractNo" placeholder="不填则后端自动生成" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </section>

              <section class="form-section">
                <header class="section-title">
                  <span>租赁信息</span>
                  <em>房源、面积、租期和免租装修期</em>
                </header>
                <el-row :gutter="18">
                  <el-col :span="12">
                    <el-form-item label="所属园区" prop="parkId">
                      <el-select
                        v-model="form.parkId"
                        filterable
                        clearable
                        placeholder="请选择园区"
                        style="width: 100%"
                        @change="handleParkChange"
                      >
                        <el-option
                          v-for="park in parkOptions"
                          :key="park.id"
                          :label="park.name"
                          :value="park.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="楼宇" prop="buildingId">
                      <el-select
                        v-model="form.buildingId"
                        filterable
                        clearable
                        placeholder="请选择楼宇"
                        style="width: 100%"
                        @change="handleBuildingChange"
                      >
                        <el-option
                          v-for="building in filteredBuildings"
                          :key="building.id"
                          :label="building.name"
                          :value="building.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="18">
                  <el-col :span="12">
                    <el-form-item label="房源" prop="roomId">
                      <el-select
                        v-model="form.roomId"
                        filterable
                        clearable
                        placeholder="请选择房间"
                        style="width: 100%"
                        @change="handleRoomSelect"
                      >
                        <el-option
                          v-for="room in roomOptions"
                          :key="room.id"
                          :label="roomLabel(room)"
                          :value="room.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="租赁面积(㎡)" prop="rentArea">
                      <el-input-number
                        v-model="form.rentArea"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="18">
                  <el-col :span="8">
                    <el-form-item label="租赁期限(月)" prop="leaseMonths">
                      <el-input-number
                        v-model="form.leaseMonths"
                        :min="1"
                        :precision="0"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="开始日期" prop="startDate">
                      <el-date-picker
                        v-model="form.startDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择开始日期"
                        style="width: 100%"
                        @change="syncLeaseMonths"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="结束日期" prop="endDate">
                      <el-date-picker
                        v-model="form.endDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择结束日期"
                        style="width: 100%"
                        @change="syncLeaseMonths"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row :gutter="18">
                  <el-col :span="8">
                    <el-form-item label="交付日期">
                      <el-date-picker
                        v-model="form.deliveryDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择交付日期"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="免租开始">
                      <el-date-picker
                        v-model="form.rentFreeStartDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择免租开始"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="免租结束">
                      <el-date-picker
                        v-model="form.rentFreeEndDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择免租结束"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
              </section>

              <section id="fee-section" class="form-section">
                <header class="section-title">
                  <span>费用条款</span>
                  <em>租金、物业费、履约保证金和滞纳金</em>
                </header>

                <template v-if="isFloatingTemplate">
                  <div class="stage-grid">
                    <div v-for="stage in rentStages" :key="stage.key" class="stage-card">
                      <div class="stage-title">{{ stage.title }}</div>
                      <el-row :gutter="12">
                        <el-col :span="12">
                          <el-form-item :label="stage.startLabel">
                            <el-date-picker
                              v-model="form[stage.startProp]"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="开始日期"
                              style="width: 100%"
                            />
                          </el-form-item>
                        </el-col>
                        <el-col :span="12">
                          <el-form-item :label="stage.endLabel">
                            <el-date-picker
                              v-model="form[stage.endProp]"
                              type="date"
                              value-format="YYYY-MM-DD"
                              placeholder="结束日期"
                              style="width: 100%"
                            />
                          </el-form-item>
                        </el-col>
                      </el-row>
                      <el-row :gutter="12">
                        <el-col :span="12">
                          <el-form-item label="租金单价">
                            <el-input-number
                              v-model="form[stage.priceProp]"
                              :min="0"
                              :precision="2"
                              style="width: 100%"
                              @change="calculateStageRent(stage)"
                            />
                          </el-form-item>
                        </el-col>
                        <el-col :span="12">
                          <el-form-item label="月租金">
                            <el-input-number
                              v-model="form[stage.monthlyProp]"
                              :min="0"
                              :precision="2"
                              style="width: 100%"
                            />
                          </el-form-item>
                        </el-col>
                      </el-row>
                    </div>
                  </div>
                </template>

                <el-row :gutter="18">
                  <el-col :span="8">
                    <el-form-item label="租金单价" prop="rentPrice">
                      <el-input-number
                        v-model="form.rentPrice"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                        @change="calculateMonthlyRent"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="月租金" prop="monthlyRent">
                      <el-input-number
                        v-model="form.monthlyRent"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="缴费周期" prop="paymentCycle">
                      <el-select
                        v-model="form.paymentCycle"
                        placeholder="请选择缴费周期"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="item in paymentCycleOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="18">
                  <el-col :span="8">
                    <el-form-item label="物业费单价">
                      <el-input-number
                        v-model="form.propertyFee"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="押金">
                      <el-input-number
                        v-model="form.deposit"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="履约保证月数">
                      <el-input-number
                        v-model="form.depositMonths"
                        :min="0"
                        :precision="0"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="18">
                  <el-col :span="8">
                    <el-form-item label="滞纳金比例">
                      <el-input-number
                        v-model="form.lateFeeRatio"
                        :min="0"
                        :precision="4"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="滞纳金单位">
                      <el-select
                        v-model="form.lateFeeUnit"
                        clearable
                        placeholder="请选择单位"
                        style="width: 100%"
                      >
                        <el-option label="%/天" value="percent_day" />
                        <el-option label="元/天" value="yuan_day" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="滞纳金上限">
                      <el-input-number
                        v-model="form.lateFeeCap"
                        :min="0"
                        :precision="2"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="18">
                  <el-col :span="8">
                    <el-form-item label="物业首期日期">
                      <el-date-picker
                        v-model="form.firstPropertyPayDate"
                        type="date"
                        value-format="YYYY-MM-DD"
                        placeholder="请选择日期"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="续签提醒天数">
                      <el-input-number
                        v-model="form.renewalRemindDays"
                        :min="0"
                        :precision="0"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="合同状态">
                      <el-select v-model="form.contractStatus" style="width: 100%">
                        <el-option label="待签约" value="0" />
                        <el-option label="履约中" value="1" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="备注">
                  <el-input
                    v-model="form.remark"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入备注"
                  />
                </el-form-item>
              </section>
            </el-form>
          </main>

          <aside class="room-column">
            <section class="room-panel">
              <header class="room-panel__header">
                <span>请选择房源</span>
                <el-button text type="primary" :loading="roomLoading" @click="loadRooms"
                  >刷新</el-button
                >
              </header>
              <div class="room-filters">
                <el-select
                  v-model="roomQuery.parkId"
                  filterable
                  clearable
                  placeholder="园区"
                  @change="handleRoomParkChange"
                >
                  <el-option
                    v-for="park in parkOptions"
                    :key="park.id"
                    :label="park.name"
                    :value="park.id"
                  />
                </el-select>
                <el-select
                  v-model="roomQuery.buildingId"
                  filterable
                  clearable
                  placeholder="楼宇"
                  @change="loadRooms"
                >
                  <el-option
                    v-for="building in roomFilterBuildings"
                    :key="building.id"
                    :label="building.name"
                    :value="building.id"
                  />
                </el-select>
                <el-input
                  v-model="roomQuery.keyword"
                  clearable
                  placeholder="搜索房号"
                  @keyup.enter="loadRooms"
                  @clear="loadRooms"
                >
                  <template #append>
                    <el-button icon="el-icon-search" @click="loadRooms" />
                  </template>
                </el-input>
              </div>

              <el-scrollbar class="room-scroll">
                <div v-loading="roomLoading" class="room-list">
                  <button
                    v-for="room in roomOptions"
                    :key="room.id"
                    type="button"
                    class="room-card"
                    :class="{ 'is-active': String(form.roomId || '') === String(room.id) }"
                    @click="selectRoomCard(room)"
                  >
                    <div>
                      <strong>{{ room.name || '-' }}</strong>
                      <span>{{ room.buildingName || '-' }} · {{ room.floor || '-' }}F</span>
                    </div>
                    <em>{{ formatNumber(room.area) }}㎡</em>
                    <small
                      >{{ roomStatusText(room.status) }} ·
                      {{ formatNumber(room.rentPrice) }}元/月</small
                    >
                  </button>
                  <el-empty
                    v-if="!roomLoading && roomOptions.length === 0"
                    description="暂无房源"
                  />
                </div>
              </el-scrollbar>
            </section>

            <section class="room-summary">
              <span>已选房源</span>
              <strong>{{ form.roomName || '暂未选择' }}</strong>
              <p>
                {{ form.buildingName || '-' }} / {{ formatNumber(form.rentArea) }}㎡ /
                {{ formatNumber(form.monthlyRent) }}元/月
              </p>
            </section>
          </aside>
        </div>

        <section class="create-footer">
          <el-button @click="backToList">返回模板列表</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitContract"
            >保存合同</el-button
          >
        </section>
      </template>
    </div>
  </basic-container>
</template>

<script>
import { ArrowLeft } from '@element-plus/icons-vue';
import { add, getDetail as getContractDetail, renew } from '@/api/contract/contract';
import { getCustomerDetail, getCustomerList } from '@/api/business/customer';
import { getList as getParkList } from '@/api/park/park';
import { getSimpleList as getBuildingList } from '@/api/park/building';
import { getRoomDetail, getRoomList } from '@/api/park/rent-control';

const TEMPLATE_BASE = '/系统所需材料/君联合同';

export default {
  name: 'ContractCreateTemplate',
  components: {
    ArrowLeft,
  },
  data() {
    return {
      pageMode: 'list',
      activeAnchor: 'basic',
      currentTemplateKey: 'fixed-rent',
      renewalMode: false,
      renewalSourceContractId: '',
      renewalSourceContract: {},
      submitLoading: false,
      customerLoading: false,
      roomLoading: false,
      customerOptions: [],
      selectedCustomer: {},
      parkOptions: [],
      buildingOptions: [],
      roomOptions: [],
      roomQuery: {
        parkId: '',
        buildingId: '',
        keyword: '',
      },
      templates: [
        {
          key: 'fixed-rent',
          name: '科技服务中心租赁合同（固定租金）',
          projectName: '服务中心',
          updateTime: '2026-06-23 00:00:00',
          expireTime: '永久',
          enabled: true,
          rentType: 'fixed',
          fileName: '科技服务中心租赁合同（固定租金）202508版 - 解锁.docx',
          previewFileName: 'preview/fixed-rent.html',
        },
        {
          key: 'floating-rent',
          name: '科技服务中心租赁合同（浮动租金）',
          projectName: '服务中心',
          updateTime: '2026-06-23 00:00:00',
          expireTime: '永久',
          enabled: true,
          rentType: 'floating',
          fileName: '科技服务中心租赁合同（浮动租金）202508版 - 解锁.docx',
          previewFileName: 'preview/floating-rent.html',
        },
      ],
      form: this.emptyForm(),
      rules: {
        contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
        customerId: [{ required: true, message: '请选择乙方企业', trigger: 'change' }],
        parkId: [{ required: true, message: '请选择所属园区', trigger: 'change' }],
        buildingId: [{ required: true, message: '请选择楼宇', trigger: 'change' }],
        roomId: [{ required: true, message: '请选择房源', trigger: 'change' }],
        rentArea: [{ required: true, message: '请输入租赁面积', trigger: 'blur' }],
        leaseMonths: [{ required: true, message: '请输入租赁期限', trigger: 'blur' }],
        startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
        endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
        signDate: [{ required: true, message: '请选择签约日期', trigger: 'change' }],
        rentPrice: [{ required: true, message: '请输入租金单价', trigger: 'blur' }],
        monthlyRent: [{ required: true, message: '请输入月租金', trigger: 'blur' }],
        paymentCycle: [{ required: true, message: '请选择缴费周期', trigger: 'change' }],
      },
      paymentCycleOptions: [
        { label: '月付', value: 'monthly' },
        { label: '季付', value: 'quarterly' },
        { label: '半年付', value: 'halfYear' },
        { label: '年付', value: 'yearly' },
      ],
      rentStages: [
        {
          key: 'stage1',
          title: '第一阶段租金',
          startLabel: '阶段开始',
          endLabel: '阶段结束',
          startProp: 'stage1StartDate',
          endProp: 'stage1EndDate',
          priceProp: 'stage1RentPrice',
          monthlyProp: 'stage1MonthlyRent',
        },
        {
          key: 'stage2',
          title: '第二阶段租金',
          startLabel: '阶段开始',
          endLabel: '阶段结束',
          startProp: 'stage2StartDate',
          endProp: 'stage2EndDate',
          priceProp: 'stage2RentPrice',
          monthlyProp: 'stage2MonthlyRent',
        },
      ],
      roomStatusOptions: [
        { label: '空置中', value: '0' },
        { label: '已出租', value: '1' },
        { label: '已预定', value: '2' },
        { label: '装修中', value: '3' },
        { label: '停用', value: '4' },
      ],
    };
  },
  computed: {
    currentTemplate() {
      return this.templates.find(item => item.key === this.currentTemplateKey) || this.templates[0];
    },
    isFloatingTemplate() {
      return this.currentTemplate && this.currentTemplate.rentType === 'floating';
    },
    filteredBuildings() {
      if (!this.form.parkId) return this.buildingOptions;
      return this.buildingOptions.filter(
        item => String(item.parkId || '') === String(this.form.parkId)
      );
    },
    roomFilterBuildings() {
      if (!this.roomQuery.parkId) return this.buildingOptions;
      return this.buildingOptions.filter(
        item => String(item.parkId || '') === String(this.roomQuery.parkId)
      );
    },
    customerContactText() {
      if (!this.selectedCustomer.customerId) return '';
      const parts = [this.selectedCustomer.contactName, this.selectedCustomer.contactPhone].filter(
        Boolean
      );
      return parts.join(' / ');
    },
  },
  created() {
    this.initFromRoute();
    this.loadBaseData();
  },
  methods: {
    emptyForm() {
      return {
        contractNo: '',
        contractName: '',
        parentContractId: '',
        customerId: '',
        customerName: '',
        parkId: '',
        parkName: '',
        buildingId: '',
        buildingName: '',
        roomId: '',
        roomName: '',
        rentArea: undefined,
        leaseMonths: undefined,
        startDate: '',
        endDate: '',
        signDate: this.today(),
        deliveryDate: '',
        rentFreeStartDate: '',
        rentFreeEndDate: '',
        rentPrice: undefined,
        monthlyRent: undefined,
        propertyFee: undefined,
        deposit: undefined,
        depositMonths: 1,
        paymentCycle: 'monthly',
        lateFeeRatio: 0.0005,
        lateFeeUnit: 'percent_day',
        lateFeeCap: undefined,
        firstPropertyPayDate: '',
        renewalRemindDays: 30,
        contractStatus: '0',
        stage1StartDate: '',
        stage1EndDate: '',
        stage1RentPrice: undefined,
        stage1MonthlyRent: undefined,
        stage2StartDate: '',
        stage2EndDate: '',
        stage2RentPrice: undefined,
        stage2MonthlyRent: undefined,
        remark: '',
      };
    },
    loadBaseData() {
      this.searchCustomers('');
      getParkList(1, 200, {}).then(res => {
        const data = res.data.data || {};
        this.parkOptions = data.records || [];
        if (this.form.parkId) {
          this.form.parkName = this.parkName(this.form.parkId);
        }
      });
      getBuildingList({}).then(res => {
        this.buildingOptions = res.data.data || [];
      });
      this.loadRooms();
    },
    handleTemplateAdd() {
      this.$router.push({ path: '/contract/print-template' });
    },
    initFromRoute() {
      const query = this.$route.query || {};
      const templateKey = query.template;
      if (templateKey && this.templates.some(item => item.key === templateKey)) {
        this.currentTemplateKey = templateKey;
        this.pageMode = 'create';
      }
      if (query.mode === 'create' || query.mode === 'renew' || query.customerId) {
        this.pageMode = 'create';
      }
      if (query.mode === 'renew' && query.sourceContractId) {
        this.renewalMode = true;
        this.renewalSourceContractId = query.sourceContractId;
        this.loadRenewalSource(query.sourceContractId, query);
      }
      if (query.customerId) {
        this.form.customerId = query.customerId;
        this.prefillCustomer(query.customerId);
      }
    },
    loadRenewalSource(contractId, query = {}) {
      getContractDetail(contractId).then(res => {
        const contract = res.data.data || {};
        this.renewalSourceContract = contract;
        this.applyRenewalSource(contract, query);
      });
    },
    applyRenewalSource(contract = {}, query = {}) {
      if (!contract.contractId) return;
      const renewStartDate =
        query.renewStartDate || this.nextDate(contract.endDate) || this.today();
      const renewEndDate =
        query.renewEndDate ||
        this.defaultRenewEndDate(renewStartDate, contract.startDate, contract.endDate);
      const templateKey = this.resolveTemplateKey(contract);
      this.currentTemplateKey = templateKey;
      Object.assign(this.form, {
        contractNo: '',
        contractName: `${contract.customerName || contract.contractName || '合同'} 续租合同`,
        parentContractId: contract.contractId,
        customerId: contract.customerId || '',
        customerName: contract.customerName || '',
        parkId: contract.parkId || '',
        parkName: contract.parkName || '',
        buildingId: contract.buildingId || '',
        buildingName: contract.buildingName || '',
        roomId: contract.roomId || '',
        roomName: contract.roomName || '',
        rentArea: this.toNumber(contract.rentArea, undefined),
        leaseMonths: undefined,
        startDate: renewStartDate,
        endDate: renewEndDate,
        signDate: this.today(),
        deliveryDate: renewStartDate,
        rentFreeStartDate: '',
        rentFreeEndDate: '',
        rentPrice: this.toNumber(contract.rentPrice, undefined),
        monthlyRent: this.toNumber(contract.monthlyRent, undefined),
        propertyFee: this.toNumber(contract.propertyFee, undefined),
        deposit: this.toNumber(contract.deposit, undefined),
        depositMonths: 1,
        paymentCycle: contract.paymentCycle || 'monthly',
        lateFeeRatio: this.toNumber(contract.lateFeeRatio, 0.0005),
        lateFeeUnit: contract.lateFeeUnit || 'percent_day',
        lateFeeCap: this.toNumber(contract.lateFeeCap, undefined),
        renewalRemindDays: contract.renewalRemindDays || 30,
        contractStatus: '0',
        remark: `续租来源合同：${contract.contractNo || contract.contractId}`,
      });
      if (this.isFloatingTemplate) {
        Object.assign(this.form, {
          stage1StartDate: renewStartDate,
          stage1EndDate: renewEndDate,
          stage1RentPrice: this.form.rentPrice,
          stage1MonthlyRent: this.form.monthlyRent,
          stage2StartDate: '',
          stage2EndDate: '',
          stage2RentPrice: undefined,
          stage2MonthlyRent: undefined,
        });
      }
      this.roomQuery.parkId = contract.parkId || '';
      this.roomQuery.buildingId = contract.buildingId || '';
      this.selectedCustomer = {
        customerId: contract.customerId,
        enterpriseName: contract.customerName,
      };
      if (
        contract.customerId &&
        !this.customerOptions.some(item => String(item.customerId) === String(contract.customerId))
      ) {
        this.customerOptions.unshift(this.selectedCustomer);
      }
      this.ensureSelectedRoomOption();
      this.syncLeaseMonths();
      if (contract.customerId) {
        this.prefillCustomer(contract.customerId);
      }
      this.$nextTick(() => {
        if (this.$refs.contractForm) {
          this.$refs.contractForm.clearValidate();
        }
      });
    },
    handleTemplateChange(templateKey) {
      if (!templateKey) return;
      this.currentTemplateKey = templateKey;
      if (!this.isFloatingTemplate) {
        Object.assign(this.form, {
          stage1StartDate: '',
          stage1EndDate: '',
          stage1RentPrice: undefined,
          stage1MonthlyRent: undefined,
          stage2StartDate: '',
          stage2EndDate: '',
          stage2RentPrice: undefined,
          stage2MonthlyRent: undefined,
        });
        return;
      }
      this.form.stage1StartDate = this.form.stage1StartDate || this.form.startDate;
      this.form.stage2EndDate = this.form.stage2EndDate || this.form.endDate;
      this.form.stage1RentPrice = this.form.stage1RentPrice || this.form.rentPrice;
      this.form.stage1MonthlyRent = this.form.stage1MonthlyRent || this.form.monthlyRent;
    },
    previewTemplate(row) {
      window.open(`${TEMPLATE_BASE}/${row.previewFileName}`, '_blank');
    },
    startCreate(row) {
      this.currentTemplateKey = row.key;
      this.form = this.emptyForm();
      this.selectedCustomer = {};
      this.pageMode = 'create';
      this.activeAnchor = 'basic';
      this.$nextTick(() => {
        if (this.$refs.contractForm) {
          this.$refs.contractForm.clearValidate();
        }
      });
    },
    backToList() {
      this.pageMode = 'list';
    },
    searchCustomers(keyword) {
      this.customerLoading = true;
      getCustomerList(1, 20, {
        keyword,
        enterpriseName: keyword,
        settlementStatus: 3,
        status: '0',
      })
        .then(res => {
          const data = res.data.data || {};
          const records = data.records || [];
          if (
            this.selectedCustomer.customerId &&
            !records.some(
              item => String(item.customerId) === String(this.selectedCustomer.customerId)
            )
          ) {
            records.unshift(this.selectedCustomer);
          }
          this.customerOptions = records;
        })
        .finally(() => {
          this.customerLoading = false;
        });
    },
    prefillCustomer(customerId) {
      getCustomerDetail(customerId).then(res => {
        const customer = res.data.data || {};
        if (
          customer.customerId &&
          !this.customerOptions.some(
            item => String(item.customerId) === String(customer.customerId)
          )
        ) {
          this.customerOptions.unshift(customer);
        }
        this.applyCustomer(customer);
      });
    },
    handleCustomerChange(customerId) {
      if (!customerId) {
        this.selectedCustomer = {};
        this.form.customerName = '';
        return;
      }
      const cached = this.customerOptions.find(
        item => String(item.customerId) === String(customerId)
      );
      if (cached) {
        this.applyCustomer(cached);
      }
      getCustomerDetail(customerId).then(res => {
        this.applyCustomer(res.data.data || {});
      });
    },
    applyCustomer(customer) {
      this.selectedCustomer = customer || {};
      this.form.customerId = customer.customerId || this.form.customerId;
      this.form.customerName = customer.enterpriseName || '';
      if (!this.form.contractName && customer.enterpriseName) {
        this.form.contractName = `${customer.enterpriseName} 租赁合同`;
      }
      if (customer.parkId) {
        if (!this.form.parkId) {
          this.form.parkId = customer.parkId;
        }
        if (String(this.form.parkId) === String(customer.parkId)) {
          this.form.parkName = this.parkName(customer.parkId) || this.form.parkName;
        }
        this.roomQuery.parkId = this.roomQuery.parkId || customer.parkId;
        this.loadRooms();
      }
    },
    handleParkChange(parkId) {
      this.form.parkName = this.parkName(parkId);
      this.form.buildingId = '';
      this.form.buildingName = '';
      this.form.roomId = '';
      this.form.roomName = '';
      this.roomQuery.parkId = parkId || '';
      this.roomQuery.buildingId = '';
      this.loadRooms();
    },
    handleBuildingChange(buildingId) {
      const building = this.buildingOptions.find(item => String(item.id) === String(buildingId));
      this.form.buildingName = building ? building.name : '';
      if (building && !this.form.parkId) {
        this.form.parkId = building.parkId;
        this.form.parkName = this.parkName(building.parkId);
      }
      this.form.roomId = '';
      this.form.roomName = '';
      this.roomQuery.parkId = this.form.parkId || '';
      this.roomQuery.buildingId = buildingId || '';
      this.loadRooms();
    },
    handleRoomParkChange(parkId) {
      this.roomQuery.buildingId = '';
      if (parkId && !this.form.parkId) {
        this.form.parkId = parkId;
        this.form.parkName = this.parkName(parkId);
      }
      this.loadRooms();
    },
    loadRooms() {
      this.roomLoading = true;
      getRoomList({
        parkId: this.roomQuery.parkId || this.form.parkId || undefined,
        buildingId: this.roomQuery.buildingId || this.form.buildingId || undefined,
        keyword: this.roomQuery.keyword || undefined,
        searchType: 'room',
      })
        .then(res => {
          const data = res.data.data || {};
          this.roomOptions = Array.isArray(data) ? data : data.records || [];
          this.ensureSelectedRoomOption();
        })
        .finally(() => {
          this.roomLoading = false;
        });
    },
    handleRoomSelect(roomId) {
      if (!roomId) {
        this.form.roomName = '';
        return;
      }
      const cached = this.roomOptions.find(item => String(item.id) === String(roomId));
      if (cached) {
        this.applyRoom(cached);
      }
      getRoomDetail(roomId).then(res => {
        this.applyRoom(res.data.data || {});
      });
    },
    selectRoomCard(room) {
      this.form.roomId = room.id;
      this.applyRoom(room);
      getRoomDetail(room.id).then(res => {
        this.applyRoom(res.data.data || room);
      });
    },
    applyRoom(room) {
      if (!room || !room.id) return;
      const building = this.buildingOptions.find(
        item => String(item.id) === String(room.buildingId)
      );
      const parkId = room.parkId || (building && building.parkId) || this.form.parkId;
      this.form.roomId = room.id;
      this.form.roomName = room.name || '';
      this.form.buildingId = room.buildingId || this.form.buildingId;
      this.form.buildingName =
        room.buildingName || (building && building.name) || this.form.buildingName;
      this.form.parkId = parkId;
      this.form.parkName = room.parkName || this.parkName(parkId);
      this.form.rentArea = this.toNumber(room.area, this.form.rentArea);
      this.form.rentPrice = this.toNumber(
        room.unitRentPrice,
        this.toNumber(room.rentUnitPrice, this.form.rentPrice)
      );
      if (!this.form.rentPrice && room.rentPrice && this.form.rentArea) {
        this.form.monthlyRent = this.toNumber(room.rentPrice, this.form.monthlyRent);
      } else if (room.rentPrice && !this.form.monthlyRent) {
        this.form.monthlyRent = this.toNumber(room.rentPrice, this.form.monthlyRent);
      }
      this.form.propertyFee = this.toNumber(room.propertyFee, this.form.propertyFee);
      if (this.isFloatingTemplate) {
        this.form.stage1RentPrice = this.form.stage1RentPrice || this.form.rentPrice;
        this.form.stage1MonthlyRent = this.form.stage1MonthlyRent || this.form.monthlyRent;
      }
      this.calculateMonthlyRent();
    },
    calculateMonthlyRent() {
      const area = Number(this.form.rentArea || 0);
      const price = Number(this.form.rentPrice || 0);
      if (area > 0 && price > 0) {
        this.form.monthlyRent = Number((area * price).toFixed(2));
      }
      if (!this.form.deposit && this.form.monthlyRent && this.form.depositMonths) {
        this.form.deposit = Number(
          (Number(this.form.monthlyRent) * Number(this.form.depositMonths)).toFixed(2)
        );
      }
    },
    calculateStageRent(stage) {
      const area = Number(this.form.rentArea || 0);
      const price = Number(this.form[stage.priceProp] || 0);
      if (area > 0 && price > 0) {
        this.form[stage.monthlyProp] = Number((area * price).toFixed(2));
      }
    },
    syncLeaseMonths() {
      if (!this.form.startDate || !this.form.endDate) return;
      const start = new Date(this.form.startDate);
      const end = new Date(this.form.endDate);
      if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || end <= start) return;
      const months =
        (end.getFullYear() - start.getFullYear()) * 12 + end.getMonth() - start.getMonth();
      this.form.leaseMonths = Math.max(months, 1);
      if (this.isFloatingTemplate) {
        this.form.stage1StartDate = this.form.stage1StartDate || this.form.startDate;
        this.form.stage2EndDate = this.form.stage2EndDate || this.form.endDate;
      }
    },
    submitContract() {
      this.$refs.contractForm.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const request =
          this.renewalMode && this.renewalSourceContractId
            ? renew(this.renewalSourceContractId, this.buildPayload())
            : add(this.buildPayload());
        request
          .then(() => {
            this.$message.success(this.renewalMode ? '续租合同创建成功' : '合同创建成功');
            this.$router.push({ path: '/contract/contract' });
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    buildPayload() {
      const payload = {
        contractNo: this.form.contractNo,
        contractName: this.form.contractName,
        parentContractId: this.form.parentContractId,
        customerId: this.form.customerId,
        customerName: this.form.customerName,
        parkId: this.form.parkId,
        buildingId: this.form.buildingId,
        buildingName: this.form.buildingName,
        roomId: this.form.roomId,
        roomName: this.form.roomName,
        rentPrice: this.form.rentPrice,
        rentArea: this.form.rentArea,
        monthlyRent: this.form.monthlyRent,
        propertyFee: this.form.propertyFee,
        deposit: this.form.deposit,
        lateFeeRatio: this.form.lateFeeRatio,
        lateFeeUnit: this.form.lateFeeUnit,
        lateFeeCap: this.form.lateFeeCap,
        startDate: this.form.startDate,
        endDate: this.form.endDate,
        signDate: this.form.signDate,
        renewalRemindDays: this.form.renewalRemindDays,
        contractStatus: this.form.contractStatus,
        paymentCycle: this.form.paymentCycle,
        rentIncreaseNode: this.isFloatingTemplate ? this.rentIncreaseSummary() : '',
        remark: this.buildRemark(),
      };
      return Object.keys(payload).reduce((result, key) => {
        if (payload[key] !== '' && payload[key] !== undefined && payload[key] !== null) {
          result[key] = payload[key];
        }
        return result;
      }, {});
    },
    buildRemark() {
      const lines = [
        `合同模板：${this.currentTemplate.name}`,
        `交付日期：${this.form.deliveryDate || '-'}`,
        `免租装修期：${this.form.rentFreeStartDate || '-'} 至 ${this.form.rentFreeEndDate || '-'}`,
        `物业首期缴费日期：${this.form.firstPropertyPayDate || '-'}`,
        `履约保证月数：${this.form.depositMonths || 0}个月`,
      ];
      if (this.isFloatingTemplate) {
        lines.push(`浮动租金：${this.rentIncreaseSummary()}`);
      }
      if (this.form.remark) {
        lines.push(`业务备注：${this.form.remark}`);
      }
      return lines.join('\n');
    },
    resolveTemplateKey(contract) {
      if (contract && contract.rentIncreaseNode) {
        return 'floating-rent';
      }
      return this.currentTemplateKey || 'fixed-rent';
    },
    ensureSelectedRoomOption() {
      if (!this.form.roomId) return;
      const exists = this.roomOptions.some(item => String(item.id) === String(this.form.roomId));
      if (exists) return;
      this.roomOptions.unshift({
        id: this.form.roomId,
        name: this.form.roomName,
        buildingId: this.form.buildingId,
        buildingName: this.form.buildingName,
        parkId: this.form.parkId,
        parkName: this.form.parkName,
        area: this.form.rentArea,
        unitRentPrice: this.form.rentPrice,
        rentPrice: this.form.monthlyRent,
        propertyFee: this.form.propertyFee,
        status: '1',
      });
    },
    rentIncreaseSummary() {
      return [
        `${this.form.stage1StartDate || '-'} 至 ${this.form.stage1EndDate || '-'}：${
          this.form.stage1RentPrice || '-'
        }元/㎡/月，${this.form.stage1MonthlyRent || '-'}元/月`,
        `${this.form.stage2StartDate || '-'} 至 ${this.form.stage2EndDate || '-'}：${
          this.form.stage2RentPrice || '-'
        }元/㎡/月，${this.form.stage2MonthlyRent || '-'}元/月`,
      ].join('；');
    },
    parkName(parkId) {
      const park = this.parkOptions.find(item => String(item.id) === String(parkId));
      return park ? park.name : '';
    },
    roomLabel(room) {
      const parts = [room.buildingName, room.floor ? `${room.floor}F` : '', room.name].filter(
        Boolean
      );
      return parts.join(' / ');
    },
    roomStatusText(status) {
      const item = this.roomStatusOptions.find(ele => ele.value === String(status));
      return item ? item.label : '空置中';
    },
    formatNumber(value) {
      const number = Number(value || 0);
      return number.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      });
    },
    toNumber(value, fallback) {
      if (value === null || value === undefined || value === '') {
        return fallback;
      }
      const number = Number(value);
      return Number.isNaN(number) ? fallback : number;
    },
    parseDate(value) {
      if (!value) return null;
      const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);
      return Number.isNaN(date.getTime()) ? null : date;
    },
    nextDate(value) {
      const date = this.parseDate(value);
      if (!date) return '';
      date.setDate(date.getDate() + 1);
      return this.formatDate(date);
    },
    defaultRenewEndDate(startDate, oldStartDate, oldEndDate) {
      const start = this.parseDate(startDate);
      if (!start) return '';
      const oldStart = this.parseDate(oldStartDate);
      const oldEnd = this.parseDate(oldEndDate);
      const months =
        oldStart && oldEnd
          ? Math.max(
              (oldEnd.getFullYear() - oldStart.getFullYear()) * 12 +
                oldEnd.getMonth() -
                oldStart.getMonth(),
              1
            )
          : 12;
      const end = new Date(start);
      end.setMonth(end.getMonth() + months);
      return this.formatDate(end);
    },
    formatDate(date) {
      const month = `${date.getMonth() + 1}`.padStart(2, '0');
      const day = `${date.getDate()}`.padStart(2, '0');
      return `${date.getFullYear()}-${month}-${day}`;
    },
    today() {
      const date = new Date();
      const month = `${date.getMonth() + 1}`.padStart(2, '0');
      const day = `${date.getDate()}`.padStart(2, '0');
      return `${date.getFullYear()}-${month}-${day}`;
    },
  },
};
</script>

<style scoped>
.contract-create-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.template-list-card,
.create-step-bar,
.form-section,
.room-panel,
.room-summary,
.create-footer {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid #edf0f5;
}

.list-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.list-header span {
  display: block;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.template-table {
  width: 100%;
}

.create-step-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 72px;
  padding: 0 20px;
}

.back-btn {
  display: flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: #606266;
  cursor: pointer;
}

.back-btn:hover {
  background: #f2f6fc;
  color: #1059c6;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #909399;
}

.step-item span {
  display: inline-flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  border-radius: 50%;
  font-weight: 600;
}

.step-item strong {
  font-size: 14px;
  font-weight: 600;
}

.step-item.is-active {
  color: #1059c6;
}

.step-item.is-active span {
  border-color: #409eff;
  background: #409eff;
  color: #fff;
}

.step-line {
  height: 1px;
  flex: 0 0 240px;
  background: #dcdfe6;
}

.selected-template {
  margin-left: auto;
  color: #606266;
  font-size: 13px;
}

.create-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 14px;
  align-items: start;
}

.form-column,
.room-column {
  min-width: 0;
}

.contract-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-section {
  padding: 18px 20px 6px;
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
  padding-bottom: 12px;
  border-bottom: 1px solid #edf0f5;
}

.section-title span {
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.section-title em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.select-option-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.select-option-row em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.stage-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stage-card {
  padding: 14px 14px 0;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #f8fafc;
}

.stage-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-weight: 600;
}

.room-column {
  position: sticky;
  top: 12px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.room-panel__header {
  display: flex;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  border-bottom: 1px solid #edf0f5;
}

.room-panel__header span {
  color: #1f2937;
  font-weight: 600;
}

.room-filters {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #edf0f5;
}

.room-filters .el-input {
  grid-column: 1 / -1;
}

.room-scroll {
  height: 520px;
}

.room-list {
  min-height: 240px;
  padding: 14px 16px;
}

.room-card {
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 6px 12px;
  margin-bottom: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.room-card:hover,
.room-card.is-active {
  border-color: #1059c6;
  background: #f4f8ff;
}

.room-card strong {
  display: block;
  color: #1f2937;
  font-size: 14px;
}

.room-card span {
  display: block;
  margin-top: 4px;
  color: #606266;
  font-size: 12px;
}

.room-card em {
  color: #1059c6;
  font-size: 13px;
  font-style: normal;
  font-weight: 600;
}

.room-card small {
  grid-column: 1 / -1;
  color: #909399;
  font-size: 12px;
}

.room-summary {
  padding: 16px;
}

.room-summary span {
  display: block;
  color: #909399;
  font-size: 12px;
}

.room-summary strong {
  display: block;
  margin-top: 8px;
  color: #1f2937;
  font-size: 16px;
}

.room-summary p {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
}

.create-footer {
  position: sticky;
  bottom: 0;
  z-index: 2;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  box-shadow: 0 -4px 16px rgba(15, 23, 42, 0.04);
}

.contract-create-page :deep(.el-input__wrapper),
.contract-create-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

.contract-create-page :deep(.el-button) {
  border-radius: 6px;
}

@media (max-width: 1280px) {
  .create-workspace {
    grid-template-columns: 1fr;
  }

  .room-column {
    position: static;
  }
}

@media (max-width: 900px) {
  .step-line,
  .selected-template {
    display: none;
  }

  .stage-grid {
    grid-template-columns: 1fr;
  }
}
</style>
