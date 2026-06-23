<template>
  <basic-container>
    <div class="building-page">
      <avue-crud
        ref="crud"
        v-model="form"
        v-model:page="page"
        :option="option"
        :data="data"
        :table-loading="loading"
        :permission="permissionList"
        @row-del="rowDel"
        @search-change="searchChange"
        @search-reset="searchReset"
        @selection-change="selectionChange"
        @current-change="currentChange"
        @size-change="sizeChange"
        @refresh-change="refreshChange"
        @on-load="onLoad"
      >
        <template #menu-left>
          <el-button
            v-if="permission.building_add"
            type="primary"
            icon="el-icon-plus"
            @click="openCreateDrawer"
          >
            新增楼宇
          </el-button>
          <el-button
            v-if="permission.building_delete"
            type="danger"
            icon="el-icon-delete"
            plain
            :disabled="selectionList.length === 0"
            @click="handleDelete"
          >
            批量删除
          </el-button>
        </template>

        <template #menu="{ row }">
          <div class="building-row-actions">
            <el-button
              v-if="permission.building_view"
              text
              type="primary"
              icon="el-icon-view"
              @click="openViewDrawer(row)"
            >
              查看
            </el-button>
            <el-button
              v-if="permission.building_edit"
              text
              type="primary"
              icon="el-icon-edit"
              @click="openEditDrawer(row)"
            >
              编辑
            </el-button>
          </div>
        </template>

        <template #status="{ row }">
          <el-tag :type="row.status === '0' || row.status === 0 ? 'success' : 'info'">
            {{ row.status === '0' || row.status === 0 ? '启用' : '停用' }}
          </el-tag>
        </template>

        <template #buildingTag="{ row }">
          {{ buildBuildingTag(row) }}
        </template>
      </avue-crud>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      size="1120px"
      direction="rtl"
      append-to-body
      :destroy-on-close="false"
      class="building-edit-drawer"
      @closed="resetDrawerForm"
    >
      <el-form
        ref="buildingDrawerFormRef"
        v-loading="drawerLoading"
        :model="drawerForm"
        :rules="drawerRules"
        :disabled="drawerReadonly"
        label-position="top"
        class="building-modal-form"
      >
        <section class="building-edit-section">
          <div class="section-title">基础信息</div>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="所属园区" prop="parkId">
                <el-select v-model="drawerForm.parkId" filterable placeholder="请选择所属园区">
                  <el-option
                    v-for="park in parkList"
                    :key="park.id"
                    :label="park.name"
                    :value="park.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="建筑编码" prop="code">
                <el-input v-model="drawerForm.code" maxlength="50" placeholder="请输入建筑编码" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="建筑名称" prop="name">
                <el-input v-model="drawerForm.name" maxlength="100" placeholder="请输入建筑名称" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="16" :md="16">
              <el-form-item label="所属地区" prop="region">
                <el-input v-model="drawerForm.region" maxlength="120" placeholder="请输入所属地区，如：北京 / 北京市 / 东城区" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8" :md="8">
              <el-form-item label="建筑地址" prop="address">
                <el-input v-model="drawerForm.address" maxlength="200" placeholder="请输入建筑地址" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="产权性质" prop="buildingType">
                <el-select v-model="drawerForm.buildingType" placeholder="请选择产权性质">
                  <el-option
                    v-for="item in propertyNatureOptions"
                    :key="item"
                    :label="item"
                    :value="item"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="不动产编号" prop="realEstateNo">
                <el-input v-model="drawerForm.realEstateNo" maxlength="80" placeholder="请输入..." />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="产权编号" prop="propertyNo">
                <el-input v-model="drawerForm.propertyNo" maxlength="80" placeholder="请输入..." />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="土地编号" prop="landNo">
                <el-input v-model="drawerForm.landNo" maxlength="80" placeholder="请输入..." />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="排序值" prop="sortNum">
                <el-input-number
                  v-model="drawerForm.sortNum"
                  :min="1"
                  :precision="0"
                  controls-position="right"
                  placeholder="请输入排序值"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="楼层数" prop="floors">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.floors"
                    :min="1"
                    :precision="0"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入楼层数"
                    @change="handleFloorsChange"
                  />
                  <span class="unit-input-suffix">层</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="状态" prop="status">
                <el-select v-model="drawerForm.status" placeholder="请选择状态">
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

        <section class="building-edit-section">
          <div class="section-title">建筑面积</div>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="建筑面积" prop="area">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.area"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入建筑面积"
                    @change="handleAreaChange"
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="产权面积" prop="propertyArea">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.propertyArea"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入产权面积"
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="可租面积" prop="rentableArea">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.rentableArea"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入..."
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="自用面积" prop="selfUseArea">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.selfUseArea"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入..."
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="配套面积" prop="supportingArea">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.supportingArea"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入..."
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="车位面积" prop="parkingArea">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.parkingArea"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入..."
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :xs="24" :sm="12" :md="8">
              <el-form-item label="标准层高" prop="standardFloorHeight">
                <div class="unit-input">
                  <el-input-number
                    v-model="drawerForm.standardFloorHeight"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入..."
                  />
                  <span class="unit-input-suffix">m</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="16">
            <el-col :span="24">
              <el-form-item label="备注" prop="memo">
                <el-input v-model="drawerForm.memo" type="textarea" :rows="3" maxlength="500" placeholder="请输入备注" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="building-edit-section">
          <div class="section-title">楼层面积配置</div>
          <div class="floor-area-summary">
            <span>楼层数：{{ floorAreaList.length }} 层</span>
            <span>楼层面积合计：{{ formatArea(totalFloorArea) }}㎡</span>
            <span>建筑面积：{{ formatArea(drawerForm.area) }}㎡</span>
          </div>
          <el-row :gutter="16">
            <el-col v-for="item in floorAreaList" :key="item.floorNo" :xs="24" :sm="12" :md="6">
              <el-form-item :label="item.floorNo + 'F 楼层面积'">
                <div class="unit-input">
                  <el-input-number
                    v-model="item.area"
                    :min="0"
                    :precision="2"
                    :controls="false"
                    controls-position="right"
                    placeholder="请输入楼层面积"
                  />
                  <span class="unit-input-suffix">㎡</span>
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </section>
      </el-form>
      <template #footer>
        <div class="building-drawer-footer">
          <el-button @click="drawerVisible = false">{{ drawerReadonly ? '关闭' : '取消' }}</el-button>
          <el-button v-if="drawerReadonly && permission.building_edit" type="primary" @click="switchToEditMode">
            编辑
          </el-button>
          <el-button v-else-if="!drawerReadonly" type="primary" :loading="saving" @click="submitDrawerForm">
            保存
          </el-button>
        </div>
      </template>
    </el-drawer>

  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getList, getDetail, remove, submit } from '@/api/park/building';
