<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      :permission="permissionList"
      :before-open="beforeOpen"
      v-model="form"
      ref="crud"
      @row-update="rowUpdate"
      @row-save="rowSave"
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
          type="danger"
          size="default"
          icon="el-icon-delete"
          plain
          v-if="permission.wf_category_delete"
          @click="handleDelete"
        >
          删 除
        </el-button>
      </template>
    </nf-crud>
  </nf-container>
</template>

<script>
import { getList, getDetail, add, update, remove, treeList } from '../../api/design/category';
import { mapGetters } from 'vuex';
import module from '../../mixins/module';

export default {
  mixins: [module()],
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
      option: {
        dialogType: 'drawer',
        size: 'default',
        align: 'center',
        height: 'auto',
        calcHeight: 30,
        tip: false,
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: true,
        selection: true,
        tree: true,
        defaultExpandAll: true,
        column: [
          {
            label: '分类名称',
            prop: 'name',
            rules: [
              {
                required: true,
                message: '请输入分类名称',
                trigger: 'blur',
              },
            ],
            align: 'left',
            search: true,
          },
          {
            label: '上级',
            prop: 'pid',
            hide: true,
            type: 'tree',
            dicUrl: '/blade-workflow/design/category/tree',
            props: {
              label: 'name',
              value: 'id',
            },
          },
          {
            label: '排序',
            prop: 'sort',
          },
          {
            label: '状态',
            prop: 'status',
            type: 'select',
            dicData: [
              {
                label: '可用',
                value: 1,
              },
              {
                label: '禁用',
                value: 2,
              },
            ],
            rules: [
              {
                required: true,
                message: '请选择状态',
                trigger: 'change',
              },
            ],
            search: true,
          },
        ],
      },
      data: [],
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.wf_category_add, false),
        viewBtn: this.validData(this.permission.wf_category_view, false),
        delBtn: this.validData(this.permission.wf_category_delete, false),
        editBtn: this.validData(this.permission.wf_category_edit, false),
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  methods: {
    rowSave(row, done, loading) {
      add(row).then(
        () => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          loading();
          window.console.log(error);
        }
      );
    },
    rowUpdate(row, index, done, loading) {
      update(row).then(
        () => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          loading();
          console.log(error);
        }
      );
    },
    rowDel(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(row.id);
        })
        .then(() => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return remove(this.ids);
        })
        .then(() => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crud.toggleSelection();
        });
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDetail(this.form.id).then(res => {
          this.form = res.data.data;
        });
      } else {
        this.form.status = 1;
      }
      done();
    },
    searchReset() {
      this.query = {};
      this.onLoad(this.page);
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
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
    onLoad(page, params = {}) {
      this.loading = true;
      const param = Object.assign(params, this.query);
      if (Object.keys(param).length > 0) {
        getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records;
          this.loading = false;
          this.selectionClear();
        });
      } else {
        treeList().then(res => {
          const data = res.data.data;
          this.data = data;
          this.loading = false;
          this.selectionClear();
        });
      }
    },
  },
};
</script>

<style></style>
