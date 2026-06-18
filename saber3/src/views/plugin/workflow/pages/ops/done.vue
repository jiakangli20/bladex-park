<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      ref="crud"
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      v-model="form"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="onLoad(page, query)"
      @on-load="onLoad"
    >
      <template #processDefinitionName="{ row }">
        <el-link
          v-if="permission.wf_ops_detail"
          style="font-size: 12px"
          type="primary"
          @click="handleDetail(row)"
          >{{ row.processDefinitionName }}</el-link
        >
        <span v-else>{{ row.processDefinitionName }}</span>
      </template>
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_ops_follow"
          text
          type="primary"
          size="default"
          icon="el-icon-search"
          @click="handleFlow(row)"
          >流程图</el-button
        >
      </template>
    </nf-crud>

    <el-dialog v-model="bpmnVisible" append-to-body destroy-on-close title="流程图">
      <nf-design ref="bpmn" style="height: 500px" :options="bpmnOption"></nf-design>
    </el-dialog>
    <!-- 流程详情 -->
    <nf-form-detail
      ref="form-detail"
      :props="props"
      is-dialog
      dialog-type="drawer"
      dialog-width="80%"
      :show-examine="false"
    />
  </nf-container>
</template>

<script>
import { detail } from '../../api/process/process';
import { getDoneList as getList } from '../../api/ops/ops';
import { mapGetters } from 'vuex';

import NfFormDetail from '../../components/nf-form-detail/index.vue';
import exForm from '../../mixins/ex-form';

export default {
  mixins: [exForm],
  components: {
    NfFormDetail,
  },
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
        rowKey: 'processInstanceId',
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
            row: true,
            type: 'tree',
            dicUrl: '/blade-workflow/design/category/tree',
            props: {
              label: 'name',
              value: 'id',
            },
            prop: 'category',
            search: true,
          },
          {
            label: '申请人',
            prop: 'startUsername',
            search: true,
          },
          {
            label: '开始时间',
            prop: 'createTime',
            type: 'datetime',
            format: 'YYYY-MM-DD HH:mm',
            width: 165,
          },
          {
            label: '结束时间',
            prop: 'endTime',
            type: 'datetime',
            format: 'YYYY-MM-DD HH:mm',
            width: 165,
          },
          {
            label: '开始时间',
            prop: 'startTimeRange',
            type: 'datetime',
            dataType: 'string',
            format: 'YYYY-MM-DD HH:mm:ss',
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            hide: true,
            search: true,
            searchRange: true,
          },
          {
            label: '结束时间',
            prop: 'endTimeRange',
            type: 'datetime',
            dataType: 'string',
            format: 'YYYY-MM-DD HH:mm:ss',
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            hide: true,
            search: true,
            searchRange: true,
          },
          {
            label: '状态',
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
      props: {},
    };
  },
  computed: {
    ...mapGetters(['permission']),
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele.taskId);
      });
      return ids.join(',');
    },
  },
  methods: {
    handleDetail(row) {
      this.dynamicRoute(row, 'detail', true).then(() => {
        this.form = { ...row };
        this.props = {
          taskId: row.taskId,
          processInsId: row.processInstanceId,
        };
        this.$refs['form-detail'].show();
      });
    },
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
    async onLoad(page, params = {}) {
      this.loading = true;
      getList(page.currentPage, page.pageSize, Object.assign(params, this.query))
        .then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records;
          this.loading = false;
        })
        .catch(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style lang="scss"></style>
