<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      :option="option"
      v-model:search="search"
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
      <template #menu-left>
        <el-button
          type="danger"
          icon="el-icon-delete"
          plain
          v-if="permission.wf_form_theme_delete"
          @click="handleDelete"
        >
          删 除
        </el-button>
      </template>
      <template #menu="{ row }">
        <el-button type="primary" text icon="el-icon-view" @click="handlePreview(row)">
          预览
        </el-button>
      </template>
    </nf-crud>

    <el-drawer title="预览" v-model="previewVisible" append-to-body destroy-on-close size="50%">
      <div class="nf-theme-custom">
        <nf-form v-model="previewForm" :option="previewOption" :style="themeCustomStyle"></nf-form>
      </div>
    </el-drawer>
  </nf-container>
</template>

<script>
import { getList, getDetail, add, update, remove } from '../../api/design/form-theme';
import { mapGetters } from 'vuex';
import theme from '../../mixins/theme';
import module from '../../mixins/module';

export default {
  mixins: [theme, module()],
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
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: false,
        selection: true,
        dialogClickModal: false,
        dialogType: 'drawer',
        dialogWidth: '50%',
        labelWidth: 140,
        menuWidth: 240,
        column: [
          {
            label: '名称',
            prop: 'name',
            type: 'input',
            rules: [
              {
                required: true,
                message: '请输入名称',
                trigger: 'blur',
              },
            ],
            search: true,
          },
          {
            label: '主题key',
            prop: 'themeKey',
            type: 'input',
            rules: [
              {
                required: true,
                message: '请输入主题key',
                trigger: 'blur',
              },
            ],
            search: true,
          },
          {
            label: '边框宽度',
            prop: 'borderWidth',
            type: 'input',
            hide: true,
            span: 24,
          },
          {
            label: '边框颜色',
            prop: 'borderColor',
            type: 'color',
            hide: true,
            span: 24,
          },
          {
            label: 'label字体颜色',
            prop: 'labelColor',
            type: 'color',
            hide: true,
            span: 24,
          },
          {
            label: 'label背景颜色',
            prop: 'labelBg',
            type: 'color',
            hide: true,
            span: 24,
          },
          // {
          //   label: "label宽度",
          //   prop: "labelWidth",
          //   type: "input",
          //   hide: true,
          //   span: 24,
          // },
          {
            label: 'label字体大小',
            prop: 'labelFontSize',
            type: 'input',
            hide: true,
            span: 24,
          },
          {
            label: 'value字体颜色',
            prop: 'valueColor',
            type: 'color',
            hide: true,
            span: 24,
          },
          {
            label: 'value背景颜色',
            prop: 'valueBg',
            type: 'color',
            hide: true,
            span: 24,
          },
          {
            label: 'value字体大小',
            prop: 'valueFontSize',
            type: 'input',
            hide: true,
            span: 24,
          },
        ],
      },
      data: [],
      previewVisible: false,
      previewForm: {},
      previewOption: {
        detail: true,
        // span: 24,
        column: [
          {
            type: 'input',
            label: '输入框',
            prop: 'input',
            value: '输入框',
          },
          {
            type: 'select',
            label: '下拉框',
            prop: 'select',
            value: '下拉框',
          },
          {
            type: 'radio',
            label: '单选框',
            prop: 'radio',
            value: '单选框1',
            dicData: [
              {
                label: '单选框1',
                value: '单选框1',
              },
              {
                label: '单选框2',
                value: '单选框2',
              },
            ],
          },
          {
            type: 'checkbox',
            label: '多选框',
            prop: 'checkbox',
            value: ['多选框1', '多选框2'],
            dicData: [
              {
                label: '多选框1',
                value: '多选框1',
              },
              {
                label: '多选框2',
                value: '多选框2',
              },
            ],
          },
          {
            type: 'datetime',
            label: '日期时间',
            prop: 'date',
            value: '2020-01-01 12:00:00',
          },
          {
            type: 'slider',
            label: '滑块',
            prop: 'slider',
            range: true,
            value: [20, 50],
          },
          {
            type: 'rate',
            label: '评分',
            prop: 'rate',
            value: 3,
          },
          {
            type: 'switch',
            label: '开关',
            prop: 'switch',
            value: true,
          },
          {
            type: 'ueditor',
            label: '富文本',
            prop: 'editor',
            span: 24,
          },
          {
            type: 'dynamic',
            label: '子表单',
            prop: 'dynamic',
            span: 24,
            value: [
              {
                input1: '输入框1',
                input2: '输入框2',
              },
            ],
            children: {
              addBtn: false,
              align: 'center',
              column: [
                {
                  type: 'input',
                  label: '输入框1',
                  prop: 'input1',
                },
                {
                  type: 'input',
                  label: '输入框2',
                  prop: 'input2',
                },
              ],
            },
          },
        ],
        group: [
          {
            label: '分组1',
            column: [
              {
                type: 'input',
                label: '输入框',
                prop: 'input3',
                value: '输入框',
              },
              {
                type: 'select',
                label: '下拉框',
                prop: 'select3',
                value: '下拉框',
              },
            ],
          },
        ],
      },
      theme: 'default',
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.wf_form_theme_add, false),
        viewBtn: this.validData(this.permission.wf_form_theme_view, false),
        delBtn: this.validData(this.permission.wf_form_theme_delete, false),
        editBtn: this.validData(this.permission.wf_form_theme_edit, false),
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
    handlePreview(row) {
      this.theme = row.themeKey;
      this.setThemeStyle();
      this.previewVisible = true;
    },
    rowSave(row, done, loading) {
      add(row).then(
        () => {
          this.onLoad(this.page);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.initTheme();
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
          this.initTheme();
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
          this.initTheme();
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
          this.initTheme();
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crud.toggleSelection();
        });
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getDetail({ id: this.form.id }).then(res => {
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