import { getList as getParkList } from '@/api/park/park';
import { tableOption } from '@/option/park/building';

const statusOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' },
];

const propertyNatureOptions = ['自持', '租赁', '合作'];

const buildingFormFields = [
  'id',
  'parkId',
  'code',
  'name',
  'region',
  'address',
  'buildingType',
  'realEstateNo',
  'propertyNo',
  'landNo',
  'sortNum',
  'floors',
  'status',
  'area',
  'propertyArea',
  'rentableArea',
  'selfUseArea',
  'supportingArea',
  'parkingArea',
  'standardFloorHeight',
  'memo',
];

const createDefaultBuildingForm = () => ({
  id: '',
  parkId: '',
  code: '',
  name: '',
  region: '',
  address: '',
  buildingType: '自持',
  realEstateNo: '',
  propertyNo: '',
  landNo: '',
  sortNum: 1,
  floors: 1,
  status: '0',
  area: null,
  propertyArea: null,
  rentableArea: null,
  selfUseArea: null,
  supportingArea: null,
  parkingArea: null,
  standardFloorHeight: null,
  memo: '',
});

export default {
  data() {
    return {
      form: {},
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      option: tableOption,
      data: [],
      parkList: [],
      floorAreaList: [],
      drawerVisible: false,
      drawerMode: 'add',
      drawerLoading: false,
      saving: false,
      drawerForm: createDefaultBuildingForm(),
      drawerRules: {
        parkId: [{ required: true, message: '请选择所属园区', trigger: 'change' }],
        code: [{ required: true, message: '请输入建筑编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入建筑名称', trigger: 'blur' }],
        region: [{ required: true, message: '请输入所属地区', trigger: 'blur' }],
        buildingType: [{ required: true, message: '请选择产权性质', trigger: 'change' }],
        sortNum: [{ required: true, message: '请输入排序值', trigger: 'change' }],
        floors: [{ required: true, message: '请输入楼层数', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        area: [{ required: true, message: '请输入建筑面积', trigger: 'change' }],
        propertyArea: [{ required: true, message: '请输入产权面积', trigger: 'change' }],
      },
      statusOptions,
      propertyNatureOptions,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: false,
        viewBtn: false,
        delBtn: false,
        editBtn: false,
      };
    },
    drawerTitle() {
      if (this.drawerMode === 'view') return '查看建筑';
      return this.drawerForm.id ? '编辑建筑' : '新增建筑';
    },
    drawerReadonly() {
      return this.drawerMode === 'view';
    },
    ids() {
      return this.selectionList.map(item => item.id).join(',');
    },
    totalFloorArea() {
      return this.floorAreaList.reduce((total, item) => total + Number(item.area || 0), 0);
    },
  },
  created() {
    this.loadParkList();
  },
  watch: {
    'drawerForm.floors'(value) {
      this.syncFloorAreaList(value);
    },
    'drawerForm.area'() {
      if (!this.floorAreaList.length) {
        this.syncFloorAreaList(this.drawerForm.floors);
      }
    },
  },
  methods: {
    loadParkList() {
      getParkList(1, 999, {}).then(res => {
        const records = (res.data.data && res.data.data.records) || [];
        this.parkList = records;
        const parkColumn = this.option.column.find(item => item.prop === 'parkId');
        if (parkColumn) {
          parkColumn.dicData = records;
        }
      });
    },
    buildDrawerForm(data = {}) {
      const form = createDefaultBuildingForm();
      buildingFormFields.forEach(field => {
        if (data[field] !== undefined && data[field] !== null) {
          form[field] = data[field];
        }
      });
      form.status = String(form.status === '' || form.status === null ? '0' : form.status);
      return form;
    },
    openCreateDrawer() {
      this.drawerMode = 'add';
      this.drawerForm = createDefaultBuildingForm();
      this.floorAreaList = [];
      this.syncFloorAreaList(this.drawerForm.floors);
      this.drawerVisible = true;
      this.$nextTick(() => {
        this.$refs.buildingDrawerFormRef && this.$refs.buildingDrawerFormRef.clearValidate();
      });
    },
    openViewDrawer(row) {
      this.openDetailDrawer(row, 'view');
    },
    openEditDrawer(row) {
      this.openDetailDrawer(row, 'edit');
    },
    openDetailDrawer(row, mode) {
      this.drawerMode = mode;
      this.drawerVisible = true;
      this.drawerLoading = true;
      this.drawerForm = this.buildDrawerForm(row);
      this.floorAreaList = [];
      this.syncFloorAreaList(this.drawerForm.floors);
      getDetail(row.id)
        .then(res => {
          const detail = res.data.data || row;
          this.drawerForm = this.buildDrawerForm(detail);
          this.syncFloorAreaList(this.drawerForm.floors);
          this.applyFloorAreas(detail.floorAreas || []);
          this.$nextTick(() => {
            this.$refs.buildingDrawerFormRef && this.$refs.buildingDrawerFormRef.clearValidate();
          });
        })
        .finally(() => {
          this.drawerLoading = false;
        });
    },
    resetDrawerForm() {
      this.drawerMode = 'add';
      this.drawerLoading = false;
      this.saving = false;
      this.drawerForm = createDefaultBuildingForm();
      this.floorAreaList = [];
      this.$nextTick(() => {
        this.$refs.buildingDrawerFormRef && this.$refs.buildingDrawerFormRef.clearValidate();
      });
    },
    switchToEditMode() {
      this.drawerMode = 'edit';
      this.$nextTick(() => {
        this.$refs.buildingDrawerFormRef && this.$refs.buildingDrawerFormRef.clearValidate();
      });
    },
    submitDrawerForm() {
      this.$refs.buildingDrawerFormRef.validate(valid => {
        if (!valid) return;
        this.saving = true;
        submit(this.normalizePayload(this.drawerForm))
          .then(() => {
            ElMessage.success('保存成功!');
            this.drawerVisible = false;
            this.onLoad(this.page, this.query);
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    handleFloorsChange(value) {
      this.syncFloorAreaList(value);
    },
    handleAreaChange() {
      if (!this.floorAreaList.length) {
        this.syncFloorAreaList(this.drawerForm.floors);
      }
    },
    applyFloorAreas(floorAreas) {
      const floorMap = {};
      (floorAreas || []).forEach(item => {
        floorMap[item.floorNo] = item.area;
      });
      this.floorAreaList = this.floorAreaList.map(item => ({
        floorNo: item.floorNo,
        area: floorMap[item.floorNo] !== undefined ? floorMap[item.floorNo] : item.area,
      }));
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
        const resData = res.data.data;
        this.page.total = resData.total;
        this.data = resData.records || [];
        this.loading = false;
        this.selectionClear();
      }).catch(() => {
        this.loading = false;
      });
    },
    rowDel(row) {
      ElMessageBox.confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => remove(row.id)).then(() => {
        this.onLoad(this.page);
        ElMessage.success('操作成功!');
      });
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        ElMessage.warning('请选择至少一条数据');
        return;
      }
      ElMessageBox.confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => remove(this.ids)).then(() => {
        this.onLoad(this.page);
        ElMessage.success('操作成功!');
        this.$refs.crud && this.$refs.crud.toggleSelection();
      });
    },
    normalizePayload(row) {
      return Object.assign({}, row, {
        floorAreas: this.floorAreaList.map(item => ({
          floorNo: item.floorNo,
          area: item.area,
        })),
      });
    },
    syncFloorAreaList(floors) {
      const floorCount = Number(floors || 0);
      if (!floorCount || floorCount < 1) {
        this.floorAreaList = [];
        return;
      }
      const areaMap = {};
      this.floorAreaList.forEach(item => {
        areaMap[item.floorNo] = item.area;
      });
      const defaultArea = this.defaultFloorArea(floorCount);
      this.floorAreaList = Array.from({ length: floorCount }, (_, index) => {
        const floorNo = index + 1;
        return {
          floorNo,
          area: areaMap[floorNo] !== undefined ? areaMap[floorNo] : defaultArea,
        };
      });
    },
    defaultFloorArea(floorCount) {
      const area = Number(this.drawerForm.area || 0);
      if (!area || !floorCount) {
        return undefined;
      }
      return Number((area / floorCount).toFixed(2));
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
    },
    searchReset() {
      this.query = {};
      this.onLoad(this.page);
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.crud && this.$refs.crud.toggleSelection();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    buildBuildingTag(row) {
      return [row.buildingType, row.memo].filter(Boolean).join(' / ') || '-';
    },
    formatArea(value) {
      const num = Number(value || 0);
      return num.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      });
    },
  },
};
</script>

