<template>
  <basic-container>
    <div class="rent-control-page">
      <aside class="rent-sidebar">
        <div class="side-search">
          <div class="side-search-bar">
            <el-select
              v-model="query.searchType"
              class="side-search-type"
              size="small"
              @change="handleSearchTypeChange"
            >
              <el-option
                v-for="item in searchTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
            <el-input
              v-model="query.keyword"
              class="side-search-input"
              :placeholder="searchPlaceholder"
              clearable
              size="small"
              @keyup.enter="handleSearch"
              @clear="handleSearchClear"
            />
            <el-button class="side-search-button" size="small" type="primary" @click="handleSearch">
              搜索
            </el-button>
          </div>
        </div>

        <el-scrollbar v-if="hasSearchKeyword" class="search-result-scroll">
          <div v-if="!searchResultRooms.length" class="search-result-empty">暂无匹配房源</div>
          <button
            v-for="room in searchResultRooms"
            :key="room.id"
            type="button"
            class="room-search-item"
            :class="{ 'is-active': String(selectedRoomId || drawerRoomId || '') === String(room.id || '') }"
            @click="handleSearchRoomClick(room)"
          >
            <el-icon><OfficeBuilding /></el-icon>
            <span class="room-search-item__body">
              <span>房间号：{{ searchRoomNo(room) }}</span>
              <em>楼宇：{{ searchRoomBuilding(room) }}</em>
            </span>
            <el-icon class="room-search-item__tenant"><UserFilled /></el-icon>
          </button>
        </el-scrollbar>

        <el-scrollbar v-else class="tree-scroll">
          <el-tree
            ref="tree"
            node-key="key"
            :data="treeData"
            :props="treeProps"
            :default-expanded-keys="defaultExpandedKeys"
            :current-node-key="selectedKey"
            highlight-current
            @node-click="handleTreeNodeClick"
          >
            <template #default="{ data }">
              <span class="rent-tree-node">
                <el-icon v-if="data.type === 'park' || data.type === 'building'"><OfficeBuilding /></el-icon>
                <el-icon v-else-if="data.type === 'floor'"><Collection /></el-icon>
                <el-icon v-else><House /></el-icon>
                <span class="rent-tree-node__label">{{ data.label }}</span>
                <i
                  v-if="data.type === 'room'"
                  class="rent-tree-node__status"
                  :class="'status-' + normalizeStatus(data.status)"
                ></i>
              </span>
            </template>
          </el-tree>
        </el-scrollbar>
      </aside>

      <main class="rent-main">
        <div class="rent-header">
          <div>
            <div class="rent-title">{{ currentTitle }}</div>
            <div class="rent-subtitle">{{ currentSubtitle }}</div>
          </div>
          <div class="rent-actions">
            <template v-if="isRoomSelection">
              <el-button
                v-if="permission.rent_control_room_delete"
                :icon="Delete"
                size="small"
                type="danger"
                plain
                @click="handleDeleteRoom(selectedRoomDetail)"
              >
                删除
              </el-button>
              <el-button
                v-if="permission.rent_control_room_edit"
                :icon="Edit"
                size="small"
                type="primary"
                @click="handleEditRoom(selectedRoomDetail)"
              >
                编辑
              </el-button>
            </template>
            <template v-else>
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
            </template>
          </div>
        </div>

        <RoomWorkbench
          v-if="isRoomSelection"
          :key="`${selectedRoomId}-${roomRevision}`"
          :room-id="selectedRoomId"
          :park-id="query.parkId"
          :building-id="query.buildingId"
          :floor-no="query.floorNo"
          :park-name="selectedRoomDetail.parkName || selectedRoom.parkName || currentPark.name"
          :building-name="selectedRoomDetail.buildingName || selectedRoom.buildingName || currentBuilding.name"
          @loaded="handleRoomLoaded"
        />

        <div v-else class="rent-tabs-shell">
          <el-button
            v-if="activeTab === 'rooms' && !isFloorSelection && permission.rent_control_room_add"
            class="room-add-tab-button"
            type="primary"
            :icon="Plus"
            @click="handleAddRoom"
          >
            新增房间
          </el-button>
          <el-tabs v-model="activeTab" :class="{ 'floor-only-tabs': isFloorSelection }">
            <el-tab-pane v-if="!isFloorSelection" label="项目概况" name="overview">
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
                  <div
                    v-for="(roomRow, rowIndex) in splitFloorRoomRows(floor)"
                    :key="`${floorRowKey(floor, index)}-${rowIndex}`"
                    class="room-grid-line"
                  >
                    <button
                      v-for="room in roomRow"
                      :key="room.id"
                      type="button"
                      class="room-tile"
                      :class="'status-' + normalizeStatus(room.status)"
                      :style="roomTileStyle(room, floor)"
                      @click="openRoom(room)"
                    >
                      <span class="room-cover">
                        <el-image v-if="roomCoverImage(room)" :src="roomCoverImage(room)" fit="cover" />
                        <span v-else class="room-cover__empty">暂无实景图</span>
                      </span>
                      <span class="room-tile__body">
                        <span class="room-name">{{ room.name }}</span>
                        <span class="room-area">{{ formatNumber(room.area) }}㎡</span>
                        <span class="room-meta">{{ statusLabel(room.status) }} · {{ formatNumber(room.rentPrice) }}元/月</span>
                        <span class="room-sync">{{ room.syncStatus === '1' ? '已同步' : '待同步' }}</span>
                      </span>
                    </button>
                  </div>
                </div>
              </section>
            </template>
            </el-tab-pane>

            <el-tab-pane v-if="!isFloorSelection" label="工单记录" name="workorders" lazy>
            <EnterprisePropertyWorkorder
              embedded
              :park-id="query.parkId"
              :filter-room-ids="workorderFilterRoomIds"
              :default-room-info="workorderDefaultRoomInfo"
            />
            </el-tab-pane>

            <el-tab-pane v-if="!isFloorSelection" label="资产记录" name="assets" lazy>
            <AssetLedger
              :park-id="query.parkId"
              :building-id="query.buildingId"
              :floor-no="query.floorNo"
            />
            </el-tab-pane>
            <el-tab-pane v-if="!isFloorSelection" label="智能水电表" name="meters" lazy>
            <SmartDeviceLedger
              :park-id="query.parkId"
              :building-id="query.buildingId"
              :floor-no="query.floorNo"
            />
            </el-tab-pane>
            <el-tab-pane v-if="!isFloorSelection" label="楼宇信息" name="building">
            <el-empty v-if="!currentBuilding.id" description="请从左侧选择具体楼宇" />
            <section v-else class="building-profile">
              <div class="building-profile-section">
                <h3>基础信息</h3>
                <div class="building-profile-grid">
                  <div
                    v-for="item in buildingBasicInfoItems"
                    :key="item.label"
                    class="building-profile-item"
                    :class="{ 'is-wide': item.wide, 'is-full': item.full }"
                  >
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </div>

              <div class="building-profile-section">
                <h3>拓展信息</h3>
                <div class="building-profile-grid">
                  <div v-for="item in buildingExtensionInfoItems" :key="item.label" class="building-profile-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </div>

              <div class="building-profile-section">
                <h3>楼宇图片</h3>
                <div v-if="buildingImageList.length" class="building-profile-images">
                  <el-image
                    v-for="image in buildingImageList"
                    :key="image"
                    :src="image"
                    :preview-src-list="buildingImageList"
                    fit="cover"
                    preview-teleported
                  />
                </div>
                <el-empty v-else description="暂无楼宇图片，可在楼宇管理中上传" :image-size="72" />
              </div>
            </section>
            </el-tab-pane>
          </el-tabs>
        </div>
      </main>
    </div>

    <el-drawer v-model="roomFormVisible" :title="roomForm.id ? '编辑房间' : '新增房间'" size="520px">
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="110px">
        <el-form-item label="所属园区" prop="parkId">
          <el-select v-model="roomForm.parkId" filterable placeholder="请选择园区" @change="handleRoomParkChange">
            <el-option v-for="park in roomParkOptions" :key="park.id" :label="park.name" :value="String(park.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属建筑" prop="buildingId">
          <el-select v-model="roomForm.buildingId" filterable placeholder="请选择建筑" @change="handleRoomBuildingChange">
            <el-option
              v-for="building in filteredRoomBuildings"
              :key="building.id"
              :label="building.name"
              :value="String(building.id)"
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
              v-for="item in baseStatusOptions"
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
        <el-form-item label="房间照片">
          <el-upload
            v-model:file-list="roomImageFileList"
            action="/api/blade-resource/oss/endpoint/put-file-attach"
            :headers="uploadHeaders"
            list-type="picture-card"
            accept="image/*"
            multiple
            :limit="9"
            :before-upload="beforeRoomImageUpload"
            :on-success="handleRoomImageUploadSuccess"
            :on-remove="handleRoomImageRemove"
            :on-error="handleRoomImageUploadError"
            :on-exceed="handleRoomImageExceed"
          >
            <el-button v-if="roomImageFileList.length < 9" :icon="Upload" text>上传</el-button>
          </el-upload>
          <div class="form-tip">支持 jpg、png 等图片格式，最多上传 9 张，单张不超过 10MB。</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roomForm.memo" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="roomSaving" @click="submitRoomForm">保存</el-button>
      </template>
    </el-drawer>

    <el-drawer
      v-model="roomWorkbenchVisible"
      class="room-workbench-drawer"
      size="60%"
      direction="rtl"
      :with-header="false"
      destroy-on-close
    >
      <div class="room-workbench-drawer__header">
        <div>
          <div class="room-workbench-drawer__title">{{ drawerRoomTitle }}</div>
          <div class="room-workbench-drawer__subtitle">{{ drawerRoomSubtitle }}</div>
        </div>
        <div class="room-workbench-drawer__actions">
          <el-button
            v-if="permission.rent_control_room_delete"
            :icon="Delete"
            type="danger"
            plain
            @click="handleDeleteRoom(drawerRoomDetail)"
          >
            删除
          </el-button>
          <el-button
            v-if="permission.rent_control_room_edit"
            :icon="Edit"
            type="primary"
            @click="handleEditRoom(drawerRoomDetail)"
          >
            编辑
          </el-button>
        </div>
      </div>
      <RoomWorkbench
        v-if="roomWorkbenchVisible && drawerRoomId"
        :key="`drawer-${drawerRoomId}-${roomRevision}`"
        :room-id="drawerRoomId"
        :park-id="drawerRoomParkId"
        :building-id="drawerRoomBuildingId"
        :floor-no="drawerRoomFloorNo"
        :park-name="drawerRoomParkName"
        :building-name="drawerRoomBuildingName"
        @loaded="handleDrawerRoomLoaded"
      />
    </el-drawer>

    <el-drawer v-model="roomDetailVisible" title="房源详情" size="460px">
      <section v-if="roomDetail.id" class="room-detail">
        <div><span>房间名称</span><strong>{{ roomDetail.name }}</strong></div>
        <div><span>所属园区</span><strong>{{ roomDetail.parkName || '-' }}</strong></div>
        <div><span>所属建筑</span><strong>{{ roomDetail.buildingName || '-' }}</strong></div>
        <div><span>楼层</span><strong>{{ roomDetail.floor || '-' }}F</strong></div>
        <div><span>面积</span><strong>{{ formatNumber(roomDetail.area) }}㎡</strong></div>
        <div><span>状态</span><strong>{{ statusLabel(roomDetail.status) }}</strong></div>
        <div v-if="normalizeStatus(roomDetail.status) === '0'">
          <span>空置开始时间</span><strong>{{ roomDetail.vacantSince || '-' }}</strong>
        </div>
        <div><span>月租金</span><strong>{{ formatNumber(roomDetail.rentPrice) }}元</strong></div>
        <div><span>同步状态</span><strong>{{ roomDetail.syncStatus === '1' ? '已同步' : '待同步' }}</strong></div>
        <div><span>核心卖点</span><strong>{{ roomDetail.highlights || '-' }}</strong></div>
        <div v-if="roomDetailImageList.length" class="room-detail-images">
          <span>房间照片</span>
          <strong>
            <el-image
              v-for="image in roomDetailImageList"
              :key="image"
              :src="image"
              :preview-src-list="roomDetailImageList"
              fit="cover"
              preview-teleported
            />
          </strong>
        </div>
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
  removeRoom,
  submitRoom,
  syncRoomMini,
} from '@/api/park/rent-control';
import { Collection, Delete, Edit, House, OfficeBuilding, Plus, Refresh, RefreshRight, Upload, UserFilled } from '@element-plus/icons-vue';
import { getToken } from '@/utils/auth';
import EnterprisePropertyWorkorder from '@/views/enterprise/property-workorder.vue';
import AssetLedger from './asset-ledger.vue';
import RoomWorkbench from './room-workbench.vue';
import SmartDeviceLedger from './smart-device.vue';

