<template>
  <div class="room-workbench">
    <el-tabs v-model="activeTab" class="room-workbench-tabs">
      <el-tab-pane label="房源信息" name="info">
        <section v-loading="roomLoading" class="room-profile">
          <div class="room-profile-section">
            <h3>基础信息</h3>
            <div class="room-info-grid">
              <div v-for="item in basicItems" :key="item.label" class="room-info-item" :class="{ 'is-wide': item.wide }">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>

          <div class="room-profile-section">
            <h3>招商信息</h3>
            <div class="room-info-grid">
              <div v-for="item in leasingItems" :key="item.label" class="room-info-item" :class="{ 'is-wide': item.wide }">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </div>

          <div class="room-profile-section">
            <h3>房源图片</h3>
            <div v-if="roomImages.length" class="room-image-grid">
              <el-image
                v-for="image in roomImages"
                :key="image"
                :src="image"
                :preview-src-list="roomImages"
                fit="cover"
                preview-teleported
              />
            </div>
            <el-empty v-else description="暂无房源图片" :image-size="70" />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="租客账单" name="bills">
        <section class="room-data-panel">
          <el-table v-loading="billLoading" :data="bills" border row-key="paymentId" empty-text="暂无关联账单">
            <el-table-column prop="contractNo" label="合同编号" min-width="150" align="center" show-overflow-tooltip />
            <el-table-column prop="customerName" label="租客名称" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="feeName" label="费用类型" min-width="130" align="center" show-overflow-tooltip />
            <el-table-column label="应收/应付金额" width="140" align="center">
              <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
            </el-table-column>
            <el-table-column label="实收/实付金额" width="140" align="center">
              <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
            </el-table-column>
            <el-table-column prop="payDeadline" label="应收/应付日期" width="140" align="center" />
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="paymentStatusType(paymentDisplayStatus(row))" effect="plain">
                  {{ paymentStatusText(paymentDisplayStatus(row)) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="room-pagination">
            <el-pagination
              background
              :current-page="billPage.currentPage"
              :page-size="billPage.pageSize"
              layout="total, prev, pager, next"
              :total="billPage.total"
              @current-change="changeBillPage"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="租客合同" name="contracts">
        <section class="room-data-panel">
          <el-table v-loading="contractLoading" :data="contracts" border row-key="contractId" empty-text="暂无关联合同">
            <el-table-column prop="contractNo" label="合同编号" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="contractName" label="合同名称" min-width="180" align="center" show-overflow-tooltip />
            <el-table-column prop="customerName" label="租客名称" min-width="160" align="center" show-overflow-tooltip />
            <el-table-column prop="startDate" label="开始日期" width="120" align="center" />
            <el-table-column prop="endDate" label="结束日期" width="120" align="center" />
            <el-table-column label="月租金" width="130" align="center">
              <template #default="{ row }">{{ formatMoney(row.monthlyRent) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="contractStatusType(row.contractStatus)" effect="plain">
                  {{ row.contractStatusName || contractStatusText(row.contractStatus) }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div class="room-pagination">
            <el-pagination
              background
              :current-page="contractPage.currentPage"
              :page-size="contractPage.pageSize"
              layout="total, prev, pager, next"
              :total="contractPage.total"
              @current-change="changeContractPage"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="工单记录" name="workorders" lazy>
        <EnterprisePropertyWorkorder
          embedded
          :park-id="parkId"
          :filter-room-ids="String(roomId)"
          :default-room-info="roomContextText"
        />
      </el-tab-pane>

      <el-tab-pane label="资产记录" name="assets" lazy>
        <AssetLedger :park-id="parkId" :building-id="buildingId" :floor-no="floorNo" :room-id="roomId" />
      </el-tab-pane>

      <el-tab-pane label="智能硬件" name="devices" lazy>
        <SmartDeviceLedger :park-id="parkId" :building-id="buildingId" :floor-no="floorNo" :room-id="roomId" />
      </el-tab-pane>

      <el-tab-pane label="水电记录" name="utilities" lazy>
        <section class="room-data-panel">
          <div class="room-panel-toolbar">
            <strong>水电记录（{{ utilityPage.total }}）</strong>
            <el-button type="primary" :icon="Plus" @click="openUtilityDialog">新增</el-button>
          </div>
          <el-table v-loading="utilityLoading" :data="utilityRecords" border row-key="recordId" empty-text="暂无水电抄表记录">
            <el-table-column prop="readingTime" label="抄表时间" width="170" align="center" />
            <el-table-column prop="recordType" label="类型" width="90" align="center">
              <template #default="{ row }">{{ row.recordType === 'water' ? '水表' : '电表' }}</template>
            </el-table-column>
            <el-table-column prop="deviceName" label="设备名称" min-width="150" align="center" show-overflow-tooltip />
            <el-table-column prop="previousReading" label="上次读数" width="110" align="center" />
            <el-table-column prop="currentReading" label="本次读数" width="110" align="center" />
            <el-table-column prop="usageAmount" label="本期用量" width="110" align="center" />
            <el-table-column label="本期金额" width="120" align="center">
              <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
            </el-table-column>
            <el-table-column prop="operatorName" label="抄表人" width="110" align="center" />
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button text type="danger" @click="deleteUtility(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="room-pagination">
            <el-pagination
              background
              :current-page="utilityPage.currentPage"
              :page-size="utilityPage.pageSize"
              layout="total, prev, pager, next"
              :total="utilityPage.total"
              @current-change="changeUtilityPage"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="绑定车辆" name="vehicles" lazy>
        <section class="room-data-panel">
          <div class="room-panel-toolbar">
            <strong>绑定车辆（{{ vehiclePage.total }}）</strong>
            <el-button type="primary" :icon="Plus" @click="openVehicleDialog">新增</el-button>
          </div>
          <el-table v-loading="vehicleLoading" :data="vehicles" border row-key="vehicleId" empty-text="暂无绑定车辆">
            <el-table-column prop="plateNo" label="车牌号" min-width="130" align="center" />
            <el-table-column prop="vehicleType" label="车辆类型" width="110" align="center">
              <template #default="{ row }">{{ vehicleTypeText(row.vehicleType) }}</template>
            </el-table-column>
            <el-table-column prop="ownerName" label="车主姓名" min-width="120" align="center" />
            <el-table-column prop="ownerPhone" label="联系电话" min-width="140" align="center" />
            <el-table-column prop="validStart" label="有效期开始" width="120" align="center" />
            <el-table-column prop="validEnd" label="有效期结束" width="120" align="center" />
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'" effect="plain">
                  {{ row.status === 'active' ? '有效' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button text type="danger" @click="deleteVehicle(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="room-pagination">
            <el-pagination
              background
              :current-page="vehiclePage.currentPage"
              :page-size="vehiclePage.pageSize"
              layout="total, prev, pager, next"
              :total="vehiclePage.total"
              @current-change="changeVehiclePage"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="备忘提醒" name="reminders" lazy>
        <section class="room-empty-panel"><el-empty description="暂无备忘提醒" /></section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="utilityDialogVisible" title="新增水电记录" width="620px" append-to-body destroy-on-close class="room-extension-dialog">
      <el-form ref="utilityFormRef" :model="utilityForm" :rules="utilityRules" label-width="100px">
        <el-form-item label="水电表" prop="deviceId">
          <el-select v-model="utilityForm.deviceId" filterable placeholder="请选择当前房源下的水表或电表" @change="handleUtilityDeviceChange">
            <el-option
              v-for="item in meterOptions"
              :key="item.deviceId"
              :label="`${item.deviceName} / ${item.deviceType === 'water' ? '水表' : '电表'}`"
              :value="item.deviceId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="抄表时间" prop="readingTime">
          <el-date-picker v-model="utilityForm.readingTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择抄表时间" />
        </el-form-item>
        <el-form-item label="上次读数" prop="previousReading">
          <el-input-number v-model="utilityForm.previousReading" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="本次读数" prop="currentReading">
          <el-input-number v-model="utilityForm.currentReading" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="本期金额">
          <el-input-number v-model="utilityForm.amount" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="抄表人">
          <el-input v-model="utilityForm.operatorName" maxlength="100" placeholder="默认当前登录用户" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="utilityForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="utilityDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="utilitySaving" @click="submitUtility">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="vehicleDialogVisible" title="新增绑定车辆" width="620px" append-to-body destroy-on-close class="room-extension-dialog">
      <el-form ref="vehicleFormRef" :model="vehicleForm" :rules="vehicleRules" label-width="100px">
        <el-form-item label="车牌号" prop="plateNo">
          <el-input v-model="vehicleForm.plateNo" maxlength="32" placeholder="请输入车牌号" />
        </el-form-item>
        <el-form-item label="车辆类型" prop="vehicleType">
          <el-select v-model="vehicleForm.vehicleType" placeholder="请选择车辆类型">
            <el-option label="小型汽车" value="car" />
            <el-option label="货车" value="truck" />
            <el-option label="摩托车" value="motorcycle" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="车主姓名">
          <el-input v-model="vehicleForm.ownerName" maxlength="100" placeholder="请输入车主姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="vehicleForm.ownerPhone" maxlength="50" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="vehicleForm.validRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            range-separator="-"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="vehicleForm.status">
            <el-radio-button label="active">有效</el-radio-button>
            <el-radio-button label="disabled">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="vehicleForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vehicleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="vehicleSaving" @click="submitVehicle">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import { getDevicePage } from '@/api/park/smart-device';
import {
  getRoomContractPage,
  getRoomDetail,
  getRoomPaymentPage,
  getRoomUtilityPage,
  getRoomVehiclePage,
  removeRoomUtility,
  removeRoomVehicle,
  submitRoomUtility,
  submitRoomVehicle,
} from '@/api/park/rent-control';
import EnterprisePropertyWorkorder from '@/views/enterprise/property-workorder.vue';
import AssetLedger from './asset-ledger.vue';
import SmartDeviceLedger from './smart-device.vue';

const emptyUtilityForm = roomId => ({
  roomId,
  deviceId: undefined,
  readingTime: '',
  previousReading: 0,
  currentReading: null,
  amount: 0,
  operatorName: '',
  remark: '',
});

const emptyVehicleForm = roomId => ({
  roomId,
  plateNo: '',
  vehicleType: 'car',
  ownerName: '',
  ownerPhone: '',
  validRange: [],
  status: 'active',
  remark: '',
});

export default {
  name: 'RoomWorkbench',
  components: { AssetLedger, EnterprisePropertyWorkorder, SmartDeviceLedger },
  props: {
    roomId: { type: [String, Number], required: true },
    parkId: [String, Number],
    buildingId: [String, Number],
    floorNo: [String, Number],
    parkName: { type: String, default: '' },
    buildingName: { type: String, default: '' },
  },
  emits: ['loaded'],
  data() {
    return {
      Plus,
      activeTab: 'info',
      roomLoading: false,
      contractLoading: false,
      billLoading: false,
      room: {},
      contracts: [],
      bills: [],
      utilityRecords: [],
      vehicles: [],
      meterOptions: [],
      contractPage: { currentPage: 1, pageSize: 10, total: 0 },
      billPage: { currentPage: 1, pageSize: 10, total: 0 },
      utilityPage: { currentPage: 1, pageSize: 10, total: 0 },
      vehiclePage: { currentPage: 1, pageSize: 10, total: 0 },
      utilityLoading: false,
      vehicleLoading: false,
      utilityDialogVisible: false,
      vehicleDialogVisible: false,
      utilitySaving: false,
      vehicleSaving: false,
      utilityForm: emptyUtilityForm(this.roomId),
      vehicleForm: emptyVehicleForm(this.roomId),
      utilityRules: {
        deviceId: [{ required: true, message: '请选择水表或电表', trigger: 'change' }],
        readingTime: [{ required: true, message: '请选择抄表时间', trigger: 'change' }],
        previousReading: [{ required: true, message: '请输入上次读数', trigger: 'change' }],
        currentReading: [{ required: true, message: '请输入本次读数', trigger: 'change' }],
      },
      vehicleRules: {
        plateNo: [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
        vehicleType: [{ required: true, message: '请选择车辆类型', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
      },
    };
  },
  computed: {
    roomContextText() {
      return [this.parkName, this.buildingName, this.floorNo ? `${this.floorNo}层` : '', this.room.name]
        .filter(Boolean)
        .join(' / ');
    },
    basicItems() {
      return [
        { label: '房源名称', value: this.value(this.room.name) },
        { label: '所属楼宇', value: this.value(this.room.buildingName || this.buildingName) },
        { label: '所在楼层', value: this.measure(this.room.floor || this.floorNo, '层') },
        { label: '房源地址', value: this.value(this.room.address || this.roomContextText), wide: true },
        { label: '建筑面积', value: this.measure(this.room.area, '㎡') },
        { label: '户型', value: this.value(this.room.houseType) },
        { label: '朝向', value: this.value(this.room.orientation) },
        { label: '物业费', value: this.measure(this.room.propertyFee, '元') },
        { label: '配套设施', value: this.value(this.room.facilities), wide: true },
      ];
    },
    leasingItems() {
      return [
        { label: '房源状态', value: this.roomStatusText(this.room.status) },
        { label: '月租金', value: this.measure(this.room.rentPrice, '元/月') },
        { label: '空置开始', value: this.value(this.room.vacantSince) },
        { label: '同步状态', value: this.room.syncStatus === '1' ? '已同步' : '待同步' },
        { label: '最近同步', value: this.value(this.room.syncTime) },
        { label: '核心卖点', value: this.value(this.room.highlights), wide: true },
        { label: '备注', value: this.value(this.room.memo), wide: true },
      ];
    },
    roomImages() {
      return this.parseImages(this.room.sceneImages);
    },
  },
  watch: {
    roomId: {
      immediate: true,
      handler() {
        this.activeTab = 'info';
        this.contractPage.currentPage = 1;
        this.billPage.currentPage = 1;
        this.utilityPage.currentPage = 1;
        this.vehiclePage.currentPage = 1;
        this.loadAll();
      },
    },
  },
  methods: {
    payload(res) {
      return res && res.data ? res.data.data : null;
    },
    loadAll() {
      this.loadRoom();
      this.loadContracts();
      this.loadBills();
      this.loadUtilityRecords();
      this.loadVehicles();
      this.loadMeterOptions();
    },
    loadRoom() {
      if (!this.roomId) return;
      this.roomLoading = true;
      getRoomDetail(this.roomId)
        .then(res => {
          this.room = this.payload(res) || {};
          this.$emit('loaded', this.room);
        })
        .finally(() => {
          this.roomLoading = false;
        });
    },
    loadContracts() {
      this.contractLoading = true;
      getRoomContractPage(this.roomId, this.contractPage.currentPage, this.contractPage.pageSize)
        .then(res => {
          const data = this.payload(res) || {};
          this.contracts = data.records || [];
          this.contractPage.total = Number(data.total || 0);
        })
        .finally(() => {
          this.contractLoading = false;
        });
    },
    loadBills() {
      this.billLoading = true;
      getRoomPaymentPage(this.roomId, this.billPage.currentPage, this.billPage.pageSize)
        .then(res => {
          const data = this.payload(res) || {};
          this.bills = data.records || [];
          this.billPage.total = Number(data.total || 0);
        })
        .finally(() => {
          this.billLoading = false;
        });
    },
    loadUtilityRecords() {
      this.utilityLoading = true;
      getRoomUtilityPage(this.roomId, this.utilityPage.currentPage, this.utilityPage.pageSize)
        .then(res => {
          const data = this.payload(res) || {};
          this.utilityRecords = data.records || [];
          this.utilityPage.total = Number(data.total || 0);
        })
        .finally(() => {
          this.utilityLoading = false;
        });
    },
    loadVehicles() {
      this.vehicleLoading = true;
      getRoomVehiclePage(this.roomId, this.vehiclePage.currentPage, this.vehiclePage.pageSize)
        .then(res => {
          const data = this.payload(res) || {};
          this.vehicles = data.records || [];
          this.vehiclePage.total = Number(data.total || 0);
        })
        .finally(() => {
          this.vehicleLoading = false;
        });
    },
    loadMeterOptions() {
      getDevicePage(1, 200, { roomId: this.roomId, deviceTypeGroup: 'meter' }).then(res => {
        const data = this.payload(res) || {};
        this.meterOptions = data.records || [];
      });
    },
    changeContractPage(current) {
      this.contractPage.currentPage = current;
      this.loadContracts();
    },
    changeBillPage(current) {
      this.billPage.currentPage = current;
      this.loadBills();
    },
    changeUtilityPage(current) {
      this.utilityPage.currentPage = current;
      this.loadUtilityRecords();
    },
    changeVehiclePage(current) {
      this.vehiclePage.currentPage = current;
      this.loadVehicles();
    },
    openUtilityDialog() {
      this.utilityForm = emptyUtilityForm(this.roomId);
      this.utilityForm.readingTime = this.formatDateTime(new Date());
      this.utilityDialogVisible = true;
      this.$nextTick(() => this.$refs.utilityFormRef && this.$refs.utilityFormRef.clearValidate());
    },
    handleUtilityDeviceChange(deviceId) {
      const device = this.meterOptions.find(item => String(item.deviceId) === String(deviceId));
      this.utilityForm.previousReading = Number((device && device.currentReading) || 0);
    },
    submitUtility() {
      this.$refs.utilityFormRef.validate(valid => {
        if (!valid) return;
        if (Number(this.utilityForm.currentReading) < Number(this.utilityForm.previousReading || 0)) {
          ElMessage.warning('本次读数不能小于上次读数');
          return;
        }
        this.utilitySaving = true;
        submitRoomUtility(this.utilityForm)
          .then(() => {
            ElMessage.success('水电记录已新增');
            this.utilityDialogVisible = false;
            this.utilityPage.currentPage = 1;
            this.loadUtilityRecords();
          })
          .finally(() => {
            this.utilitySaving = false;
          });
      });
    },
    deleteUtility(row) {
      ElMessageBox.confirm('确定删除这条水电记录吗？', '提示', { type: 'warning' })
        .then(() => removeRoomUtility(row.recordId))
        .then(() => {
          ElMessage.success('删除成功');
          this.loadUtilityRecords();
        });
    },
    openVehicleDialog() {
      this.vehicleForm = emptyVehicleForm(this.roomId);
      this.vehicleDialogVisible = true;
      this.$nextTick(() => this.$refs.vehicleFormRef && this.$refs.vehicleFormRef.clearValidate());
    },
    submitVehicle() {
      this.$refs.vehicleFormRef.validate(valid => {
        if (!valid) return;
        const range = this.vehicleForm.validRange || [];
        const payload = {
          ...this.vehicleForm,
          validStart: range[0] || null,
          validEnd: range[1] || null,
        };
        delete payload.validRange;
        this.vehicleSaving = true;
        submitRoomVehicle(payload)
          .then(() => {
            ElMessage.success('车辆已绑定');
            this.vehicleDialogVisible = false;
            this.vehiclePage.currentPage = 1;
            this.loadVehicles();
          })
          .finally(() => {
            this.vehicleSaving = false;
          });
      });
    },
    deleteVehicle(row) {
      ElMessageBox.confirm(`确定删除车辆「${row.plateNo}」吗？`, '提示', { type: 'warning' })
        .then(() => removeRoomVehicle(row.vehicleId))
        .then(() => {
          ElMessage.success('删除成功');
          this.loadVehicles();
        });
    },
    vehicleTypeText(value) {
      return { car: '小型汽车', truck: '货车', motorcycle: '摩托车', other: '其他' }[value] || '-';
    },
    formatDateTime(date) {
      const pad = value => String(value).padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    },
    value(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    measure(value, unit) {
      if (value === null || value === undefined || value === '') return '-';
      return `${Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 })}${unit}`;
    },
    formatMoney(value) {
      return `￥${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    },
    roomStatusText(value) {
      return { 0: '空置', 1: '待清退/短租', 2: '预留', 3: '待退出', 4: '90天内到期', 5: '30天内到期', 6: '已到期', 7: '已出租' }[String(value)] || '-';
    },
    paymentStatusText(value) {
      return { 0: '待缴费', 1: '已缴费', 2: '已逾期', 3: '部分缴费' }[String(value)] || '待缴费';
    },
    paymentStatusType(value) {
      return { 0: 'primary', 1: 'success', 2: 'danger', 3: 'warning' }[String(value)] || 'info';
    },
    paymentDisplayStatus(row) {
      if (String(row.payStatus) === '1' || String(row.payStatus) === '3') return String(row.payStatus);
      if (row.payDeadline && new Date(`${row.payDeadline}T23:59:59`).getTime() < Date.now()) return '2';
      return String(row.payStatus || '0');
    },
    contractStatusText(value) {
      return { 0: '待审批', 1: '生效', 2: '已到期', 3: '已续签', 4: '已退租', 5: '待盖章', 6: '退租中', 7: '退租交接中', 8: '房屋验收中' }[String(value)] || '-';
    },
    contractStatusType(value) {
      return { 0: 'primary', 1: 'success', 2: 'warning', 3: 'success', 4: 'info', 5: 'warning', 6: 'warning', 7: 'warning', 8: 'warning' }[String(value)] || 'info';
    },
    parseImages(value) {
      if (!value) return [];
      if (Array.isArray(value)) return value.map(item => (typeof item === 'string' ? item : item.url || item.link || '')).filter(Boolean);
      const text = String(value).trim();
      if (!text) return [];
      if (text.startsWith('[')) {
        try {
          return this.parseImages(JSON.parse(text));
        } catch (error) {
          return [];
        }
      }
      return text.split(',').map(item => item.trim()).filter(Boolean);
    },
  },
};
</script>

<style scoped>
.room-workbench {
  min-width: 0;
}

.room-workbench-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.room-profile,
.room-data-panel,
.room-empty-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.room-profile {
  padding: 20px 22px;
}

.room-profile-section + .room-profile-section {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px solid #ebeef5;
}

.room-profile-section h3 {
  margin: 0 0 16px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.room-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 30px;
}

.room-info-item {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  line-height: 24px;
}

.room-info-item.is-wide {
  grid-column: span 2;
}

.room-info-item span {
  color: #909399;
  white-space: nowrap;
}

.room-info-item strong {
  min-width: 0;
  color: #606266;
  font-weight: 400;
  overflow-wrap: anywhere;
}

.room-image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, 148px);
  gap: 12px;
}

.room-image-grid :deep(.el-image) {
  width: 148px;
  height: 106px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f5f7fa;
}

.room-data-panel {
  overflow: hidden;
}

.room-panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.room-panel-toolbar strong {
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

:global(.room-extension-dialog .el-select),
:global(.room-extension-dialog .el-date-editor),
:global(.room-extension-dialog .el-input-number) {
  width: 100%;
}

.room-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 16px 14px;
}

.room-empty-panel {
  min-height: 320px;
  padding-top: 40px;
}

@media (max-width: 1100px) {
  .room-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .room-info-grid {
    grid-template-columns: 1fr;
  }

  .room-info-item.is-wide {
    grid-column: auto;
  }
}
</style>
