<template>
  <basic-container>
    <!-- ========== 列表模式 ========== -->
    <avue-crud
      v-if="viewMode === 'list'"
      ref="crudList"
      v-model="formList"
      :option="optionList"
      :table-loading="listLoading"
      :data="dataList"
      :page="pageList"
      :permission="permissionListScope"
      :before-open="beforeOpenList"
      @row-del="rowDelList"
      @row-update="rowUpdateList"
      @row-save="rowSaveList"
      @search-change="searchChangeList"
      @search-reset="searchResetList"
      @selection-change="selectionChangeList"
      @current-change="currentChangeList"
      @size-change="sizeChangeList"
      @refresh-change="refreshChangeList"
      @on-load="onLoadList"
    >
      <template #menu-left>
        <el-button type="danger" icon="el-icon-delete" plain @click="handleDeleteList"
          >删 除
        </el-button>
      </template>
      <template #menu-right>
        <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'"
          >列表模式
        </el-button>
        <el-button :type="viewMode === 'tree' ? 'primary' : ''" @click="viewMode = 'tree'"
          >树形模式
        </el-button>
      </template>
      <template #scopeType="{ row }">
        <el-tag>{{ row.scopeTypeName }}</el-tag>
      </template>
    </avue-crud>

    <!-- ========== 树形模式 ========== -->
    <avue-crud
      v-if="viewMode === 'tree'"
      ref="crudTree"
      :option="optionTree"
      :table-loading="treeLoading"
      :data="dataTree"
      @search-change="searchChangeTree"
      @search-reset="searchResetTree"
      @refresh-change="refreshChangeTree"
      @on-load="onLoadTree"
      @tree-load="treeLoad"
    >
      <template #menu-right>
        <el-button :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'"
          >列表模式
        </el-button>
        <el-button :type="viewMode === 'tree' ? 'primary' : ''" @click="viewMode = 'tree'"
          >树形模式
        </el-button>
      </template>
      <template #menu="{ row }">
        <el-button
          type="primary"
          text
          icon="el-icon-setting"
          v-if="permission.api_scope_setting"
          plain
          style="border: 0; background-color: transparent !important"
          @click.stop="handleDataScope(row)"
          >权限配置
        </el-button>
      </template>
      <template #name="{ row }">
        <i :class="row.source" style="margin-right: 5px" />
        <span>{{ row.name }}</span>
      </template>
      <template #source="{ row }">
        <div style="text-align: center">
          <i :class="row.source" />
        </div>
      </template>
    </avue-crud>

    <!-- ========== 接口权限配置抽屉（树形模式） ========== -->
    <el-drawer
      :title="`[${scopeMenuName}] 接口权限配置`"
      v-model="drawerVisible"
      :direction="direction"
      append-to-body
      :before-close="handleDrawerClose"
      size="1200px"
    >
      <basic-container>
        <avue-crud
          :option="optionScope"
          :data="dataScope"
          :page="pageScope"
          v-model="formScope"
          :table-loading="scopeLoading"
          ref="crudScope"
          @row-del="rowDelScope"
          @row-update="rowUpdateScope"
          @row-save="rowSaveScope"
          :before-open="beforeOpenScope"
          @search-change="searchChangeScope"
          @search-reset="searchResetScope"
          @selection-change="selectionChangeScope"
          @current-change="currentChangeScope"
          @size-change="sizeChangeScope"
          @on-load="onLoadScope"
        >
          <template #menu-left>
            <el-button type="danger" icon="el-icon-delete" plain @click="handleDeleteScope"
              >删 除
            </el-button>
          </template>
          <template #scopeType="{ row }">
            <el-tag>{{ row.scopeTypeName }}</el-tag>
          </template>
        </avue-crud>
      </basic-container>
    </el-drawer>
  </basic-container>
</template>