export default {
  components: { AssetLedger, Collection, EnterprisePropertyWorkorder, House, OfficeBuilding, RoomWorkbench, SmartDeviceLedger, UserFilled },
  data() {
    return {
      UserFilled,
      Plus,
      Refresh,
      RefreshRight,
      Delete,
      Edit,
      Upload,
      activeTab: 'overview',
      loading: false,
      boardRequestId: 0,
      treeLoaded: false,
      selectedKey: '',
      defaultExpandedKeys: [],
      selectedRoomId: undefined,
      selectedRoom: {},
      selectedRoomDetail: {},
      roomRevision: 0,
      roomWorkbenchVisible: false,
      drawerRoomId: undefined,
      drawerRoom: {},
      drawerRoomDetail: {},
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
        { label: '租客名', value: 'tenant' },
        { label: '手机号', value: 'mobile' },
      ],
      statusOptions: [
        { label: '空置', value: '0' },
        { label: '待清退/短租', value: '1' },
        { label: '预留', value: '2' },
        { label: '待退出', value: '3' },
        { label: '90天内到期', value: '4' },
        { label: '30天内到期', value: '5' },
        { label: '已到期', value: '6' },
        { label: '已出租', value: '7' },
      ],
      baseStatusOptions: [
        { label: '空置', value: '0' },
        { label: '待清退/短租', value: '1' },
        { label: '预留', value: '2' },
        { label: '待退出', value: '3' },
        { label: '90天内到期', value: '4' },
        { label: '30天内到期', value: '5' },
        { label: '已到期', value: '6' },
        { label: '已出租', value: '7' },
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
      treeParks: [],
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
      roomImageFileList: [],
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      roomRules: {
        parkId: [{ required: true, message: '请选择所属园区', trigger: 'change' }],
        buildingId: [{ required: true, message: '请选择所属建筑', trigger: 'change' }],
        name: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
        floor: [{ required: true, message: '请输入楼层', trigger: 'change' }],
      },
      roomDetailVisible: false,
      roomDetail: {},
    };
  },
  computed: {
    ...mapGetters(['permission']),
    treeData() {
      return this.getTreeParks().map(park => ({
        key: `park-${park.id}`,
        type: 'park',
        id: park.id,
        label: park.name,
        children: (park.buildings || []).map(building => {
          const floorNodes = Array.isArray(building.floorNodes)
            ? building.floorNodes
            : (building.floors || []).map(floorNo => ({ floorNo, rooms: [] }));
          return {
            key: `building-${building.id}`,
            type: 'building',
            id: building.id,
            parkId: building.parkId,
            label: building.name,
            children: floorNodes.map(floor => ({
              key: `floor-${building.id}-${floor.floorNo}`,
              type: 'floor',
              id: `${building.id}-${floor.floorNo}`,
              parkId: building.parkId,
              buildingId: building.id,
              floorNo: floor.floorNo,
              label: `${floor.floorNo}层`,
              children: (floor.rooms || []).map(room => ({
                key: `room-${room.id}`,
                type: 'room',
                id: room.id,
                roomId: room.id,
                parkId: room.parkId || building.parkId,
                buildingId: room.buildingId || building.id,
                floorNo: room.floorNo || floor.floorNo,
                label: room.name,
                area: room.area,
                status: room.status,
              })),
            })),
          };
        }),
      }));
    },
    hasSearchKeyword() {
      return String(this.query.keyword || '').trim().length > 0;
    },
    searchPlaceholder() {
      const option = this.searchTypeOptions.find(item => item.value === this.query.searchType);
      return `请输入${option ? option.label : '房号'}`;
    },
    searchResultRooms() {
      const roomMap = new Map();
      (this.floors || []).forEach(floor => {
        (floor.rooms || []).forEach(room => {
          if (!room || !room.id || roomMap.has(String(room.id))) {
            return;
          }
          roomMap.set(String(room.id), {
            ...room,
            floor: room.floor || room.floorNo || floor.floor,
            floorNo: room.floorNo || room.floor || floor.floor,
            buildingId: room.buildingId || floor.buildingId,
            buildingName: room.buildingName || floor.buildingName,
            parkName: room.parkName || this.currentPark.name,
          });
        });
      });
      return Array.from(roomMap.values());
    },
    currentTitle() {
      if (this.isRoomSelection) {
        return this.selectedRoomDetail.name || this.selectedRoom.name || '房源信息';
      }
      if (this.isFloorSelection) {
        return `${this.query.floorNo}层`;
      }
      return this.currentBuilding.name || this.currentPark.name || '租控管理';
    },
    currentSubtitle() {
      if (this.isRoomSelection) {
        return [
          this.selectedRoomDetail.parkName || this.selectedRoom.parkName || this.currentPark.name,
          this.selectedRoomDetail.buildingName || this.selectedRoom.buildingName || this.currentBuilding.name,
          this.query.floorNo ? `${this.query.floorNo}层` : '',
        ]
          .filter(Boolean)
          .join(' · ');
      }
      if (this.isFloorSelection) {
        return [this.currentPark.name, this.currentBuilding.name].filter(Boolean).join(' · ');
      }
      return this.currentPark.name ? `${this.currentPark.name} · 房态与经营概况` : '请选择左侧园区或建筑';
    },
    isFloorSelection() {
      return !this.isRoomSelection && this.query.floorNo !== undefined && this.query.floorNo !== null && this.query.floorNo !== '';
    },
    isRoomSelection() {
      return this.selectedRoomId !== undefined && this.selectedRoomId !== null && this.selectedRoomId !== '';
    },
    drawerRoomTitle() {
      return this.drawerRoomDetail.name || this.drawerRoom.name || '房源详情';
    },
    drawerRoomSubtitle() {
      return [this.drawerRoomParkName, this.drawerRoomBuildingName, this.drawerRoomFloorNo ? `${this.drawerRoomFloorNo}层` : '']
        .filter(Boolean)
        .join(' · ');
    },
    drawerRoomParkId() {
      return this.drawerRoomDetail.parkId || this.drawerRoom.parkId || this.query.parkId;
    },
    drawerRoomBuildingId() {
      return this.drawerRoomDetail.buildingId || this.drawerRoom.buildingId || this.query.buildingId;
    },
    drawerRoomFloorNo() {
      return this.drawerRoomDetail.floorNo || this.drawerRoomDetail.floor || this.drawerRoom.floorNo || this.drawerRoom.floor || this.query.floorNo;
    },
    drawerRoomParkName() {
      return this.drawerRoomDetail.parkName || this.drawerRoom.parkName || this.currentPark.name;
    },
    drawerRoomBuildingName() {
      return this.drawerRoomDetail.buildingName || this.drawerRoom.buildingName || this.currentBuilding.name;
    },
    buildingBasicInfoItems() {
      return [
        { label: '所属园区', value: this.displayValue(this.currentPark.name || this.currentBuilding.parkName) },
        { label: '楼宇名称', value: this.displayValue(this.currentBuilding.name) },
        { label: '楼宇编码', value: this.displayValue(this.currentBuilding.code) },
        { label: '楼宇类型', value: this.displayValue(this.currentBuilding.buildingType) },
        { label: '楼宇地址', value: this.displayValue(this.currentBuilding.address || this.currentPark.detailAddress), wide: true },
        { label: '所属地区', value: this.displayValue(this.currentBuilding.region) },
        { label: '建筑面积', value: this.formatMeasure(this.currentBuilding.area, '㎡') },
        { label: '管理面积', value: this.formatMeasure(this.overview.managementArea, '㎡') },
        { label: '可租面积', value: this.formatMeasure(this.currentBuilding.rentableArea, '㎡') },
        { label: '房源数量', value: this.formatMeasure(this.currentBuilding.roomCount || this.overview.totalRoomCount, '间') },
        { label: '在租房源', value: this.formatMeasure(this.overview.rentedRoomCount, '间') },
        { label: '楼宇简介', value: this.displayValue(this.currentBuilding.memo), full: true },
      ];
    },
    buildingExtensionInfoItems() {
      return [
        { label: '排序值', value: this.displayValue(this.currentBuilding.sortNum) },
        { label: '楼层数', value: this.formatMeasure(this.currentBuilding.floors, '层') },
        { label: '标准层高', value: this.formatMeasure(this.currentBuilding.standardFloorHeight, 'm') },
        { label: '产权面积', value: this.formatMeasure(this.currentBuilding.propertyArea, '㎡') },
        { label: '自用面积', value: this.formatMeasure(this.currentBuilding.selfUseArea, '㎡') },
        { label: '配套面积', value: this.formatMeasure(this.currentBuilding.supportingArea, '㎡') },
        { label: '车位面积', value: this.formatMeasure(this.currentBuilding.parkingArea, '㎡') },
        { label: '不动产编号', value: this.displayValue(this.currentBuilding.realEstateNo) },
        { label: '产权编号', value: this.displayValue(this.currentBuilding.propertyNo) },
        { label: '土地编号', value: this.displayValue(this.currentBuilding.landNo) },
        { label: '状态', value: this.currentBuilding.status === '1' ? '停用' : '启用' },
        { label: '创建时间', value: this.displayValue(this.currentBuilding.createTime) },
      ];
    },
    buildingImageList() {
      return this.parseRoomImageUrls(this.currentBuilding.sceneImages);
    },
    roomDetailImageList() {
      return this.parseRoomImageUrls(this.roomDetail.sceneImages);
    },
    workorderFilterRoomIds() {
      if (!this.query.buildingId) return '';
      const ids = this.floors.flatMap(floor => (floor.rooms || []).map(room => room.id)).filter(Boolean);
      return ids.length ? ids.join(',') : '-1';
    },
    workorderDefaultRoomInfo() {
      return [
        this.currentPark.name,
        this.currentBuilding.name,
        this.query.floorNo ? `${this.query.floorNo}F` : '',
      ].filter(Boolean).join(' / ');
    },
    roomParkOptions() {
      const parks = this.getTreeParks();
      if (parks.length) {
        return parks;
      }
      const parkMap = new Map();
      this.allBuildings.forEach(building => {
        if (building.parkId && !parkMap.has(String(building.parkId))) {
          parkMap.set(String(building.parkId), {
            id: building.parkId,
            name: building.parkName || `园区${building.parkId}`,
          });
        }
      });
      return Array.from(parkMap.values());
    },
    filteredRoomBuildings() {
      if (!this.roomForm.parkId) {
        return this.allBuildings;
      }
      return this.allBuildings.filter(building => String(building.parkId) === String(this.roomForm.parkId));
    },
  },
  created() {
    if (this.$route.query.tab === 'meters') {
      this.activeTab = 'meters';
    }
    this.loadBoard();
    this.loadAllBuildings();
  },
  methods: {
    emptyRoomForm() {
      return {
        id: undefined,
        parkId: undefined,
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
      const requestId = ++this.boardRequestId;
      this.loading = true;
      const params = {
        parkId: this.query.parkId,
        buildingId: this.query.buildingId,
        floorNo: this.query.floorNo,
        keyword: this.query.keyword,
        searchType: this.query.searchType,
        status: this.query.status,
        orientation: this.query.orientation,
        includeTree: !this.treeLoaded,
      };
      getBoard(params)
        .then(res => {
          if (requestId !== this.boardRequestId) {
            return;
          }
          const data = res.data.data || {};
          const hasTreeData = Array.isArray(data.parks);
          const parks = hasTreeData ? this.sortParks(data.parks) : this.parks;
          if (hasTreeData) {
            this.treeLoaded = true;
          }
          if (!this.query.parkId && parks.length) {
            this.parks = parks;
            this.treeParks = parks;
            this.query.parkId = Number(parks[0].id);
            this.query.buildingId = undefined;
            this.query.floorNo = undefined;
            this.selectedKey = `park-${parks[0].id}`;
            this.loadBoard();
            return;
          }
          this.board = data;
          this.currentPark = data.currentPark || {};
          this.currentBuilding = data.currentBuilding || {};
          if (hasTreeData) {
            this.parks = parks;
            this.treeParks = parks;
          }
          if (Array.isArray(data.buildings)) {
            this.buildings = this.sortBuildings(data.buildings);
          }
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
          if (requestId === this.boardRequestId) {
            this.loading = false;
          }
        });
    },
    loadAllBuildings() {
      getBuildingList({}).then(res => {
        this.allBuildings = res.data.data || [];
      });
    },
    handleSearchTypeChange() {
      if (this.hasSearchKeyword) {
        this.handleSearch();
      }
    },
    handleSearch() {
      this.clearRoomSelection();
      this.roomWorkbenchVisible = false;
      this.query.buildingId = undefined;
      this.query.floorNo = undefined;
      this.activeTab = 'rooms';
      this.loadBoard();
    },
    handleSearchClear() {
      this.clearRoomSelection();
      this.roomWorkbenchVisible = false;
      this.loadBoard();
    },
    handleSearchRoomClick(room) {
      this.selectRoom(room);
    },
    searchRoomNo(room = {}) {
      const floorNo = room.floorNo || room.floor;
      return [floorNo ? `${floorNo}层` : '', room.name || '-'].filter(Boolean).join(' / ');
    },
    searchRoomBuilding(room = {}) {
      return [room.parkName || this.currentPark.name, room.buildingName || '-'].filter(Boolean).join(' / ');
    },
    getTreeParks() {
      return this.treeParks.length ? this.treeParks : this.parks;
    },
    syncTreeState() {
      const expanded = [];
      this.getTreeParks().forEach(park => {
        expanded.push(`park-${park.id}`);
        (park.buildings || []).forEach(building => {
          if (this.query.buildingId && String(this.query.buildingId) === String(building.id)) {
            expanded.push(`building-${building.id}`);
            if (this.query.floorNo) {
              expanded.push(`floor-${building.id}-${this.query.floorNo}`);
            }
          }
        });
      });
      this.defaultExpandedKeys = expanded;
      if (this.isRoomSelection) {
        this.selectedKey = `room-${this.selectedRoomId}`;
      } else if (this.query.floorNo && this.query.buildingId) {
        this.selectedKey = `floor-${this.query.buildingId}-${this.query.floorNo}`;
      } else if (this.query.buildingId) {
        this.selectedKey = `building-${this.query.buildingId}`;
      } else if (this.query.parkId) {
        this.selectedKey = `park-${this.query.parkId}`;
      }
    },
    handleTreeNodeClick(node) {
      if (node.type === 'park') {
        this.clearRoomSelection();
        this.query.parkId = node.id;
        this.query.buildingId = undefined;
        this.query.floorNo = undefined;
        this.activeTab = 'overview';
      }
      if (node.type === 'building') {
        this.clearRoomSelection();
        this.query.parkId = node.parkId;
        this.query.buildingId = node.id;
        this.query.floorNo = undefined;
        this.activeTab = 'rooms';
      }
      if (node.type === 'floor') {
        this.clearRoomSelection();
        this.query.parkId = node.parkId;
        this.query.buildingId = node.buildingId;
        this.query.floorNo = node.floorNo;
        this.activeTab = 'rooms';
      }
      if (node.type === 'room') {
        this.selectRoom(node);
      }
      this.selectedKey = node.key;
      this.loadBoard();
    },
    clearRoomSelection() {
      this.selectedRoomId = undefined;
      this.selectedRoom = {};
      this.selectedRoomDetail = {};
    },
    selectRoom(room) {
      this.query.parkId = room.parkId || this.query.parkId;
      this.query.buildingId = room.buildingId || this.query.buildingId;
      this.query.floorNo = room.floorNo || room.floor || this.query.floorNo;
      this.selectedRoomId = room.roomId || room.id;
      this.selectedRoom = { ...room };
      this.selectedRoomDetail = { ...room };
      this.selectedKey = `room-${this.selectedRoomId}`;
    },
    handleRoomLoaded(room) {
      this.selectedRoomDetail = room || {};
    },
    handleReset() {
      const { parkId, buildingId, floorNo } = this.query;
      this.query = {
        parkId,
        buildingId,
        floorNo,
        keyword: '',
        searchType: 'room',
        status: '',
        orientation: '',
      };
      this.activeTab = 'overview';
      this.loadBoard();
    },
    sortParks(parks) {
      return [...parks].sort((left, right) => {
        const leftOrder = this.getParkOrder(left.name);
        const rightOrder = this.getParkOrder(right.name);
        if (leftOrder !== rightOrder) {
          return leftOrder - rightOrder;
        }
        const nameCompare = String(left.name || '').localeCompare(String(right.name || ''), 'zh-Hans-CN', { numeric: true });
        if (nameCompare !== 0) {
          return nameCompare;
        }
        return Number(left.id || 0) - Number(right.id || 0);
      });
    },
    sortBuildings(buildings) {
      return [...buildings].sort((left, right) => {
        const sortCompare = Number(left.sortNum || 999999) - Number(right.sortNum || 999999);
        if (sortCompare !== 0) {
          return sortCompare;
        }
        const nameCompare = String(left.name || '').localeCompare(String(right.name || ''), 'zh-Hans-CN', { numeric: true });
        if (nameCompare !== 0) {
          return nameCompare;
        }
        return Number(left.id || 0) - Number(right.id || 0);
      });
    },
    getParkOrder(name) {
      const match = String(name || '').match(/园区([一二三四五六七八九十百\d]+)号|([一二三四五六七八九十百\d]+)号园区/);
      if (!match) {
        return 999999;
      }
      const value = match[1] || match[2];
      if (/^\d+$/.test(value)) {
        return Number(value);
      }
      return this.parseChineseNumber(value);
    },
    parseChineseNumber(text) {
      const map = { 一: 1, 二: 2, 三: 3, 四: 4, 五: 5, 六: 6, 七: 7, 八: 8, 九: 9 };
      if (text === '十') {
        return 10;
      }
      if (text.includes('十')) {
        const parts = text.split('十');
        const tens = parts[0] ? map[parts[0]] : 1;
        const ones = parts[1] ? map[parts[1]] : 0;
        return tens * 10 + ones;
      }
      return map[text] || 999999;
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
    formatMeasure(value, unit) {
      if (value === null || value === undefined || value === '') return '-';
      return `${this.formatNumber(value)}${unit}`;
    },
    normalizeStatus(status) {
      return ['0', '1', '2', '3', '4', '5', '6', '7'].includes(String(status)) ? String(status) : '0';
    },
    statusLabel(status) {
      const option = this.statusOptions.find(item => item.value === this.normalizeStatus(status));
      return option ? option.label : '空置';
    },
    splitFloorRoomRows(floor = {}) {
      const rooms = Array.isArray(floor.rooms) ? floor.rooms : [];
      if (rooms.length <= 4) {
        return [rooms];
      }

      const roomAreas = rooms.map(room => Math.max(Number(room.area || 0), 1));
      const totalArea = roomAreas.reduce((total, area) => total + area, 0);
      const lowerSplit = Math.floor(rooms.length / 2);
      const upperSplit = Math.ceil(rooms.length / 2);
      let firstRowCount = upperSplit;
      let bestAreaDifference = Number.POSITIVE_INFINITY;

      for (let split = lowerSplit; split <= upperSplit; split += 1) {
        const firstRowArea = roomAreas.slice(0, split).reduce((total, area) => total + area, 0);
        const areaDifference = Math.abs(totalArea - firstRowArea * 2);
        if (areaDifference < bestAreaDifference) {
          bestAreaDifference = areaDifference;
          firstRowCount = split;
        }
      }

      return [
        rooms.slice(0, firstRowCount),
        rooms.slice(firstRowCount),
      ].filter(row => row.length);
    },
    roomTileStyle(room = {}, floor = {}) {
      const rooms = Array.isArray(floor.rooms) ? floor.rooms : [];
      const maxArea = rooms
        .map(item => Number(item.area || 0))
        .filter(area => area > 0)
        .reduce((max, area) => Math.max(max, area), 0);
      const area = Number(room.area || 0);
      if (!maxArea || !area) {
        return {};
      }
      const width = Math.round(210 + (area / maxArea) * 220);
      return {
        flexBasis: `${Math.min(Math.max(width, 210), 430)}px`,
        flexGrow: Math.max(area, 1),
        flexShrink: 1,
      };
    },
    floorRowKey(floor, index) {
      return `${floor.buildingId || 'building'}-${floor.floor || index}`;
    },
    handleAddRoom() {
      this.roomForm = this.emptyRoomForm();
      this.roomImageFileList = [];
      this.roomDetail = {};
      const firstVisibleFloor = this.floors[0] || {};
      const defaultBuildingId = this.query.buildingId || this.currentBuilding.id || firstVisibleFloor.buildingId;
      const defaultParkId = this.query.parkId || this.currentPark.id || this.currentBuilding.parkId;
      const defaultFloorNo = this.query.floorNo || firstVisibleFloor.floor || firstVisibleFloor.floorNo;
      if (defaultParkId) {
        this.roomForm.parkId = String(defaultParkId);
      }
      if (defaultBuildingId) {
        this.roomForm.buildingId = String(defaultBuildingId);
        this.syncRoomParkByBuilding();
      }
      if (defaultFloorNo) {
        this.roomForm.floor = defaultFloorNo;
      }
      this.syncCurrentBuildingFloors();
      this.syncFloorAreaInfo();
      this.roomFormVisible = true;
    },
    openRoom(room) {
      this.drawerRoomId = room.roomId || room.id;
      this.drawerRoom = { ...room };
      this.drawerRoomDetail = { ...room };
      this.roomWorkbenchVisible = true;
    },
    handleDrawerRoomLoaded(room) {
      this.drawerRoomDetail = room || { ...this.drawerRoom };
    },
    handleEditRoom(room) {
      this.roomForm = Object.assign(this.emptyRoomForm(), room);
      if (this.roomForm.buildingId) {
        this.roomForm.buildingId = String(this.roomForm.buildingId);
      }
      if (this.roomForm.parkId) {
        this.roomForm.parkId = String(this.roomForm.parkId);
      }
      this.roomForm.status = this.normalizeBaseStatus(room.baseStatus || room.status);
      this.roomImageFileList = this.parseRoomImageFiles(this.roomForm.sceneImages);
      this.roomDetail = Object.assign({}, room);
      this.roomDetailVisible = false;
      this.roomWorkbenchVisible = false;
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
          if (String(this.drawerRoomId || '') === String(room.id || '')) {
            this.roomWorkbenchVisible = false;
            this.drawerRoomId = undefined;
            this.drawerRoom = {};
            this.drawerRoomDetail = {};
          }
          if (String(this.selectedRoomId || '') === String(room.id || '')) {
            this.clearRoomSelection();
            this.activeTab = 'rooms';
            this.selectedKey = `floor-${this.query.buildingId}-${this.query.floorNo}`;
          }
          this.treeLoaded = false;
          this.loadBoard();
        });
    },
    handleChangeRoomStatus(room) {
      const currentStatus = this.normalizeBaseStatus(room.baseStatus || room.status);
      ElMessageBox.prompt('请输入目标状态：0空置、1待清退/短租、2预留、3待退出、4为90天内到期、5为30天内到期、6为已到期、7为已出租', '状态流转', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: currentStatus,
        inputPattern: /^[0-7]$/,
        inputErrorMessage: '状态值必须为 0-7',
      })
        .then(({ value }) => changeRoomStatus(room.id, value))
        .then(() => {
          ElMessage.success('状态已更新');
          this.roomDetailVisible = false;
          this.roomRevision += 1;
          this.treeLoaded = false;
          this.loadBoard();
        });
    },
    normalizeBaseStatus(status) {
      return ['0', '1', '2', '3', '4', '5', '6', '7'].includes(String(status)) ? String(status) : '0';
    },
    handleSyncRoom(room) {
      syncRoomMini(room.id).then(() => {
        ElMessage.success('已标记同步');
        this.roomDetailVisible = false;
        this.loadBoard();
      });
    },
    handleRoomBuildingChange() {
      this.syncRoomParkByBuilding();
      this.syncCurrentBuildingFloors();
      if (this.currentBuildingFloors && this.roomForm.floor > this.currentBuildingFloors) {
        this.roomForm.floor = undefined;
      }
      this.syncFloorAreaInfo();
    },
    handleRoomParkChange() {
      const building = this.allBuildings.find(item => String(item.id) === String(this.roomForm.buildingId));
      if (building && String(building.parkId) !== String(this.roomForm.parkId)) {
        this.roomForm.buildingId = undefined;
        this.roomForm.floor = undefined;
      }
      this.syncCurrentBuildingFloors();
      this.syncFloorAreaInfo();
    },
    syncRoomParkByBuilding() {
      const building = this.allBuildings.find(item => String(item.id) === String(this.roomForm.buildingId));
      if (building && building.parkId) {
        this.roomForm.parkId = String(building.parkId);
      }
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
        if (this.hasUploadingRoomImages()) {
          ElMessage.warning('房间照片正在上传，请稍后保存');
          return;
        }
        this.syncRoomImageField();
        if (
          this.currentFloorRemainArea !== null &&
          this.roomForm.area !== undefined &&
          Math.round(Number(this.roomForm.area || 0) * 100) > Math.round(Number(this.currentFloorRemainArea || 0) * 100)
        ) {
          ElMessage.warning(`房源面积不能超过当前楼层剩余面积 ${this.formatNumber(this.currentFloorRemainArea)}㎡`);
          return;
        }
        this.roomSaving = true;
        submitRoom(this.roomForm)
          .then(() => {
            ElMessage.success('保存成功');
            this.roomFormVisible = false;
            this.roomRevision += 1;
            this.treeLoaded = false;
            this.loadBoard();
          })
          .finally(() => {
            this.roomSaving = false;
          });
      });
    },
    beforeRoomImageUpload(file) {
      const isImage = file.type && file.type.indexOf('image/') === 0;
      const isValidSize = file.size / 1024 / 1024 <= 10;
      if (!isImage) {
        ElMessage.warning('请上传图片文件');
      }
      if (!isValidSize) {
        ElMessage.warning('单张图片不能超过 10MB');
      }
      return isImage && isValidSize;
    },
    handleRoomImageUploadSuccess(response, uploadFile, uploadFiles) {
      const success = response && (response.success || response.code === 200 || response.code === 0);
      const data = response && response.data;
      const url = typeof data === 'string' ? data : (data && (data.link || data.url)) || '';
      if (!success || !url) {
        this.roomImageFileList = (uploadFiles || []).filter(file => file.uid !== uploadFile.uid);
        this.syncRoomImageField();
        ElMessage.error((response && response.msg) || '上传失败');
        return;
      }
      uploadFile.url = url;
      uploadFile.name = (data && (data.originalName || data.name)) || uploadFile.name;
      uploadFile.status = 'success';
      this.roomImageFileList = this.normalizeRoomImageFiles(uploadFiles || []);
      this.syncRoomImageField();
      ElMessage.success('上传成功');
    },
    handleRoomImageRemove(file, uploadFiles) {
      this.roomImageFileList = this.normalizeRoomImageFiles(uploadFiles || []);
      this.syncRoomImageField();
    },
    handleRoomImageUploadError() {
      ElMessage.error('上传失败');
    },
    handleRoomImageExceed() {
      ElMessage.warning('房间照片最多上传 9 张');
    },
    hasUploadingRoomImages() {
      return this.roomImageFileList.some(file => file.status && file.status !== 'success');
    },
    syncRoomImageField() {
      this.roomForm.sceneImages = this.roomImageFileList.map(file => file.url).filter(Boolean).join(',');
    },
    normalizeRoomImageFiles(files) {
      return (Array.isArray(files) ? files : [])
        .map((file, index) => {
          const responseData = file.response && file.response.data;
          const url = file.url || (typeof responseData === 'string' ? responseData : responseData && (responseData.link || responseData.url)) || '';
          return {
            uid: file.uid || `${Date.now()}-${index}`,
            name: file.name || (url ? url.split('/').pop() : `房间照片${index + 1}`),
            url,
            status: file.status || 'success',
          };
        })
        .filter(file => file.url || file.status !== 'success');
    },
    parseRoomImageFiles(value) {
      return this.parseRoomImageUrls(value).map((url, index) => ({
        uid: `${Date.now()}-${index}`,
        name: url.split('/').pop() || `房间照片${index + 1}`,
        url,
        status: 'success',
      }));
    },
    parseRoomImageUrls(value) {
      if (!value) {
        return [];
      }
      if (Array.isArray(value)) {
        return value.map(item => (typeof item === 'string' ? item : item.url || item.link || '')).filter(Boolean);
      }
      const text = String(value).trim();
      if (!text) {
        return [];
      }
      if (text.startsWith('[')) {
        try {
          const list = JSON.parse(text);
          return this.parseRoomImageUrls(list);
        } catch (error) {
          return [];
        }
      }
      return text.split(',').map(item => item.trim()).filter(Boolean);
    },
    roomCoverImage(room = {}) {
      return this.parseRoomImageUrls(room.sceneImages)[0] || '';
    },
  },
};
</script>

