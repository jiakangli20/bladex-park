<template>
  <div class="asset-ledger">
    <div class="asset-panel">
      <header class="asset-header">
        <h3>资产登记台账（{{ page.total }}）</h3>
        <div class="asset-header__actions">
          <el-button :icon="Refresh" @click="reload">刷新</el-button>
          <el-button v-if="permission.rent_control_asset_add" type="primary" :icon="Plus" @click="openAdd">
            登记资产
          </el-button>
        </div>
      </header>

      <div class="asset-filter">
        <el-input v-model="query.assetName" clearable placeholder="资产名称" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.assetCategory" clearable placeholder="资产分类" @change="search">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.assetStatus" clearable placeholder="资产状态" @change="search">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="data"
        row-key="assetId"
        class="asset-table"
        scrollbar-always-on
        empty-text="暂无资产登记"
      >
        <el-table-column prop="assetCode" label="资产编号" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column prop="assetName" label="资产名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="资产分类" width="120" align="center">
          <template #default="{ row }">{{ categoryText(row.assetCategory) }}</template>
        </el-table-column>
        <el-table-column label="安装位置" min-width="240" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ locationText(row) }}</template>
        </el-table-column>
        <el-table-column prop="brandModel" label="品牌型号" min-width="140" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.brandModel || '-' }}</template>
        </el-table-column>
        <el-table-column label="数量" width="100" align="center">
          <template #default="{ row }">{{ row.quantity || 0 }} {{ row.unit || '件' }}</template>
        </el-table-column>
        <el-table-column label="资产原值" width="130" align="center">
          <template #default="{ row }">{{ formatMoney(row.originalValue) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.assetStatus)" effect="plain">{{ statusText(row.assetStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responsiblePerson" label="责任人" width="120" align="center">
          <template #default="{ row }">{{ row.responsiblePerson || '-' }}</template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="购置日期" width="120" align="center">
          <template #default="{ row }">{{ row.purchaseDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right" align="center">
          <template #default="{ row }">
            <el-dropdown
              v-if="permission.rent_control_asset_edit || permission.rent_control_asset_delete"
              trigger="click"
              @command="command => handleCommand(command, row)"
            >
              <el-button text type="primary">更多<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="permission.rent_control_asset_edit" command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item v-if="permission.rent_control_asset_delete" command="delete" divided>
                    删除
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="asset-pagination">
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
      :title="form.assetId ? '编辑资产登记' : '登记资产'"
      size="860px"
      append-to-body
      destroy-on-close
      class="asset-drawer"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="asset-form">
        <section class="form-section">
          <h4>登记信息</h4>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="资产编号" prop="assetCode">
                <el-input v-model="form.assetCode" maxlength="100" placeholder="请输入资产编号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产名称" prop="assetName">
                <el-input v-model="form.assetName" maxlength="100" placeholder="请输入资产名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产分类" prop="assetCategory">
                <el-select v-model="form.assetCategory" placeholder="请选择资产分类">
                  <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="品牌型号">
                <el-input v-model="form.brandModel" maxlength="150" placeholder="请输入品牌及型号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="数量" prop="quantity">
                <el-input-number v-model="form.quantity" :min="1" :precision="0" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="计量单位" prop="unit">
                <el-select v-model="form.unit" filterable allow-create default-first-option placeholder="请选择或输入单位">
                  <el-option v-for="item in unitOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="购置日期">
                <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择购置日期" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产原值">
                <el-input-number v-model="form.originalValue" :min="0" :precision="2" controls-position="right" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="资产状态" prop="assetStatus">
                <el-select v-model="form.assetStatus" placeholder="请选择资产状态">
                  <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="责任人">
                <el-input v-model="form.responsiblePerson" maxlength="100" placeholder="请输入责任人" />
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
          <el-form-item label="安装位置补充">
            <el-input v-model="form.locationNote" maxlength="200" placeholder="如：设备间东侧" />
          </el-form-item>
        </section>

        <section class="form-section">
          <h4>备注</h4>
          <el-form-item>
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit />
          </el-form-item>
        </section>
      </el-form>

      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ArrowDown, Plus, Refresh, Search } from '@element-plus/icons-vue';
