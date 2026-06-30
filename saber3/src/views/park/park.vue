<template>
  <basic-container>
    <template v-if="!editPageVisible">
      <div class="park-page">
        <section class="summary-grid">
          <div v-for="item in summaryCards" :key="item.key" class="summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="park-search">
          <el-form :inline="true" :model="query">
            <el-form-item label="园区名称">
              <el-input
                v-model="query.name"
                clearable
                placeholder="请输入园区名称"
                @keyup.enter="searchChange"
              />
            </el-form-item>
            <el-form-item label="园区编号">
              <el-input
                v-model="query.code"
                clearable
                placeholder="请输入园区编号"
                @keyup.enter="searchChange"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="请选择">
                <el-option label="启用" value="0" />
                <el-option label="停用" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
              <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
            </el-form-item>
          </el-form>
        </section>

        <section class="park-toolbar">
          <div class="park-toolbar-left">
            <el-button
              v-if="permission.park_add"
              type="primary"
              icon="el-icon-plus"
              @click="openCreatePage"
            >
              新增园区
            </el-button>
            <el-button
              v-if="permission.park_delete"
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
          ref="parkTable"
          v-loading="loading"
          :data="data"
          border
          fit
          row-key="id"
          class="park-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="44" align="center" />
          <el-table-column prop="name" label="园区名称" min-width="150" align="center" show-overflow-tooltip />
          <el-table-column prop="code" label="园区编号" min-width="120" align="center" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90" align="center">
            <template #default="{ row }">
              <el-switch
                class="park-status-switch"
                :model-value="row.status === '0' || row.status === 0"
                :loading="statusChangingMap[row.id]"
                :disabled="!permission.park_edit"
                :width="44"
                @change="checked => toggleStatus(row, checked)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="managementArea" label="管理面积(㎡)" min-width="130" align="center">
            <template #default="{ row }">{{ formatArea(row.managementArea) }}</template>
          </el-table-column>
          <el-table-column prop="rentableArea" label="可招商面积(㎡)" min-width="145" align="center">
            <template #default="{ row }">{{ formatArea(row.rentableArea) }}</template>
          </el-table-column>
          <el-table-column prop="totalRoomCount" label="总房源数" min-width="105" align="center">
            <template #default="{ row }">{{ row.totalRoomCount || 0 }}</template>
          </el-table-column>
          <el-table-column prop="rentableRoomCount" label="可招商房源" min-width="120" align="center">
            <template #default="{ row }">{{ row.rentableRoomCount || 0 }}</template>
          </el-table-column>
          <el-table-column prop="contactPhone" label="联系电话" min-width="112" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.contactPhone || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="150" align="center">
            <template #default="{ row }">
              <div class="park-table-actions">
                <el-button
                  v-if="permission.park_view"
                  text
                  type="primary"
                  icon="el-icon-view"
                  @click="openViewPage(row)"
                >
                  查看
                </el-button>
                <el-button
                  v-if="permission.park_edit"
                  text
                  type="primary"
                  icon="el-icon-edit"
                  @click="openEditPage(row)"
                >
                  编辑
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="park-pagination">
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
    </template>

    <section v-else class="park-edit-page">
      <div class="park-edit-header">
        <div>
          <h3>{{ editPageTitle }}</h3>
          <span>{{ editMode === 'add' ? '新建园区主档' : editForm.name || '-' }}</span>
        </div>
        <div class="park-edit-actions">
          <el-button @click="closeEditPage">返回</el-button>
          <el-button
            v-if="isViewMode && permission.park_edit"
            type="primary"
            icon="el-icon-edit"
            @click="switchToEditMode"
          >
            编辑
          </el-button>
          <el-button v-else-if="!isViewMode" type="primary" :loading="saving" @click="handleSubmitEdit">
            保存
          </el-button>
        </div>
      </div>

      <el-form
        ref="parkEditFormRef"
        v-loading="editLoading"
        :model="editForm"
        :rules="editRules"
        :disabled="isViewMode"
        label-width="128px"
        label-position="left"
        class="park-edit-form"
      >
        <section class="edit-section">
          <header class="section-head">
            <span>基础信息</span>
            <em>用于园区主档信息展示</em>
          </header>
          <div class="section-body">
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="园区编号" prop="code">
                  <el-input v-model="editForm.code" maxlength="50" placeholder="请输入园区编号" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="园区名称" prop="name">
                  <el-input v-model="editForm.name" maxlength="100" placeholder="请输入园区名称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="园区总面积" prop="area">
                  <el-input-number
                    v-model="editForm.area"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="请输入园区总面积"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="园区总房间数" prop="totalRooms">
                  <el-input-number
                    v-model="editForm.totalRooms"
                    :min="0"
                    :precision="0"
                    controls-position="right"
                    placeholder="请输入总房间数"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="停车费(元/小时)" prop="parkingFee">
                  <el-input-number
                    v-model="editForm.parkingFee"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="请输入停车费"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="状态" prop="status">
                  <el-select v-model="editForm.status" placeholder="请选择状态">
                    <el-option label="启用" value="0" />
                    <el-option label="停用" value="1" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </section>

        <section class="edit-section">
          <header class="section-head">
            <span>区位与招商信息</span>
            <em>用于地图定位与招商租金展示</em>
          </header>
          <div class="section-body">
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="纬度" prop="latitude">
                  <el-input-number
                    v-model="editForm.latitude"
                    :min="-90"
                    :max="90"
                    :precision="6"
                    controls-position="right"
                    placeholder="请输入纬度"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="经度" prop="longitude">
                  <el-input-number
                    v-model="editForm.longitude"
                    :min="-180"
                    :max="180"
                    :precision="6"
                    controls-position="right"
                    placeholder="请输入经度"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="租金范围(起)" prop="rentMin">
                  <el-input-number
                    v-model="editForm.rentMin"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="起始租金"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="租金范围(止)" prop="rentMax">
                  <el-input-number
                    v-model="editForm.rentMax"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    placeholder="结束租金"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="租金单位" prop="rentUnit">
                  <el-select v-model="editForm.rentUnit" placeholder="请选择单位">
                    <el-option label="元/㎡/天" value="元/㎡/天" />
                    <el-option label="元/㎡/月" value="元/㎡/月" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="地区" prop="region">
                  <el-input v-model="editForm.region" maxlength="100" placeholder="请输入所属地区" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="详细地址" prop="detailAddress">
              <el-input v-model="editForm.detailAddress" maxlength="300" placeholder="请输入详细地址" />
            </el-form-item>
          </div>
        </section>

        <section class="edit-section">
          <header class="section-head">
            <span>展示与运营内容</span>
            <em>用于前台展示业务介绍</em>
          </header>
          <div class="section-body">
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="园区小图" prop="thumbnailUrl">
                  <el-upload
                    action="/api/blade-resource/oss/endpoint/put-file-attach"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :disabled="isViewMode"
                    accept="image/*"
                    :on-success="response => handleImageUploadSuccess('thumbnailUrl', response)"
                    :on-error="handleImageUploadError"
                    :before-upload="beforeImageUpload"
                  >
                    <el-button v-if="!isViewMode" icon="el-icon-upload">上传图片</el-button>
                  </el-upload>
                  <div v-if="editForm.thumbnailUrl" class="preview-box">
                    <img :src="editForm.thumbnailUrl" alt="园区小图" />
                    <el-button v-if="!isViewMode" text type="danger" @click="removeEditImage('thumbnailUrl')">删除图片</el-button>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="园区Banner" prop="bannerUrl">
                  <el-upload
                    action="/api/blade-resource/oss/endpoint/put-file-attach"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :disabled="isViewMode"
                    accept="image/*"
                    :on-success="response => handleImageUploadSuccess('bannerUrl', response)"
                    :on-error="handleImageUploadError"
                    :before-upload="beforeImageUpload"
                  >
                    <el-button v-if="!isViewMode" icon="el-icon-upload">上传图片</el-button>
                  </el-upload>
                  <div v-if="editForm.bannerUrl" class="preview-box preview-box--banner">
                    <img :src="editForm.bannerUrl" alt="园区Banner" />
                    <el-button v-if="!isViewMode" text type="danger" @click="removeEditImage('bannerUrl')">删除图片</el-button>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="园区简介" prop="summary">
              <el-input
                v-model="editForm.summary"
                type="textarea"
                :rows="3"
                maxlength="1000"
                placeholder="请输入园区简介"
              />
            </el-form-item>
            <el-form-item label="配套服务" prop="supportingServices">
              <el-input
                v-model="editForm.supportingServices"
                type="textarea"
                :rows="3"
                maxlength="1000"
                placeholder="请输入配套服务"
              />
            </el-form-item>
            <el-form-item label="交通信息" prop="trafficInfo">
              <el-input
                v-model="editForm.trafficInfo"
                type="textarea"
                :rows="3"
                maxlength="1000"
                placeholder="请输入交通信息"
              />
            </el-form-item>
            <el-form-item label="园区图文介绍" prop="content">
              <avue-ueditor
                v-model="editForm.content"
                action="/blade-resource/oss/endpoint/put-file"
                :props-http="uploadProps"
                :disabled="isViewMode"
              />
            </el-form-item>
          </div>
        </section>

        <section class="edit-section">
          <header class="section-head">
            <span>联系方式</span>
            <em>便于对接与通知</em>
          </header>
          <div class="section-body">
            <el-row :gutter="18">
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="联系人" prop="contactName">
                  <el-input v-model="editForm.contactName" maxlength="50" placeholder="请输入联系人" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="24" :md="12">
                <el-form-item label="联系电话" prop="contactPhone">
                  <el-input v-model="editForm.contactPhone" maxlength="30" placeholder="请输入联系电话" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注" prop="memo">
              <el-input
                v-model="editForm.memo"
                type="textarea"
                :rows="3"
                maxlength="500"
                placeholder="备注"
              />
            </el-form-item>
          </div>
        </section>
      </el-form>
    </section>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getToken } from '@/utils/auth';