<style scoped>
.rent-control-page {
  display: grid;
  grid-template-columns: 276px minmax(0, 1fr);
  gap: 14px;
  height: calc(100vh - 160px);
  min-height: 620px;
  overflow: hidden;
}

.rent-sidebar {
  display: flex;
  flex-direction: column;
  min-height: 0;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.side-search {
  padding: 14px 12px 10px;
  border-bottom: 1px solid #ebeef5;
}

.side-search-bar {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 48px;
  align-items: center;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background: #fff;
}

.side-search-type,
.side-search-input {
  width: 100%;
}

.side-search-bar :deep(.el-select__wrapper),
.side-search-bar :deep(.el-input__wrapper) {
  min-height: 30px;
  border-radius: 0;
  box-shadow: none;
}

.side-search-type :deep(.el-select__wrapper) {
  background: #f5f7fa;
  border-right: 1px solid #dcdfe6;
}

.side-search-input :deep(.el-input__wrapper) {
  padding-left: 8px;
}

.side-search-button {
  width: 48px;
  height: 32px;
  border-radius: 0;
  padding: 0;
}

.tree-scroll {
  flex: 1;
  min-height: 0;
  padding: 8px;
}

.search-result-scroll {
  flex: 1;
  min-height: 0;
  padding: 8px 12px;
}

.search-result-empty {
  padding: 28px 0;
  color: #909399;
  font-size: 13px;
  text-align: center;
}

.room-search-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) 18px;
  gap: 8px;
  width: 100%;
  padding: 12px 4px;
  border: 0;
  border-bottom: 1px solid #dcdfe6;
  background: transparent;
  color: #606266;
  text-align: left;
  cursor: pointer;
}

