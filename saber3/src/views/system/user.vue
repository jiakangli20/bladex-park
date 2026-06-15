<template>
  <el-row>
    <el-col :span="5">
      <div class="box">
        <el-scrollbar>
          <basic-container>
            <avue-tree :option="treeOption" :data="treeData" @node-click="nodeClick" />
          </basic-container>
        </el-scrollbar>
      </div>
    </el-col>
    <el-col :span="19">
      <basic-container>
        <avue-crud
          :option="userOption"
          v-model:search="search"
          :table-loading="loading"
          :data="data"
          ref="crud"
          v-model="form"
          :permission="permissionList"
          @row-del="rowDel"
          @row-update="rowUpdate"
          @row-save="rowSave"
          :before-open="beforeOpen"
          v-model:page="page"
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
              v-if="permission.user_add && !auditMode"
              type="primary"
              icon="el-icon-plus"
              @click="$refs.crud.rowAdd()"
              >新 增
            </el-button>
            <el-button
              type="danger"
              plain
              icon="el-icon-delete"
              v-if="permission.user_delete && !auditMode"
              @click="handleDelete"
              >删 除
            </el-button>
            <el-button
              type="primary"
              plain
              icon="el-icon-operation"
              v-if="userInfo.authority.includes('admin') && !auditMode"
              @click="handleAudit"
              >审 核
            </el-button>
            <el-button
              type="success"
              plain
              icon="el-icon-check"
              v-if="userInfo.authority.includes('admin') && auditMode"
              @click="handleAuditPass"
              >通 过
            </el-button>
            <el-button
              type="danger"
              plain
              icon="el-icon-close"
              v-if="userInfo.authority.includes('admin') && auditMode"
              @click="handleAuditRefuse"
              >拒 绝
            </el-button>
            <el-button
              type="primary"
              plain
              icon="el-icon-refresh-left"
              v-if="userInfo.authority.includes('admin') && auditMode"
              @click="handleAuditBack"
              >返 回
            </el-button>
            <el-button
              type="info"
              plain
              v-if="permission.user_role && !auditMode"
              icon="el-icon-user"
              @click="handleGrant"
              >角色配置
            </el-button>
            <el-dropdown
              v-if="userInfo.authority.includes('admin') && !auditMode"
              @command="handleDataManageCommand"
              style="margin-left: 10px"
            >
              <el-button type="primary" plain icon="el-icon-files">
                数据管理<i class="el-icon-arrow-down el-icon--right"></i>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="import">
                    <el-icon><upload /></el-icon>
                    导入数据
                  </el-dropdown-item>
                  <el-dropdown-item command="export">
                    <el-icon><download /></el-icon>
                    导出数据
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template #menu-right>
            <el-button
              v-if="userInfo.authority.includes('admin') && !auditMode"
              icon="el-icon-document"
              @click="$refs.authLog.openAuthLog()"
              >认证日志
            </el-button>
            <el-button
              v-if="userInfo.authority.includes('admin') && !auditMode"
              icon="el-icon-lock"
              @click="$refs.authLock.openLockLog()"
              >锁定管理
            </el-button>
          </template>
          <template #menu="{ row, index, size }">
            <el-button
              text
              type="primary"
              icon="el-icon-view"
              v-if="this.permission.user_view"
              @click.stop="$refs.crud.rowView(row, index)"
              >查 看
            </el-button>
            <el-button
              text
              type="primary"
              icon="el-icon-edit"
              v-if="this.permission.user_edit"
              @click.stop="$refs.crud.rowEdit(row, index)"
              >编 辑
            </el-button>
            <el-dropdown @command="command => handleDropdownCommand(command, row, index)">
              <el-button text type="primary" icon="el-icon-setting">更 多 </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="setLeader" v-if="userInfo.authority.includes('admin')">
                    <el-icon><circle-close v-if="row.isLeader === 1" /><coordinate v-else /></el-icon>
                    {{ row.isLeader === 1 ? '取消主管' : '设置主管' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="grantRole" v-if="permission.user_role">
                    <el-icon><user /></el-icon>
                    角色配置
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="platformConfig"
                    v-if="userInfo.authority.includes('admin')"
                  >
                    <el-icon><setting /></el-icon>
                    平台配置
                  </el-dropdown-item>
                  <el-dropdown-item command="authLog" v-if="userInfo.authority.includes('admin')">
                    <el-icon><document /></el-icon>
                    认证日志
                  </el-dropdown-item>
                  <el-dropdown-item command="lockUser" v-if="userInfo.authority.includes('admin')">
                    <el-icon><lock /></el-icon>
                    锁定账号
                  </el-dropdown-item>
                  <el-dropdown-item command="resetPassword" v-if="permission.user_reset">
                    <el-icon><refresh /></el-icon>
                    密码重置
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" v-if="this.permission.user_delete">
                    <el-icon><delete /></el-icon>
                    删除用户
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template #tenantName="{ row }">
            <el-tag>{{ row.tenantName }}</el-tag>
          </template>
          <template #roleName="{ row }">
            <el-tag>{{ row.roleName }}</el-tag>
          </template>
          <template #deptName="{ row }">
            <el-tag>{{ row.deptName }}</el-tag>
          </template>
          <template #userTypeName="{ row }">
            <el-tag>{{ row.userTypeName }}</el-tag>
          </template>
        </avue-crud>

        <!-- 用户角色配置 -->
        <el-dialog title="用户角色配置" append-to-body v-model="roleBox" width="345px">
          <el-row
            justify="space-between"
            align="middle"
            style="margin-bottom: 12px; background: #f5f7fa; padding: 6px 10px; border-radius: 4px"
          >
            <span style="display: inline-flex; align-items: center">
              <el-switch
                v-model="roleLinked"
                active-text="节点联动"
                size="small"
                @change="handleLinkedChange('treeRole')"
              />
              <el-tooltip content="开启后勾选父节点会自动勾选所有子节点，关闭则可独立勾选任意节点" placement="top">
                <el-icon style="margin-left: 4px; color: #909399; cursor: pointer"><el-icon-question-filled /></el-icon>
              </el-tooltip>
            </span>
            <el-button-group>
              <el-button size="small" plain @click="handleSelectAll('treeRole', roleGrantList)"
                >全选</el-button
              >
              <el-button
                size="small"
                plain
                @click="handleInvertSelect('treeRole', roleGrantList, roleLinked)"
                >反选</el-button
              >
            </el-button-group>
          </el-row>
          <el-tree
            :data="roleGrantList"
            show-checkbox
            :check-strictly="!roleLinked"
            default-expand-all
            node-key="id"
            ref="treeRole"
            :default-checked-keys="roleTreeObj"
            :props="props"
          >
          </el-tree>
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="roleBox = false">取 消</el-button>
              <el-button type="primary" @click="submitRole">确 定</el-button>
            </span>
          </template>
        </el-dialog>

        <!-- 用户数据导入 -->
        <el-dialog title="用户数据导入" append-to-body v-model="excelBox" width="555px">
          <avue-form :option="excelOption" v-model="excelForm" :upload-after="uploadAfter">
            <template #excelTemplate>
              <el-button type="primary" @click="handleTemplate">
                点击下载<i class="el-icon-download el-icon--right"></i>
              </el-button>
            </template>
          </avue-form>
        </el-dialog>

        <!-- 用户平台配置（行级别） -->
        <el-dialog title="用户平台配置" append-to-body v-model="platformBox" width="600px">
          <avue-form ref="platformFormRef" :option="platformFormOption" v-model="platformForm" />
          <template #footer>
            <span class="dialog-footer">
              <el-button @click="platformBox = false">取 消</el-button>
              <el-button type="primary" @click="submitPlatformConfig">确 定</el-button>
            </span>
          </template>
        </el-dialog>

        <!-- 认证日志组件 -->
        <auth-log ref="authLog" />
        <!-- 认证锁定配置组件 -->
        <auth-lock ref="authLock" @lock-success="onLoad(page)" />
      </basic-container>
    </el-col>
  </el-row>
</template>

<script>
import {
  getList,
  getUser,
  getUserPlatform,
  remove,
  update,
  updatePlatform,
  add,
  grant,
  resetPassword,
  auditPass,
  auditRefuse,
  setLeader,
  getLeaderList,
} from '@/api/system/user';
import { exportBlob } from '@/api/common';
import { getDeptTree } from '@/api/system/dept';
import { getRoleTree } from '@/api/system/role';
import { getPostList } from '@/api/system/post';
import { mapGetters } from 'vuex';
import website from '@/config/website';
import { getToken } from '@/utils/auth';
import { downloadXls } from '@/utils/util';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';
import func from '@/utils/func';
import { sensitive } from '@/utils/sensitive';
import { excelOption, platformFormOption, treeOption, userOption } from '@/option/system/user';
import AuthLog from './authlog.vue';
import AuthLock from './authlock.vue';

export default {
  components: { AuthLog, AuthLock },
  data() {
    return {
      form: {},
      search: {},
      sensitiveManager: null,
      roleBox: false,
      roleLinked: false,
      grantUserId: '',
      excelBox: false,
      platformBox: false,
      initFlag: true,
      auditMode: false,
      selectionList: [],
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      props: {
        label: 'title',
        value: 'key',
      },
      roleGrantList: [],
      roleTreeObj: [],
      treeDeptId: '',
      treeData: [],
      treeOption: treeOption,
      userOption: userOption(this),
      platformFormOption: platformFormOption,
      data: [],
      platformForm: {},
      excelForm: {},
      excelOption: excelOption,
    };
  },
  watch: {
    'form.tenantId'() {
      if (this.form.tenantId !== '' && this.initFlag) {
        this.initData(this.form.tenantId);
      }
    },
    'excelForm.isCovered'() {
      if (this.excelForm.isCovered !== '') {
        const column = this.findColumn(this.excelOption.column, 'excelFile');
        column.action = `/blade-system/user/import-user?isCovered=${this.excelForm.isCovered}`;
      }
    },
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.user_add, false),
        viewBtn: false,
        delBtn: false,
        editBtn: false,
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
    // 非租户模式默认加载管理组数据
    if (!website.tenantMode) {
      this.initData(website.tenantId);
    } else {
      this.initData();
    }
  },
  created() {
    // 创建脱敏工具实例
    this.sensitiveManager = sensitive.create({
      fields: ['phone', 'email'], // 配置需要脱敏的字段
    });
  },
  methods: {
    nodeClick(data) {
      this.treeDeptId = data.id;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    handleDropdownCommand(command, row, index) {
      if (command === 'setLeader') {
        this.handleSetLeader(row);
      } else if (command === 'grantRole') {
        this.handleGrantForRow(row);
      } else if (command === 'authLog') {
        this.$refs.authLog.openUserAuthLog(row.id, row.name);
      } else if (command === 'lockUser') {
        this.$refs.authLock.openLockUser(row.id, row.account);
      } else if (command === 'platformConfig') {
        this.handlePlatformForRow(row);
      } else if (command === 'resetPassword') {
        this.handleResetForRow(row);
      } else if (command === 'delete') {
        this.$refs.crud.rowDel(row, index);
      }
    },
    handleResetForRow(row) {
      this.$confirm(`确定将账号【${row.account}】的密码重置为初始密码?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return resetPassword(row.id);
        })
        .then(() => {
          this.$message({
            type: 'success',
            message: '密码重置成功!',
          });
        });
    },
    handleDataManageCommand(command) {
      if (command === 'import') {
        this.handleImport();
      } else if (command === 'export') {
        this.handleExport();
      }
    },
    handleSetLeader(row) {
      const tip = row.isLeader === 1 ? '确定取消用户的主管职务?' : '确定设置用户为主管职务?';
      const message = row.isLeader === 1 ? '取消主管成功!' : '设置主管成功!';
      this.$confirm(tip, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return setLeader(row.id);
        })
        .then(() => {
          this.$message({
            type: 'success',
            message: message,
          });
          this.onLoad(this.page);
        });
    },
    initData(tenantId) {
      getRoleTree(tenantId).then(res => {
        const column = this.findColumn(this.userOption.group, 'roleId');
        column.dicData = res.data.data;
      });
      getDeptTree(tenantId).then(res => {
        const column = this.findColumn(this.userOption.group, 'deptId');
        column.dicData = res.data.data;
      });
      getPostList(tenantId).then(res => {
        const column = this.findColumn(this.userOption.group, 'postId');
        column.dicData = res.data.data;
      });
      getLeaderList(tenantId).then(res => {
        const column = this.findColumn(this.userOption.group, 'leaderId');
        column.dicData = res.data.data;
      });
    },
    submitRole() {
      const roleList = this.$refs.treeRole.getCheckedKeys().join(',');
      grant(this.grantUserId, roleList).then(() => {
        this.roleBox = false;
        this.$message({
          type: 'success',
          message: '操作成功!',
        });
        this.onLoad(this.page);
      });
    },
    rowSave(row, done, loading) {
      row.deptId = func.join(row.deptId);
      row.roleId = func.join(row.roleId);
      row.postId = func.join(row.postId);
      row.leaderId = func.join(row.leaderId);
      add(row).then(
        () => {
          this.initFlag = false;
          this.onLoad(this.page);
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
    rowUpdate(row, index, done, loading) {
      row.deptId = func.join(row.deptId);
      row.roleId = func.join(row.roleId);
      row.postId = func.join(row.postId);
      row.leaderId = func.join(row.leaderId);
      // 获取需要提交的数据
      const submitData = this.sensitiveManager.getSubmitData(row);
      update(submitData).then(
        () => {
          this.initFlag = false;
          this.onLoad(this.page);
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
    searchReset() {
      this.query = {};
      this.treeDeptId = '';
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
    handleAudit() {
      this.auditMode = true;
      this.onLoad(this.page, this.query);
    },
    handleAuditPass() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据通过审核?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return auditPass(this.ids);
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
    handleAuditRefuse() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据拒绝审核?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return auditRefuse(this.ids);
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
    handleAuditBack() {
      this.auditMode = false;
      this.onLoad(this.page, this.query);
    },
    handleReset() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择账号密码重置为初始密码?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          return resetPassword(this.ids);
        })
        .then(() => {
          this.$message({
            type: 'success',
            message: '操作成功!',
          });
          this.$refs.crud.toggleSelection();
        });
    },
    handleGrant() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.roleTreeObj = [];
      this.roleLinked = false;
      this.grantUserId = this.ids;
      if (this.selectionList.length === 1) {
        this.roleTreeObj = this.selectionList[0].roleId.split(',');
      }
      getRoleTree().then(res => {
        this.roleGrantList = res.data.data;
        this.roleBox = true;
      });
    },
    handleGrantForRow(row) {
      this.roleTreeObj = row.roleId ? row.roleId.split(',') : [];
      this.roleLinked = false;
      this.grantUserId = row.id;
      getRoleTree().then(res => {
        this.roleGrantList = res.data.data;
        this.roleBox = true;
      });
    },
    getAllNodeKeys(nodes) {
      let keys = [];
      nodes.forEach(node => {
        keys.push(node.id);
        if (node.children && node.children.length > 0) {
          keys = keys.concat(this.getAllNodeKeys(node.children));
        }
      });
      return keys;
    },
    getLeafKeys(nodes) {
      let keys = [];
      nodes.forEach(node => {
        if (!node.children || node.children.length === 0) {
          keys.push(node.id);
        } else {
          keys = keys.concat(this.getLeafKeys(node.children));
        }
      });
      return keys;
    },
    handleSelectAll(treeRef, dataList) {
      const tree = this.$refs[treeRef];
      if (!tree) return;
      const allKeys = this.getAllNodeKeys(dataList);
      tree.setCheckedKeys(allKeys);
    },
    handleInvertSelect(treeRef, dataList, isLinked) {
      const tree = this.$refs[treeRef];
      if (!tree) return;
      const checkedKeys = new Set(tree.getCheckedKeys());
      if (isLinked) {
        const leafKeys = this.getLeafKeys(dataList);
        const invertedKeys = leafKeys.filter(key => !checkedKeys.has(key));
        tree.setCheckedKeys(invertedKeys);
      } else {
        const allKeys = this.getAllNodeKeys(dataList);
        const invertedKeys = allKeys.filter(key => !checkedKeys.has(key));
        tree.setCheckedKeys(invertedKeys);
      }
    },
    handleLinkedChange(treeRef) {
      const tree = this.$refs[treeRef];
      if (!tree) return;
      const checkedKeys = tree.getCheckedKeys();
      const halfCheckedKeys = tree.getHalfCheckedKeys();
      this.$nextTick(() => {
        tree.setCheckedKeys([...checkedKeys, ...halfCheckedKeys]);
      });
    },
    // ========== 平台配置（行级别） ==========
    handlePlatformForRow(row) {
      getUserPlatform(row.id).then(res => {
        this.platformForm = { ...res.data.data, account: row.account };
        this.platformBox = true;
      });
    },
    submitPlatformConfig() {
      updatePlatform(
        this.platformForm.id,
        this.platformForm.userType,
        this.platformForm.userExt
      ).then(() => {
        this.platformBox = false;
        this.$message({ type: 'success', message: '操作成功!' });
      });
    },
    handleImport() {
      this.excelBox = true;
    },
    uploadAfter(res, done, loading, column) {
      window.console.log(column);
      this.excelBox = false;
      this.refreshChange();
      done();
    },
    handleExport() {
      const account = func.toStr(this.search.account);
      const realName = func.toStr(this.search.realName);
      this.$confirm('是否导出用户数据?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        NProgress.start();
        exportBlob(
          `/blade-system/user/export-user?${
            this.website.tokenHeader
          }=${getToken()}&account=${account}&realName=${realName}`
        ).then(res => {
          downloadXls(res.data, `用户数据表${this.$dayjs().format('YYYY-MM-DD')}.xlsx`);
          NProgress.done();
        });
      });
    },
    handleTemplate() {
      exportBlob(
        `/blade-system/user/export-template?${this.website.tokenHeader}=${getToken()}`
      ).then(res => {
        downloadXls(res.data, '用户数据模板.xlsx');
      });
    },
    beforeOpen(done, type) {
      if (['edit', 'view'].includes(type)) {
        getUser(this.form.id).then(res => {
          this.form = res.data.data;
          if (this.form.hasOwnProperty('deptId')) {
            this.form.deptId = func.split(this.form.deptId);
          }
          if (this.form.hasOwnProperty('roleId')) {
            this.form.roleId = func.split(this.form.roleId);
          }
          if (this.form.hasOwnProperty('postId')) {
            this.form.postId = func.split(this.form.postId);
          }
          if (this.form.hasOwnProperty('leaderId')) {
            this.form.leaderId = func.split(this.form.leaderId);
          }
          if (this.form.sex === -1) {
            this.form.sex = '';
          }
          // 保存初始脱敏数据
          this.sensitiveManager.saveInitialData(this.form);
        });
      }
      this.initFlag = true;
      done();
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
      getList(
        page.currentPage,
        page.pageSize,
        Object.assign(params, this.query, { status: this.auditMode ? 0 : 1 }),
        this.treeDeptId
      ).then(res => {
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

<style>
.box {
  height: 800px;
}

.el-scrollbar {
  height: 100%;
}

.box .el-scrollbar__wrap {
  overflow: scroll;
}
</style>