import { add, getDetail, getList, getStatistics, remove, update } from '@/api/park/park';

const parkFormFields = [
  'id',
  'code',
  'name',
  'area',
  'totalRooms',
  'parkingFee',
  'latitude',
  'longitude',
  'rentMin',
  'rentMax',
  'rentUnit',
  'region',
  'detailAddress',
  'thumbnailUrl',
  'bannerUrl',
  'summary',
  'supportingServices',
  'trafficInfo',
  'content',
  'contactName',
  'contactPhone',
  'status',
  'memo',
];

const createDefaultParkForm = () => ({
  id: '',
  code: '',
  name: '',
  area: null,
  totalRooms: null,
  parkingFee: null,
  latitude: null,
  longitude: null,
  rentMin: null,
  rentMax: null,
  rentUnit: '元/㎡/天',
  region: '',
  detailAddress: '',
  thumbnailUrl: '',
  bannerUrl: '',
  summary: '',
  supportingServices: '',
  trafficInfo: '',
  content: '',
  contactName: '',
  contactPhone: '',
  status: '0',
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
      statistics: {
        managementArea: 0,
        rentableArea: 0,
        totalRoomCount: 0,
        rentableRoomCount: 0,
      },
      data: [],
      editPageVisible: false,
      editMode: 'add',
      editLoading: false,
      saving: false,
      statusChangingMap: {},
      editForm: createDefaultParkForm(),
      editRules: {
        name: [{ required: true, message: '请输入园区名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
      },
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
      },
      uploadProps: {
        res: 'data',
        url: 'link',
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    editPageTitle() {
      if (this.editMode === 'view') return '查看园区信息';
      return this.editMode === 'edit' ? '编辑园区信息' : '新建园区信息';
    },
    isViewMode() {
      return this.editMode === 'view';
    },
    summaryCards() {
      return [
        {
          key: 'managementArea',
          label: '管理面积',
          value: `${this.formatArea(this.statistics.managementArea)} ㎡`,
        },
        {
          key: 'rentableArea',
          label: '可招商面积',
          value: `${this.formatArea(this.statistics.rentableArea)} ㎡`,
        },
        {
          key: 'totalRoomCount',
          label: '总房源数',
          value: `${this.statistics.totalRoomCount || 0} 间`,
        },
        {
          key: 'rentableRoomCount',
          label: '可招商房源',
          value: `${this.statistics.rentableRoomCount || 0} 间`,
        },
      ];
    },
    ids() {
      const ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  created() {
    this.loadStatistics();
    this.onLoad(this.page);
  },
  methods: {
    formatArea(value) {
      const num = Number(value || 0);
      return num.toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      });
    },
    loadStatistics() {
      getStatistics().then(res => {
        const data = res.data.data || {};
        this.statistics = {
          managementArea: data.managementArea || data.management_area || 0,
          rentableArea: data.rentableArea || data.rentable_area || 0,
          totalRoomCount: data.totalRoomCount || data.total_room_count || 0,
          rentableRoomCount: data.rentableRoomCount || data.rentable_room_count || 0,
        };
      });
    },
    refreshStatistics() {
      this.loadStatistics();
    },
    buildEditForm(data = {}) {
      const form = createDefaultParkForm();
      parkFormFields.forEach(field => {
        if (data[field] !== undefined && data[field] !== null) {
          form[field] = data[field];
        }
      });
      form.status = String(form.status === '' || form.status === null ? '0' : form.status);
      return form;
    },
    openCreatePage() {
      this.editMode = 'add';
      this.editForm = createDefaultParkForm();
      this.editPageVisible = true;
      this.$nextTick(() => {
        this.$refs.parkEditFormRef && this.$refs.parkEditFormRef.clearValidate();
      });
    },
    openViewPage(row) {
      this.openDetailPage(row, 'view');
    },
    openEditPage(row) {
      this.openDetailPage(row, 'edit');
    },
    openDetailPage(row, mode) {
      this.editMode = mode;
      this.editPageVisible = true;
      this.editLoading = true;
      this.editForm = this.buildEditForm(row);
      getDetail(row.id)
        .then(res => {
          this.editForm = this.buildEditForm(res.data.data || row);
          this.$nextTick(() => {
            this.$refs.parkEditFormRef && this.$refs.parkEditFormRef.clearValidate();
          });
        })
        .finally(() => {
          this.editLoading = false;
        });
    },
    switchToEditMode() {
      this.editMode = 'edit';
      this.$nextTick(() => {
        this.$refs.parkEditFormRef && this.$refs.parkEditFormRef.clearValidate();
      });
    },
    closeEditPage() {
      this.editPageVisible = false;
      this.editMode = 'add';
      this.editLoading = false;
      this.saving = false;
      this.editForm = createDefaultParkForm();
      this.$nextTick(() => {
        this.$refs.parkEditFormRef && this.$refs.parkEditFormRef.clearValidate();
      });
    },
    handleSubmitEdit() {
      this.$refs.parkEditFormRef.validate(valid => {
        if (!valid) return;
        this.saving = true;
        const payload = { ...this.editForm };
        const request = payload.id ? update : add;
        request(payload)
          .then(() => {
            ElMessage.success('保存成功!');
            this.closeEditPage();
            this.onLoad(this.page, this.query);
            this.refreshStatistics();
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    toggleStatus(row, checked) {
      if (!row || !row.id || this.statusChangingMap[row.id]) return;
      const nextStatus = checked ? '0' : '1';
      this.statusChangingMap = {
        ...this.statusChangingMap,
        [row.id]: true,
      };
      update({
        ...row,
        status: nextStatus,
      })
        .then(() => {
          row.status = nextStatus;
          ElMessage.success(nextStatus === '0' ? '已启用' : '已停用');
          this.refreshStatistics();
        })
        .finally(() => {
          const nextMap = { ...this.statusChangingMap };
          delete nextMap[row.id];
          this.statusChangingMap = nextMap;
        });
    },
    beforeImageUpload(file) {
      const isImage = file.type && file.type.indexOf('image/') === 0;
      if (!isImage) {
        ElMessage.warning('请上传图片文件');
      }
      return isImage;
    },
    handleImageUploadSuccess(field, response) {
      const success = response && (response.success || response.code === 200 || response.code === 0);
      const data = response && response.data;
      const url = typeof data === 'string' ? data : (data && (data.link || data.url)) || '';
      if (success && url) {
        this.editForm[field] = url;
        ElMessage.success('上传成功');
        return;
      }
      ElMessage.error((response && response.msg) || '上传失败');
    },
    handleImageUploadError() {
      ElMessage.error('上传失败');
    },
    removeEditImage(field) {
      this.editForm[field] = '';
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, { ...this.query, ...params }).then(res => {
        const resData = res.data.data;
        this.page.total = resData.total;
        this.data = resData.records;
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
        this.refreshStatistics();
        ElMessage.success('操作成功!');
      });
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
      this.$refs.parkTable && this.$refs.parkTable.clearSelection();
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
      this.refreshStatistics();
    },
  },
};
</script>

<style scoped>
.park-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 100%;
}

.park-search,
.park-toolbar {
  border-radius: 10px;
}

.park-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.park-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.park-search :deep(.el-input),
.park-search :deep(.el-select) {
  width: 190px;
}

.park-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.park-toolbar-left {
  display: flex;
  gap: 10px;
}

.park-table {
  width: 100%;
  border-radius: 0;
}

.park-page :deep(.el-table th),
.park-page :deep(.el-table td),
.park-page :deep(.el-table .cell) {
  text-align: center;
}

.park-page :deep(.el-table .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.park-table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.park-table-actions .el-button + .el-button {
  margin-left: 0;
}

.park-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.park-status-switch {
  --el-switch-on-color: #19be4b;
  --el-switch-off-color: #dcdfe6;
}

.park-page :deep(.el-table),
.park-page :deep(.el-table__inner-wrapper),
.park-page :deep(.el-table__header-wrapper),
.park-page :deep(.el-table__body-wrapper),
.park-page :deep(.el-table__border-left-patch) {
  border-radius: 0;
}

.park-edit-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.park-edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 86px;
  box-sizing: border-box;
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.park-edit-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.park-edit-header span {
  display: block;
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.park-edit-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.park-edit-actions .el-button {
  border-radius: 6px;
}

.park-edit-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.edit-section {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #edf0f5;
  color: #1f2329;
  font-weight: 600;
}

.section-head em {
  color: #9aa0a6;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.section-body {
  padding: 20px 22px 4px;
}

.park-edit-form :deep(.el-input-number),
.park-edit-form :deep(.el-select) {
  width: 100%;
}

.park-edit-form :deep(.el-form-item__label) {
  justify-content: flex-start;
  text-align: left;
}

.park-edit-form :deep(.el-form-item__content) {
  justify-content: flex-start;
  text-align: left;
}

.park-edit-form :deep(.el-input__inner),
.park-edit-form :deep(.el-textarea__inner) {
  text-align: left;
}

.park-edit-form :deep(.el-input__wrapper),
.park-edit-form :deep(.el-textarea__inner) {
  border-radius: 6px;
}

.park-edit-form :deep(.el-upload),
.park-edit-form :deep(.avue-ueditor),
.park-edit-form :deep(.w-e-text-container),
.park-edit-form :deep(.w-e-toolbar) {
  text-align: left;
}

.park-edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.preview-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-top: 12px;
}

.preview-box img {
  width: 160px;
  height: 120px;
  object-fit: cover;
  border: 1px solid #d9dfe8;
  border-radius: 6px;
  background: #f8fafc;
}

.preview-box--banner img {
  width: 260px;
}

.park-page :deep(.el-button),
.park-edit-page :deep(.el-upload .el-button) {
  border-radius: 6px;
}

@media (max-width: 768px) {
  .park-edit-header,
  .section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .park-edit-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .section-body {
    padding: 16px 14px 2px;
  }

  .preview-box {
    flex-direction: column;
  }

  .preview-box img,
  .preview-box--banner img {
    width: 100%;
    max-width: 320px;
  }
}
</style>
