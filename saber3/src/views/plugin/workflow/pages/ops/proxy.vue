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
      <template #userId="{ row }">
        {{ getUserName(row.userId) }}
      </template>
      <template #proxyUserId="{ row }">
        {{ getUserName(row.proxyUserId) }}
      </template>
      <template #time="{ row }">
        <span v-if="row.type == 1">永久</span>
        <span v-else>{{ row.startTime }} ~ {{ row.endTime }}</span>
      </template>
      <template #processDefKey="{ row }">
        <span v-if="row.processDefKey == 'WF_ALL_PROCESS'">全部流程</span>
        <span v-else>{{ row.processDefKey }}</span>
      </template>
      <template #menu-left>
        <el-button
          type="danger"
          size="default"
          icon="el-icon-delete"
          plain
          v-if="permission.wf_ops_proxy_delete"
          @click="handleDelete"
        >
          删 除
        </el-button>
      </template>
    </nf-crud>
  </nf-container>
</template>

<script>
import { getList, add, update, remove } from '../../api/ops/proxy';
import { getList as userList } from '@/api/system/user';
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
        selection: true,
        dialogType: 'drawer',
        align: 'center',
        searchMenuSpan: 6,
        searchSize: 'default',
        searchIndex: 3,
        searchIcon: true,
        column: [
          {
            label: '委托人',
            prop: 'userId',
            component: 'wf-user-select',
            params: {
              checkType: 'radio',
            },
            rules: [
              {
                required: true,
                message: '请选择委托人',
                trigger: 'change',
              },
            ],
            span: 24,
            width: 120,
            overHidden: true,
            search: true,
          },
          {
            label: '代理人',
            prop: 'proxyUserId',
            component: 'wf-user-select',
            params: {
              checkType: 'radio',
            },
            rules: [
              {
                required: true,
                message: '请选择代理人',
                trigger: 'change',
              },
            ],
            span: 24,
            width: 120,
            overHidden: true,
            search: true,
          },
          {
            label: '流程',
            prop: 'process',
            type: 'radio',
            dicData: [
              {
                label: '全部',
                value: '1',
              },
              {
                label: '指定',
                value: '2',
              },
            ],
            hide: true,
            change: ({ value }) => {
              const processDefKey = this.findObject(this.option.column, 'processDefKey');
              if (value == '2') {
                processDefKey.display = true;
                if (this.form.processDefKey == 'WF_ALL_PROCESS') this.form.processDefKey = '';
              } else {
                processDefKey.display = false;
                this.form.processDefKey = 'WF_ALL_PROCESS';
              }
            },
            rules: [
              {
                required: true,
                message: '请选择流程',
                trigger: 'blur',
              },
            ],
            value: '1',
          },
          {
            label: '指定流程',
            prop: 'processDefKey',
            dataType: 'string',
            type: 'tree',
            dicUrl: '/blade-workflow/design/deployment/list?size=-1',
            dicFormatter: res => {
              const data = res.data.data.records;
              if (data && data.length > 0) {
                return [
                  {
                    name: '全部',
                    key: 'all',
                    children: data,
                  },
                ];
              }
              return data;
            },
            defaultExpandAll: true,
            leafOnly: true,
            props: {
              label: 'name',
              value: 'key',
            },
            multiple: true,
            rules: [
              {
                required: true,
                message: '请选择流程',
                trigger: 'blur',
              },
            ],
            span: 24,
            overHidden: true,
            search: true,
            display: true,
            filterable: true,
          },
          {
            label: '类型',
            prop: 'type',
            type: 'radio',
            dicData: [
              {
                label: '永久',
                value: '1',
              },
              {
                label: '定时',
                value: '2',
              },
            ],
            rules: [
              {
                required: true,
                message: '请选择类型',
                trigger: 'blur',
              },
            ],
            change: ({ value }) => {
              const time = this.findObject(this.option.column, 'time');
              if (value == '2') {
                time.display = true;
              } else {
                time.display = false;
              }
            },
            span: 24,
            search: true,
            width: 120,
            overHidden: true,
          },
          {
            label: '有效时间',
            prop: 'time',
            type: 'datetimerange',
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            rules: [
              {
                required: true,
                message: '请选择有效时间',
                trigger: 'blur',
              },
            ],
            display: false,
            span: 24,
            overHidden: true,
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
            value: 1,
            search: true,
            span: 24,
            width: 100,
          },
        ],
      },
      data: [],
      userList: [],
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.wf_ops_proxy_add, false),
        viewBtn: this.validData(this.permission.wf_ops_proxy_view, false),
        delBtn: this.validData(this.permission.wf_ops_proxy_delete, false),
        editBtn: this.validData(this.permission.wf_ops_proxy_edit, false),
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
    userList(1, -1).then(res => {
      this.userList = res.data.data.records;
    });
  },
  methods: {
    getUserName(id) {
      const user = this.userList.find(u => u.id == id);
      if (user) return user.realName;
      return '';
    },
    rowSave(row, done, loading) {
      if (row.type == '2' && row.time && row.time.length > 0) {
        const startTime = row.time[0];
        const endTime = row.time[1];
        row.startTime = startTime;
        row.endTime = endTime;
        delete row.time;
      }
      add(row).then(
        res => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: res.data.msg,
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
      if (row.type == '2' && row.time && row.time.length > 0) {
        const startTime = row.time[0];
        const endTime = row.time[1];
        row.startTime = startTime;
        row.endTime = endTime;
        delete row.time;
      }
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
      const processDefKey = this.findObject(this.option.column, 'processDefKey');
      if (['edit'].includes(type)) {
        processDefKey.multiple = false;
        if (this.form.processDefKey == 'WF_ALL_PROCESS') this.form.process = '1';
        else this.form.process = '2';
      } else if (['add'].includes(type)) {
        this.form.process = '1';
        processDefKey.multiple = true;
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
