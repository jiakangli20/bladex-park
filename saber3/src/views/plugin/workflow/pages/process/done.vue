<template>
  <nf-container v-if="moduleLoadInit">
    <nf-process-dashboard active="finished" />
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
      @on-load="onLoad"
    >
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_process_done_detail"
          text
          type="primary"
          size="default"
          icon="el-icon-info-filled"
          @click="dynamicRoute(row, 'detail')"
        >
          详情
        </el-button>
        <el-button
          v-if="permission.wf_process_done_follow"
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
import { doneList as getList, detail } from '../../api/process/process';
import { mapGetters } from 'vuex';

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
        rowKey: 'taskId',
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
        searchIndex: 3,
        searchIcon: true,
        column: [
          {
            label: '流程名称',
            prop: 'processDefinitionName',
            search: true,
            overHidden: true,
          },
          {
            label: '流程标识',
            prop: 'processDefinitionKey',
            search: true,
            overHidden: true,
          },
          {
            label: '流水号',
            prop: 'serialNumber',
            bind: 'variables.serialNumber',
            search: true,
            overHidden: true,
          },
          {
            label: '流程分类',
            type: 'tree',
            row: true,
            dicUrl: '/blade-workflow/design/category/tree',
            props: {
              label: 'name',
              value: 'id',
            },
            prop: 'category',
            search: true,
          },
          {
            label: '当前节点',
            prop: 'taskName',
          },
          {
            label: '申请人',
            prop: 'startUsername',
            search: true,
          },
          {
            label: '申请时间',
            prop: 'createTime',
            type: 'datetime',
            format: 'YYYY-MM-DD HH:mm',
            width: 165,
          },
          {
            label: '流程状态',
            prop: 'processIsFinished',
            dicData: [
              {
                label: '已完成',
                value: 'finished',
              },
              {
                label: '已终结',
                value: 'terminate',
              },
              {
                label: '已撤销',
                value: 'withdraw',
              },
              {
                label: '已删除',
                value: 'deleted',
              },
            ],
            type: 'select',
            search: true,
          },
        ],
      },
      data: [],
      bpmnVisible: false,
      bpmnOption: {},
    };
  },
  computed: {
    ...mapGetters(['permission']),
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
  },
  methods: {
    handleFlow(row) {
      const { taskId, processInstanceId } = row;
      detail({ taskId, processInsId: processInstanceId }).then(res => {
        const { process, flow } = res.data.data;

        this.bpmnOption = {
          mode: 'view',
          xml: process.xml,
          flows: this.handleResolveFlows(flow),
        };

        this.bpmnVisible = true;
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
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    onLoad(page, params = {}) {
      this.loading = true;
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
