<template>
  <basic-container>
    <stat-cards :items="statCards" compact />

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
          v-if="permission.park_delete"
          type="danger"
          icon="el-icon-delete"
          plain
          @click="handleDelete"
        >
          删 除
        </el-button>
      </template>

      <template #status="{ row }">
        <el-tag :type="row.status === '0' || row.status === 0 ? 'success' : 'info'">
          {{ row.status === '0' || row.status === 0 ? '启用' : '停用' }}
        </el-tag>
      </template>
    </avue-crud>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage, ElMessageBox } from 'element-plus';
import { add, getDetail, getList, getStatistics, remove, update } from '@/api/park/park';
import { tableOption } from '@/option/park/park';

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
      statistics: {
        managementArea: 0,
        rentableArea: 0,
        totalRoomCount: 0,
        rentableRoomCount: 0,
      },
      option: tableOption,
      data: [],
    };
  },
  computed: {
    ...mapGetters(['permission']),
    statCards() {
      return [
        {
          label: '管理面积',
          value: `${this.formatArea(this.statistics.managementArea)} ㎡`,
        },
        {
          label: '可招商面积',
          value: `${this.formatArea(this.statistics.rentableArea)} ㎡`,
        },
        {
          label: '总房源数',
          value: `${this.statistics.totalRoomCount || 0} 间`,
        },
        {
          label: '可招商房源',
          value: `${this.statistics.rentableRoomCount || 0} 间`,
        },
      ];
    },
    permissionList() {
      return {
        addBtn: this.validData(this.permission.park_add, false),
        viewBtn: this.validData(this.permission.park_view, false),
        delBtn: this.validData(this.permission.park_delete, false),
        editBtn: this.validData(this.permission.park_edit, false),
      };
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
    onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
        const resData = res.data.data;
        this.page.total = resData.total;
        this.data = resData.records;
        this.loading = false;
        this.selectionClear();
      }).catch(() => {
        this.loading = false;
      });
    },
    rowSave(row, done, loading) {
      add(row).then(() => {
        this.onLoad(this.page);
        this.refreshStatistics();
        ElMessage.success('操作成功!');
        done();
      }).catch(() => {
        loading();
      });
    },
    rowUpdate(row, index, done, loading) {
      update(row).then(() => {
        this.onLoad(this.page);
        this.refreshStatistics();
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
        this.refreshStatistics();
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
        this.refreshStatistics();
        ElMessage.success('操作成功!');
        this.$refs.crud.toggleSelection();
      });
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDetail(this.form.id).then(res => {
          this.form = res.data.data;
        });
      }
      done();
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
      this.refreshStatistics();
    },
  },
};
</script>