.room-search-item:hover,
.room-search-item.is-active {
  background: #ecf5ff;
}

.room-search-item > .el-icon {
  margin-top: 4px;
  color: #4b5563;
  font-size: 16px;
}

.room-search-item__body {
  display: grid;
  gap: 3px;
  min-width: 0;
  font-size: 13px;
  line-height: 1.35;
}

.room-search-item__body span,
.room-search-item__body em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-search-item__body em {
  color: #303133;
  font-style: normal;
}

.room-search-item .room-search-item__tenant {
  color: #52c41a;
  font-size: 14px;
}

.rent-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  width: 100%;
  padding-right: 8px;
}

.rent-tree-node .el-icon {
  flex: 0 0 auto;
  color: #7b8794;
}

.rent-tree-node__label {
  overflow: hidden;
  flex: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rent-tree-node__status {
  width: 7px;
  height: 7px;
  flex: 0 0 7px;
  border-radius: 50%;
}

.rent-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.rent-header {
  display: flex;
  flex: 0 0 auto;
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

.rent-tabs-shell {
  position: relative;
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.room-add-tab-button {
  position: absolute;
  top: -9px;
  right: 10px;
  z-index: 8;
  height: 32px;
}

.rent-main :deep(.el-tabs__nav) {
  display: flex;
}

.rent-main :deep(.el-tabs) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.rent-main :deep(.el-tabs__header) {
  flex: 0 0 auto;
  min-height: 46px;
  margin-bottom: 0;
  padding-right: 150px;
  border-bottom: 1px solid #e4e7ed;
}

.rent-main :deep(.el-tabs__nav-wrap) {
  min-height: 46px;
}

.rent-main :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.rent-main :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-top: 10px;
}

.rent-main :deep(#tab-rooms) {
  order: -1;
  padding-left: 0;
}

.rent-main :deep(#tab-overview) {
  padding-left: 20px;
}

.floor-only-tabs :deep(.el-tabs__header) {
  display: none;
}

.floor-only-tabs :deep(.el-tabs__content) {
  padding-top: 0;
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
.floor-row {
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
  margin-bottom: 6px;
  padding: 0 0 4px;
  background: #fff;
}

.status-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: #606266;
  font-size: 12px;
}

.status-legend span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 28px;
  padding: 0 11px 0 6px;
  border: 1px solid #e5e7eb;
  border-radius: 999px;
  background: #fff;
  line-height: 1;
  white-space: nowrap;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.status-legend i {
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  border-radius: 2px;
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
  gap: 8px;
  overflow: hidden;
  padding: 10px;
}

.room-grid-line {
  display: flex;
  gap: 8px;
  width: 100%;
  min-width: 0;
}

.room-tile {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr);
  gap: 8px;
  min-height: 98px;
  flex: 0 0 210px;
  min-width: 0;
  padding: 8px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.room-cover {
  display: flex;
  width: 82px;
  height: 82px;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 5px;
  background: #f5f7fa;
}

.room-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.room-cover__empty {
  padding: 0 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
}

.room-tile__body {
  display: grid;
  min-width: 0;
  grid-template-rows: repeat(4, auto);
  align-content: center;
  gap: 4px;
}

.room-tile:hover {
  border-color: #409eff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
}

.room-tile:hover .room-cover {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.18);
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
  background: #fff000;
}

.status-1 {
  background: #ff4d4f;
}

.status-2 {
  background: #00b7e8;
}

.status-3 {
  background: #f4b400;
}

.status-4 {
  background: #8b5cf6;
}

.status-5 {
  background: #ef7d22;
}

.status-6 {
  background: #8b1e1e;
}

.status-7 {
  background: #22c55e;
}

.room-tile.status-0 {
  border-left: 4px solid #fff000;
}

.room-tile.status-1 {
  border-left: 4px solid #ff4d4f;
}

.room-tile.status-2 {
  border-left: 4px solid #00b7e8;
}

.room-tile.status-3 {
  border-left: 4px solid #f4b400;
}

.room-tile.status-4 {
  border-left: 4px solid #8b5cf6;
}

.room-tile.status-5 {
  border-left: 4px solid #ef7d22;
}

.room-tile.status-6 {
  border-left: 4px solid #8b1e1e;
}

.room-tile.status-7 {
  border-left: 4px solid #22c55e;
}

.room-tile:hover {
  border: 1px solid #409eff;
  padding-left: 11px;
  outline: none;
}

.building-profile {
  padding: 20px 22px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.building-profile-section + .building-profile-section {
  margin-top: 24px;
  padding-top: 22px;
  border-top: 1px solid #ebeef5;
}

.building-profile-section h3 {
  margin: 0 0 16px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.building-profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 32px;
  row-gap: 16px;
}

.building-profile-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  line-height: 24px;
}

.building-profile-item.is-wide {
  grid-column: span 2;
}

.building-profile-item.is-full {
  grid-column: 1 / -1;
}

.building-profile-item span {
  color: #909399;
  white-space: nowrap;
}

.building-profile-item strong {
  min-width: 0;
  color: #606266;
  font-weight: 400;
  overflow-wrap: anywhere;
}

.building-profile-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, 144px);
  gap: 12px;
}

