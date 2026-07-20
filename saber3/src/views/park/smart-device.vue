<template>
  <div class="smart-device-ledger">
    <div class="meter-page">
      <header class="meter-header">
        <h3>智能水电表（{{ page.total }}）</h3>
        <div class="meter-header__actions">
          <el-button :icon="Refresh" @click="reload">刷新</el-button>
          <el-button v-if="permission.rent_control_device_add" type="primary" :icon="Plus" @click="openAdd">
            新增
          </el-button>
        </div>
      </header>

      <el-table
        v-loading="loading"
        :data="data"
        row-key="deviceId"
        class="meter-table"
        scrollbar-always-on
        empty-text="暂无智能水电表"
      >
        <el-table-column prop="deviceName" label="表名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="deviceType" label="设备类型" width="120" align="center">
          <template #default="{ row }">{{ deviceTypeText(row.deviceType) }}</template>
        </el-table-column>
        <el-table-column prop="paymentType" label="付费类型" width="120" align="center">
          <template #default="{ row }">{{ paymentTypeText(row.paymentType) }}</template>
        </el-table-column>
        <el-table-column label="所在位置" min-width="260" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ locationText(row) }}</template>
        </el-table-column>
        <el-table-column prop="currentReading" label="当前读数" width="130" align="center">
          <template #default="{ row }">{{ formatNumber(row.currentReading) }}</template>
        </el-table-column>
        <el-table-column prop="currentBalance" label="当前余额" width="130" align="center">
          <template #default="{ row }">{{ formatNumber(row.currentBalance) }}</template>
        </el-table-column>
        <el-table-column prop="deviceCode" label="序列号" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="onlineStatus" label="通信状态" width="120" align="center">
          <template #default="{ row }">
            <span class="communication-status" :class="communicationClass(row)">
              <i></i>{{ communicationText(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-dropdown
              v-if="permission.rent_control_device_edit || permission.rent_control_device_delete"
              trigger="click"
              @command="command => handleCommand(command, row)"
            >
              <el-button text type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="permission.rent_control_device_edit" command="edit">
                    编辑
                  </el-dropdown-item>
                  <el-dropdown-item v-if="permission.rent_control_device_delete" command="delete" divided>
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="meter-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-size="page.pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="form.deviceId ? '修改智能水电表' : '新增智能水电表'"
      size="860px"
      append-to-body
      destroy-on-close
      class="meter-drawer"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="meter-form">
        <section class="form-section">
          <h4>智能参数</h4>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="设备厂商" prop="brand">
                <el-select
                  v-model="form.brand"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="请选择或输入设备厂商"
                >
                  <el-option v-for="item in vendorOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="设备类型" prop="deviceType">
                <el-select v-model="form.deviceType" placeholder="请选择设备类型">
                  <el-option v-for="item in deviceTypes" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="设备序列号" prop="deviceCode">
                <el-input v-model="form.deviceCode" maxlength="100" placeholder="请输入设备序列号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="设备型号">
                <el-input v-model="form.deviceModel" maxlength="100" placeholder="请输入设备型号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="付费类型" prop="paymentType">
                <el-select v-model="form.paymentType" placeholder="请选择付费类型">
                  <el-option label="预付费" value="prepaid" />
                  <el-option label="后付费" value="postpaid" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <h4>基本信息</h4>
          <el-form-item label="表类型" prop="meterType">
            <el-select v-model="form.meterType" placeholder="请选择表类型">
              <el-option label="分表" value="branch" />
              <el-option label="总表" value="total" />
              <el-option label="公摊表" value="public" />
            </el-select>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="用途" prop="purpose">
                <el-radio-group v-model="form.purpose">
                  <el-radio-button label="分表" value="分表" />
                  <el-radio-button label="总表" value="总表" />
                  <el-radio-button label="公摊表" value="公摊表" />
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="表名称" prop="deviceName">
                <el-input v-model="form.deviceName" maxlength="100" placeholder="请输入表名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="倍率" prop="multiplier">
                <el-input-number v-model="form.multiplier" :min="0.01" :precision="2" :step="0.1" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="最大读数" prop="maxReading">
                <el-input-number v-model="form.maxReading" :min="0.01" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当前读数">
                <el-input-number v-model="form.currentReading" :min="0" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当前余额">
                <el-input-number v-model="form.currentBalance" :min="0" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section location-section">
          <h4>安装位置</h4>
          <el-form-item prop="buildingId" class="location-tree-form-item">
            <div class="location-card">
              <div class="location-card__header">
                <strong>选择房源</strong>
                <el-button text type="primary" :disabled="!selectedLocationText" @click="showSelectedLocation">
                  查看已选
                </el-button>
              </div>
              <div v-loading="locationTreeLoading" class="location-card__body">
                <el-tree
                  v-if="locationTree.length"
                  ref="locationTreeRef"
                  :data="locationTree"
                  node-key="key"
                  show-checkbox
                  check-strictly
                  check-on-click-node
                  highlight-current
                  :expand-on-click-node="false"
                  :default-expanded-keys="locationExpandedKeys"
                  :props="locationTreeProps"
                  class="location-tree"
                  @check="handleLocationTreeCheck"
                >
                  <template #default="{ node, data }">
                    <span class="location-tree-node">
                      <span class="location-tree-label">{{ node.label }}</span>
                      <el-tag v-if="data.type === 'building'" size="small" effect="plain">
                        {{ data.roomCount || 0 }}间
                      </el-tag>
                      <span v-if="data.type === 'room' && data.area" class="location-tree-meta">
                        {{ formatNumber(data.area) }}㎡
                      </span>
                    </span>
                  </template>
                </el-tree>
                <el-empty v-else description="暂无可选房源" :image-size="64" />
              </div>
              <div class="selected-location">
                <span>已选位置</span>
                <strong>{{ selectedLocationText || '暂未选择' }}</strong>
              </div>
            </div>
          </el-form-item>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="安装位置补充">
                <el-input v-model="form.installLocation" maxlength="200" placeholder="如：配电间东侧" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="通信状态">
                <el-select v-model="form.onlineStatus">
                  <el-option label="在线" value="0" />
                  <el-option label="离线" value="1" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section warning-section">
          <div class="section-heading">
            <h4>预警值设置</h4>
            <el-button type="primary" :icon="Plus" @click="addWarningRule">添加</el-button>
          </div>
          <el-table :data="form.warningRuleList" border class="warning-table" empty-text="暂无预警规则">
            <el-table-column label="预警值区间" min-width="300" align="center">
              <template #default="{ row }">
                <div class="warning-range">
                  <el-input-number v-model="row.minValue" :min="0" :precision="2" controls-position="right" />
                  <span>至</span>
                  <el-input-number v-model="row.maxValue" :min="0" :precision="2" controls-position="right" />
                </div>
              </template>
            </el-table-column>
            <el-table-column label="预警等级" min-width="150" align="center">
              <template #default="{ row }">
                <el-select v-model="row.level" placeholder="请选择">
                  <el-option label="提示" value="info" />
                  <el-option label="预警" value="warning" />
                  <el-option label="严重" value="danger" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ $index }">
                <el-button text type="danger" @click="removeWarningRule($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-form-item label="预警单位" class="warning-unit">
            <el-select v-model="form.warningUnit" clearable placeholder="请选择预警单位">
              <el-option label="元" value="yuan" />
              <el-option label="度" value="kwh" />
              <el-option label="立方米" value="m3" />
            </el-select>
          </el-form-item>
        </section>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Plus, Refresh } from '@element-plus/icons-vue';
import { getList as getParkList } from '@/api/park/park';
import { getBuildingList, getRoomList } from '@/api/park/rent-control';
import { getDeviceDetail, getDevicePage, removeDevice, submitDevice } from '@/api/park/smart-device';

const deviceTypes = [
  { label: '电表', value: 'electric' },
  { label: '水表', value: 'water' },
];

const emptyForm = () => ({
  deviceId: undefined,
  parkId: undefined,
  buildingId: undefined,
  floorNo: undefined,
  roomId: undefined,
  deviceName: '',
  deviceCode: '',
  deviceType: 'electric',
  brand: '',
  deviceModel: '',
  paymentType: 'postpaid',
  meterType: 'branch',
  purpose: '分表',
  currentReading: 0,
  currentBalance: 0,
  multiplier: 1,
  maxReading: undefined,
  warningUnit: '',
  warningRuleList: [],
  installLocation: '',
  onlineStatus: '1',
  enabledStatus: '0',
  remark: '',
});

export default {
  name: 'SmartDeviceLedger',
  components: { ArrowDown },
  props: {
    parkId: [String, Number],
    buildingId: [String, Number],
    floorNo: [String, Number],
  },
  data() {
    return {
      Plus,
      Refresh,
      deviceTypes,
      loading: false,
      saving: false,
      data: [],
      page: { currentPage: 1, pageSize: 10, total: 0 },
      drawerVisible: false,
      form: emptyForm(),
      parkOptions: [],
      buildingOptions: [],
      roomOptions: [],
      locationTreeLoading: false,
      selectedLocationKey: '',
      locationTreeProps: {
        label: 'label',
        children: 'children',
        disabled: 'disabled',
      },
      vendorOptions: ['正泰', '德力西', '威胜', '安科瑞', '海兴'],
      rules: {
        brand: [{ required: true, message: '请选择或输入设备厂商', trigger: 'change' }],
        deviceType: [{ required: true, message: '请选择设备类型', trigger: 'change' }],
        deviceCode: [{ required: true, message: '请输入设备序列号', trigger: 'blur' }],
        paymentType: [{ required: true, message: '请选择付费类型', trigger: 'change' }],
        meterType: [{ required: true, message: '请选择表类型', trigger: 'change' }],
        purpose: [{ required: true, message: '请选择用途', trigger: 'change' }],
        deviceName: [{ required: true, message: '请输入表名称', trigger: 'blur' }],
        multiplier: [{ required: true, message: '请输入倍率', trigger: 'change' }],
        maxReading: [{ required: true, message: '请输入最大读数', trigger: 'change' }],
        buildingId: [{ required: true, message: '请选择安装位置', trigger: 'change' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    locationFilterKey() {
      return [this.parkId || '', this.buildingId || '', this.floorNo || ''].join('|');
    },
    locationTree() {
      return this.buildLocationTree();
    },
    locationExpandedKeys() {
      const keys = [];
      if (this.form.parkId) keys.push(`park-${this.form.parkId}`);
      if (this.form.buildingId && (this.form.floorNo || this.form.roomId)) {
        keys.push(`building-${this.form.buildingId}`);
      }
      if (this.form.buildingId && this.form.floorNo && this.form.roomId) {
        keys.push(`floor-${this.form.buildingId}-${this.form.floorNo}`);
      }
      return keys;
    },
    selectedLocationText() {
      if (!this.form.buildingId) return '';
      const park = this.parkOptions.find(item => String(item.id) === String(this.form.parkId));
      const building = this.buildingOptions.find(item => String(item.id) === String(this.form.buildingId));
      const room = this.roomOptions.find(item => String(item.id) === String(this.form.roomId));
      return [
        park && park.name,
        building && building.name,
        this.form.floorNo ? `${this.form.floorNo}F` : '',
        room && room.name,
      ].filter(Boolean).join(' / ');
    },
  },
  watch: {
    locationFilterKey() {
      this.refreshByLocation();
    },
  },
  created() {
    this.loadReferences();
    this.loadPage();
  },
  methods: {
    loadReferences() {
      this.locationTreeLoading = true;
      Promise.all([getParkList(1, 999, {}), getBuildingList({}), getRoomList({})])
        .then(([parkRes, buildingRes, roomRes]) => {
          this.parkOptions = (parkRes.data.data && parkRes.data.data.records) || [];
          this.buildingOptions = buildingRes.data.data || [];
          this.roomOptions = roomRes.data.data || [];
          this.$nextTick(this.syncLocationTreeSelection);
        })
        .finally(() => {
          this.locationTreeLoading = false;
        });
    },
    reload() {
      this.loadPage();
      this.loadReferences();
    },
    loadPage() {
      this.loading = true;
      getDevicePage(this.page.currentPage, this.page.pageSize, {
        deviceTypeGroup: 'meter',
        parkId: this.parkId || undefined,
        buildingId: this.buildingId || undefined,
        floorNo: this.floorNo || undefined,
      })
        .then(res => {
          const payload = res.data.data || {};
          this.data = payload.records || [];
          this.page.total = Number(payload.total) || 0;
          this.mergeVendorOptions();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    refreshByLocation() {
      this.page.currentPage = 1;
      this.loadPage();
    },
    mergeVendorOptions() {
      const vendors = this.data.map(item => item.brand).filter(Boolean);
      this.vendorOptions = Array.from(new Set([...this.vendorOptions, ...vendors]));
    },
    sizeChange(size) {
      this.page.pageSize = size;
      this.page.currentPage = 1;
      this.loadPage();
    },
    currentChange(current) {
      this.page.currentPage = current;
      this.loadPage();
    },
    deviceTypeText(value) {
      return this.deviceTypes.find(item => item.value === value)?.label || '-';
    },
    paymentTypeText(value) {
      return value === 'prepaid' ? '预付费' : value === 'postpaid' ? '后付费' : '-';
    },
    formatNumber(value) {
      const number = Number(value || 0);
      return number.toLocaleString(undefined, { maximumFractionDigits: 2 });
    },
    locationText(row) {
      return [
        row.parkName,
        row.buildingName,
        row.floorNo ? `${row.floorNo}F` : '',
        row.roomName,
        row.installLocation,
      ].filter(Boolean).join('/') || '-';
    },
    communicationText(row) {
      if (String(row.enabledStatus) === '1') return '已停用';
      return String(row.onlineStatus) === '0' ? '在线' : '离线';
    },
    communicationClass(row) {
      if (String(row.enabledStatus) === '1') return 'is-disabled';
      return String(row.onlineStatus) === '0' ? 'is-online' : 'is-offline';
    },
    openAdd() {
      this.form = {
        ...emptyForm(),
        parkId: this.parkId ? String(this.parkId) : undefined,
        buildingId: this.buildingId ? String(this.buildingId) : undefined,
        floorNo: this.floorNo || undefined,
      };
      this.selectedLocationKey = this.locationKeyFromForm();
      this.drawerVisible = true;
      this.$nextTick(this.syncLocationTreeSelection);
    },
    openEdit(row) {
      getDeviceDetail(row.deviceId).then(res => {
        const detail = res.data.data || row;
        this.form = {
          ...emptyForm(),
          ...detail,
          parkId: detail.parkId ? String(detail.parkId) : undefined,
          buildingId: detail.buildingId ? String(detail.buildingId) : undefined,
          roomId: detail.roomId ? String(detail.roomId) : undefined,
          warningRuleList: this.parseWarningRules(detail.warningRules),
        };
        this.selectedLocationKey = this.locationKeyFromForm();
        this.drawerVisible = true;
        this.$nextTick(this.syncLocationTreeSelection);
      });
    },
    handleCommand(command, row) {
      if (command === 'edit') this.openEdit(row);
      if (command === 'delete') this.removeRow(row);
    },
    buildLocationTree() {
      const roomsByBuilding = new Map();
      this.roomOptions.forEach(room => {
        const key = String(room.buildingId || '');
        if (!key) return;
        if (!roomsByBuilding.has(key)) roomsByBuilding.set(key, []);
        roomsByBuilding.get(key).push(room);
      });
      return this.parkOptions.map(park => {
        const buildings = this.buildingOptions
          .filter(building => String(building.parkId) === String(park.id))
          .map(building => this.buildLocationBuildingNode(park, building, roomsByBuilding.get(String(building.id)) || []));
        return {
          key: `park-${park.id}`,
          label: park.name || '未命名园区',
          type: 'park',
          parkId: park.id,
          disabled: true,
          children: buildings,
        };
      });
    },
    buildLocationBuildingNode(park, building, rooms) {
      const floorMap = new Map();
      [...rooms]
        .sort((left, right) => Number(left.floor || 0) - Number(right.floor || 0))
        .forEach(room => {
          const floorValue = room.floor === null || room.floor === undefined ? '' : room.floor;
          const floorKey = String(floorValue || 'none');
          if (!floorMap.has(floorKey)) {
            floorMap.set(floorKey, {
              key: `floor-${building.id}-${floorKey}`,
              label: floorValue ? `${floorValue}F` : '未配置楼层',
              type: 'floor',
              parkId: park.id,
              buildingId: building.id,
              floorNo: floorValue || undefined,
              disabled: !floorValue,
              children: [],
            });
          }
          floorMap.get(floorKey).children.push({
            key: `room-${room.id}`,
            label: room.name || '未命名房间',
            type: 'room',
            parkId: room.parkId || park.id,
            buildingId: room.buildingId || building.id,
            floorNo: room.floor,
            roomId: room.id,
            area: room.area,
          });
        });
      return {
        key: `building-${building.id}`,
        label: building.name || '未命名楼宇',
        type: 'building',
        parkId: park.id,
        buildingId: building.id,
        roomCount: rooms.length,
        children: Array.from(floorMap.values()),
      };
    },
    handleLocationTreeCheck(data, state) {
      const tree = this.$refs.locationTreeRef;
      if (!tree || data.disabled) {
        this.syncLocationTreeSelection();
        return;
      }
      const checked = (state.checkedKeys || []).includes(data.key);
      if (!checked) {
        this.clearLocationSelection();
        return;
      }
      tree.setCheckedKeys([data.key]);
      tree.setCurrentKey(data.key);
      this.applyLocationNode(data);
    },
    applyLocationNode(data) {
      this.selectedLocationKey = data.key;
      this.form.parkId = data.parkId ? String(data.parkId) : undefined;
      this.form.buildingId = data.buildingId ? String(data.buildingId) : undefined;
      this.form.floorNo = data.type === 'floor' || data.type === 'room' ? data.floorNo : undefined;
      this.form.roomId = data.type === 'room' && data.roomId ? String(data.roomId) : undefined;
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate('buildingId'));
    },
    clearLocationSelection() {
      this.selectedLocationKey = '';
      this.form.parkId = undefined;
      this.form.buildingId = undefined;
      this.form.floorNo = undefined;
      this.form.roomId = undefined;
      const tree = this.$refs.locationTreeRef;
      if (tree) {
        tree.setCheckedKeys([]);
        tree.setCurrentKey(null);
      }
    },
    locationKeyFromForm() {
      if (this.form.roomId) return `room-${this.form.roomId}`;
      if (this.form.buildingId && this.form.floorNo) return `floor-${this.form.buildingId}-${this.form.floorNo}`;
      if (this.form.buildingId) return `building-${this.form.buildingId}`;
      return '';
    },
    syncLocationTreeSelection() {
      const tree = this.$refs.locationTreeRef;
      if (!tree) return;
      const key = this.locationKeyFromForm();
      this.selectedLocationKey = key;
      tree.setCheckedKeys(key ? [key] : []);
      tree.setCurrentKey(key || null);
    },
    showSelectedLocation() {
      if (this.selectedLocationText) ElMessage.info(this.selectedLocationText);
    },
    addWarningRule() {
      this.form.warningRuleList.push({ minValue: 0, maxValue: undefined, level: 'warning' });
    },
    removeWarningRule(index) {
      this.form.warningRuleList.splice(index, 1);
    },
    parseWarningRules(value) {
      if (!value) return [];
      try {
        const rules = typeof value === 'string' ? JSON.parse(value) : value;
        return Array.isArray(rules) ? rules : [];
      } catch (error) {
        return [];
      }
    },
    validateWarningRules() {
      const invalid = this.form.warningRuleList.some(item =>
        item.maxValue === undefined || item.maxValue === null || Number(item.minValue) > Number(item.maxValue) || !item.level
      );
      if (invalid) ElMessage.warning('请完整填写预警区间，且起始值不能大于结束值');
      return !invalid;
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid || !this.validateWarningRules()) return;
        const { warningRuleList, ...payload } = this.form;
        payload.warningRules = warningRuleList.length ? JSON.stringify(warningRuleList) : '';
        this.saving = true;
        submitDevice(payload)
          .then(() => {
            ElMessage.success('保存成功');
            this.drawerVisible = false;
            this.loadPage();
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    removeRow(row) {
      ElMessageBox.confirm(`确定删除智能表“${row.deviceName}”?`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeDevice(row.deviceId))
        .then(() => {
          ElMessage.success('删除成功');
          this.loadPage();
        });
    },
  },
};
</script>

<style scoped>
.meter-page {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.meter-header {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e7eaf0;
}

.meter-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.meter-header__actions {
  display: flex;
  gap: 12px;
}

.meter-table :deep(.el-table__header th) {
  height: 58px;
  color: #6f7785;
  background: #fff;
  font-weight: 500;
}

.meter-table :deep(.el-table__row td) {
  height: 54px;
}

.meter-table :deep(.el-table__cell),
.meter-table :deep(.cell) {
  text-align: center;
}

.communication-status {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  white-space: nowrap;
}

.communication-status i {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  border-radius: 50%;
}

.communication-status.is-online i { background: #32b620; }
.communication-status.is-offline i { background: #f23b42; }
.communication-status.is-disabled i { background: #9ca3af; }

.meter-pagination {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  border-top: 1px solid #edf0f5;
}

.meter-form {
  padding: 0 8px 24px;
}

.form-section + .form-section {
  margin-top: 24px;
}

.form-section h4 {
  position: relative;
  margin: 0 0 18px;
  padding-left: 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.form-section h4::before {
  position: absolute;
  top: 1px;
  bottom: 1px;
  left: 0;
  width: 4px;
  border-radius: 2px;
  background: #2f8dfd;
  content: '';
}

.meter-form :deep(.el-select),
.meter-form :deep(.el-input-number) {
  width: 100%;
}

.location-tree-form-item :deep(.el-form-item__content) {
  display: block;
}

.location-card {
  overflow: hidden;
  width: 100%;
  border: 1px solid #dfe4ec;
  border-radius: 6px;
  background: #fff;
}

.location-card__header {
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 0 18px;
  border-bottom: 1px solid #edf0f5;
}

.location-card__header strong {
  position: relative;
  padding-left: 12px;
  color: #303133;
  font-size: 14px;
}

.location-card__header strong::before {
  position: absolute;
  top: 1px;
  bottom: 1px;
  left: 0;
  width: 4px;
  border-radius: 2px;
  background: #2f8dfd;
  content: '';
}

.location-card__body {
  min-height: 230px;
  max-height: 320px;
  overflow: auto;
  padding: 14px 18px;
}

.location-tree {
  color: #606266;
  font-size: 14px;
}

.location-tree :deep(.el-tree-node__content) {
  min-height: 34px;
  border-radius: 4px;
}

.location-tree :deep(.el-tree-node__content:hover) {
  background: #f2f6fc;
}

.location-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #eef6ff;
}

.location-tree :deep(.el-tree-node.is-disabled > .el-tree-node__content > .el-checkbox) {
  display: none;
}

.location-tree-node {
  min-width: 0;
  flex: 1;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.location-tree-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-tree-meta {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.selected-location {
  min-height: 48px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 18px;
  border-top: 1px solid #edf0f5;
  background: #fafbfc;
}

.selected-location span {
  flex: 0 0 auto;
  color: #909399;
}

.selected-location strong {
  min-width: 0;
  overflow: hidden;
  color: #303133;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.section-heading h4 {
  margin-bottom: 0;
}

.warning-table :deep(.el-table__header th),
.warning-table :deep(.el-table__cell),
.warning-table :deep(.cell) {
  text-align: center;
}

.warning-range {
  display: grid;
  grid-template-columns: minmax(100px, 1fr) auto minmax(100px, 1fr);
  align-items: center;
  gap: 10px;
}

.warning-unit {
  margin-top: 20px;
}

@media (max-width: 900px) {
  .meter-header {
    align-items: flex-start;
    gap: 12px;
    padding: 16px;
  }

  .meter-form :deep(.el-col-12) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
