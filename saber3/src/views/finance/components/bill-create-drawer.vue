<template>
  <el-drawer
    v-model="visible"
    :size="drawerSize"
    append-to-body
    class="bill-create-drawer"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <template #header>
      <div class="bill-create-header">
        <span>{{ drawerTitle }}</span>
      </div>
    </template>

    <div class="bill-create-body">
      <section class="bill-create-form-card">
        <div class="bill-create-section-title">账单信息</div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="bill-create-form"
        >
          <el-row :gutter="20">
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="关联合同" prop="contractId">
                <el-select
                  v-model="form.contractId"
                  filterable
                  remote
                  clearable
                  :remote-method="loadContractOptions"
                  :loading="contractLoading"
                  placeholder="输入租客名称、合同名称或编号搜索"
                  @change="handleContractChange"
                >
                  <el-option
                    v-for="item in contractOptions"
                    :key="item.contractId"
                    :label="contractOptionLabel(item)"
                    :value="item.contractId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item :label="partyLabel">
                <el-input v-model="form.partyName" disabled :placeholder="`选择合同后自动带出${partyLabel}`" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="费用类型" prop="feeType">
                <el-select v-model="form.feeType" placeholder="请选择费用类型" @change="handleFeeTypeChange">
                  <el-option
                    v-for="item in feeTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="币种">
                <el-select v-model="form.currency" disabled>
                  <el-option label="人民币" value="CNY" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="计费周期" prop="periodRange">
                <el-date-picker
                  v-model="form.periodRange"
                  type="daterange"
                  value-format="YYYY-MM-DD"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  range-separator="-"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item :label="amountLabel" prop="amountDue">
                <el-input-number
                  v-model="form.amountDue"
                  :min="0"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  :placeholder="`请输入${amountLabel}`"
                />
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="税率">
                <div class="bill-number-unit bill-number-unit--percent">
                  <el-input-number v-model="form.taxRate" :min="0" :max="100" :precision="2" :controls="false" />
                  <span class="bill-number-unit__suffix">%</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item :label="deadlineLabel" prop="payDeadline">
                <el-date-picker
                  v-model="form.payDeadline"
                  type="date"
                  value-format="YYYY-MM-DD"
                  :placeholder="`请选择${deadlineLabel}`"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="特殊账单类型" prop="specialBillType">
                <el-select v-model="form.specialBillType" placeholder="请选择类型">
                  <el-option label="常规账单" value="regular" />
                  <el-option label="临时账单" value="temporary" />
                  <el-option label="调整账单" value="adjustment" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="滞纳金起算天数">
                <div class="bill-number-unit bill-number-unit--day">
                  <el-input-number v-model="form.lateFeeStartDays" :min="0" :controls="false" placeholder="请输入起算天数" />
                  <span class="bill-number-unit__suffix">天</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="滞纳金比例">
                <div class="bill-number-unit bill-number-unit--rate-day">
                  <el-input-number v-model="form.lateFeeRatio" :min="0" :precision="4" :controls="false" placeholder="请输入滞纳金比例" />
                  <span class="bill-number-unit__suffix">%/天</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="滞纳金上限">
                <div class="bill-number-unit bill-number-unit--percent">
                  <el-input-number v-model="form.lateFeeCap" :min="0" :precision="2" :controls="false" placeholder="请输入滞纳金上限" />
                  <span class="bill-number-unit__suffix">%</span>
                </div>
              </el-form-item>
            </el-col>

            <el-col :xs="24" :md="12" :xl="8">
              <el-form-item label="所属公司">
                <el-select v-model="form.companyName">
                  <el-option label="吴中金融招商服务有限公司" value="吴中金融招商服务有限公司" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="账单备注">
                <el-input
                  v-model="form.remark"
                  type="textarea"
                  :rows="4"
                  maxlength="500"
                  show-word-limit
                  placeholder="请输入账单备注"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="上传附件">
                <el-upload
                  ref="uploadRef"
                  action="/api/blade-resource/oss/endpoint/put-file"
                  :headers="uploadHeaders"
                  :limit="1"
                  :file-list="uploadFileList"
                  accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.pdf"
                  :before-upload="beforeUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :on-remove="handleUploadRemove"
                >
                  <el-button type="primary" plain icon="el-icon-upload">选择文件</el-button>
                  <template #tip>
                    <div class="bill-upload-tip">单个文件不超过 5MB，支持 doc/xls/ppt/txt/pdf 格式</div>
                  </template>
                </el-upload>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </section>

      <aside class="bill-create-room-card">
        <div class="bill-create-section-title">可选空置房源</div>
        <div v-loading="roomLoading" class="bill-create-room-content">
          <el-tree
            v-if="selectedRoomTree.length"
            ref="roomTreeRef"
            :data="selectedRoomTree"
            node-key="id"
            show-checkbox
            check-on-click-node
            default-expand-all
            :expand-on-click-node="false"
            :props="roomTreeProps"
            class="bill-create-room-tree"
            @check="handleRoomTreeCheck"
          >
            <template #default="{ node, data }">
              <span :class="['bill-room-tree-node', data.type === 'room' ? 'is-room' : '']">
                <span class="bill-room-tree-label">{{ node.label }}</span>
                <span v-if="data.type === 'room' && data.area" class="bill-room-tree-meta">
                  {{ formatNumber(data.area) }}㎡
                </span>
              </span>
            </template>
          </el-tree>
          <el-empty v-else description="暂无可选空置房源" :image-size="72" />
        </div>
      </aside>
    </div>

    <template #footer>
      <div class="bill-create-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit(false)">保存</el-button>
        <el-button type="primary" :loading="saving" @click="submit(true)">保存并新建下一个</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script>
