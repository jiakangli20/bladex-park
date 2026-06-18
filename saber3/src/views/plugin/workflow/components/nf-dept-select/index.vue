<template>
  <el-dialog
    ref="nf-dialog"
    class="nf-dialog"
    v-model="visible"
    title="部门选择"
    width="60%"
    :before-close="handleClose"
    append-to-body
  >
    <nf-crud
      v-if="isInit && visible"
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model="form"
      ref="crud"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionList = $event"
      @row-click="rowClick"
      @on-load="onLoad"
      @tree-load="treeLoad"
    >
    </nf-crud>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose" size="default">取消</el-button>
        <el-button type="primary" @click="handleConfirm" size="default">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>
<script>
export default {
  props: {
    defaultChecked: String,
    url: String,
    customOption: Object,
  },
  watch: {
    visible: {
      handler(val) {
        if (val) this.changeDefaultChecked();
      },
    },
  },
  computed: {
    ids() {
      let ids = [];
      this.selectionList.forEach(ele => {
        ids.push(ele[this.props.id]);
      });
      return ids.join(',');
    },
    names() {
      let names = [];
      this.selectionList.forEach(ele => {
        names.push(ele[this.props.name]);
      });
      return names.join(',');
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
        name: 'deptName',
        data: 'data.data',
      },
      option: {
        size: 'default',
        searchSize: 'default',
        searchMenuSpan: 6,
        menu: false,
        addBtn: false,
        header: false,
        border: true,
        tip: false,
        tree: true,
        lazy: true,
        reserveSelection: true,
        highlightCurrentRow: true,
        gutter: 1,
        selection: true,
        column: [
          {
            label: '部门名称',
            prop: 'deptName',
            overHidden: true,
            search: true,
          },
          {
            label: '部门全称',
            prop: 'fullName',
            overHidden: true,
            search: true,
          },
          {
            label: '部门分类',
            prop: 'deptCategoryName',
            overHidden: true,
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
          const { deptColumn, deptProps } = this.customOption;
          if (deptColumn) this.option.column = deptColumn;
          if (deptProps) this.props = deptProps;
        }
        this.isInit = true;
      }
    },
    handleConfirm() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请至少选择一项');
        return;
      }
      this.$emit('confirm', this.ids, this.names);
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
      this.query = {
        ...params,
        parentId: '',
      };
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
    },
    selectionClear() {
      this.selectionList = [];
      if (this.$refs.crud) this.$refs.crud.toggleSelection();
    },
    rowClick(row) {
      this.$refs.crud.toggleSelection([row]);
    },
    changeDefaultChecked() {
      // this.selectionClear()
      if (this.defaultChecked) {
        const checks = this.defaultChecked.split(',');
        if (checks.length > 0) {
          setTimeout(() => {
            checks.forEach(c => {
              let row = this.findRow(this.data, c);
              if (!row) {
                row = this.selectionList.find(d => d.id == c); // 勾选列表查找
              }
              if (row && this.$refs.crud) this.$refs.crud.toggleRowSelection(row, true);
            });
          }, 500);
        }
      }
    },
    findRow(list, id) {
      if (!list) return;
      for (let i = 0; i < list.length; i++) {
        const data = list[i];
        if (data.id == id) return data;
        else if (data.children) return this.findRow(data.children, id);
      }
    },
    onLoad(page, params = {}) {
      this.loading = true;
      const param = {
        parentId: 0,
        ...Object.assign(params, this.query),
      };
      this.$axios.get(this.url, { params: param }).then(res => {
        this.data = this.getAsVal(res, this.props.data) || [];
        this.loading = false;

        this.changeDefaultChecked();
      });
    },
    treeLoad(tree, treeNode, resolve) {
      const parentId = tree.id;
      this.$axios.get(this.url, { params: { parentId } }).then(res => {
        const list = this.getAsVal(res, this.props.data) || [];
        resolve(list);
        const parent = this.findRow(this.data, parentId);
        parent.children = list;
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
