<template>
  <el-dialog
    ref="nf-dialog"
    class="nf-dialog"
    v-model="visible"
    title="人员选择"
    width="60%"
    :before-close="handleClose"
    append-to-body
  >
    <nf-crud
      v-if="isInit && visible"
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      v-model="form"
      ref="crud"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionList = $event"
      @current-change="page.currentPage = $event"
      @size-change="page.pageSize = $event"
      @row-click="rowClick"
      @on-load="onLoad"
    >
      <template v-if="checkType == 'radio'" #radio="{ row }">
        <el-radio v-model="form.radio" :value="row.id"><i></i></el-radio>
      </template>
    </nf-crud>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose" size="default">取 消</el-button>
        <el-button type="primary" @click="handleConfirm" size="default">确 定</el-button>
      </span>
    </template>
  </el-dialog>
</template>
<script>
import { getUser } from '@/api/system/user';

export default {
  props: {
    defaultChecked: String,
    userUrl: {
      type: String,
      default: () => {
        return '/blade-system/search/user';
      },
    },
    customOption: Object,
    checkType: {
      type: String,
      default: () => {
        return 'radio';
      },
    },
  },
  watch: {
    checkType: {
      handler(val) {
        if (val == 'radio') {
          this.option.selection = false;
          this.findObject(this.option.column, 'radio').hide = false;
        } else {
          this.option.selection = true;
          this.findObject(this.option.column, 'radio').hide = true;
        }
      },
      immediate: true,
    },
  },
  computed: {
    ids() {
      let ids = new Set();
      this.selectionList.forEach(ele => {
        ids.add(ele.id);
      });
      return Array.from(ids).join(',');
    },
    names() {
      let names = new Set();
      this.selectionList.forEach(ele => {
        names.add(ele.realName);
      });
      return Array.from(names).join(',');
    },
  },
  data() {
    return {
      isInit: false,
      visible: false,
      form: {},
      query: {},
      loading: false,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      data: [],
      props: {
        id: 'id',
        name: 'realName',
        records: 'data.data.records',
        total: 'data.data.total',
      },
      option: {
        size: 'default',
        searchSize: 'default',
        align: 'center',
        menu: false,
        addBtn: false,
        header: false,
        border: true,
        tip: false,
        reserveSelection: true,
        highlightCurrentRow: true,
        gutter: 5,
        searchMenuSpan: 6,
        selection: true,
        column: [
          {
            label: '',
            prop: 'radio',
            type: 'radio',
            width: 55,
            hide: true,
          },
          {
            label: '头像',
            prop: 'avatar',
            type: 'upload',
            width: 90,
          },
          {
            label: '姓名',
            prop: 'name',
            overHidden: true,
            search: true,
          },
          {
            label: '部门',
            prop: 'deptName',
            overHidden: true,
            search: true,
          },
          {
            label: '职位',
            prop: 'postName',
            overHidden: true,
            search: true,
          },
        ],
      },
    };
  },
  mounted() {
    this.init();
  },
  methods: {
    init() {
      if (!this.isInit) {
        if (this.customOption) {
          const { column, userProps } = this.customOption;
          if (column) this.option.column = column;
          if (userProps) this.props = userProps;
        }
        this.isInit = true;
      }
    },
    handleConfirm() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$emit('onConfirm', this.ids, this.names);
      this.handleClose();
    },
    handleClose(done) {
      // this.selectionClear()
      this.visible = false;
      if (done && typeof done == 'function') done();
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
    selectionClear() {
      this.selectionList = [];
      if (this.$refs.crud) this.$refs.crud.toggleSelection();
    },
    rowClick(row) {
      if (this.checkType == 'radio') {
        this.selectionList = [row];
        this.form.radio = row.id;
      } else this.$refs.crud.toggleSelection([row]);
    },
    async changeDefaultChecked() {
      if (!this.defaultChecked) return;
      let defaultChecked = this.defaultChecked;

      if (this.checkType == 'checkbox') {
        // this.selectionClear()
        const checks = defaultChecked.split(',');
        if (checks.length > 0) {
          setTimeout(() => {
            checks.forEach(async c => {
              let row = this.data.find(d => d.id == c); // 当前页查找
              if (!row) {
                row = this.selectionList.find(d => d.id == c); // 勾选列表查找
                if (!row) {
                  let res = await getUser(c); // 接口查找
                  if (res.data.data) row = res.data.data;
                }
              }
              if (row && this.$refs.crud) this.$refs.crud.toggleRowSelection(row, true);
            });
          }, 500);
        }
      } else {
        let row = this.data.find(d => d.id == defaultChecked);
        if (!row) {
          let res = await getUser(defaultChecked);
          if (res.data.data) row = res.data.data;
        }

        if (row) {
          this.selectionList = [row];
          this.form.radio = defaultChecked;
        } else {
          this.selectionList = [];
          this.form.radio = '';
        }
      }
    },
    onLoad(page, params = {}) {
      this.loading = true;
      const param = {
        current: page.currentPage,
        size: page.pageSize,
        ...Object.assign(params, this.query),
      };
      this.$axios.get(this.userUrl, { params: param }).then(res => {
        this.page.total = this.getAsVal(res, this.props.total);
        this.data = this.getAsVal(res, this.props.records) || [];
        this.loading = false;

        this.changeDefaultChecked();
      });
    },
    getAsVal(obj, bind = '') {
      let result = this.deepClone(obj);
      if (this.validatenull(bind)) return result;
      bind.split('.').forEach(ele => {
        if (!this.validatenull(result[ele])) {
          result = result[ele];
        } else {
          result = '';
        }
      });
      return result;
    },
  },
};
</script>
<style lang="scss">
.nf-dialog {
  display: flex;
  flex-direction: column;
  margin: 0 !important;
  position: absolute;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -40%);
  max-height: calc(100% - 30px);
  max-width: calc(100% - 30px);
  .el-dialog__body {
    flex: 1;
    overflow: auto;
  }
}
</style>
