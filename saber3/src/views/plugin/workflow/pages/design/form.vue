<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      ref="crud"
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      v-model:search="query"
      :permission="permissionList"
      v-model="form"
      @row-update="rowUpdate"
      @row-save="rowSave"
      @row-del="rowDel"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="onLoad(page, query)"
      @on-load="onLoad"
      @tree-node-click="nodeClick"
      @tree-node-load="handleCategoryListChange"
    >
      <template #menu-left>
        <el-button
          type="success"
          size="default"
          icon="el-icon-connection"
          v-if="permission.wf_design_form_change_category"
          @click="handleChangeCategory"
        >
          更改分类
        </el-button>
      </template>
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_design_form_design"
          text
          type="primary"
          size="default"
          icon="el-icon-edit"
          @click="handleDesign(row)"
        >
          设计
        </el-button>
        <el-button
          v-if="permission.wf_design_form_copy"
          text
          type="primary"
          size="default"
          icon="el-icon-document-copy"
          @click="handleCopy(row)"
        >
          拷贝
        </el-button>
        <el-button
          v-if="permission.wf_design_form_history"
          text
          type="primary"
          size="default"
          icon="el-icon-clock"
          @click="handleHistory(row)"
        >
          历史
        </el-button>
      </template>
    </nf-crud>
    <el-drawer
      v-model="formVisible"
      class="formDesignClass"
      append-to-body
      :title="title"
      size="100%"
    >
      <nf-form-design
        v-if="formVisible"
        style="height: 90vh"
        ref="formDesign"
        :toolbar="['clear', 'preview', 'import', 'generate']"
        :customFields="customFields"
        :default-values="defaultValues"
        :options="options"
        :database-option="databaseOption"
      >
        <template #toolbar>
          <el-button
            style="padding: 0"
            text
            type="primary"
            size="default"
            icon="el-icon-download"
            @click="handleSubmit"
          >
            保存
          </el-button>
        </template>
      </nf-form-design>
    </el-drawer>

    <el-dialog v-model="copyVisible" append-to-body title="拷贝表单">
      <nf-form :option="copyOption" v-model="form" ref="copyForm" @submit="handleCopySubmit">
      </nf-form>
    </el-dialog>

    <el-dialog v-model="categoryVisible" append-to-body title="选择分类">
      <nf-form
        v-model="form"
        :option="{
          column: [
            {
              type: 'tree',
              label: '流程分类',
              span: 24,
              props: { label: 'name', value: 'id' },
              dicFormatter: res => {
                return res.data.data;
              },
              prop: 'category',
              dicUrl: '/blade-workflow/design/category/tree',
              required: true,
              rules: [{ required: true, message: '请选择分类' }],
            },
          ],
        }"
        @submit="handleChangeCategorySubmit"
      ></nf-form>
    </el-dialog>
  </nf-container>
</template>

<script>
import { getList, add, update, remove, listType, changeCategory } from '../../api/design/form';

import { mapGetters } from 'vuex';
import module from '../../mixins/module';

import customFields from '../../mixins/custom-fields';

