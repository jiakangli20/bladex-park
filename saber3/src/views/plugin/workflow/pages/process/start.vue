<template>
  <nf-container v-if="moduleLoadInit">
    <nf-process-dashboard active="unfinished" />
    <nf-crud
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      :permission="permissionList"
      v-model="form"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="onLoad(page, query)"
      @tree-node-click="nodeClick"
      @tree-node-load="findObject(option.column, 'category').dicData = $event"
      @on-load="onLoad"
    >
      <template #menu-left v-if="isDev">
        <el-tag type="warning" effect="dark" size="default">
          <i class="el-icon-warning"></i>
          部署的流程不显示？请查看使用文档或到 模型设计中配置权限。
        </el-tag>
      </template>
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_process_start_flow"
          text
          type="primary"
          size="default"
          icon="el-icon-video-play"
          @click="dynamicRoute(row, 'start')"
        >
          发起
        </el-button>
        <el-button
          v-if="permission.wf_process_start_flow"
          text
          type="primary"
          size="default"
          icon="el-icon-search"
          @click="handleFlow(row)"
        >
          流程图
        </el-button>
      </template>
    </nf-crud>

    <el-dialog
      v-model="bpmnVisible"
      append-to-body
      destroy-on-close
      title="流程图"
      width="70%"
      class="nf-dialog"
    >
      <nf-design ref="bpmn" style="height: 60vh" :options="bpmnOption"></nf-design>
    </el-dialog>
  </nf-container>
</template>

<script>
import { processList as getList, getXmlByProcessDefId } from '../../api/process/process';

import exForm from '../../mixins/ex-form';
import NfProcessDashboard from '../../components/nf-process-dashboard/index.vue';

export default {
  mixins: [exForm],
  components: { NfProcessDashboard },
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
          width: '200px',
          url: '/blade-workflow/design/category/tree',
          props: {
            label: 'name',
            value: 'id',
          },
        },
        size: 'default',
        height: 'auto',
        calcHeight: 30,
        tip: false,
        border: true,
        selection: true,
        dialogType: 'drawer',
        addBtn: false,
        editBtn: false,
        delBtn: false,
        align: 'center',
        searchMenuSpan: 6,
        searchSize: 'default',
        column: [
          {
            label: '图标',
            prop: 'icon',
            type: 'upload',
            width: 80,
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
            label: '流程分类',
            row: true,
            type: 'tree',
            dicData: [],
            props: {
              label: 'name',
              value: 'id',
            },
            prop: 'category',
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
          },
        ],
      },
      data: [],
      bpmnVisible: false,
      bpmnOption: {},
    };
  },
  computed: {
    permissionList() {
      return {
        addBtn: this.validData(this.permission.deployment_add, false),
        viewBtn: this.validData(this.permission.deployment_view, false),
        delBtn: this.validData(this.permission.deployment_delete, false),
        editBtn: this.validData(this.permission.deployment_edit, false),
      };
    },
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
    isDev() {
      return process.env.NODE_ENV === 'development';
    },
  },
  methods: {
    handleFlow(row) {
      getXmlByProcessDefId({ processDefId: row.id }).then(res => {
        const xml = res.data.data;
        if (xml) {
          this.bpmnOption = {
            mode: 'view',
            xml,
          };
          this.bpmnVisible = true;
        } else this.$message.error('没有可显示的流程图');
      });
    },
    nodeClick({ id }) {
      this.categoryId = id;
      this.searchChange(this.query);
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
      this.onLoad(this.page, this.query);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.onLoad(this.page, this.query);
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

<style lang="scss" scoped>
:deep(.nf-crud__img) {
  img {
    width: 32px;
    height: 32px;
  }
}
</style>
