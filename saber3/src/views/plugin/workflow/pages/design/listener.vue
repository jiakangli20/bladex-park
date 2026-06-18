<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      :option="option"
      v-model:search="search"
      v-model:page="page"
      v-model="form"
      :table-loading="loading"
      :data="data"
      :permission="permissionList"
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
          icon="el-icon-delete"
          plain
          v-if="permission.wf_listener_delete"
          @click="handleDelete"
        >
          删 除
        </el-button>
        <el-tag style="margin-left: 8px" type="warning" size="default" effect="dark">
            此配置只做快捷选择用，具体逻辑请自行实现。具体配置方法请查看
            <el-link
              underline="never"
              target="_blank"
              href="https://tkjohn.github.io/flowable-userguide/#taskListeners"
            >
              使用文档
            </el-link>
        </el-tag>
      </template>
    </nf-crud>
  </nf-container>
</template>

<script>
import { getList, add, update, remove } from '../../api/design/listener';
import { mapGetters } from 'vuex';
import module from '../../mixins/module';

export default {
  mixins: [module()],
  data() {
    return {
      form: {},
      query: {},
      search: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      option: {
        height: 'auto',
        calcHeight: 30,
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: true,
        selection: true,
        dialogClickModal: false,
        align: 'center',
        span: 24,
        column: [
          {
            label: '名称',
            prop: 'name',
            type: 'input',
            rules: [{ required: true, message: '请输入名称', trigger: 'blur' }],
            search: true,
          },
          {
            label: '分类',
            prop: 'category',
            type: 'radio',
            dicData: [
              {
                label: '任务监听',
                value: 'Task',
              },
              {
                label: '执行监听',
                value: 'Execution',
              },
            ],
            value: 'Task',
            rules: [{ required: true, message: '请选择分类', trigger: 'change' }],
            change: ({ value }) => {
              if (!value) return;

              const event = this.findObject(this.option.column, 'event');
              let dicData = [];
              if (value == 'Task') {
                dicData = [
                  {
                    label: '创建 - create',
                    value: 'create',
                  },
                  {
                    label: '分配审核人 - assignment',
                    value: 'assignment',
                  },
                  {
                    label: '完成 - complete',
                    value: 'complete',
                  },
                  {
                    label: '删除 - delete',
                    value: 'delete',
                  },
                ];
              } else if (value == 'Execution') {
                dicData = [
                  {
                    label: '开始 - start',
                    value: 'start',
                  },
                  {
                    label: '到达 - take',
                    value: 'take',
                  },
                  {
                    label: '结束 - end',
                    value: 'end',
                  },
                ];
              }
              event.dicData = dicData;
              this.form.event = '';
            },
            search: true,
          },
          {
            label: '事件',
            prop: 'event',
            type: 'radio',
            dicData: [],
            rules: [{ required: true, message: '请选择事件', trigger: 'change' }],
          },
          {
            label: '类型',
            prop: 'type',
            type: 'radio',
            dicData: [
              {
                label: '类',
                value: 'class',
              },
              {
                label: '表达式',
                value: 'expression',
              },
              {
                label: '代理表达式',
                value: 'delegateExpression',
              },
            ],
            rules: [{ required: true, message: '请选择类型', trigger: 'change' }],
            value: 'delegateExpression',
          },
          {
            label: '值',
            prop: 'val',
            type: 'input',
            rules: [{ required: true, message: '请输入值', trigger: 'blur' }],
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
        addBtn: this.validData(this.permission.wf_listener_add, false),
        viewBtn: this.validData(this.permission.wf_listener_view, false),
        delBtn: this.validData(this.permission.wf_listener_delete, false),
        editBtn: this.validData(this.permission.wf_listener_edit, false),
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