import { createPayment, getPaymentContractOptions } from '@/api/ics/payment';
import { getRoomList } from '@/api/park/rent-control';
import { getToken } from '@/utils/auth';

const COMPANY_NAME = '吴中金融招商服务有限公司';

const createDefaultForm = direction => ({
  contractId: '',
  partyName: '',
  feeType: direction === 'payable' ? 'deposit_refund' : 'rent',
  feeName: direction === 'payable' ? '押金退还' : '租金',
  currency: 'CNY',
  periodRange: [],
  amountDue: undefined,
  selectedRoomIds: '',
  selectedRoomName: '',
  selectedBuildingName: '',
  taxRate: 0,
  payDeadline: '',
  specialBillType: 'regular',
  lateFeeStartDays: 0,
  lateFeeRatio: 0,
  lateFeeCap: undefined,
  companyName: COMPANY_NAME,
  remark: '',
  attachmentName: '',
  attachmentUrl: '',
});

export default {
  name: 'BillCreateDrawer',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    direction: {
      type: String,
      default: 'receivable',
    },
  },
  emits: ['update:modelValue', 'saved'],
  data() {
    return {
      saving: false,
      contractLoading: false,
      roomLoading: false,
      contractOptions: [],
      selectedContract: {},
      availableRoomTree: [],
      selectedRoomKeys: [],
      roomTreeProps: {
        label: 'label',
        children: 'children',
        disabled: 'disabled',
      },
      form: createDefaultForm(this.direction),
      uploadFileList: [],
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      rules: {
        contractId: [{ required: true, message: '请选择关联合同', trigger: 'change' }],
        feeType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
        periodRange: [{ type: 'array', required: true, len: 2, message: '请选择计费周期', trigger: 'change' }],
        amountDue: [{ required: true, message: '请输入账单金额', trigger: 'blur' }],
        payDeadline: [{ required: true, message: '请选择账单日期', trigger: 'change' }],
        specialBillType: [{ required: true, message: '请选择特殊账单类型', trigger: 'change' }],
      },
    };
  },
  computed: {
    visible: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      },
    },
    isPayable() {
      return this.direction === 'payable';
    },
    drawerTitle() {
      return this.isPayable ? '新建付款账单' : '新建收款账单';
    },
    partyLabel() {
      return this.isPayable ? '收款方' : '付款方';
    },
    amountLabel() {
      return this.isPayable ? '应付金额' : '应收金额';
    },
    deadlineLabel() {
      return this.isPayable ? '应付日期' : '应收日期';
    },
    drawerSize() {
      return '92%';
    },
    feeTypeOptions() {
      if (this.isPayable) {
        return [
          { label: '押金退还', value: 'deposit_refund' },
          { label: '维修支出', value: 'maintenance' },
          { label: '服务采购', value: 'service_procurement' },
          { label: '其他支出', value: 'other' },
        ];
      }
      return [
        { label: '租金', value: 'rent' },
        { label: '物业费', value: 'property' },
        { label: '管业管理费', value: 'management' },
        { label: '公摊费', value: 'public' },
        { label: '水电费', value: 'utility' },
        { label: '其他', value: 'other' },
      ];
    },
    selectedRoomTree() {
      return this.availableRoomTree;
    },
  },
  watch: {
    modelValue(value) {
      if (value) {
        this.resetForm();
        this.loadContractOptions('');
        this.loadAvailableRooms();
      }
    },
  },
  methods: {
    loadContractOptions(keyword = '') {
      this.contractLoading = true;
      getPaymentContractOptions(keyword)
        .then(res => {
          this.contractOptions = res.data.data || [];
        })
        .finally(() => {
          this.contractLoading = false;
        });
    },
    loadAvailableRooms() {
      this.roomLoading = true;
      getRoomList({})
        .then(res => {
          const rooms = (res.data.data || []).filter(room => this.isVacantRoom(room));
          this.availableRoomTree = this.buildRoomTreeFromRooms(rooms);
          this.$nextTick(() => {
            this.syncRoomTreeCheckedKeys(this.selectedRoomKeys);
          });
        })
        .finally(() => {
          this.roomLoading = false;
        });
    },
    isVacantRoom(room) {
      const baseStatus = room && room.baseStatus !== undefined && room.baseStatus !== null && room.baseStatus !== ''
        ? room.baseStatus
        : room.status;
      return String(baseStatus) === '0';
    },
    handleContractChange(contractId) {
      this.selectedContract = this.contractOptions.find(item => String(item.contractId) === String(contractId)) || {};
      const contract = this.selectedContract;
      this.form.partyName = contract.customerName || '';
      this.form.periodRange = [];
      this.form.payDeadline = '';
      if (contract.startDate && contract.endDate) {
        this.form.periodRange = [this.dateOnly(contract.startDate), this.dateOnly(contract.endDate)];
      }
      this.form.payDeadline = this.dateOnly(contract.startDate) || this.form.payDeadline;
      this.form.lateFeeRatio = Number(contract.lateFeeRatio || 0);
      this.form.lateFeeCap = contract.lateFeeCap === null || contract.lateFeeCap === undefined
        ? undefined
        : Number(contract.lateFeeCap);
    },
    buildRoomTreeFromRooms(rooms, contract = {}) {
      const parkMap = {};
      rooms.forEach(room => {
        const parkId = room.parkId || contract.parkId || 'none';
        const parkName = room.parkName || contract.parkName || '未配置园区';
        const buildingId = room.buildingId || 'none';
        const buildingName = room.buildingName || contract.buildingName || '未配置楼宇';
        const floorNo = room.floor === null || room.floor === undefined ? '未配置楼层' : `${room.floor}F`;
        const parkKey = `park-${parkId}`;
        const buildingKey = `${parkKey}-building-${buildingId}`;
        const floorKey = `${buildingKey}-floor-${floorNo}`;
        if (!parkMap[parkKey]) {
          parkMap[parkKey] = {
            id: parkKey,
            label: parkName,
            type: 'park',
            children: [],
            buildingMap: {},
          };
        }
        const parkNode = parkMap[parkKey];
        if (!parkNode.buildingMap[buildingKey]) {
          parkNode.buildingMap[buildingKey] = {
            id: buildingKey,
            label: buildingName,
            type: 'building',
            children: [],
            floorMap: {},
          };
          parkNode.children.push(parkNode.buildingMap[buildingKey]);
        }
        const buildingNode = parkNode.buildingMap[buildingKey];
        if (!buildingNode.floorMap[floorKey]) {
          buildingNode.floorMap[floorKey] = {
            id: floorKey,
            label: floorNo,
            type: 'floor',
            children: [],
          };
          buildingNode.children.push(buildingNode.floorMap[floorKey]);
        }
        buildingNode.floorMap[floorKey].children.push({
          id: `room-${room.id}`,
          roomId: room.id,
          label: room.name || '未命名房间',
          type: 'room',
          parkName,
          buildingName,
          floor: room.floor,
          area: room.area,
        });
      });
      return Object.values(parkMap).map(park => {
        delete park.buildingMap;
        park.children.forEach(building => {
          delete building.floorMap;
        });
        return park;
      });
    },
    handleRoomTreeCheck(data, checked) {
      const checkedNodes = checked && Array.isArray(checked.checkedNodes) ? checked.checkedNodes : [];
      const roomNodes = checkedNodes.filter(item => item && item.type === 'room' && !item.disabled);
      this.selectedRoomKeys = roomNodes.map(item => item.id);
      this.updateSelectedRooms(roomNodes);
    },
    syncSelectedRoomsFromTree() {
      const tree = this.$refs.roomTreeRef;
      if (!tree || !tree.getCheckedNodes) {
        return;
      }
      const checkedNodes = tree.getCheckedNodes(false, true) || [];
      this.updateSelectedRooms(checkedNodes.filter(item => item && item.type === 'room' && !item.disabled));
    },
    syncRoomTreeCheckedKeys(keys) {
      const tree = this.$refs.roomTreeRef;
      if (tree && tree.setCheckedKeys) {
        tree.setCheckedKeys(keys || []);
      }
    },
    updateSelectedRooms(roomNodes) {
      const roomMap = {};
      const buildingNames = [];
      const roomIds = [];
      const roomNames = [];
      (roomNodes || []).forEach(room => {
        const key = room.roomId || room.id;
        if (!key || roomMap[key]) return;
        roomMap[key] = true;
        if (room.roomId) roomIds.push(String(room.roomId));
        roomNames.push(room.label || '');
        if (room.buildingName && !buildingNames.includes(room.buildingName)) {
          buildingNames.push(room.buildingName);
        }
      });
      this.form.selectedRoomIds = roomIds.join(',');
      this.form.selectedRoomName = roomNames.filter(Boolean).join('、');
      this.form.selectedBuildingName = buildingNames.join('、');
    },
    roomLeafNodes(nodes = []) {
      const result = [];
      nodes.forEach(node => {
        if (node.type === 'room') {
          result.push(node);
          return;
        }
        result.push(...this.roomLeafNodes(node.children || []));
      });
      return result;
    },
    hasSelectableRooms() {
      return this.roomLeafNodes(this.selectedRoomTree).some(item => !item.disabled && item.roomId);
    },
    handleFeeTypeChange(value) {
      const item = this.feeTypeOptions.find(option => option.value === value);
      this.form.feeName = item ? item.label : '';
    },
    contractOptionLabel(item) {
      return [item.contractNo, item.customerName || item.contractName].filter(Boolean).join(' - ');
    },
    dateOnly(value) {
      return value ? String(value).slice(0, 10) : '';
    },
    formatNumber(value) {
      const number = Number(value || 0);
      if (!Number.isFinite(number)) return '0';
      return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 });
    },
    beforeUpload(file) {
      const allowed = /\.(doc|docx|xls|xlsx|ppt|pptx|txt|pdf)$/i.test(file.name || '');
      const underLimit = file.size / 1024 / 1024 <= 5;
      if (!allowed) this.$message.error('仅支持 doc、xls、ppt、txt、pdf 格式文件');
      if (!underLimit) this.$message.error('文件大小不能超过 5MB');
      return allowed && underLimit;
    },
    handleUploadSuccess(response, file) {
      if (!response || response.success === false) {
        this.$message.error((response && response.msg) || '附件上传失败');
        return;
      }
      const data = response.data || {};
      const fileUrl = typeof data === 'string'
        ? data
        : data.link || data.url || data.path || response.link || response.url || '';
      if (!fileUrl) {
        this.$message.error('附件上传成功，但未返回文件地址');
        return;
      }
      this.form.attachmentName = (file && file.name) || data.originalName || data.name || '';
      this.form.attachmentUrl = fileUrl;
      this.uploadFileList = [{ name: this.form.attachmentName, url: fileUrl }];
      this.$message.success('附件上传成功');
    },
    handleUploadError(error) {
      this.$message.error((error && error.message) || '附件上传失败');
    },
    handleUploadRemove() {
      this.form.attachmentName = '';
      this.form.attachmentUrl = '';
      this.uploadFileList = [];
    },
    submit(continueCreate) {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.syncSelectedRoomsFromTree();
        if (this.hasSelectableRooms() && !this.form.selectedRoomIds) {
          this.$message.warning('请选择房源');
          return;
        }
        const [periodStart, periodEnd] = this.form.periodRange;
        this.saving = true;
        createPayment({
          ...this.form,
          direction: this.direction,
          periodStart,
          periodEnd,
          periodRange: undefined,
          currency: undefined,
          partyName: undefined,
        })
          .then(res => {
            this.$message.success(`${this.drawerTitle}已保存`);
            this.$emit('saved', {
              direction: this.direction,
              payment: res.data.data || {},
            });
            if (continueCreate) {
              this.resetForm();
              this.loadContractOptions('');
              this.loadAvailableRooms();
            } else {
              this.visible = false;
            }
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    resetForm() {
      this.form = createDefaultForm(this.direction);
      this.selectedContract = {};
      this.selectedRoomKeys = [];
      this.availableRoomTree = [];
      this.uploadFileList = [];
      this.$nextTick(() => {
        if (this.$refs.formRef) this.$refs.formRef.clearValidate();
        if (this.$refs.uploadRef && this.$refs.uploadRef.clearFiles) this.$refs.uploadRef.clearFiles();
        this.syncRoomTreeCheckedKeys([]);
      });
    },
    handleClosed() {
      this.saving = false;
      this.resetForm();
    },
  },
};
</script>

<style lang="scss" scoped>
.bill-create-body {
  min-height: calc(100vh - 150px);
  padding: 20px;
  display: grid;
  grid-template-columns: minmax(620px, 1fr) minmax(330px, 0.95fr);
  gap: 18px;
  background: #f4f4f6;
}

.bill-create-form-card,
.bill-create-room-card {
  min-width: 0;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  overflow: hidden;
}

.bill-create-section-title {
  height: 58px;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
  color: #303133;
  font-size: 16px;
  line-height: 58px;
  font-weight: 600;
}

.bill-create-form {
  padding: 20px;
}

.bill-create-form :deep(.el-form-item) {
  position: relative;
  margin-bottom: 18px;
}

.bill-create-form :deep(.el-select),
.bill-create-form :deep(.el-date-editor),
.bill-create-form :deep(.el-input-number) {
  width: 100%;
}

.bill-create-form :deep(.el-form-item__label) {
  color: #606266;
  font-size: 14px;
}

.bill-number-unit {
  --bill-unit-width: 38px;
  --bill-unit-input-space: 48px;
  position: relative;
  width: 100%;
}

.bill-number-unit--day {
  --bill-unit-width: 38px;
}

.bill-number-unit--rate-day {
  --bill-unit-width: 58px;
  --bill-unit-input-space: 68px;
}

.bill-number-unit :deep(.el-input-number) {
  width: 100%;
}

.bill-number-unit :deep(.el-input-number .el-input__wrapper) {
  padding-right: var(--bill-unit-input-space);
}

.bill-number-unit :deep(.el-input-number .el-input__inner) {
  text-align: right;
}

.bill-number-unit__suffix {
  position: absolute;
  top: 1px;
  right: 1px;
  bottom: 1px;
  z-index: 2;
  width: var(--bill-unit-width);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-left: 1px solid #dcdfe6;
  border-radius: 0 3px 3px 0;
  background: #f5f7fa;
  color: #909399;
  font-size: 13px;
  line-height: 1;
  pointer-events: none;
}

.bill-upload-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

.bill-create-room-content {
  min-height: 520px;
  padding: 18px 20px;
}

.bill-create-room-tree {
  color: #606266;
  font-size: 14px;
}

.bill-create-room-tree :deep(.el-tree-node__content) {
  height: 34px;
  border-radius: 4px;
}

.bill-create-room-tree :deep(.el-tree-node__content:hover) {
  background: #f2f6fc;
}

.bill-create-room-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #eef6ff;
}

.bill-room-tree-node {
  min-width: 0;
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.bill-room-tree-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bill-room-tree-meta {
  flex: 0 0 auto;
  color: #909399;
  font-size: 12px;
}

.bill-create-header {
  color: #303133;
  font-size: 17px;
  font-weight: 600;
}

.bill-create-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.bill-create-drawer :deep(.el-drawer__header) {
  height: 58px;
  margin: 0;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
}

.bill-create-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f4f4f6;
}

.bill-create-drawer :deep(.el-drawer__footer) {
  padding: 12px 20px;
  border-top: 1px solid #e5e7eb;
  background: #fff;
}

@media (max-width: 1180px) {
  .bill-create-body {
    grid-template-columns: 1fr;
  }

  .bill-create-room-content {
    min-height: 240px;
  }
}
</style>
