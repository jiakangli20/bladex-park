<template>
  <basic-container>
    <div class="floor-page">
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
          <el-form-item label="楼层状态">
            <el-select v-model="query.status" clearable placeholder="请选择" @change="handleSearch">
              <el-option v-for="item in floorStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="房间状态">
            <el-select v-model="query.roomStatus" clearable placeholder="请选择" @change="handleSearch">
              <el-option v-for="item in roomStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="floor-actions">
          <el-button v-if="permission.floor_add" type="primary" :icon="Plus" @click="openCreate">
            新增
          </el-button>
          <el-button v-if="permission.floor_delete" type="danger" plain :icon="Delete" @click="handleBatchDelete">
            删除
          </el-button>
          <el-button v-if="permission.floor_sync" plain :icon="Connection" @click="handleSyncBuilding">
            同步建筑
          </el-button>
          <el-button v-if="permission.floor_sync" plain :icon="RefreshRight" @click="handleSyncAll">
            同步全部
          </el-button>
          <el-button :icon="Refresh" @click="refreshData">刷新</el-button>
        </div>
      </section>

      <stat-cards :items="statCards" />

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
          <template #default="{ row }">
            <div class="room-panel">
              <el-table :data="row.rooms || []" size="small" border empty-text="暂无房间">
                <el-table-column prop="name" label="房间名称" min-width="140" />
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
                <el-table-column prop="status" label="状态" width="100" align="center">
                  <template #default="{ row: room }">
                    <el-tag :type="roomStatusType(room.status)">{{ roomStatusLabel(room.status) }}</el-tag>
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
        <el-table-column prop="parkName" label="所属园区" min-width="140" show-overflow-tooltip />
        <el-table-column prop="buildingName" label="所属建筑" min-width="140" show-overflow-tooltip />
        <el-table-column prop="floorNo" label="楼层" width="90" align="center">
          <template #default="{ row }">{{ row.floorNo }}F</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'info' : 'success'">{{ floorStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="area" label="楼层面积(㎡)" width="130" align="right">
          <template #default="{ row }">{{ formatNumber(row.area) }}</template>
        </el-table-column>
        <el-table-column prop="usedArea" label="已用面积(㎡)" width="130" align="right">
          <template #default="{ row }">{{ formatNumber(row.usedArea) }}</template>
        </el-table-column>
        <el-table-column prop="totalCount" label="房间数" width="95" align="center" />
        <el-table-column prop="occupancyRate" label="出租率" width="100" align="center">
          <template #default="{ row }">{{ formatNumber(row.occupancyRate) }}%</template>
        </el-table-column>
        <el-table-column label="房态统计" min-width="220">
          <template #default="{ row }">
            <div class="status-tags">
              <el-tag size="small" type="success">空 {{ row.vacantCount || 0 }}</el-tag>
              <el-tag size="small">租 {{ row.rentedCount || 0 }}</el-tag>
              <el-tag size="small" type="warning">订 {{ row.reservedCount || 0 }}</el-tag>
              <el-tag size="small" type="info">装 {{ row.renovatingCount || 0 }}</el-tag>
              <el-tag size="small" type="danger">停 {{ row.disabledCount || 0 }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="memo" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="permission.floor_view" link type="primary" :icon="View" @click="openView(row)">
              查看
            </el-button>
            <el-button v-if="permission.floor_edit" link type="primary" :icon="Edit" @click="openEdit(row)">
              编辑
            </el-button>
            <el-button v-if="permission.floor_delete" link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
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
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button v-if="!formReadonly" type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-drawer>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Connection, Delete, Edit, Plus, Refresh, RefreshRight, Search, View } from '@element-plus/icons-vue';
import { getSimpleList as getBuildingList } from '@/api/park/building';
import {
  getDetail,
  getList,
  getStatistics,
  remove,
  submit,
  syncAll,
  syncBuilding,
} from '@/api/park/floor';

export default {
  data() {
    return {
      Connection,
      Delete,
      Edit,
      Plus,
      Refresh,
      RefreshRight,
      Search,
      View,
      loading: false,
      saving: false,
      drawerVisible: false,
      formMode: 'add',
      data: [],
      selectionList: [],
      buildingOptions: [],
      statistics: {},
      query: {
        buildingId: undefined,
        floorNo: undefined,
        status: '',
        roomStatus: '',
      },
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      form: this.emptyForm(),
      formRules: {
        buildingId: [{ required: true, message: '请选择所属建筑', trigger: 'change' }],
        floorNo: [{ required: true, message: '请输入楼层号', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
      },
      floorStatusOptions: [
        { label: '启用', value: '0' },
        { label: '停用', value: '1' },
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
    formBuildingFloors() {
      const building = this.findBuilding(this.form.buildingId);
      return building && building.floors ? Number(building.floors) : null;
    },
    statCards() {
      return [
        {
          label: '楼层总数',
          value: this.statistics.floorCount || 0,
          desc: `房间 ${this.statistics.totalCount || 0} 间`,
        },
        {
          label: '楼层面积',
          value: `${this.formatNumber(this.statistics.totalArea)}㎡`,
          desc: `已用 ${this.formatNumber(this.statistics.usedArea)}㎡`,
        },
        {
          label: '已出租房间',
          value: this.statistics.rentedCount || 0,
          desc: `空置 ${this.statistics.vacantCount || 0} 间`,
        },
        {
          label: '出租率',
          value: `${this.formatNumber(this.statistics.occupancyRate)}%`,
          desc: `预定 ${this.statistics.reservedCount || 0} 间`,
        },
        {
          label: '不可用房间',
          value: Number(this.statistics.renovatingCount || 0) + Number(this.statistics.disabledCount || 0),
          desc: `装修 ${this.statistics.renovatingCount || 0} / 停用 ${this.statistics.disabledCount || 0}`,
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
        status: '',
        roomStatus: '',
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
      getDetail(id, { roomStatus: this.query.roomStatus }).then(res => {
        this.form = Object.assign(this.emptyForm(), res.data.data || {});
        this.drawerVisible = true;
      });
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
    handleSyncBuilding() {
      if (!this.query.buildingId) {
        ElMessage.warning('请先选择建筑');
        return;
      }
      syncBuilding(this.query.buildingId).then(() => {
        ElMessage.success('同步成功');
        this.refreshData();
      });
    },
    handleSyncAll() {
      syncAll().then(() => {
        ElMessage.success('同步成功');
        this.refreshData();
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
      return option ? option.label : '空置中';
    },
    roomStatusType(status) {
      const typeMap = {
        0: 'success',
        1: 'primary',
        2: 'warning',
        3: 'info',
        4: 'danger',
      };
      return typeMap[String(status)] || 'success';
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
}

.floor-search {
  flex: 1;
  min-width: 0;
}

.floor-search :deep(.el-select) {
  width: 220px;
}

.floor-search :deep(.el-input-number) {
  width: 130px;
}

.floor-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.floor-table {
  width: 100%;
}

.room-panel {
  padding: 10px 18px;
  background: #f7f8fa;
}

.status-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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

  .floor-actions {
    justify-content: flex-start;
  }

  .page-footer {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