export default {
  mixins: [customFields, module(['fd'])],
  data() {
    return {
      formVisible: false,
      options: {},
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
        treeOption: {
          width: "200px",
          url: '/blade-workflow/design/category/tree',
          props: {
            label: 'name',
            value: 'id',
          },
        },
        menuWidth: 350,
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
            label: '表单key',
            prop: 'formKey',
            rules: [
              {
                required: true,
                message: '请输入表单key',
                trigger: 'blur',
              },
            ],
            search: true,
          },
          {
            label: '表单名称',
            prop: 'name',
            rules: [
              {
                required: true,
                message: '请输入表单名称',
                trigger: 'blur',
              },
            ],
            search: true,
          },
          {
            label: '分类',
            prop: 'categoryId',
            type: 'tree',
            props: {
              label: 'name',
              value: 'id',
            },
            dicData: [],
            rules: [
              {
                required: true,
                message: '请选择分类',
                trigger: 'change',
              },
            ],
            overHidden: true,
          },
          {
            label: '版本',
            prop: 'version',
            display: false,
          },
          {
            label: '状态',
            prop: 'status',
            type: 'select',
            value: 1,
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
          {
            label: '备注',
            prop: 'remark',
            type: 'textarea',
            span: 24,
            overHidden: true,
          },
        ],
      },
      data: [],
      defaultValues: {},
      copyOption: {
        column: [
          {
            label: '表单key',
            prop: 'formKey',
            rules: [
              {
                required: true,
                message: '请输入表单key',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '表单名称',
            prop: 'name',
            rules: [
              {
                required: true,
                message: '请输入表单名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '分类',
            prop: 'categoryId',
            type: 'tree',
            props: {
              label: 'name',
              value: 'id',
            },
            dicData: [],
            rules: [
              {
                required: true,
                message: '请选择分类',
                trigger: 'change',
              },
            ],
          },
          {
            label: '状态',
            prop: 'status',
            type: 'select',
            value: 1,
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
          {
            label: '备注',
            prop: 'remark',
            type: 'textarea',
            span: 24,
          },
        ],
      },
      isNewVersion: false,
      isCopy: false,
      copyVisible: false,
      categoryVisible: false,
      title: '表单设计',
      databaseOption: {
        enable: false, // 是否开启匹配已有数据库设计模式
        datasourceUrl: '/blade-develop/datasource/select', // 数据源选择接口
        tableListUrl: '/blade-develop/model/table-list', // 表列表接口
        tableInfoUrl: '/blade-develop/model/table-info', // 表信息接口
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.wf_design_form_add, false),
        viewBtn: this.validData(this.permission.wf_design_form_view, false),
        delBtn: this.validData(this.permission.wf_design_form_delete, false),
        editBtn: this.validData(this.permission.wf_design_form_edit, false),
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
    this.getDefaultValues();
  },
  methods: {
    handleCategoryListChange(val) {
      this.findObject(this.option.column, 'categoryId').dicData = val;
      this.findObject(this.copyOption.column, 'categoryId').dicData = val;
    },
    handleChangeCategorySubmit(form, done) {
      const { category } = form;
      changeCategory({ ids: this.ids, category }).then(() => {
        this.$message.success('修改成功');
        done();
        this.categoryVisible = false;
        this.onLoad(this.page, this.query);
      });
    },
    handleChangeCategory() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.categoryVisible = true;
    },
    nodeClick({ id }) {
      this.categoryId = id;
      this.searchChange(this.query);
    },
    handleSubmit() {
      this.$refs.formDesign.getData('string').then(data => {
        this.$refs.formDesign.getData('app').then(appData => {
          if (this.isCopy) {
            this.copyVisible = true;
            this.form.content = data;
            this.form.appContent = JSON.stringify(appData);
          } else {
            this.row.content = data;
            this.row.appContent = JSON.stringify(appData);

            if (this.isNewVersion) {
              this.row.newVersion = false;

              update(this.row).then(() => {
                this.$message.success('保存成功');
                this.onLoad(this.page, this.query);
                this.formVisible = false;
              });
            } else {
              this.$confirm('是否将此表单保存为新版本？这意味着可以返回到以前的版本。', '提示', {
                distinguishCancelAndClose: true,
                confirmButtonText: '否',
                cancelButtonText: '是',
                type: 'warning',
              })
                .then(() => {
                  this.row.newVersion = false;

                  update(this.row).then(() => {
                    this.$message.success('保存成功');
                    this.onLoad(this.page, this.query);
                    this.formVisible = false;
                  });
                })
                .catch(action => {
                  if (action == 'cancel') {
                    this.row.newVersion = true;

                    update(this.row).then(() => {
                      this.$message.success('保存成功');
                      this.onLoad(this.page, this.query);
                      this.formVisible = false;
                    });
                  }
                });
            }
          }
        });
      });
    },
    handleDesign(row) {
      this.options = this.deepClone(row.content || '{column: []}');
      if (row.content) this.isNewVersion = false;
      else this.isNewVersion = true;
      this.row = row;
      this.isCopy = false;
      this.formVisible = true;
      this.title = `表单设计 - ${row.name}`;
    },
    handleCopy(row) {
      this.options = this.deepClone(row.content || '{column: []}');
      this.isCopy = true;
      this.formVisible = true;
    },
    handleCopySubmit(form, done) {
      const param = {
        ...form,
        content: this.form.content,
      };
      add(param)
        .then(() => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.copyForm.resetFields();
          done();
          this.copyVisible = false;
          this.formVisible = false;
        })
        .catch(() => {
          done();
        });
    },
    handleHistory(row) {
      this.$router.push('/plugin/workflow/pages/design/form-history/' + row.id);
    },
    getDefaultValues() {
      listType().then(res => {
        this.defaultValues = res.data.data;
      });
    },
    rowSave(row, loading, done) {
      add(row).then(
        () => {
          loading();
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        },
        error => {
          done();
          console.log(error);
        }
      );
    },
    rowUpdate(row, index, loading, done) {
      update(row).then(
        () => {
          loading();
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        },
        error => {
          done();
          console.log(error);
        }
      );
    },
    rowDel(row) {
      this.$confirm('删除全部版本或者回退到最后版本?', {
        distinguishCancelAndClose: true,
        confirmButtonText: '回退',
        cancelButtonText: '全部删除',
        type: 'warning',
      })
        .then(() => {
          const param = {
            id: row.id,
            rollback: true,
          };
          remove(param).then(() => {
            this.onLoad(this.page);
            this.$message.success('操作成功');
          });
        })
        .catch(action => {
          if (action == 'cancel') {
            const param = {
              id: row.id,
              rollback: false,
            };
            remove(param).then(() => {
              this.onLoad(this.page);
              this.$message.success('操作成功');
            });
          }
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
    searchReset() {
      this.query = {};
      this.onLoad(this.page);
    },
    searchChange(params, done) {
      this.query = params;
      this.onLoad(this.page, params);
      if (done && typeof done == 'function') done();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    onLoad(page, params = {}) {
      this.loading = true;

      if (this.categoryId) params['categoryId_equal'] = this.categoryId;
      else delete params['categoryId_equal'];

      getList(page.currentPage, page.pageSize, Object.assign(params, this.query)).then(res => {
        const data = res.data.data;
        this.page.total = data.total;
        this.data = data.records;
        this.loading = false;
      });
    },
  },
};
</script>

<style lang="scss">
.formDesignClass {
  .el-drawer__header {
    margin-bottom: 10px;
  }
  .el-drawer__body {
    padding: 10px;
  }
}
</style>