.building-profile-images :deep(.el-image) {
  width: 144px;
  height: 104px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f5f7fa;
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

.room-detail-images strong {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.room-detail-images :deep(.el-image) {
  width: 78px;
  height: 78px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

:deep(.room-workbench-drawer) {
  max-width: calc(100vw - 486px);
}

:deep(.room-workbench-drawer .el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding: 0;
  background: #f4f4f6;
}

.room-workbench-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 24px 14px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.room-workbench-drawer__title {
  color: #303133;
  font-size: 20px;
  font-weight: 650;
  line-height: 1.3;
}

.room-workbench-drawer__subtitle {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.room-workbench-drawer__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.room-workbench-drawer__actions :deep(.el-button) {
  min-width: 76px;
  height: 34px;
  padding: 0 16px;
  font-size: 13px;
}

:deep(.room-workbench-drawer .room-workbench) {
  flex: 1;
  min-height: 0;
  padding: 14px 24px 24px;
  overflow: auto;
}

@media (max-width: 1100px) {
  .rent-control-page {
    grid-template-columns: 1fr;
    height: auto;
    overflow: visible;
  }

  .rent-sidebar {
    min-height: auto;
  }

  .tree-scroll,
  .search-result-scroll {
    height: 260px;
    flex: none;
  }

  :deep(.room-workbench-drawer) {
    max-width: calc(100vw - 140px);
  }

  .metric-grid,
  .analysis-grid,
  .building-profile-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .building-profile-item.is-full {
    grid-column: 1 / -1;
  }
}

@media (max-width: 720px) {
  .rent-header,
  .room-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .rent-actions {
    flex-wrap: wrap;
  }

  .metric-grid,
  .analysis-grid,
  .building-profile-grid {
    grid-template-columns: 1fr;
  }

  .building-profile-item.is-wide,
  .building-profile-item.is-full {
    grid-column: auto;
  }

  .floor-row {
    grid-template-columns: 1fr;
  }
}
</style>
