<template>
  <basic-container>
    <avue-crud
      ref="crud"
      v-model="form"
      v-model:page="page"
      :option="option"
      :data="data"
      :table-loading="loading"
      :permission="permissionList"
      :before-open="beforeOpen"
      @row-save="rowSave"
      @row-update="rowUpdate"
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
          v-if="permission.building_delete"
          type="danger"
          icon="el-icon-delete"
          plain
          @click="handleDelete"
        >
          删 除
        </el-button>
        <el-button
          v-if="permission.building_import"
          type="primary"
          icon="el-icon-upload"
          plain
          @click="excelBox = true"
        >
          导 入
        </el-button>
        <el-button
          v-if="permission.building_export"
          type="primary"
          icon="el-icon-download"
          plain
          @click="handleExport"
        >
          导 出
        </el-button>
      </template>

      <template #status="{ row }">
        <el-tag :type="row.status === '0' || row.status === 0 ? 'success' : 'info'">
          {{ row.status === '0' || row.status === 0 ? '启用' : '停用' }}
        </el-tag>
      </template>

      <template #buildingTag="{ row }">
        {{ buildBuildingTag(row) }}
      </template>

      <template #floorAreas-form>
        <section class="floor-area-panel">
          <div class="floor-area-head">
            <span>楼层面积配置</span>
            <em>楼层数 {{ floorAreaList.length }}，合计 {{ formatArea(totalFloorArea) }}㎡</em>
          </div>
          <div v-if="floorAreaList.length" class="floor-area-grid">
            <div v-for="item in floorAreaList" :key="item.floorNo" class="floor-area-item">
              <span>{{ item.floorNo }}F</span>
              <el-input-number
                v-model="item.area"
                :min="0"
                :precision="2"
                controls-position="right"
              />
            </div>
          </div>
          <el-empty v-else description="请输入楼层数" :image-size="70" />
        </section>
      </template>
    </avue-crud>

    <el-dialog title="建筑数据导入" append-to-body v-model="excelBox" width="555px">
      <avue-form :option="excelOption" v-model="excelForm" :upload-after="uploadAfter">
        <template #excelTemplate>
          <el-button type="primary" @click="handleTemplate">
            点击下载<i class="el-icon-download el-icon--right"></i>
          </el-button>
        </template>
      </avue-form>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { downloadXls } from '@/utils/util';
import { getToken } from '@/utils/auth';
import { exportBlob } from '@/api/common';
import { getList, getDetail, remove, submit } from '@/api/park/building';
import { getList as getParkList } from '@/api/park/park';
import { tableOption } from '@/option/park/building';

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
      excelBox: false,
      excelForm: {},
      excelOption: {
        submitBtn: false,
        emptyBtn: false,
        column: [
          {
            label: '模板上传',
            prop: 'excelFile',
            type: 'upload',
            drag: true,
            loadText: '模板上传中，请稍等',
            span: 24,
            propsHttp: {
              res: 'data',
            },
            tip: '请上传 .xls,.xlsx 标准格式文件',
            action: '/blade-park/building/import',
          },
          {
            label: '模板下载',
            prop: 'excelTemplate',
            formslot: true,
            span: 24,
          },
        ],
      },
    };
  },
  computed: {
    ...mapGetters(['permission', 'website']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.building_add, false),
        viewBtn: this.validData(this.permission.building_view, false),
        delBtn: this.validData(this.permission.building_delete, false),
        editBtn: this.validData(this.permission.building_edit, false),
      };
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
    'form.floors'(value) {
      this.syncFloorAreaList(value);
    },
    'form.area'() {
      if (!this.floorAreaList.length) {
        this.syncFloorAreaList(this.form.floors);
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
    rowSave(row, done, loading) {
      submit(this.normalizePayload(row)).then(() => {
        this.onLoad(this.page);
        ElMessage.success('操作成功!');
        done();
      }).catch(() => {
        loading();
      });
    },
    rowUpdate(row, index, done, loading) {
      submit(this.normalizePayload(row)).then(() => {
        this.onLoad(this.page);
        ElMessage.success('操作成功!');
        done();
      }).catch(() => {
        loading();
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
        this.$refs.crud.toggleSelection();
      });
    },
    beforeOpen(done, type) {
      if (type === 'add') {
        this.form.status = '0';
        this.form.sortNum = 1;
        this.form.floors = 1;
        this.form.buildingType = '自持';
        this.floorAreaList = [];
        this.syncFloorAreaList(1);
      }
      if (['edit', 'view'].includes(type)) {
        getDetail(this.form.id).then(res => {
          this.form = Object.assign({}, res.data.data || {});
          this.syncFloorAreaList(this.form.floors);
          const floorMap = {};
          (this.form.floorAreas || []).forEach(item => {
            floorMap[item.floorNo] = item.area;
          });
          this.floorAreaList = this.floorAreaList.map(item => ({
            floorNo: item.floorNo,
            area: floorMap[item.floorNo] !== undefined ? floorMap[item.floorNo] : item.area,
          }));
        });
      }
      done();
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
      const area = Number(this.form.area || 0);
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
      this.$refs.crud.toggleSelection();
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
    handleExport() {
      exportBlob('/blade-park/building/export', this.query).then(res => {
        downloadXls(res.data, `建筑数据${this.$dayjs().format('YYYY-MM-DD HH:mm:ss')}.xlsx`);
      });
    },
    handleTemplate() {
      exportBlob(
        `/blade-park/building/export-template?${this.website.tokenHeader}=${getToken()}`
      ).then(res => {
        downloadXls(res.data, '建筑数据模板.xlsx');
      });
    },
    uploadAfter() {
      ElMessage.success('导入成功');
      this.excelBox = false;
      this.onLoad(this.page);
    },
  },
};
</script>

<style scoped>
.floor-area-panel {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.floor-area-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: #303133;
  font-weight: 600;
}

.floor-area-head em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
}

.floor-area-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 10px;
}

.floor-area-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.floor-area-item span {
  color: #606266;
  font-size: 13px;
}
</style>
