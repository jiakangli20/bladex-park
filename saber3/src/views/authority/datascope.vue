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
          v-if="permission.data_scope_setting"
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

    <!-- ========== 数据权限配置抽屉（树形模式） ========== -->
    <el-drawer
      :title="`[${scopeMenuName}] 数据权限配置`"
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
  addDataScope,
  removeDataScope,
  updateDataScope,
  getListDataScope,
  getMenuDataScope,
} from '@/api/system/scope';
import { mapGetters } from 'vuex';
import func from '@/utils/func';

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
                message: '请输入数据权限名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限编号',
            prop: 'resourceCode',
            search: true,
            width: 120,
            rules: [
              {
                required: true,
                message: '请输入数据权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限字段',
            prop: 'scopeColumn',
            width: 130,
            rules: [
              {
                required: true,
                message: '请输入权限字段',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '规则类型',
            type: 'select',
            dicUrl: '/blade-system/dict/dictionary?code=data_scope_type',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            width: 140,
            prop: 'scopeType',
            rules: [
              {
                required: true,
                message: '请选择规则类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '可见字段',
            prop: 'scopeField',
            span: 24,
            hide: true,
            value: '*',
            rules: [
              {
                required: true,
                message: '请输入数据权限可见的字段',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限类名',
            prop: 'scopeClass',
            span: 24,
            hide: true,
            rules: [
              {
                required: true,
                message: '请输入MybatisMapper对应方法的完整类名路径',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '规则值',
            prop: 'scopeValue',
            span: 24,
            minRows: 5,
            type: 'textarea',
            display: false,
            hide: true,
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
      // ========== 数据权限抽屉（树形模式） ==========
      drawerVisible: false,
      direction: 'rtl',
      scopeMenuId: 0,
      scopeMenuCode: '',
      scopeMenuName: '菜单',
      scopeLoading: false,
      watchMode: true,
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
            value: '',
            rules: [
              {
                required: true,
                message: '请输入数据权限名称',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限编号',
            prop: 'resourceCode',
            search: true,
            width: 100,
            rules: [
              {
                required: true,
                message: '请输入数据权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限字段',
            prop: 'scopeColumn',
            width: 130,
            rules: [
              {
                required: true,
                message: '请输入数据权限编号',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '规则类型',
            type: 'select',
            dicUrl: '/blade-system/dict/dictionary?code=data_scope_type',
            props: {
              label: 'dictValue',
              value: 'dictKey',
            },
            dataType: 'number',
            slot: true,
            width: 140,
            prop: 'scopeType',
            rules: [
              {
                required: true,
                message: '请输入通知类型',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '可见字段',
            prop: 'scopeField',
            span: 24,
            hide: true,
            value: '*',
            rules: [
              {
                required: true,
                message: '请输入数据权限可见的字段',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '权限类名',
            prop: 'scopeClass',
            span: 24,
            hide: true,
            rules: [
              {
                required: true,
                message: '请输入MybatisMapper对应方法的完整类名路径',
                trigger: 'blur',
              },
            ],
          },
          {
            label: '规则值',
            prop: 'scopeValue',
            span: 24,
            minRows: 5,
            type: 'textarea',
            display: true,
            hide: true,
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
    'formList.scopeType'() {
      this.initListScope();
    },
    'formScope.scopeType'() {
      this.initScope();
    },
    viewMode() {
      this.drawerVisible = false;
    },
  },
  computed: {
    ...mapGetters(['permission']),
    permissionListScope() {
      return {
        addBtn: this.validData(this.permission.data_scope_setting, false),
        viewBtn: true,
        delBtn: this.validData(this.permission.data_scope_setting, false),
        editBtn: this.validData(this.permission.data_scope_setting, false),
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
    initListScope() {
      const scopeType = func.toInt(this.formList.scopeType);
      const column = this.$refs.crudList?.option?.column;
      if (column) {
        column.forEach(item => {
          if (item.prop === 'scopeValue') {
            item.display = scopeType === 5;
          }
        });
      }
    },
    rowSaveList(row, done, loading) {
      addDataScope(row).then(
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
      updateDataScope(row).then(
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
          return removeDataScope(row.id);
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
          return removeDataScope(this.listIds);
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
        getMenuDataScope(this.formList.id).then(res => {
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
      getListDataScope(page.currentPage, page.pageSize, Object.assign(params, this.queryList)).then(
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
      this.scopeMenuCode = row.code;
      this.scopeMenuName = row.name;
      this.onLoadScope(this.pageScope);
    },
    handleDrawerClose(hide) {
      hide();
    },

    // ==================== 抽屉内权限 CRUD ====================
    initScope() {
      const scopeType = func.toInt(this.formScope.scopeType);
      const watchMode = this.watchMode;
      let column = '-',
        name = '暂无';
      if (scopeType === 1) {
        column = '-';
        name = '全部可见';
      } else if (scopeType === 2) {
        column = 'create_user';
        name = '本人可见';
      } else if (scopeType === 3) {
        column = 'create_dept';
        name = '所在机构可见';
      } else if (scopeType === 4) {
        column = 'create_dept';
        name = '所在机构可见及子级可见';
      } else if (scopeType === 5) {
        column = '';
        name = '自定义';
      }
      this.$refs.crudScope.option.column.filter(item => {
        if (watchMode) {
          if (item.prop === 'scopeName') {
            this.formScope.scopeName = `${this.scopeMenuName} [${name}]`;
          }
          if (item.prop === 'resourceCode') {
            this.formScope.resourceCode = this.scopeMenuCode;
          }
          if (item.prop === 'scopeColumn') {
            this.formScope.scopeColumn = column;
          }
        }
        if (item.prop === 'scopeValue') {
          item.display = scopeType === 5;
        }
      });
    },
    rowSaveScope(row, done, loading) {
      row = {
        ...row,
        menuId: this.scopeMenuId,
      };
      addDataScope(row).then(
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
      updateDataScope(row).then(
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
          return removeDataScope(row.id);
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
          return removeDataScope(this.scopeIds);
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
        this.watchMode = true;
        this.initScope();
      }
      if (['edit', 'view'].includes(type)) {
        this.watchMode = false;
        getMenuDataScope(this.formScope.id).then(res => {
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
      getListDataScope(page.currentPage, page.pageSize, values).then(res => {
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