<style scoped>
.building-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.building-page :deep(.avue-crud__search) {
  border-radius: 10px;
  overflow: hidden;
}

.building-page :deep(.avue-crud__search .el-col) {
  width: auto;
  max-width: none;
  flex: 0 0 auto;
}

.building-page :deep(.avue-crud__search .avue-form__menu--left) {
  margin-left: 28px;
}

.building-page :deep(.avue-crud__search .el-form-item) {
  margin-bottom: 0;
}

.building-page :deep(.avue-crud__search .el-input),
.building-page :deep(.avue-crud__search .el-select) {
  width: 220px;
}

.building-page :deep(.avue-crud__search .el-button + .el-button) {
  margin-left: 10px;
}

.building-page :deep(.avue-crud .el-button) {
  border-radius: 6px;
}

.building-page :deep(.avue-crud__menu) {
  overflow: visible;
  padding: 2px 0 0;
  background: transparent;
  border-radius: 0;
}

.building-page :deep(.avue-crud__left) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.building-row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.building-row-actions .el-button + .el-button {
  margin-left: 0;
}

.building-page :deep(.avue-crud__table),
.building-page :deep(.avue-crud__table .el-card),
.building-page :deep(.avue-crud__table .el-card__body),
.building-page :deep(.el-table),
.building-page :deep(.el-table__inner-wrapper),
.building-page :deep(.el-table__header-wrapper),
.building-page :deep(.el-table__body-wrapper),
.building-page :deep(.el-table__border-left-patch) {
  border-radius: 0;
}

