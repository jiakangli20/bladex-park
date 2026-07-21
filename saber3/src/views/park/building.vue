<template>
  <basic-container>
    <div class="building-page">
      <section class="summary-grid">
        <div v-for="item in buildingSummaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="building-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="建筑名称">
            <el-input
              v-model="query.name"
              clearable
              placeholder="请输入建筑名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="所属园区">
            <el-select v-model="query.parkId" clearable filterable placeholder="请选择所属园区">
              <el-option
                v-for="park in parkList"
                :key="park.id"
                :label="park.name"
                :value="park.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="building-toolbar">
        <div class="building-toolbar-left">
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
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="refreshChange" />
        </el-tooltip>
      </section>

      <el-table
        ref="buildingTable"
        v-loading="loading"
        :data="data"
        border
        fit
        row-key="id"
        class="building-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column prop="name" label="建筑名称" min-width="150" align="center" show-overflow-tooltip />
        <el-table-column label="所属园区" min-width="140" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ parkNameText(row) }}</template>
        </el-table-column>
        <el-table-column prop="code" label="建筑编号" min-width="120" align="center" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              class="building-status-switch"
              :model-value="row.status === '0' || row.status === 0"
              :loading="statusChangingMap[row.id]"
              :disabled="!permission.building_edit"
              :width="44"
              @change="checked => toggleStatus(row, checked)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="area" label="建筑面积(㎡)" min-width="130" align="center">
          <template #default="{ row }">{{ formatArea(row.area) }}</template>
        </el-table-column>
        <el-table-column prop="rentableArea" label="可租面积(㎡)" min-width="130" align="center">
          <template #default="{ row }">{{ formatArea(row.rentableArea) }}</template>
        </el-table-column>
        <el-table-column prop="managementArea" label="管理面积(㎡)" min-width="130" align="center">
          <template #default="{ row }">{{ formatArea(row.managementArea) }}</template>
        </el-table-column>
        <el-table-column prop="roomCount" label="房间总数" min-width="105" align="center">
          <template #default="{ row }">{{ row.roomCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="rentRate" label="出租率(%)" min-width="105" align="center">
          <template #default="{ row }">{{ formatRate(row.rentRate) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" align="center">
          <template #default="{ row }">
            <div class="building-table-actions">
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
        </el-table-column>
      </el-table>

      <div class="building-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-sizes="[10, 20, 30, 40, 50, 100]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>
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

        <section class="building-edit-section">
          <div class="section-title">楼宇图片</div>
          <div v-if="drawerReadonly" class="building-image-gallery">
            <el-image
              v-for="image in buildingImageUrls"
              :key="image"
              :src="image"
              :preview-src-list="buildingImageUrls"
              fit="cover"
              preview-teleported
            />
            <el-empty v-if="!buildingImageUrls.length" description="暂无楼宇图片" :image-size="64" />
          </div>
          <el-form-item v-else label="实景图片">
            <el-upload
              v-model:file-list="buildingImageFileList"
              action="/api/blade-resource/oss/endpoint/put-file-attach"
              :headers="uploadHeaders"
              list-type="picture-card"
              accept="image/*"
              multiple
              :limit="9"
              :before-upload="beforeBuildingImageUpload"
              :on-success="handleBuildingImageUploadSuccess"
              :on-preview="handleBuildingImagePreview"
              :on-remove="handleBuildingImageRemove"
              :on-error="handleBuildingImageUploadError"
              :on-exceed="handleBuildingImageExceed"
            >
              <el-button v-if="buildingImageFileList.length < 9" :icon="Upload" text>上传</el-button>
            </el-upload>
            <div class="building-form-tip">支持 jpg、png 等图片格式，最多上传 9 张，单张不超过 10MB。</div>
          </el-form-item>
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

    <el-dialog v-model="buildingImagePreviewVisible" title="楼宇图片预览" width="760px" append-to-body>
      <el-image :src="buildingImagePreviewUrl" fit="contain" class="building-image-preview" />
    </el-dialog>

  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Upload } from '@element-plus/icons-vue';
import { getList, getDetail, remove, submit } from '@/api/park/building';
import { getList as getParkList } from '@/api/park/park';
import { getToken } from '@/utils/auth';

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
  'sceneImages',
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
  sceneImages: '',
  memo: '',
});

