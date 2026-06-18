<template>
  <nf-container v-if="moduleLoadInit">
    <nf-crud
      :option="option"
      :table-loading="loading"
      :data="data"
      ref="crud"
      v-model="form"
      v-model:page="page"
      :permission="permissionList"
      @row-del="rowDel"
      @search-change="searchChange"
      @search-reset="searchReset"
      @selection-change="selectionChange"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="onLoad(page, query)"
      @on-load="onLoad"
      @tree-node-click="nodeClick"
      @tree-node-load="findObject(option.column, 'categoryId').dicData = $event"
    >
      <template #menu-left>
        <el-button
          type="primary"
          size="default"
          icon="el-icon-plus"
          v-if="permission.wf_design_model_add"
          @click="handleDesign({})"
        >
          新 增
        </el-button>
        <el-button
          type="success"
          size="default"
          icon="el-icon-connection"
          v-if="permission.wf_design_model_change_category"
          @click="handleChangeCategory"
        >
          更改分类
        </el-button>
      </template>
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_design_model_design"
          text
          type="primary"
          icon="el-icon-edit"
          size="default"
          @click="handleDesign(row)"
        >
          设计
        </el-button>
        <el-button
          v-if="permission.wf_design_model_deploy"
          text
          type="primary"
          icon="el-icon-promotion"
          size="default"
          @click="handleDeploy(row)"
        >
          部署
        </el-button>
        <el-button
          v-if="permission.wf_design_model_scope"
          text
          type="primary"
          size="default"
          icon="el-icon-lock"
          @click="handleScope(row)"
        >
          权限
        </el-button>
        <el-dropdown style="margin-left: 10px">
          <el-button size="default" text type="primary">
            更多操作
            <el-icon class="el-icon-right">
              <el-icon-arrow-down />
            </el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="permission.wf_design_model_icon" @click="handleIcon(row)">
                <el-button text type="primary" size="default" icon="el-icon-warning">
                  图标
                </el-button>
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_design_model_history"
                @click="handleHistory(row)"
              >
                <el-button text type="primary" size="default" icon="el-icon-clock">
                  历史
                </el-button>
              </el-dropdown-item>
              <el-dropdown-item v-if="permission.wf_design_model_copy" @click="handleCopy(row)">
                <el-button text type="primary" size="default" icon="el-icon-document-copy">
                  拷贝
                </el-button>
              </el-dropdown-item>
              <el-dropdown-item v-if="permission.wf_design_model_delete" @click="rowDel(row)">
                <el-button
                  style="color: #f56c6c"
                  text
                  type="primary"
                  size="default"
                  icon="el-icon-delete"
                >
                  删除
                </el-button>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </nf-crud>

    <el-dialog v-model="categoryVisible" append-to-body destroy-on-close title="选择分类">
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
        @submit="handleDeploySubmit"
      ></nf-form>
    </el-dialog>

    <el-dialog v-model="iconVisible" append-to-body destroy-on-close title="上传图标">
      <nf-form
        v-model="form"
        :option="{
          column: [
            {
              label: '图标',
              type: 'upload',
              propsHttp: { res: 'data', url: 'link', name: 'originalName' },
              tip: '建议尺寸：50 * 50',
              action: '/blade-resource/oss/endpoint/put-file',
              display: true,
              showFileList: true,
              prop: 'icon',
              listType: 'picture-img',
              required: true,
              rules: [{ required: true, message: '图标必须填写' }],
              accept: 'image/*',
            },
          ],
        }"
        @submit="handleIconSubmit"
      ></nf-form>
    </el-dialog>

    <nf-user-option
      ref="userOption"
      :user-option="userOption"
      all
      @submit="handleScopeSubmit"
    ></nf-user-option>
  </nf-container>
</template>

<script>
import {
  getList,
  remove,
  deploy,
  changeCategory,
  getDetail,
  changeIcon,
} from '../../api/design/model';
import { getList as scopeList, submit as scopeSubmit } from '../../api/design/model-scope';

import { mapGetters } from 'vuex';
import module from '../../mixins/module';

import NfUserOption from '../../components/nf-user-option/index.vue';