:global(.building-edit-drawer .el-drawer__body) {
  padding: 16px 24px 88px;
}

.building-modal-form .building-edit-section {
  margin-bottom: 8px;
}

.building-modal-form .section-title {
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #1890ff;
  color: #1f2329;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
}

.building-modal-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.building-modal-form :deep(.el-row) {
  align-items: flex-start;
}

.building-modal-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  padding-bottom: 6px;
  color: #303133;
  min-height: 28px;
  line-height: 22px;
  text-align: left;
}

.building-modal-form :deep(.el-form-item__content) {
  align-items: flex-start;
  width: 100%;
}

.building-modal-form :deep(.el-input),
.building-modal-form :deep(.el-select),
.building-modal-form :deep(.el-input-number) {
  width: 100%;
}

.building-modal-form :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.unit-input {
  position: relative;
  width: 100%;
}

.unit-input :deep(.el-input-number .el-input__inner) {
  padding-right: 58px;
}

.unit-input :deep(.el-input-number .el-input__wrapper) {
  padding-right: 0;
}

.unit-input-suffix {
  position: absolute;
  top: 0;
  right: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 100%;
  min-height: 32px;
  padding: 0;
  border-left: 1px solid #d9d9d9;
  border-radius: 0 4px 4px 0;
  background: #fafafa;
  color: #8c8c8c;
  font-size: 13px;
  pointer-events: none;
}

.floor-area-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 12px;
  color: #606266;
  font-size: 13px;
}

.building-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 20px;
}

.building-page :deep(.avue-crud__menu .el-button),
.building-drawer-footer .el-button {
  border-radius: 6px;
}
</style>