export default {
  data() {
    return {
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      data: [],
      parkList: [],
      floorAreaList: [],
      drawerVisible: false,
      drawerMode: 'add',
      drawerLoading: false,
      saving: false,
      statusChangingMap: {},
      drawerForm: createDefaultBuildingForm(),
      buildingImageFileList: [],
      buildingImagePreviewVisible: false,
      buildingImagePreviewUrl: '',
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      Upload,
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
    buildingImageUrls() {
      return this.parseBuildingImageUrls(this.drawerForm.sceneImages);
    },
    buildingSummaryCards() {
      const total = this.data.length;
      const managementArea = this.sumBy(this.data, 'managementArea');
      const rentedArea = this.sumBy(this.data, 'rentedArea');
      const vacantArea = Math.max(managementArea - rentedArea, 0);
      return [
        {
          key: 'buildingCount',
          label: '建筑数量',
          value: `${total} 栋`,
        },
        {
          key: 'managementArea',
          label: '管理面积',
          value: `${this.formatArea(managementArea)} ㎡`,
        },
        {
          key: 'rentedArea',
          label: '在租面积',
          value: `${this.formatArea(rentedArea)} ㎡`,
        },
        {
          key: 'vacantArea',
          label: '待租面积',
          value: `${this.formatArea(vacantArea)} ㎡`,
        },
      ];
    },
  },
  created() {
    this.loadParkList();
    this.onLoad(this.page);
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
      this.buildingImageFileList = [];
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
      this.buildingImageFileList = this.parseBuildingImageFiles(this.drawerForm.sceneImages);
      this.floorAreaList = [];
      this.syncFloorAreaList(this.drawerForm.floors);
      getDetail(row.id)
        .then(res => {
          const detail = res.data.data || row;
          this.drawerForm = this.buildDrawerForm(detail);
          this.buildingImageFileList = this.parseBuildingImageFiles(this.drawerForm.sceneImages);
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
      this.buildingImageFileList = [];
      this.buildingImagePreviewVisible = false;
      this.buildingImagePreviewUrl = '';
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
      if (this.hasUploadingBuildingImages()) {
        ElMessage.warning('楼宇图片正在上传，请稍后保存');
        return;
      }
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
    beforeBuildingImageUpload(file) {
      const isImage = file.type && file.type.indexOf('image/') === 0;
      const isValidSize = file.size / 1024 / 1024 <= 10;
      if (!isImage) ElMessage.warning('请上传图片文件');
      if (!isValidSize) ElMessage.warning('单张图片不能超过 10MB');
      return isImage && isValidSize;
    },
    handleBuildingImageUploadSuccess(response, uploadFile, uploadFiles) {
      const success = response && (response.success || response.code === 200 || response.code === 0);
      const data = response && response.data;
      const url = typeof data === 'string' ? data : (data && (data.link || data.url)) || '';
      if (!success || !url) {
        this.buildingImageFileList = (uploadFiles || []).filter(file => file.uid !== uploadFile.uid);
        this.syncBuildingImageField();
        ElMessage.error((response && response.msg) || '上传失败');
        return;
      }
      uploadFile.url = url;
      uploadFile.name = (data && (data.originalName || data.name)) || uploadFile.name;
      uploadFile.status = 'success';
      this.buildingImageFileList = this.normalizeBuildingImageFiles(uploadFiles || []);
      this.syncBuildingImageField();
      ElMessage.success('上传成功');
    },
    handleBuildingImagePreview(file) {
      this.buildingImagePreviewUrl = file.url || '';
      this.buildingImagePreviewVisible = Boolean(this.buildingImagePreviewUrl);
    },
    handleBuildingImageRemove(file, uploadFiles) {
      this.buildingImageFileList = this.normalizeBuildingImageFiles(uploadFiles || []);
      this.syncBuildingImageField();
    },
    handleBuildingImageUploadError() {
      ElMessage.error('上传失败');
    },
    handleBuildingImageExceed() {
      ElMessage.warning('楼宇图片最多上传 9 张');
    },
    hasUploadingBuildingImages() {
      return this.buildingImageFileList.some(file => file.status && file.status !== 'success');
    },
    syncBuildingImageField() {
      this.drawerForm.sceneImages = this.buildingImageFileList.map(file => file.url).filter(Boolean).join(',');
    },
    normalizeBuildingImageFiles(files) {
      return (Array.isArray(files) ? files : [])
        .map((file, index) => {
          const responseData = file.response && file.response.data;
          const url = file.url || (typeof responseData === 'string' ? responseData : responseData && (responseData.link || responseData.url)) || '';
          return {
            uid: file.uid || `${Date.now()}-${index}`,
            name: file.name || (url ? url.split('/').pop() : `楼宇图片${index + 1}`),
            url,
            status: file.status || 'success',
          };
        })
        .filter(file => file.url || file.status !== 'success');
    },
    parseBuildingImageFiles(value) {
      return this.parseBuildingImageUrls(value).map((url, index) => ({
        uid: `${Date.now()}-${index}`,
        name: url.split('/').pop() || `楼宇图片${index + 1}`,
        url,
        status: 'success',
      }));
    },
    parseBuildingImageUrls(value) {
      if (!value) return [];
      if (Array.isArray(value)) {
        return value.map(item => (typeof item === 'string' ? item : item.url || item.link || '')).filter(Boolean);
      }
      const text = String(value).trim();
      if (!text) return [];
      if (text.startsWith('[')) {
        try {
          return this.parseBuildingImageUrls(JSON.parse(text));
        } catch (error) {
          return [];
        }
      }
      return text.split(',').map(item => item.trim()).filter(Boolean);
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
      getList(page.currentPage, page.pageSize, { ...this.query, ...params }).then(res => {
        const resData = res.data.data;
        this.page.total = resData.total;
        this.data = resData.records || [];
        this.loading = false;
        this.selectionClear();
      }).catch(() => {
        this.loading = false;
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
      });
    },
    toggleStatus(row, checked) {
      if (!row || !row.id || this.statusChangingMap[row.id]) return;
      const nextStatus = checked ? '0' : '1';
      this.statusChangingMap = {
        ...this.statusChangingMap,
        [row.id]: true,
      };
      submit({
        ...row,
        status: nextStatus,
      })
        .then(() => {
          row.status = nextStatus;
          ElMessage.success(nextStatus === '0' ? '已启用' : '已停用');
        })
        .finally(() => {
          const nextMap = { ...this.statusChangingMap };
          delete nextMap[row.id];
          this.statusChangingMap = nextMap;
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
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.buildingTable && this.$refs.buildingTable.clearSelection();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    parkNameText(row) {
      if (row.parkName) return row.parkName;
      const park = this.parkList.find(item => String(item.id) === String(row.parkId));
      return park ? park.name : '-';
    },
    sumBy(list, field) {
      return (list || []).reduce((total, item) => total + Number(item[field] || 0), 0);
    },
    formatArea(value) {
      const num = Number(value || 0);
      return num.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      });
    },
    formatRate(value) {
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
  width: 100%;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-card span {
  color: #606266;
  font-size: 13px;
}

.summary-card strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

.building-search,
.building-toolbar {
  border-radius: 10px;
}

.building-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.building-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.building-search :deep(.el-input),
.building-search :deep(.el-select) {
  width: 190px;
}

.building-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.building-toolbar-left {
  display: flex;
  gap: 10px;
}

.building-table {
  width: 100%;
  border-radius: 0;
}

.building-page :deep(.el-table th),
.building-page :deep(.el-table td),
.building-page :deep(.el-table .cell) {
  text-align: center;
}

.building-page :deep(.el-table .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.building-table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.building-table-actions .el-button + .el-button {
  margin-left: 0;
}

.building-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.building-status-switch {
  --el-switch-on-color: #19be4b;
  --el-switch-off-color: #dcdfe6;
}

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

.building-image-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, 136px);
  gap: 12px;
  min-height: 96px;
}

.building-image-gallery :deep(.el-image) {
  width: 136px;
  height: 96px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #f5f7fa;
}

.building-image-gallery :deep(.el-empty) {
  grid-column: 1 / -1;
  padding: 8px 0;
}

.building-form-tip {
  width: 100%;
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.building-image-preview {
  display: block;
  width: 100%;
  height: min(68vh, 620px);
}

.building-drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 20px;
}

.building-page :deep(.el-button),
.building-drawer-footer .el-button {
  border-radius: 6px;
}
</style>
