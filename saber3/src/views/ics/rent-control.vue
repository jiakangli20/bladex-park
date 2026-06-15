<template>
  <basic-container>
    <div class="rent-control-page">
      <aside class="rent-sidebar">
        <div class="side-search">
          <el-segmented v-model="query.searchType" :options="searchTypeOptions" size="small" />
          <el-input
            v-model="query.keyword"
            :placeholder="query.searchType === 'building' ? '搜索建筑' : '搜索房号'"
            clearable
            @keyup.enter="loadBoard"
            @clear="loadBoard"
          >
            <template #append>
              <el-button :icon="Search" @click="loadBoard" />
            </template>
          </el-input>
        </div>

        <el-scrollbar class="tree-scroll">
          <el-tree
            ref="tree"
            node-key="key"
            :data="treeData"
            :props="treeProps"
            :default-expanded-keys="defaultExpandedKeys"
            :current-node-key="selectedKey"
            highlight-current
            @node-click="handleTreeNodeClick"
          />
        </el-scrollbar>
      </aside>

      <main class="rent-main">
        <div class="rent-header">
          <div>
            <div class="rent-title">{{ currentTitle }}</div>
            <div class="rent-subtitle">{{ currentSubtitle }}</div>
          </div>
          <div class="rent-actions">
            <el-select v-model="query.status" clearable placeholder="房态" size="small" @change="loadBoard">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-select v-model="query.orientation" clearable placeholder="朝向" size="small" @change="loadBoard">
              <el-option v-for="item in orientationOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-button :icon="Refresh" size="small" @click="handleReset">重置</el-button>
            <el-button :icon="RefreshRight" size="small" type="primary" @click="loadBoard">刷新</el-button>
          </div>
        </div>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="项目概况" name="overview">
            <section class="metric-grid">
              <div class="metric-item">
                <span>在租房间数</span>
                <strong>{{ overview.rentedRoomCount || 0 }}</strong>
                <em>房源数量 {{ overview.totalRoomCount || 0 }}</em>
              </div>
              <div class="metric-item">
                <span>管理面积</span>
                <strong>{{ formatNumber(overview.managementArea) }}㎡</strong>
                <em>楼层面积 {{ formatNumber(overview.floorArea || overview.buildingArea) }}㎡</em>
              </div>
              <div class="metric-item">
                <span>在租实时均价</span>
                <strong>{{ formatNumber(overview.avgRentPrice) }}</strong>
                <em>在租面积 {{ formatNumber(overview.rentedArea) }}㎡</em>
              </div>
              <div class="metric-item">
                <span>出租率</span>
                <strong>{{ overview.rentRate || 0 }}%</strong>
                <em>待租面积 {{ formatNumber(overview.vacantArea) }}㎡</em>
              </div>
            </section>

            <section class="analysis-grid">
              <div class="analysis-item">
                <div class="panel-title">租客行业</div>
                <el-empty v-if="!analysis.industry.length" description="暂无行业标签数据" :image-size="70" />
                <div v-else class="tag-list">
                  <el-tag v-for="item in analysis.industry" :key="item.name" type="primary">
                    {{ item.name }} {{ item.value }}
                  </el-tag>
                </div>
              </div>
              <div class="analysis-item">
                <div class="panel-title">租客标签</div>
                <el-empty v-if="!analysis.tenantTags.length" description="暂无客户标签数据" :image-size="70" />
                <div v-else class="tag-list">
                  <el-tag v-for="item in analysis.tenantTags" :key="item.name" type="success">
                    {{ item.name }} {{ item.value }}
                  </el-tag>
                </div>
              </div>
              <div class="analysis-item">
                <div class="panel-title">租赁面积排行</div>
                <el-empty v-if="!analysis.rentAreaRanking.length" description="暂无排行数据" :image-size="70" />
                <template v-else>
                  <div
                    v-for="(item, index) in analysis.rentAreaRanking"
                    :key="item.name + index"
                    class="rank-row"
                  >
                    <b>{{ index + 1 }}</b>
                    <span>{{ item.resource }}</span>
                    <em>{{ formatNumber(item.area) }}㎡</em>
                  </div>
                </template>
              </div>
            </section>

            <section class="ranking-panel">
              <div class="panel-title">租金单价排行</div>
              <el-table :data="analysis.rentUnitPriceRanking" size="small" empty-text="暂无排行数据">
                <el-table-column type="index" label="排名" width="80" align="center" />
                <el-table-column prop="resource" label="房源" min-width="180" align="center" />
                <el-table-column prop="area" label="面积(㎡)" width="120" align="center">
                  <template #default="{ row }">{{ formatNumber(row.area) }}</template>
                </el-table-column>
                <el-table-column prop="rentPrice" label="月租金(元)" width="140" align="center">
                  <template #default="{ row }">{{ formatNumber(row.rentPrice) }}</template>
                </el-table-column>
              </el-table>
            </section>
          </el-tab-pane>

          <el-tab-pane label="房态管理" name="rooms">
            <section class="room-toolbar">
              <div class="status-legend">
                <span v-for="item in statusOptions" :key="item.value">
                  <i :class="'status-' + item.value"></i>{{ item.label }}
                </span>
              </div>
              <el-button v-if="permission.rent_control_room_add" type="primary" :icon="Plus" @click="handleAddRoom">
                新增房间
              </el-button>
            </section>

            <el-empty v-if="!floors.length" description="暂无房态数据" />
            <template v-else>
              <section v-for="(floor, index) in floors" :key="floorRowKey(floor, index)" class="floor-row">
                <div class="floor-label">
                  <strong>{{ floor.floor || '-' }}F</strong>
                  <span>{{ floor.buildingName || '未关联建筑' }}</span>
                  <em>{{ formatNumber(floor.totalArea) }}㎡</em>
                </div>
                <div class="room-grid">
                  <button
                    v-for="room in floor.rooms"
                    :key="room.id"
                    type="button"
                    class="room-tile"
                    :class="'status-' + normalizeStatus(room.status)"
                    @click="openRoom(room)"
                  >
                    <span class="room-name">{{ room.name }}</span>
                    <span class="room-area">{{ formatNumber(room.area) }}㎡</span>
                    <span class="room-meta">{{ statusLabel(room.status) }} · {{ formatNumber(room.rentPrice) }}元/月</span>
                    <span class="room-sync">{{ room.syncStatus === '1' ? '已同步' : '待同步' }}</span>
                  </button>
                </div>
              </section>
            </template>
          </el-tab-pane>

          <el-tab-pane label="工单记录" name="workorders">
            <section class="workorder-head">
              <div class="workorder-metrics">
                <div><span>待派工单</span><strong>{{ workorderStats.pendingAssign }}</strong></div>
                <div><span>未办结超时</span><strong>{{ workorderStats.overdueOpen }}</strong></div>
                <div><span>当月超时率</span><strong>{{ workorderStats.monthOverdueRate }}%</strong></div>
                <div><span>满意度</span><strong>{{ workorderStats.monthSatisfaction }}%</strong></div>
              </div>
              <div class="workorder-actions">
                <el-button
                  v-if="permission.rent_control_workorder_list"
                  :icon="RefreshRight"
                  @click="loadWorkorders"
                >
                  刷新
                </el-button>
                <el-button
                  v-if="permission.rent_control_workorder_report"
                  type="primary"
                  :icon="Plus"
                  @click="handleReportWorkorder"
                >
                  上报工单
                </el-button>
              </div>
            </section>
            <el-alert
              v-if="workorderMessage"
              :title="workorderMessage"
              type="info"
              show-icon
              :closable="false"
            />
            <el-empty description="暂无工单记录" />
          </el-tab-pane>

          <el-tab-pane label="资产记录" name="assets">
            <el-empty description="资产记录待后续模块迁移后接入" />
          </el-tab-pane>
          <el-tab-pane label="智能硬件" name="hardware">
            <el-empty description="智能硬件待后续模块迁移后接入" />
          </el-tab-pane>
          <el-tab-pane label="楼宇信息" name="building">
            <section class="building-info">
              <div v-for="item in buildingInfoItems" :key="item.label">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </section>
          </el-tab-pane>
        </el-tabs>
      </main>
    </div>

    <el-drawer v-model="roomFormVisible" :title="roomForm.id ? '编辑房间' : '新增房间'" size="520px">
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="110px">
        <el-form-item label="所属建筑" prop="buildingId">
          <el-select v-model="roomForm.buildingId" filterable placeholder="请选择建筑" @change="handleRoomBuildingChange">
            <el-option
              v-for="building in allBuildings"
              :key="building.id"
              :label="buildingLabel(building)"
              :value="building.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="房间名称" prop="name">
          <el-input v-model="roomForm.name" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="楼层" prop="floor">
          <el-input-number
            v-model="roomForm.floor"
            :min="1"
            :max="currentBuildingFloors || undefined"
            controls-position="right"
            @change="syncFloorAreaInfo"
          />
        </el-form-item>
        <el-form-item label="面积(㎡)" prop="area">
          <el-input-number v-model="roomForm.area" :min="0" :precision="2" controls-position="right" />
          <div v-if="currentFloorArea !== null" class="form-tip">
            当前楼层总面积 {{ formatNumber(currentFloorArea) }}㎡，已用 {{ formatNumber(currentFloorUsedArea) }}㎡，剩余
            {{ formatNumber(currentFloorRemainArea) }}㎡
          </div>
        </el-form-item>
        <el-form-item label="月租金(元)">
          <el-input-number v-model="roomForm.rentPrice" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="物业费">
          <el-input-number v-model="roomForm.propertyFee" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="户型">
          <el-input v-model="roomForm.houseType" placeholder="如：开放办公" />
        </el-form-item>
        <el-form-item label="朝向">
          <el-select v-model="roomForm.orientation" clearable placeholder="请选择朝向">
            <el-option v-for="item in orientationOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roomForm.status">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="房源地址">
          <el-input v-model="roomForm.address" placeholder="请输入房源地址" />
        </el-form-item>
        <el-form-item label="配套设施">
          <el-input v-model="roomForm.facilities" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="核心卖点">
          <el-input v-model="roomForm.highlights" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roomForm.memo" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="roomSaving" @click="submitRoomForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer v-model="roomDetailVisible" title="房源详情" size="460px">
      <section v-if="roomDetail.id" class="room-detail">
        <div><span>房间名称</span><strong>{{ roomDetail.name }}</strong></div>
        <div><span>所属建筑</span><strong>{{ roomDetail.buildingName || '-' }}</strong></div>
        <div><span>楼层</span><strong>{{ roomDetail.floor || '-' }}F</strong></div>
        <div><span>面积</span><strong>{{ formatNumber(roomDetail.area) }}㎡</strong></div>
        <div><span>状态</span><strong>{{ statusLabel(roomDetail.status) }}</strong></div>
        <div><span>月租金</span><strong>{{ formatNumber(roomDetail.rentPrice) }}元</strong></div>
        <div><span>同步状态</span><strong>{{ roomDetail.syncStatus === '1' ? '已同步' : '待同步' }}</strong></div>
        <div><span>核心卖点</span><strong>{{ roomDetail.highlights || '-' }}</strong></div>
      </section>
      <template #footer>
        <el-button
          v-if="permission.rent_control_room_status"
          @click="handleChangeRoomStatus(roomDetail)"
        >
          状态流转
        </el-button>
        <el-button
          v-if="permission.rent_control_room_sync"
          @click="handleSyncRoom(roomDetail)"
        >
          同步小程序
        </el-button>
        <el-button
          v-if="permission.rent_control_room_edit"
          type="primary"
          @click="handleEditRoom(roomDetail)"
        >
          编辑
        </el-button>
        <el-button
          v-if="permission.rent_control_room_delete"
          type="danger"
          @click="handleDeleteRoom(roomDetail)"
        >
          删除
        </el-button>
      </template>
    </el-drawer>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  changeRoomStatus,
  getBoard,
  getBuildingList,
  getFloorList,
  getRoomDetail,
  getWorkorders,
  removeRoom,
  reportWorkorder,
  submitRoom,
  syncRoomMini,
} from '@/api/ics/rent-control';
import { Plus, Refresh, RefreshRight, Search } from '@element-plus/icons-vue';

