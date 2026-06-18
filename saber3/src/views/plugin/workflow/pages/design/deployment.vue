<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      :permission="permissionList"
      v-model="form"
      @row-del="rowDel"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="onLoad(page, query)"
      @on-load="onLoad"
      @tree-node-click="nodeClick"
      @tree-node-load="findObject(option.column, 'category').dicData = $event"
    >
      <template #menu-left>
        <el-button
          type="success"
          size="default"
          icon="el-icon-connection"
          v-if="permission.wf_design_deployment_category"
          @click="handleChangeCategory"
        >
          更改分类
        </el-button>
      </template>
      <template #status="{ row }">
        <el-tag size="default" :type="row.status == 1 ? 'primary' : 'danger'">
          {{ row.$status }}
        </el-tag>
      </template>
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_design_deployment_status && row.status == 1"
          text
          type="primary"
          size="default"
          icon="el-icon-refresh-left"
          @click="handleChangeStatus(row, 'suspended')"
        >
          挂起
        </el-button>
        <el-button
          v-if="permission.wf_design_deployment_status && row.status == 2"
          text
          type="primary"
          size="default"
          icon="el-icon-refresh-right"
          @click="handleChangeStatus(row, 'active')"
        >
          激活
        </el-button>
        <el-button
          v-if="permission.wf_design_deployment_category"
          text
          type="primary"
          size="default"
          icon="el-icon-pie-chart"
          @click="handleCategory(row)"
        >
          更改分类
        </el-button>
      </template>
    </nf-crud>

    <el-dialog v-model="categoryVisible" append-to-body title="选择分类">
      <nf-form
        v-if="categoryVisible"
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
        @submit="handleCategorySubmit"
      ></nf-form>
    </el-dialog>
  </nf-container>
</template>

<script>
import { getList, changeStatus, changeCategory, remove } from '../../api/design/deployment';

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
        treeOption: {
          width: "200px",
          url: '/blade-workflow/design/category/tree',
          props: {
            label: 'name',
            value: 'id',
          },
        },
        size: 'default',
        searchSize: 'default',
        height: 'auto',
        calcHeight: 30,
        tip: false,
        border: true,
        selection: true,
        dialogType: 'drawer',
        addBtn: false,
        editBtn: false,
        align: 'center',
        searchMenuSpan: 6,
        menuWidth: 240,
        column: [
          {
            label: 'id',
            prop: 'id',
            overHidden: true,
          },
          {
            label: '流程名称',
            prop: 'name',
            overHidden: true,
            search: true,
          },
          {
            label: '流程标识',
            prop: 'key',
            overHidden: true,
            search: true,
          },
          {
            label: '分类',
            prop: 'category',
            overHidden: true,
            type: 'tree',
            dicData: [],
            props: {
              label: 'name',
              value: 'id',
            },
          },
          {
            label: '版本',
            prop: 'version',
            width: 90,
          },
          {
            label: '状态',
            prop: 'status',
            dicData: [
              {
                label: '激活',
                value: 1,
              },
              {
                label: '挂起',
                value: 2,
              },
            ],
            width: 100,
            slot: true,
          },
          {
            label: '部署时间',
            prop: 'deployTime',
            overHidden: true,
          },
        ],
      },
      data: [],
      categoryVisible: false,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.deployment_add, false),
        viewBtn: this.validData(this.permission.deployment_view, false),
        delBtn: this.validData(this.permission.wf_design_deployment_delete, false),
        editBtn: this.validData(this.permission.deployment_edit, false),
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.deploymentId);
      });
      return ids.join(',');
    },
  },
  methods: {
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
    handleCategorySubmit(form, done) {
      let { deploymentId, category } = form;
      if (!deploymentId) deploymentId = this.ids;
      changeCategory({ deploymentId, category }).then(() => {
        this.$message.success('操作成功');
        done();
        this.form = {};
        this.categoryVisible = false;
        this.onLoad(this.page, this.query);
      });
    },
    handleCategory(row) {
      this.form.deploymentId = row.deploymentId;
      this.categoryVisible = true;
    },
    handleChangeStatus(row, status) {
      const param = {
        id: row.id,
        status,
      };
      changeStatus(param).then(() => {
        this.$message.success('操作成功');
        this.onLoad(this.page, this.query);
      });
    },
    rowDel(row) {
      this.$confirm('此操作会级联删除当前正在进行的流程实例，且无法恢复，确定要删除吗?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        this.$confirm(
          '二次确认！此操作会级联删除当前正在进行的流程实例，且无法恢复，确定要删除吗?',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        ).then(() => {
          remove({ deploymentId: row.deploymentId }).then(() => {
            this.onLoad(this.page);
            this.$message({
              type: 'success',
              message: '操作成功!',
            });
          });
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
          const deploymentIds = this.selectionList.map(item => item.deploymentId).join(',');
          return remove({ deploymentIds });
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

      if (this.categoryId) params['category'] = this.categoryId;
      else delete params['category'];

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

<style></style>