<script>
import { getLazyMenuList } from '@/api/system/menu';
import {
  addApiScope,
  removeApiScope,
  updateApiScope,
  getListApiScope,
  getMenuApiScope,
} from '@/api/system/scope';
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      viewMode: 'list',
      // ========== 列表模式 ==========
      formList: {},
      queryList: {},
      selectionListData: [],
      listLoading: true,
      dataList: [],
      pageList: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      optionList: {
        height: 'auto',
        calcHeight: 32,
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: true,
        selection: true,
        labelWidth: 120,
        searchLabelWidth: 120,
        menuWidth: 250,
        dialogWidth: 900,
        dialogClickModal: false,
        column: [
          {
            label: '所属菜单',
            labelTip: '仅用于权限规则的分组归类，不参与实际鉴权与数据过滤逻辑',
            prop: 'menuId',
            type: 'tree',
            dicUrl: '/blade-system/menu/tree',
            props: {
              label: 'title',
            },
            search: true,
            span: 24,
            width: 200,
            rules: [
              {
                required: true,
                message: '请选择所属菜单',
                trigger: 'click',
              },
            ],
          },
          {
            label: '权限名称',
            prop: 'scopeName',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入权限名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限编号',
            prop: 'resourceCode',
            search: true,
            width: 180,
            rules: [
              {
                required: true,
                message: '请输入权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限路径',
            prop: 'scopePath',
            width: 180,
            rules: [
              {
                required: true,
                message: '请输入权限路径',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '接口类型',
            type: 'select',
            dicUrl: '/blade-system/dict/dictionary?code=api_scope_type',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            width: 100,
            prop: 'scopeType',
            rules: [
              {
                required: true,
                message: '请选择接口类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '备注',
            prop: 'remark',
            span: 24,
            hide: true,
          },
        ],
      },
      // ========== 树形模式 ==========
      dataTree: [],
      treeLoading: true,
      parentId: 0,
      queryTree: {},
      optionTree: {
        lazy: true,
        tip: false,
        simplePage: true,
        searchShow: true,
        searchMenuSpan: 6,
        tree: true,
        border: true,
        index: true,
        selection: true,
        viewBtn: false,
        editBtn: false,
        addBtn: false,
        delBtn: false,
        menuWidth: 200,
        dialogClickModal: false,
        column: [
          {
            label: '菜单名称',
            prop: 'name',
            slot: true,
            width: 300,
            search: true,
          },
          {
            label: '菜单编号',
            prop: 'code',
            search: true,
          },
          {
            label: '路由地址',
            prop: 'path',
          },
          {
            label: '菜单图标',
            prop: 'source',
            slot: true,
            width: 85,
          },
          {
            label: '菜单别名',
            prop: 'alias',
          },
          {
            label: '菜单排序',
            prop: 'sort',
            width: 85,
          },
        ],
      },
      // ========== 接口权限抽屉（树形模式） ==========
      drawerVisible: false,
      direction: 'rtl',
      scopeMenuId: 0,
      scopeMenuName: '菜单',
      scopeLoading: false,
      formScope: {},
      selectionListScope: [],
      pageScope: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      optionScope: {
        tip: false,
        searchShow: true,
        searchMenuSpan: 6,
        border: true,
        index: true,
        viewBtn: true,
        selection: true,
        labelWidth: 120,
        menuWidth: 250,
        dialogWidth: 900,
        dialogClickModal: false,
        column: [
          {
            label: '所属菜单',
            labelTip: '仅用于权限规则的分组归类，不参与实际鉴权与数据过滤逻辑',
            prop: 'menuId',
            type: 'tree',
            dicUrl: '/blade-system/menu/tree',
            props: {
              label: 'title',
            },
            span: 24,
            disabled: true,
            hide: true,
            rules: [
              {
                required: true,
                message: '请选择所属菜单',
                trigger: 'click',
              },
            ],
          },
          {
            label: '权限名称',
            prop: 'scopeName',
            search: true,
            rules: [
              {
                required: true,
                message: '请输入权限名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限编号',
            prop: 'resourceCode',
            search: true,
            width: 180,
            rules: [
              {
                required: true,
                message: '请输入权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限路径',
            prop: 'scopePath',
            width: 180,
            rules: [
              {
                required: true,
                message: '请输入权限路径',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '接口类型',
            type: 'select',
            dicUrl: '/blade-system/dict/dictionary?code=api_scope_type',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            width: 100,
            prop: 'scopeType',
            rules: [
              {
                required: true,
                message: '请选择接口类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '备注',
            prop: 'remark',
            span: 24,
            hide: true,
          },
        ],
      },
      dataScope: [],
    };
  },
  watch: {
    viewMode() {
      this.drawerVisible = false;
    },
  },
  computed: {
    ...mapGetters(['permission']),
    permissionListScope() {
      return {
        addBtn: this.validData(this.permission.api_scope_setting, false),
        viewBtn: true,
        delBtn: this.validData(this.permission.api_scope_setting, false),
        editBtn: this.validData(this.permission.api_scope_setting, false),
      };
    },
    listIds() {
      let ids = [];
      this.selectionListData.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
    scopeIds() {
      let ids = [];
      this.selectionListScope.forEach(ele => {
        ids.push(ele.id);
      });
      return ids.join(',');
    },
  },
  methods: {
    // ==================== 列表模式 ====================
    rowSaveList(row, done, loading) {
      addApiScope(row).then(
        () => {
          this.onLoadList(this.pageList);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowUpdateList(row, index, done, loading) {
      updateApiScope(row).then(
        () => {
          this.onLoadList(this.pageList);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowDelList(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return removeApiScope(row.id);
        })
        .then(() => {
          this.onLoadList(this.pageList);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    handleDeleteList() {
      if (this.selectionListData.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return removeApiScope(this.listIds);
        })
        .then(() => {
          this.onLoadList(this.pageList);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crudList.toggleSelection();
        });
    },
    beforeOpenList(done, type) {
      if (['edit', 'view'].includes(type)) {
        getMenuApiScope(this.formList.id).then(res => {
          this.formList = res.data.data;
        });
      }
      done();
    },
    searchResetList() {
      this.queryList = {};
      this.pageList.currentPage = 1;
      this.onLoadList(this.pageList);
    },
    searchChangeList(params, done) {
      this.queryList = params;
      this.pageList.currentPage = 1;
      this.onLoadList(this.pageList, params);
      done();
    },
    selectionChangeList(list) {
      this.selectionListData = list;
    },
    currentChangeList(currentPage) {
      this.pageList.currentPage = currentPage;
    },
    sizeChangeList(pageSize) {
      this.pageList.pageSize = pageSize;
    },
    refreshChangeList() {
      this.onLoadList(this.pageList, this.queryList);
    },
    onLoadList(page, params = {}) {
      this.listLoading = true;
      getListApiScope(page.currentPage, page.pageSize, Object.assign(params, this.queryList)).then(
        res => {
          const data = res.data.data;
          this.pageList.total = data.total;
          this.dataList = data.records;
          this.selectionListData = [];
          this.listLoading = false;
        }
      );
    },

    // ==================== 树形模式 ====================
    onLoadTree(page, params = {}) {
      this.treeLoading = true;
      getLazyMenuList(this.parentId, Object.assign(params, this.queryTree)).then(res => {
        this.dataTree = res.data.data;
        this.treeLoading = false;
      });
    },
    treeLoad(tree, treeNode, resolve) {
      const parentId = tree.id;
      getLazyMenuList(parentId).then(res => {
        resolve(res.data.data);
      });
    },
    searchResetTree() {
      this.queryTree = {};
      this.parentId = 0;
      this.onLoadTree();
    },
    searchChangeTree(params, done) {
      this.queryTree = params;
      this.parentId = '';
      this.onLoadTree({}, params);
      done();
    },
    refreshChangeTree() {
      this.onLoadTree({}, this.queryTree);
    },
    handleDataScope(row) {
      this.drawerVisible = true;
      this.scopeMenuId = row.id;
      this.scopeMenuName = row.name;
      this.onLoadScope(this.pageScope);
    },
    handleDrawerClose(hide) {
      hide();
    },

    // ==================== 抽屉内权限 CRUD ====================
    rowSaveScope(row, done, loading) {
      row = {
        ...row,
        menuId: this.scopeMenuId,
      };
      addApiScope(row).then(
        () => {
          this.onLoadScope(this.pageScope);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowUpdateScope(row, index, done, loading) {
      row = {
        ...row,
        menuId: this.scopeMenuId,
      };
      updateApiScope(row).then(
        () => {
          this.onLoadScope(this.pageScope);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          done();
        },
        error => {
          window.console.log(error);
          loading();
        }
      );
    },
    rowDelScope(row) {
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return removeApiScope(row.id);
        })
        .then(() => {
          this.onLoadScope(this.pageScope);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
        });
    },
    handleDeleteScope() {
      if (this.selectionListScope.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return removeApiScope(this.scopeIds);
        })
        .then(() => {
          this.onLoadScope(this.pageScope);
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crudScope.toggleSelection();
        });
    },
    beforeOpenScope(done, type) {
      if (['add'].includes(type)) {
        this.formScope.menuId = this.scopeMenuId;
      }
      if (['edit', 'view'].includes(type)) {
        getMenuApiScope(this.formScope.id).then(res => {
          this.formScope = res.data.data;
        });
      }
      done();
    },
    searchResetScope() {
      this.onLoadScope(this.pageScope);
    },
    searchChangeScope(params, done) {
      this.onLoadScope(this.pageScope, params);
      done();
    },
    selectionChangeScope(list) {
      this.selectionListScope = list;
    },
    currentChangeScope(currentPage) {
      this.pageScope.currentPage = currentPage;
    },
    sizeChangeScope(pageSize) {
      this.pageScope.pageSize = pageSize;
    },
    onLoadScope(page, params = {}) {
      this.scopeLoading = true;
      const values = {
        ...params,
        menuId: this.scopeMenuId,
      };
      getListApiScope(page.currentPage, page.pageSize, values).then(res => {
        const data = res.data.data;
        this.pageScope.total = data.total;
        this.dataScope = data.records;
        this.selectionListScope = [];
        this.scopeLoading = false;
      });
    },
  },
};
</script>