export default {
  data() {
    return {
      Search,
      Plus,
      Refresh,
      RefreshRight,
      activeTab: 'overview',
      loading: false,
      selectedKey: '',
      defaultExpandedKeys: [],
      query: {
        parkId: undefined,
        buildingId: undefined,
        floorNo: undefined,
        keyword: '',
        searchType: 'room',
        status: '',
        orientation: '',
      },
      searchTypeOptions: [
        { label: '房号', value: 'room' },
        { label: '建筑', value: 'building' },
      ],
      statusOptions: [
        { label: '空置中', value: '0' },
        { label: '已出租', value: '1' },
        { label: '已预定', value: '2' },
        { label: '装修中', value: '3' },
        { label: '停用', value: '4' },
      ],
      orientationOptions: ['朝南', '朝北', '朝东', '朝西', '南北通透'],
      treeProps: {
        label: 'label',
        children: 'children',
      },
      board: {},
      currentPark: {},
      currentBuilding: {},
      parks: [],
      buildings: [],
      floors: [],
      overview: {},
      analysis: {
        industry: [],
        tenantTags: [],
        rentAreaRanking: [],
        rentUnitPriceRanking: [],
      },
      summary: {},
      allBuildings: [],
      currentBuildingFloors: null,
      currentFloorArea: null,
      currentFloorUsedArea: 0,
      currentFloorRemainArea: null,
      roomFormVisible: false,
      roomSaving: false,
      roomForm: this.emptyRoomForm(),
      roomRules: {
        buildingId: [{ required: true, message: '请选择所属建筑', trigger: 'change' }],
        name: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
        floor: [{ required: true, message: '请输入楼层', trigger: 'change' }],
      },
      roomDetailVisible: false,
      roomDetail: {},
      workorderStats: {
        pendingAssign: 0,
        overdueOpen: 0,
        processing: 0,
        monthOverdueRate: 0,
        monthOverdue: 0,
        monthSatisfaction: 0,
        monthRated: 0,
      },
      workorderMessage: '',
    };
  },
  computed: {
    ...mapGetters(['permission']),
    treeData() {
      return this.parks.map(park => ({
        key: `park-${park.id}`,
        type: 'park',
        id: park.id,
        label: park.name,
        children: (park.buildings || []).map(building => ({
          key: `building-${building.id}`,
          type: 'building',
          id: building.id,
          parkId: building.parkId,
          label: building.name,
          children: (building.floors || []).map(floor => ({
            key: `floor-${building.id}-${floor}`,
            type: 'floor',
            id: `${building.id}-${floor}`,
            parkId: building.parkId,
            buildingId: building.id,
            floorNo: floor,
            label: `${floor}F`,
          })),
        })),
      }));
    },
    currentTitle() {
      return this.currentBuilding.name || this.currentPark.name || '租赁管理';
    },
    currentSubtitle() {
      if (this.query.floorNo) {
        return `${this.currentPark.name || ''} ${this.currentBuilding.name || ''} ${this.query.floorNo}F`;
      }
      return this.currentPark.name ? `${this.currentPark.name} · 房态与经营概况` : '请选择左侧园区或建筑';
    },
    buildingInfoItems() {
      return [
        { label: '所属园区', value: this.displayValue(this.currentPark.name || this.currentBuilding.parkName) },
        { label: '园区编码', value: this.displayValue(this.currentPark.code) },
        { label: '建筑名称', value: this.displayValue(this.currentBuilding.name) },
        { label: '建筑编码', value: this.displayValue(this.currentBuilding.code) },
        { label: '建筑地址', value: this.displayValue(this.currentBuilding.address || this.currentPark.detailAddress) },
        { label: '楼层数', value: this.currentBuilding.floors ? `${this.currentBuilding.floors} 层` : '-' },
        { label: '建筑面积', value: `${this.formatNumber(this.currentBuilding.area)}㎡` },
        { label: '可租面积', value: `${this.formatNumber(this.currentBuilding.rentableArea)}㎡` },
        { label: '管理面积', value: `${this.formatNumber(this.overview.managementArea)}㎡` },
        { label: '联系人', value: this.displayValue(this.currentPark.contactName) },
        { label: '联系电话', value: this.displayValue(this.currentPark.contactPhone) },
        { label: '状态', value: this.currentBuilding.status === '1' ? '停用' : '启用' },
      ];
    },
  },
  created() {
    this.loadBoard();
    this.loadAllBuildings();
  },
  methods: {
    emptyRoomForm() {
      return {
        id: undefined,
        buildingId: undefined,
        name: '',
        floor: undefined,
        area: undefined,
        rentPrice: undefined,
        propertyFee: undefined,
        houseType: '',
        orientation: '',
        status: '0',
        address: '',
        facilities: '',
        highlights: '',
        sceneImages: '',
        floorPlanImages: '',
        memo: '',
      };
    },
    loadBoard() {
      this.loading = true;
      const params = {
        parkId: this.query.parkId,
        buildingId: this.query.buildingId,
        floorNo: this.query.floorNo,
        keyword: this.query.keyword,
        searchType: this.query.searchType,
        status: this.query.status,
        orientation: this.query.orientation,
      };
      getBoard(params)
        .then(res => {
          const data = res.data.data || {};
          this.board = data;
          this.currentPark = data.currentPark || {};
          this.currentBuilding = data.currentBuilding || {};
          this.parks = data.parks || [];
          this.buildings = data.buildings || [];
          this.floors = data.floors || [];
          this.overview = data.overview || {};
          this.analysis = Object.assign(
            { industry: [], tenantTags: [], rentAreaRanking: [], rentUnitPriceRanking: [] },
            data.analysis || {}
          );
          this.summary = data.summary || {};
          this.syncTreeState();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadAllBuildings() {
      getBuildingList({}).then(res => {
        this.allBuildings = res.data.data || [];
      });
    },
    syncTreeState() {
      const expanded = [];
      this.parks.forEach(park => {
        expanded.push(`park-${park.id}`);
        (park.buildings || []).forEach(building => {
          if (this.query.buildingId && String(this.query.buildingId) === String(building.id)) {
            expanded.push(`building-${building.id}`);
          }
        });
      });
      this.defaultExpandedKeys = expanded;
      if (this.query.floorNo && this.query.buildingId) {
        this.selectedKey = `floor-${this.query.buildingId}-${this.query.floorNo}`;
      } else if (this.query.buildingId) {
        this.selectedKey = `building-${this.query.buildingId}`;
      } else if (this.query.parkId) {
        this.selectedKey = `park-${this.query.parkId}`;
      }
    },
    handleTreeNodeClick(node) {
      if (node.type === 'park') {
        this.query.parkId = node.id;
        this.query.buildingId = undefined;
        this.query.floorNo = undefined;
        this.activeTab = 'overview';
      }
      if (node.type === 'building') {
        this.query.parkId = node.parkId;
        this.query.buildingId = node.id;
        this.query.floorNo = undefined;
        this.activeTab = 'rooms';
      }
      if (node.type === 'floor') {
        this.query.parkId = node.parkId;
        this.query.buildingId = node.buildingId;
        this.query.floorNo = node.floorNo;
        this.activeTab = 'rooms';
      }
      this.selectedKey = node.key;
      this.loadBoard();
    },
    handleReset() {
      this.query = {
        parkId: undefined,
        buildingId: undefined,
        floorNo: undefined,
        keyword: '',
        searchType: 'room',
        status: '',
        orientation: '',
      };
      this.selectedKey = '';
      this.activeTab = 'overview';
      this.loadBoard();
    },
    formatNumber(value) {
      const number = Number(value || 0);
      return number.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      });
    },
    displayValue(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    normalizeStatus(status) {
      return ['0', '1', '2', '3', '4'].includes(String(status)) ? String(status) : '0';
    },
    statusLabel(status) {
      const option = this.statusOptions.find(item => item.value === this.normalizeStatus(status));
      return option ? option.label : '空置中';
    },
    floorRowKey(floor, index) {
      return `${floor.buildingId || 'building'}-${floor.floor || index}`;
    },
    buildingLabel(building) {
      return `${building.parkName || '未关联园区'} / ${building.name}`;
    },
    handleAddRoom() {
      this.roomForm = this.emptyRoomForm();
      this.roomDetail = {};
      if (this.query.buildingId) {
        this.roomForm.buildingId = this.query.buildingId;
      }
      if (this.query.floorNo) {
        this.roomForm.floor = this.query.floorNo;
      }
      this.syncCurrentBuildingFloors();
      this.syncFloorAreaInfo();
      this.roomFormVisible = true;
    },
    openRoom(room) {
      getRoomDetail(room.id).then(res => {
        this.roomDetail = res.data.data || {};
        this.roomDetailVisible = true;
      });
    },
    handleEditRoom(room) {
      this.roomForm = Object.assign(this.emptyRoomForm(), room);
      this.roomDetail = Object.assign({}, room);
      this.roomDetailVisible = false;
      this.syncCurrentBuildingFloors();
      this.syncFloorAreaInfo();
      this.roomFormVisible = true;
    },
    handleDeleteRoom(room) {
      ElMessageBox.confirm('确定删除该房源?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeRoom(room.id))
        .then(() => {
          ElMessage.success('删除成功');
          this.roomDetailVisible = false;
          this.loadBoard();
        });
    },
    handleChangeRoomStatus(room) {
      ElMessageBox.prompt('请输入目标状态：0空置中、1已出租、2已预定、3装修中、4停用', '状态流转', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: this.normalizeStatus(room.status),
        inputPattern: /^[0-4]$/,
        inputErrorMessage: '状态值必须为 0-4',
      })
        .then(({ value }) => changeRoomStatus(room.id, value))
        .then(() => {
          ElMessage.success('状态已更新');
          this.roomDetailVisible = false;
          this.loadBoard();
        });
    },
    handleSyncRoom(room) {
      syncRoomMini(room.id).then(() => {
        ElMessage.success('已标记同步');
        this.roomDetailVisible = false;
        this.loadBoard();
      });
    },
    handleRoomBuildingChange() {
      this.syncCurrentBuildingFloors();
      if (this.currentBuildingFloors && this.roomForm.floor > this.currentBuildingFloors) {
        this.roomForm.floor = undefined;
      }
      this.syncFloorAreaInfo();
    },
    syncCurrentBuildingFloors() {
      const building = this.allBuildings.find(item => String(item.id) === String(this.roomForm.buildingId));
      this.currentBuildingFloors = building && building.floors ? Number(building.floors) : null;
    },
    syncFloorAreaInfo() {
      this.currentFloorArea = null;
      this.currentFloorUsedArea = 0;
      this.currentFloorRemainArea = null;
      if (!this.roomForm.buildingId || !this.roomForm.floor) {
        return;
      }
      getFloorList({ buildingId: this.roomForm.buildingId, floorNo: this.roomForm.floor }).then(res => {
        const floor = (res.data.data || [])[0];
        if (!floor || floor.area === null || floor.area === undefined) {
          return;
        }
        const totalArea = Number(floor.area || 0);
        const isSameFloor =
          this.roomForm.id &&
          String(this.roomDetail.buildingId) === String(this.roomForm.buildingId) &&
          String(this.roomDetail.floor) === String(this.roomForm.floor);
        const originalArea = isSameFloor ? Number(this.roomDetail.area || 0) : 0;
        const usedArea = Math.max(Number(floor.usedArea || 0) - originalArea, 0);
        this.currentFloorArea = totalArea;
        this.currentFloorUsedArea = usedArea;
        this.currentFloorRemainArea = Math.max(totalArea - usedArea, 0);
      });
    },
    submitRoomForm() {
      this.$refs.roomFormRef.validate(valid => {
        if (!valid) {
          return;
        }
        if (
          this.currentFloorRemainArea !== null &&
          this.roomForm.area !== undefined &&
          Number(this.roomForm.area || 0) > Number(this.currentFloorRemainArea || 0)
        ) {
          ElMessage.warning(`房源面积不能超过当前楼层剩余面积 ${this.formatNumber(this.currentFloorRemainArea)}㎡`);
          return;
        }
        this.roomSaving = true;
        submitRoom(this.roomForm)
          .then(() => {
            ElMessage.success('保存成功');
            this.roomFormVisible = false;
            this.loadBoard();
          })
          .finally(() => {
            this.roomSaving = false;
          });
      });
    },
    loadWorkorders() {
      getWorkorders().then(res => {
        const data = res.data.data || {};
        this.workorderStats = Object.assign(this.workorderStats, data.stats || {});
        this.workorderMessage = data.message || '';
      });
    },
    handleReportWorkorder() {
      reportWorkorder({}).then(res => {
        const data = res.data.data || {};
        ElMessage.info(data.message || '工单模块暂未迁移');
      });
    },
  },
};
</script>

<style scoped>
.rent-control-page {
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 14px;
  min-height: calc(100vh - 160px);
}

.rent-sidebar {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  min-height: 620px;
  overflow: hidden;
}

.side-search {
  display: grid;
  gap: 10px;
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
}

.tree-scroll {
  height: 560px;
  padding: 8px;
}

.rent-main {
  min-width: 0;
}

.rent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 4px 0 12px;
}

.rent-title {
  font-size: 20px;
  font-weight: 650;
  color: #303133;
}

.rent-subtitle {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.rent-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rent-actions .el-select {
  width: 118px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.metric-item,
.analysis-item,
.ranking-panel,
.floor-row,
.workorder-head,
.building-info {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.metric-item {
  padding: 16px;
}

.metric-item span {
  display: block;
  color: #909399;
  font-size: 12px;
}

.metric-item strong {
  display: block;
  margin-top: 8px;
  color: #303133;
  font-size: 22px;
  line-height: 1.2;
}

.metric-item em {
  display: block;
  margin-top: 8px;
  color: #909399;
  font-style: normal;
  font-size: 12px;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.analysis-item,
.ranking-panel {
  padding: 14px;
}

.panel-title {
  margin-bottom: 12px;
  color: #303133;
  font-weight: 600;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.rank-row {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  line-height: 34px;
  color: #606266;
}

.rank-row b {
  color: #409eff;
}

.rank-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row em {
  font-style: normal;
  color: #303133;
}

.room-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.status-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #606266;
  font-size: 13px;
}

.status-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.status-legend i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.floor-row {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  margin-bottom: 10px;
  overflow: hidden;
}

.floor-label {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 12px;
  background: #f5f7fa;
  color: #606266;
}

.floor-label strong {
  color: #303133;
  font-size: 20px;
}

.floor-label span,
.floor-label em {
  font-size: 12px;
  font-style: normal;
}

.room-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(178px, 1fr));
  gap: 10px;
  padding: 12px;
}

.room-tile {
  display: grid;
  grid-template-rows: auto auto auto auto;
  gap: 5px;
  min-height: 112px;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.room-tile:hover {
  border-color: #409eff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.room-name {
  color: #303133;
  font-weight: 650;
}

.room-area,
.room-meta,
.room-sync {
  color: #606266;
  font-size: 12px;
}

.status-0 {
  background: #67c23a;
}

.status-1 {
  background: #409eff;
}

.status-2 {
  background: #e6a23c;
}

.status-3 {
  background: #909399;
}

.status-4 {
  background: #f56c6c;
}

.room-tile.status-0 {
  border-left: 4px solid #67c23a;
}

.room-tile.status-1 {
  border-left: 4px solid #409eff;
}

.room-tile.status-2 {
  border-left: 4px solid #e6a23c;
}

.room-tile.status-3 {
  border-left: 4px solid #909399;
}

.room-tile.status-4 {
  border-left: 4px solid #f56c6c;
}

.workorder-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  margin-bottom: 12px;
}

.workorder-metrics {
  display: grid;
  grid-template-columns: repeat(4, 130px);
  gap: 10px;
}

.workorder-metrics div {
  display: grid;
  gap: 4px;
}

.workorder-metrics span {
  color: #909399;
  font-size: 12px;
}

.workorder-metrics strong {
  color: #303133;
  font-size: 20px;
}

.workorder-actions {
  display: flex;
  gap: 8px;
}

.building-info {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  overflow: hidden;
}

.building-info div {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-right: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.building-info span {
  color: #909399;
  font-size: 12px;
}

.building-info strong {
  color: #303133;
  font-weight: 500;
}

.form-tip {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.room-detail {
  display: grid;
  gap: 1px;
  background: #ebeef5;
}

.room-detail div {
  display: grid;
  grid-template-columns: 98px minmax(0, 1fr);
  gap: 12px;
  padding: 12px;
  background: #fff;
}

.room-detail span {
  color: #909399;
}

.room-detail strong {
  color: #303133;
  font-weight: 500;
}

@media (max-width: 1100px) {
  .rent-control-page {
    grid-template-columns: 1fr;
  }

  .rent-sidebar {
    min-height: auto;
  }

  .tree-scroll {
    height: 260px;
  }

  .metric-grid,
  .analysis-grid,
  .building-info {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .rent-header,
  .workorder-head,
  .room-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .rent-actions,
  .workorder-actions {
    flex-wrap: wrap;
  }

  .metric-grid,
  .analysis-grid,
  .building-info,
  .workorder-metrics {
    grid-template-columns: 1fr;
  }

  .floor-row {
    grid-template-columns: 1fr;
  }
}
</style>
