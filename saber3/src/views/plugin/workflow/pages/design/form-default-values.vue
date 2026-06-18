<template>
  <nf-container v-if="moduleLoadInit">
    <el-container>
      <el-aside width="200px">
        <el-tree :props="treeOption.props" :data="treeData" @node-click="nodeClick"></el-tree>
      </el-aside>
      <el-main style="margin-left: 10px">
        <nf-crud
          :option="option"
          :table-loading="loading"
          :data="data"
          v-model:page="page"
          :permission="permissionList"
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
              v-if="permission.wf_form_default_values_delete"
              @click="handleDelete"
            >
              删 除
            </el-button>
          </template>
          <template #menu="{ row }">
            <el-button
              v-if="permission.wf_form_default_values_copy"
              text
              type="primary"
              icon="el-icon-document-copy"
              size="default"
              @click="handleCopy(row)"
            >
              复制
            </el-button>
          </template>
        </nf-crud>
      </el-main>
    </el-container>
  </nf-container>
</template>

<script>
import {
  getList,
  getDetail,
  add,
  update,
  remove,
  listType,
} from '../../api/design/form-default-values';
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
        size: 'default',
        height: 'auto',
        calcHeight: 30,
        tip: false,
        border: true,
        index: true,
        selection: true,
        dialogType: 'drawer',
        align: 'center',
        searchMenuSpan: 6,
        column: [
          {
            label: '名称',
            prop: 'name',
            rules: [
              {
                required: true,
                message: '请输入名称',
                trigger: 'blur',
              },
            ],
            span: 24,
            search: true,
          },
          {
            label: '内容',
            prop: 'content',
            placeholder: '请输入 内容，以${}包裹则表示执行代码，请确保代码可执行',
            rules: [
              {
                required: true,
                message: '请输入内容',
                trigger: 'blur',
              },
            ],
            span: 24,
            overHidden: true,
          },
          {
            label: '字段类型',
            prop: 'fieldType',
            type: 'select',
            filterable: true,
            allowCreate: true,
            dicData: [
              {
                label: 'input',
                value: 'input',
              },
              {
                label: 'textarea',
                value: 'textarea',
              },
              {
                label: 'select',
                value: 'select',
              },
              {
                label: 'tree',
                value: 'tree',
              },
            ],
            rules: [
              {
                required: true,
                message: '请选择/输入类型',
                trigger: 'blur',
              },
            ],
            span: 24,
            width: 120,
          },
        ],
      },
      data: [],
      treeData: [],
      treeOption: {
        size: 'default',
        addBtn: false,
        props: {
          label: 'fieldType',
          value: 'fieldType',
        },
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.wf_form_default_values_add, false),
        viewBtn: this.validData(this.permission.wf_form_default_values_view, false),
        delBtn: this.validData(this.permission.wf_form_default_values_delete, false),
        editBtn: this.validData(this.permission.wf_form_default_values_edit, false),
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
  mounted() {
    this.getListType();
  },
  methods: {
    nodeClick({ fieldType }) {
      this.fieldType = fieldType;
      this.searchChange(this.query);
    },
    getListType() {
      listType().then(res => {
        this.treeData = res.data.data;
        this.treeData.unshift({
          fieldType: '全部',
        });
      });
    },
    handleCopy(row) {
      delete row.id;
      this.form = this.deepClone(row);
      this.$refs.crud.rowAdd();
    },
    rowSave(row, done, loading) {
      add(row).then(
        () => {
          this.getListType();
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        () => {
          loading();
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
      if (done) done();
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
      if (this.fieldType) {
        if (this.fieldType == '全部') delete params.fieldType;
        else params.fieldType = this.fieldType;
      }
      getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
        this.selectionClear();
      });
    },
  },
};
</script>

<style></style>