import { getList as getParkList } from '@/api/park/park';
import { getBuildingList, getRoomList } from '@/api/park/rent-control';
import { getAssetDetail, getAssetPage, removeAsset, submitAsset } from '@/api/park/asset-record';

const categoryOptions = [
  { label: '设施设备', value: 'facility' },
  { label: '办公资产', value: 'office' },
  { label: '安防资产', value: 'safety' },
  { label: '其他资产', value: 'other' },
];

const statusOptions = [
  { label: '在用', value: 'in_use', tagType: 'success' },
  { label: '闲置', value: 'idle', tagType: 'info' },
  { label: '维修中', value: 'repair', tagType: 'warning' },
  { label: '已报废', value: 'scrapped', tagType: 'danger' },
];

const emptyForm = () => ({
  assetId: undefined,
  parkId: undefined,
  buildingId: undefined,
  floorNo: undefined,
  roomId: undefined,
  assetCode: '',
  assetName: '',
  assetCategory: 'facility',
  brandModel: '',
  quantity: 1,
  unit: '件',
  purchaseDate: '',
  originalValue: 0,
  assetStatus: 'in_use',
  responsiblePerson: '',
  locationNote: '',
  remark: '',
});

export default {
  name: 'AssetLedger',
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
      Search,
      categoryOptions,
      statusOptions,
      unitOptions: ['件', '台', '套', '个', '组'],
      loading: false,
      saving: false,
      data: [],
      query: { assetName: '', assetCategory: '', assetStatus: '' },
      page: { currentPage: 1, pageSize: 10, total: 0 },
      drawerVisible: false,
      form: emptyForm(),
      parkOptions: [],
      buildingOptions: [],
      roomOptions: [],
      locationTreeLoading: false,
      locationTreeProps: { label: 'label', children: 'children', disabled: 'disabled' },
      rules: {
        assetCode: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
        assetName: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
        assetCategory: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
        quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
        unit: [{ required: true, message: '请选择或输入单位', trigger: 'change' }],
        assetStatus: [{ required: true, message: '请选择资产状态', trigger: 'change' }],
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
      if (this.form.buildingId && (this.form.floorNo || this.form.roomId)) keys.push(`building-${this.form.buildingId}`);
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
      return [park && park.name, building && building.name, this.form.floorNo ? `${this.form.floorNo}F` : '', room && room.name]
        .filter(Boolean)
        .join(' / ');
    },
  },
  watch: {
    locationFilterKey() {
      this.page.currentPage = 1;
      this.loadPage();
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
    loadPage() {
      this.loading = true;
      getAssetPage(this.page.currentPage, this.page.pageSize, {
        ...this.query,
        parkId: this.parkId || undefined,
        buildingId: this.buildingId || undefined,
        floorNo: this.floorNo || undefined,
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
    reload() {
      this.loadPage();
      this.loadReferences();
    },
    search() {
      this.page.currentPage = 1;
      this.loadPage();
    },
    resetSearch() {
      this.query = { assetName: '', assetCategory: '', assetStatus: '' };
      this.search();
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
    categoryText(value) {
      return this.categoryOptions.find(item => item.value === value)?.label || '-';
    },
    statusText(value) {
      return this.statusOptions.find(item => item.value === value)?.label || '-';
    },
    statusTagType(value) {
      return this.statusOptions.find(item => item.value === value)?.tagType || 'info';
    },
    formatNumber(value) {
      return Number(value || 0).toLocaleString(undefined, { maximumFractionDigits: 2 });
    },
    formatMoney(value) {
      return `¥${Number(value || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    },
    locationText(row) {
      return [row.parkName, row.buildingName, row.floorNo ? `${row.floorNo}F` : '', row.roomName, row.locationNote]
        .filter(Boolean)
        .join('/') || '-';
    },
    openAdd() {
      this.form = {
        ...emptyForm(),
        parkId: this.parkId ? String(this.parkId) : undefined,
        buildingId: this.buildingId ? String(this.buildingId) : undefined,
        floorNo: this.floorNo || undefined,
      };
      this.drawerVisible = true;
      this.$nextTick(this.syncLocationTreeSelection);
    },
    openEdit(row) {
      getAssetDetail(row.assetId).then(res => {
        const detail = res.data.data || row;
        this.form = {
          ...emptyForm(),
          ...detail,
          parkId: detail.parkId ? String(detail.parkId) : undefined,
          buildingId: detail.buildingId ? String(detail.buildingId) : undefined,
          roomId: detail.roomId ? String(detail.roomId) : undefined,
        };
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
      return this.parkOptions.map(park => ({
        key: `park-${park.id}`,
        label: park.name || '未命名园区',
        type: 'park',
        parkId: park.id,
        disabled: true,
        children: this.buildingOptions
          .filter(building => String(building.parkId) === String(park.id))
          .map(building => this.buildLocationBuildingNode(park, building, roomsByBuilding.get(String(building.id)) || [])),
      }));
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
      if (!(state.checkedKeys || []).includes(data.key)) {
        this.clearLocationSelection();
        return;
      }
      tree.setCheckedKeys([data.key]);
      tree.setCurrentKey(data.key);
      this.form.parkId = data.parkId ? String(data.parkId) : undefined;
      this.form.buildingId = data.buildingId ? String(data.buildingId) : undefined;
      this.form.floorNo = data.type === 'floor' || data.type === 'room' ? data.floorNo : undefined;
      this.form.roomId = data.type === 'room' && data.roomId ? String(data.roomId) : undefined;
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate('buildingId'));
    },
    clearLocationSelection() {
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
      tree.setCheckedKeys(key ? [key] : []);
      tree.setCurrentKey(key || null);
    },
    showSelectedLocation() {
      if (this.selectedLocationText) ElMessage.info(this.selectedLocationText);
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.saving = true;
        submitAsset(this.form)
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
      ElMessageBox.confirm(`确定删除资产“${row.assetName}”?`, '删除确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeAsset(row.assetId))
        .then(() => {
          ElMessage.success('删除成功');
          this.loadPage();
        });
    },
  },
};
</script>

<style scoped>
.asset-panel {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.asset-header {
  min-height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e7eaf0;
}

.asset-header h3 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.asset-header__actions,
.asset-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.asset-filter {
  padding: 16px 20px;
  border-bottom: 1px solid #edf0f5;
}

.asset-filter .el-input,
.asset-filter .el-select {
  width: 180px;
}

.asset-table :deep(.el-table__header th) {
  height: 56px;
  color: #6f7785;
  background: #fff;
  font-weight: 500;
}

.asset-table :deep(.el-table__row td) {
  height: 54px;
}

.asset-table :deep(.el-table__cell),
.asset-table :deep(.cell) {
  text-align: center;
}

.asset-pagination {
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  border-top: 1px solid #edf0f5;
}

.asset-form {
  padding: 0 8px 24px;
}

.asset-form :deep(.el-select),
.asset-form :deep(.el-input-number),
.asset-form :deep(.el-date-editor) {
  width: 100%;
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

.form-section h4::before,
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

.location-card__body {
  min-height: 220px;
  max-height: 320px;
  overflow: auto;
  padding: 14px 18px;
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

@media (max-width: 900px) {
  .asset-header,
  .asset-filter {
    align-items: flex-start;
    flex-wrap: wrap;
    padding: 16px;
  }

  .asset-filter .el-input,
  .asset-filter .el-select {
    width: 100%;
  }

  .asset-form :deep(.el-col-12) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