export default {
  components: { NfUserOption },
  mixins: [module(['d'])],
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
        height: 'auto',
        calcHeight: 30,
        tip: false,
        border: true,
        index: true,
        viewBtn: false,
        addBtn: false,
        editBtn: false,
        delBtn: false,
        selection: true,
        dialogType: 'drawer',
        align: 'center',
        searchMenuSpan: 6,
        menuWidth: 300,
        column: [
          {
            label: '图标',
            prop: 'icon',
            type: 'upload',
            width: 80,
          },
          {
            label: '模型key',
            prop: 'modelKey',
            overHidden: true,
            search: true,
          },
          {
            label: '模型名称',
            prop: 'name',
            overHidden: true,
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
            label: '描述',
            prop: 'description',
            overHidden: true,
          },
          {
            label: '版本',
            prop: 'version',
            width: 80,
          },
        ],
      },
      data: [],
      row: '',
      categoryVisible: false,
      userOption: {
        userUrl: '/blade-system/search/user',
        roleUrl: '/blade-system/search/role',
        deptUrl: '/blade-system/search/dept',
        postUrl: '/blade-system/search/post',
      },
      iconVisible: false,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.wf_design_model_add, false),
        viewBtn: this.validData(this.permission.wf_design_model_view, false),
        delBtn: this.validData(this.permission.wf_design_model_delete, false),
        editBtn: this.validData(this.permission.wf_design_model_edit, false),
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
    handleIcon(row) {
      this.form = { id: row.id, icon: row.icon };
      this.iconVisible = true;
    },
    handleIconSubmit(form, done) {
      const { id, icon } = this.form;
      changeIcon({ id, icon }).then(() => {
        const row = this.data.find(d => d.id == id);
        if (row) row.icon = icon;
        this.$message.success('设置图标成功');
        this.iconVisible = false;
        done();
      });
    },
    handleCopy(row) {
      getDetail(row.id).then(res => {
        const { modelXml } = res.data.data;
        this.$Clipboard({
          text: modelXml,
        })
          .then(() => {
            this.$message.success('拷贝xml成功');
          })
          .catch(() => {});
      });
    },
    handleChangeCategory() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.categoryType = 'change';
      this.categoryVisible = true;
    },
    nodeClick({ id }) {
      this.categoryId = id;
      this.searchChange(this.query);
    },
    handleScopeSubmit(list, all) {
      list.forEach(l => (l.val = l.value));

      const { id, modelKey } = this.row;
      const param = {
        modelId: id,
        modelKey,
        scopeList: all ? list.concat([{ type: 'WF_ALL', val: 'WF_ALL' }]) : list,
      };
      scopeSubmit(param).then(() => {
        this.$message.success('操作成功');
      });
    },
    handleScope(row) {
      scopeList({ modelId: row.id }).then(res => {
        const data = res.data.data;
        const index = data.findIndex(d => d.type == 'WF_ALL');
        if (index != -1) {
          this.$refs.userOption.switchAll = true;
          data.splice(index, 1);
        } else this.$refs.userOption.switchAll = false;

        this.userOption.data = data.map(d => {
          return {
            value: d.val,
            text: d.text,
            type: d.type,
          };
        });
        this.row = row;
        this.$refs.userOption.visible = true;
      });
    },
    handleDeploySubmit(form, done) {
      const { id, category } = form;
      if (this.categoryType == 'change') {
        changeCategory({ ids: this.ids, category }).then(() => {
          this.$message.success('修改成功');
          this.categoryType = '';
          done();
          this.categoryVisible = false;
          this.onLoad(this.page, this.query);
        });
      } else {
        deploy({ id, category }).then(() => {
          this.$message.success('部署成功');
          done();
          this.categoryVisible = false;
        });
      }
    },
    handleDeploy(row) {
      this.form.id = row.id;
      this.form.category = row.categoryId;
      this.categoryVisible = true;
    },
    handleDesign(row) {
      this.$router.push('/plugin/workflow/pages/design/process/' + (row.id || 0));
    },
    handleHistory(row) {
      this.$router.push('/plugin/workflow/pages/design/model-history/' + row.id);
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
          return remove({ id: this.ids });
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

<style lang="scss" scoped>
:deep(.nf-crud__img) {
  img {
    width: 32px;
    height: 32px;
  }
}
</style>
