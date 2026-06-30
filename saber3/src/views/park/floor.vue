<template>
  <basic-container>
    <div class="floor-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="floor-toolbar">
        <el-form :model="query" inline class="floor-search">
          <el-form-item label="所属建筑">
            <el-select
              v-model="query.buildingId"
              clearable
              filterable
              placeholder="请选择建筑"
              @change="handleSearch"
            >
              <el-option
                v-for="building in buildingOptions"
                :key="building.id"
                :label="buildingLabel(building)"
                :value="building.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="楼层号">
            <el-input-number
              v-model="query.floorNo"
              :min="1"
              :precision="0"
              controls-position="right"
              placeholder="楼层"
              @change="handleSearch"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <div class="table-action-bar">
        <el-button v-if="permission.floor_add" type="primary" :icon="Plus" @click="openCreate">
          新增楼层
        </el-button>
        <el-button
          v-if="permission.floor_delete"
          type="danger"
          plain
          :icon="Delete"
          :disabled="selectionList.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="data"
        row-key="id"
        border
        stripe
        class="floor-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column v-if="permission.floor_room_view" type="expand" width="48">
          <template #default="{ row: floor }">
            <div class="room-panel">
              <el-table :data="floor.rooms || []" size="small" border empty-text="暂无房间">
                <el-table-column prop="name" label="房间名称" min-width="180" align="center">
                  <template #default="{ row: room }">
                    <div class="room-name-cell">
                      <span class="room-name-text">{{ room.name || '-' }}</span>
                      <el-button
                        v-if="permission.floor_edit && room.id"
                        link
                        type="danger"
                        :icon="Delete"
                        @click.stop="handleDeleteRoom(room, floor)"
                      >
                        删除
                      </el-button>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="area" label="面积(㎡)" width="110" align="center">
                  <template #default="{ row: room }">{{ formatNumber(room.area) }}</template>
                </el-table-column>
                <el-table-column prop="rentPrice" label="月租金" width="120" align="center">
                  <template #default="{ row: room }">{{ formatNumber(room.rentPrice) }}</template>
                </el-table-column>
                <el-table-column prop="propertyFee" label="物业费" width="110" align="center">
                  <template #default="{ row: room }">{{ formatNumber(room.propertyFee) }}</template>
                </el-table-column>
                <el-table-column prop="orientation" label="朝向" width="100" align="center" />
                <el-table-column prop="status" label="状态" width="120" align="center">
                  <template #default="{ row: room }">
                    <el-tag :class="roomStatusClass(room.status)" effect="plain">
                      {{ roomStatusLabel(room.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="syncStatus" label="小程序同步" width="120" align="center">
                  <template #default="{ row: room }">
                    <el-tag :type="room.syncStatus === '1' ? 'success' : 'info'">
                      {{ room.syncStatus === '1' ? '已同步' : '待同步' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="parkName" label="所属园区" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="buildingName" label="所属建筑" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column prop="floorNo" label="楼层" width="90" align="center">
          <template #default="{ row }">{{ row.floorNo }}F</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'info' : 'success'">{{ floorStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="area" label="楼层面积(㎡)" width="130" align="center">
          <template #default="{ row }">{{ formatNumber(row.area) }}</template>
        </el-table-column>
        <el-table-column prop="usedArea" label="已用面积(㎡)" width="130" align="center">
          <template #default="{ row }">{{ formatNumber(row.usedArea) }}</template>
        </el-table-column>
        <el-table-column prop="totalCount" label="房间数" width="95" align="center" />
        <el-table-column prop="occupancyRate" label="出租率" width="100" align="center">
          <template #default="{ row }">{{ formatNumber(row.occupancyRate) }}%</template>
        </el-table-column>
        <el-table-column label="房态统计" min-width="180" align="center">
          <template #default="{ row }">
            <div class="status-tags">
              <template v-if="visibleRoomStatusTags(row).length">
                <el-tag
                  v-for="item in visibleRoomStatusTags(row)"
                  :key="item.status"
                  size="small"
                  :class="roomStatusClass(item.status)"
                  effect="plain"
                >
                  {{ item.label }} {{ item.count }}
                </el-tag>
              </template>
              <span v-else class="status-empty">暂无房态</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="memo" label="备注" min-width="140" align="center" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button v-if="permission.floor_view" link type="primary" :icon="View" @click="openView(row)">
                查看
              </el-button>
              <el-button v-if="permission.floor_edit" link type="primary" :icon="Edit" @click="openEdit(row)">
                编辑
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="page-footer">
        <el-pagination
          v-model:current-page="page.currentPage"
          v-model:page-size="page.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="page.total"
          background
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-drawer v-model="drawerVisible" :title="drawerTitle" size="520px" @closed="resetForm">
      <el-form
        ref="floorFormRef"
        :model="form"
        :rules="formRules"
        :disabled="formReadonly"
        label-width="110px"
      >
        <el-form-item label="所属建筑" prop="buildingId">
          <el-select
            v-model="form.buildingId"
            filterable
            placeholder="请选择建筑"
            @change="handleFormBuildingChange"
          >
            <el-option
              v-for="building in buildingOptions"
              :key="building.id"
              :label="buildingLabel(building)"
              :value="building.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层号" prop="floorNo">
          <el-input-number
            v-model="form.floorNo"
            :min="1"
            :max="formBuildingFloors || undefined"
            :precision="0"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="楼层面积" prop="area">
          <el-input-number v-model="form.area" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status">
            <el-option v-for="item in floorStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.memo" type="textarea" :rows="4" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <section v-if="form.id" class="floor-room-editor">
        <div class="drawer-section-title">
          <span>房源管理</span>
          <el-button
            v-if="!formReadonly"
            type="primary"
            :icon="Plus"
            @click="openRoomCreate(form)"
          >
            新增房源
          </el-button>
        </div>
        <div v-if="form.rooms && form.rooms.length" class="drawer-room-list">
          <div v-for="room in form.rooms" :key="room.id || room.name" class="drawer-room-item">
            <span class="drawer-room-name">{{ room.name || '-' }}</span>
            <el-tag size="small" :class="roomStatusClass(room.status)" effect="plain">
              {{ roomStatusLabel(room.status) }}
            </el-tag>
            <el-button
              v-if="!formReadonly && room.id"
              link
              type="danger"
              :icon="Delete"
              @click="handleDeleteRoom(room, form)"
            >
              删除
            </el-button>
          </div>
        </div>
        <el-empty v-else description="暂无房源" :image-size="72" />
      </section>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button v-if="!formReadonly" type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog
      v-model="roomDialogVisible"
      title="新增房源"
      width="560px"
      append-to-body
      @closed="resetRoomForm"
    >
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="110px" class="room-form">
        <el-form-item label="所属楼层">
          <el-input :model-value="roomFloorLabel" disabled />
        </el-form-item>
        <el-form-item label="房间名称" prop="name">
          <el-input v-model="roomForm.name" maxlength="50" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="面积(㎡)">
          <el-input-number v-model="roomForm.area" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="月租金">
          <el-input-number v-model="roomForm.rentPrice" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="物业费">
          <el-input-number v-model="roomForm.propertyFee" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="户型">
          <el-input v-model="roomForm.houseType" maxlength="50" placeholder="如：开放办公" />
        </el-form-item>
        <el-form-item label="朝向">
          <el-select v-model="roomForm.orientation" clearable placeholder="请选择朝向">
            <el-option v-for="item in orientationOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="roomForm.status">
            <el-option v-for="item in roomStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="房源地址">
          <el-input v-model="roomForm.address" maxlength="100" placeholder="请输入房源地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="roomForm.memo" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roomSaving" @click="submitRoomForm">保存</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Edit, Plus, Refresh, Search, View } from '@element-plus/icons-vue';
import { getSimpleList as getBuildingList } from '@/api/park/building';
import {
  getDetail,
  getList,
  getStatistics,
  remove,
  removeRoom,
  submit,
  submitRoom,
} from '@/api/park/floor';

export default {
  data() {
    return {
      Delete,
      Edit,
      Plus,
      Refresh,
      Search,
      View,
      loading: false,
      saving: false,
      roomSaving: false,
      drawerVisible: false,
      roomDialogVisible: false,
      formMode: 'add',
      activeRoomFloorId: undefined,
      data: [],
      selectionList: [],
      buildingOptions: [],
      statistics: {},
      query: {
        buildingId: undefined,
        floorNo: undefined,
      },
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      form: this.emptyForm(),
      roomForm: this.emptyRoomForm(),
      formRules: {
        buildingId: [{ required: true, message: '请选择所属建筑', trigger: 'change' }],
        floorNo: [{ required: true, message: '请输入楼层号', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
      },
      roomRules: {
        name: [{ required: true, message: '请输入房间名称', trigger: 'blur' }],
      },
      floorStatusOptions: [
        { label: '启用', value: '0' },
        { label: '停用', value: '1' },
      ],
      roomStatusOptions: [
        { label: '空置', value: '0', countKey: 'vacantCount' },
        { label: '待清退/短租', value: '1', countKey: 'rentedCount' },
        { label: '预留', value: '2', countKey: 'reservedCount' },
        { label: '待退出', value: '3', countKey: 'renovatingCount' },
        { label: '90天内到期', value: '4', countKey: 'expiring90Count' },
        { label: '30天内到期', value: '5', countKey: 'expiring30Count' },
        { label: '已到期', value: '6', countKey: 'expiredCount' },
        { label: '已出租', value: '7', countKey: 'disabledCount' },
      ],
      orientationOptions: ['朝南', '朝北', '朝东', '朝西', '南北通透'],
    };
  },
  computed: {
    ...mapGetters(['permission']),
    ids() {
      return this.selectionList.map(item => item.id).join(',');
    },
    drawerTitle() {
      if (this.formMode === 'view') {
        return '查看楼层';
      }
      return this.form.id ? '编辑楼层' : '新增楼层';
    },
    formReadonly() {
      return this.formMode === 'view';
    },
    roomFloorLabel() {
      const building = this.findBuilding(this.roomForm.buildingId);
      const buildingName = building ? this.buildingLabel(building) : this.form.buildingName || '未选择建筑';
      return `${buildingName} / ${this.roomForm.floor || '-'}F`;
    },
    formBuildingFloors() {
      const building = this.findBuilding(this.form.buildingId);
      return building && building.floors ? Number(building.floors) : null;
    },
    summaryCards() {
      return [
        {
          key: 'floorCount',
          label: '楼层总数',
          value: `${this.statistics.floorCount || 0} 层`,
        },
        {
          key: 'totalArea',
          label: '楼层面积',
          value: `${this.formatNumber(this.statistics.totalArea)} ㎡`,
        },
        {
          key: 'totalCount',
          label: '房间总数',
          value: `${this.statistics.totalCount || 0} 间`,
        },
        {
          key: 'occupancyRate',
          label: '出租率',
          value: `${this.formatNumber(this.statistics.occupancyRate)}%`,
        },
      ];
    },
  },
  created() {
    this.loadBuildings();
    this.refreshData();
  },
  methods: {
    emptyForm() {
      return {
        id: undefined,
        parkId: undefined,
        buildingId: undefined,
        floorNo: undefined,
        area: undefined,
        status: '0',
        memo: '',
        rooms: [],
      };
    },
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
    loadBuildings() {
      getBuildingList({}).then(res => {
        this.buildingOptions = res.data.data || [];
      });
    },
    refreshData() {
      this.loadData();
      this.loadStatistics();
    },
    loadData() {
      this.loading = true;
      getList(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
        .then(res => {
          const data = res.data.data || {};
          this.data = data.records || [];
          this.page.total = data.total || 0;
          this.selectionList = [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadStatistics() {
      getStatistics(this.cleanParams(this.query)).then(res => {
        this.statistics = res.data.data || {};
      });
    },
    handleSearch() {
      this.page.currentPage = 1;
      this.refreshData();
    },
    handleReset() {
      this.query = {
        buildingId: undefined,
        floorNo: undefined,
      };
      this.page.currentPage = 1;
      this.refreshData();
    },
    openCreate() {
      this.formMode = 'add';
      this.form = this.emptyForm();
      if (this.query.buildingId) {
        this.form.buildingId = this.query.buildingId;
        this.handleFormBuildingChange(this.query.buildingId);
      }
      this.drawerVisible = true;
    },
    openView(row) {
      this.formMode = 'view';
      this.openDetail(row.id);
    },
    openEdit(row) {
      this.formMode = 'edit';
      this.openDetail(row.id);
    },
    openDetail(id) {
      getDetail(id).then(res => {
        this.form = Object.assign(this.emptyForm(), res.data.data || {});
        this.drawerVisible = true;
      });
    },
    openRoomCreate(floor = this.form) {
      if (!floor || !floor.id || !floor.buildingId || !floor.floorNo) {
        ElMessage.warning('请先保存楼层信息后再新增房源');
        return;
      }
      this.activeRoomFloorId = floor.id;
      this.roomForm = Object.assign(this.emptyRoomForm(), {
        parkId: floor.parkId,
        buildingId: floor.buildingId,
        floor: floor.floorNo,
        status: '0',
      });
      this.roomDialogVisible = true;
    },
    handleFormBuildingChange(buildingId) {
      const building = this.findBuilding(buildingId);
      this.form.parkId = building ? building.parkId : undefined;
      if (building && this.form.floorNo && Number(this.form.floorNo) > Number(building.floors || 0)) {
        this.form.floorNo = undefined;
      }
      if (building && !this.form.area && building.area && building.floors) {
        this.form.area = Number((Number(building.area) / Number(building.floors)).toFixed(2));
      }
    },
    submitForm() {
      this.$refs.floorFormRef.validate(valid => {
        if (!valid) {
          return;
        }
        const building = this.findBuilding(this.form.buildingId);
        if (!building) {
          ElMessage.warning('请选择有效建筑');
          return;
        }
        if (building.floors && Number(this.form.floorNo) > Number(building.floors)) {
          ElMessage.warning(`楼层号不能超过 ${building.floors} 层`);
          return;
        }
        this.saving = true;
        submit(this.normalizePayload())
          .then(() => {
            ElMessage.success('保存成功');
            this.drawerVisible = false;
            this.refreshData();
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    handleDelete(row) {
      ElMessageBox.confirm('确定删除该楼层?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(row.id))
        .then(() => {
          ElMessage.success('删除成功');
          this.refreshData();
        });
    },
    handleBatchDelete() {
      if (!this.selectionList.length) {
        ElMessage.warning('请选择至少一条数据');
        return;
      }
      ElMessageBox.confirm('确定删除选中的楼层?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(this.ids))
        .then(() => {
          ElMessage.success('删除成功');
          this.refreshData();
        });
    },
    handleDeleteRoom(room, floor) {
      if (!room || !room.id) {
        return;
      }
      ElMessageBox.confirm(`确定删除房源“${room.name || ''}”?`, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeRoom(room.id))
        .then(() => {
          ElMessage.success('删除成功');
          this.refreshFloorAfterRoomChange(floor && floor.id);
        });
    },
    submitRoomForm() {
      this.$refs.roomFormRef.validate(valid => {
        if (!valid) {
          return;
        }
        if (!this.roomForm.buildingId || !this.roomForm.floor) {
          ElMessage.warning('缺少楼层信息，请重新打开新增房源');
          return;
        }
        this.roomSaving = true;
        submitRoom(this.roomForm)
          .then(() => {
            ElMessage.success('保存成功');
            this.roomDialogVisible = false;
            this.refreshFloorAfterRoomChange(this.activeRoomFloorId);
          })
          .finally(() => {
            this.roomSaving = false;
          });
      });
    },
    refreshFloorAfterRoomChange(floorId) {
      this.loadStatistics();
      if (!floorId) {
        this.loadData();
        return;
      }
      getDetail(floorId).then(res => {
        const latest = Object.assign(this.emptyForm(), res.data.data || {});
        const index = this.data.findIndex(item => String(item.id) === String(floorId));
        if (index > -1) {
          this.data[index] = Object.assign({}, this.data[index], latest);
        }
        if (this.form && String(this.form.id) === String(floorId)) {
          Object.assign(this.form, {
            rooms: latest.rooms || [],
            usedArea: latest.usedArea,
            totalCount: latest.totalCount,
            rentedCount: latest.rentedCount,
            vacantCount: latest.vacantCount,
            reservedCount: latest.reservedCount,
            renovatingCount: latest.renovatingCount,
            disabledCount: latest.disabledCount,
            expiring90Count: latest.expiring90Count,
            expiring30Count: latest.expiring30Count,
            expiredCount: latest.expiredCount,
            occupancyRate: latest.occupancyRate,
          });
        }
      });
    },
    handleSelectionChange(list) {
      this.selectionList = list;
    },
    normalizePayload() {
      return {
        id: this.form.id,
        parkId: this.form.parkId,
        buildingId: this.form.buildingId,
        floorNo: this.form.floorNo,
        area: this.form.area,
        status: this.form.status,
        memo: this.form.memo,
      };
    },
    resetForm() {
      this.form = this.emptyForm();
      if (this.$refs.floorFormRef) {
        this.$refs.floorFormRef.clearValidate();
      }
    },
    resetRoomForm() {
      this.roomForm = this.emptyRoomForm();
      this.activeRoomFloorId = undefined;
      if (this.$refs.roomFormRef) {
        this.$refs.roomFormRef.clearValidate();
      }
    },
    findBuilding(buildingId) {
      return this.buildingOptions.find(item => String(item.id) === String(buildingId));
    },
    buildingLabel(building) {
      return `${building.parkName || '未关联园区'} / ${building.name}`;
    },
    floorStatusLabel(status) {
      return String(status) === '1' ? '停用' : '启用';
    },
    roomStatusLabel(status) {
      const option = this.roomStatusOptions.find(item => item.value === String(status));
      return option ? option.label : '空置';
    },
    roomStatusClass(status) {
      return `room-tag-${['0', '1', '2', '3', '4', '5', '6', '7'].includes(String(status)) ? status : '0'}`;
    },
    visibleRoomStatusTags(row = {}) {
      return this.roomStatusOptions
        .map(item => ({
          ...item,
          status: item.value,
          count: Number(row[item.countKey]) || 0,
        }))
        .filter(item => item.count > 0);
    },
    cleanParams(params) {
      const result = {};
      Object.keys(params).forEach(key => {
        const value = params[key];
        if (value !== undefined && value !== null && value !== '') {
          result[key] = value;
        }
      });
      return result;
    },
    formatNumber(value) {
      const number = Number(value || 0);
      return number.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      });
    },
  },
};
</script>

<style scoped>
.floor-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.floor-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(16, 89, 198, 0.05);
}

.floor-page :deep(.el-button) {
  border-radius: 6px;
}

.floor-page .table-action-bar {
  gap: 10px;
}

.floor-search {
  flex: 1;
  min-width: 0;
}

.floor-search :deep(.el-form-item) {
  margin-bottom: 0;
}

.floor-search :deep(.el-select) {
  width: 220px;
}

.floor-search :deep(.el-input-number) {
  width: 130px;
}

.floor-table {
  width: 100%;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}

.room-panel {
  padding: 10px 18px;
  background: #f7f8fa;
}

.room-name-cell,
.drawer-room-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.room-name-text,
.drawer-room-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.room-name-text {
  flex: 1;
}

.floor-room-editor {
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.drawer-section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.drawer-room-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.drawer-room-item {
  min-height: 38px;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f8fafc;
}

.drawer-room-name {
  flex: 1;
  color: #303133;
  font-weight: 500;
}

.room-form :deep(.el-input),
.room-form :deep(.el-select),
.room-form :deep(.el-input-number) {
	width: 100%;
}

.room-tag-0 {
  --el-tag-bg-color: rgba(255, 240, 0, 0.18);
  --el-tag-border-color: #fff000;
  --el-tag-text-color: #8a6f00;
}

.room-tag-1 {
  --el-tag-bg-color: rgba(255, 77, 79, 0.12);
  --el-tag-border-color: #ff4d4f;
  --el-tag-text-color: #c62828;
}

.room-tag-2 {
  --el-tag-bg-color: rgba(0, 183, 232, 0.14);
  --el-tag-border-color: #00b7e8;
  --el-tag-text-color: #0277a8;
}

.room-tag-3 {
  --el-tag-bg-color: rgba(244, 180, 0, 0.16);
  --el-tag-border-color: #f4b400;
  --el-tag-text-color: #a15c00;
}

.room-tag-4 {
  --el-tag-bg-color: rgba(139, 92, 246, 0.12);
  --el-tag-border-color: #8b5cf6;
  --el-tag-text-color: #5b3bb8;
}

.room-tag-5 {
  --el-tag-bg-color: rgba(239, 125, 34, 0.14);
  --el-tag-border-color: #ef7d22;
  --el-tag-text-color: #b45309;
}

.room-tag-6 {
  --el-tag-bg-color: rgba(139, 30, 30, 0.12);
  --el-tag-border-color: #8b1e1e;
  --el-tag-text-color: #8b1e1e;
}

.room-tag-7 {
  --el-tag-bg-color: rgba(34, 197, 94, 0.14);
  --el-tag-border-color: #22c55e;
  --el-tag-text-color: #15803d;
}

.status-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.status-empty {
  color: #909399;
  font-size: 13px;
  line-height: 24px;
}

.page-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
}

:deep(.el-drawer__body) {
  padding-right: 24px;
}

:deep(.el-drawer__footer) {
  border-top: 1px solid #ebeef5;
}

@media (max-width: 960px) {
  .floor-toolbar {
    flex-direction: column;
  }

  .page-footer {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
