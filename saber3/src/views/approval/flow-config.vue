<template>
  <basic-container>
    <div class="flow-config-page">
      <section class="flow-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="流程名称">
            <el-input v-model="query.flowName" clearable placeholder="请输入流程名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="业务类型">
            <el-select v-model="query.businessType" clearable placeholder="全部类型">
              <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option label="草稿" value="0" />
              <el-option label="已发布" value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="flow-toolbar">
        <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="handleAdd">
          新建流程
        </el-button>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <el-table v-loading="loading" :data="data" border row-key="flowId" class="flow-table">
        <el-table-column prop="flowName" label="流程名称" min-width="190" show-overflow-tooltip />
        <el-table-column prop="businessType" label="业务类型" width="140">
          <template #default="{ row }">{{ businessTypeText(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="flowVersion" label="版本" width="90" align="center">
          <template #default="{ row }">
            <el-tag effect="plain">v{{ row.flowVersion || 1 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="节点链路" min-width="280">
          <template #default="{ row }">
            <div class="node-chain">
              <template v-for="(node, index) in displayNodes(row)" :key="`${row.flowId}-${index}`">
                <el-tag :type="nodeTypeTag(node.nodeType)" effect="plain">
                  {{ node.nodeName }}
                </el-tag>
                <span v-if="index < displayNodes(row).length - 1" class="node-arrow">→</span>
              </template>
              <span v-if="displayNodes(row).length === 0">-</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '1' ? 'success' : 'warning'" effect="plain">
              {{ row.status === '1' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="300" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="permissionList.editBtn" text type="primary" icon="el-icon-edit" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button v-if="permissionList.publishBtn" text type="success" icon="el-icon-upload" @click="handlePublish(row)">
              发布
            </el-button>
            <el-button v-if="permissionList.copyBtn" text type="primary" icon="el-icon-copy-document" @click="handleCopy(row)">
              复制
            </el-button>
            <el-button v-if="permissionList.delBtn" text type="danger" icon="el-icon-delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flow-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-sizes="[10, 20, 30, 50]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>

      <el-drawer v-model="drawerVisible" :title="drawerTitle" size="78%" append-to-body>
        <el-form ref="flowFormRef" :model="form" :rules="rules" label-width="110px" class="flow-form">
          <el-row :gutter="18">
            <el-col :span="12">
              <el-form-item label="流程名称" prop="flowName">
                <el-input v-model="form.flowName" placeholder="请输入流程名称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="业务类型" prop="businessType">
                <el-select v-model="form.businessType" style="width: 100%" placeholder="请选择业务类型">
                  <el-option v-for="item in businessTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="规则说明">
            <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入规则说明" />
          </el-form-item>
          <el-form-item label="审批节点">
            <div class="node-editor">
              <div class="node-editor__head">
                <span>序号</span>
                <span>节点名称</span>
                <span>节点类型</span>
                <span>审批账号</span>
                <span>审批人</span>
                <span>完成条件</span>
                <span>时限(h)</span>
                <span>抄送账号</span>
                <span></span>
              </div>
              <div v-for="(node, index) in nodes" :key="index" class="node-editor__row">
                <span>{{ index + 1 }}</span>
                <el-input v-model="node.nodeName" placeholder="节点名称" />
                <el-select v-model="node.nodeType">
                  <el-option label="发起" value="submit" />
                  <el-option label="审批" value="approve" />
                  <el-option label="抄送" value="cc" />
                </el-select>
                <el-input v-model="node.approverLogin" placeholder="账号，多个逗号分隔" :disabled="node.nodeType === 'submit'" />
                <el-input v-model="node.approverName" placeholder="显示名" :disabled="node.nodeType === 'submit'" />
                <el-select v-model="node.completeCondition" :disabled="node.nodeType !== 'approve'">
                  <el-option label="全部同意" value="all" />
                  <el-option label="任一同意" value="any" />
                </el-select>
                <el-input-number v-model="node.timeLimit" :min="1" controls-position="right" :disabled="node.nodeType === 'submit'" />
                <el-input v-model="node.ccUsers" placeholder="账号，多个逗号分隔" />
                <el-button icon="el-icon-delete" circle :disabled="nodes.length <= 1" @click="removeNode(index)" />
              </div>
              <el-button type="primary" plain icon="el-icon-plus" @click="addNode">添加节点</el-button>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="drawer-footer">
            <el-button @click="drawerVisible = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="submitFlow">保存</el-button>
          </div>
        </template>
      </el-drawer>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import {
  copyApprovalFlow,
  getApprovalFlowDetail,
  getApprovalFlowList,
  getApprovalFlowNodes,
  publishApprovalFlow,
  removeApprovalFlow,
  saveApprovalFlow,
  updateApprovalFlow,
} from '@/api/approval/approval';

const businessTypeOptions = [
  { value: 'tenant_entry', label: '入驻审批' },
  { value: 'contract_renewal', label: '合同续签' },
  { value: 'termination', label: '退租审批' },
];

const defaultNodes = () => [
  { nodeName: '经办人提交', nodeType: 'submit', completeCondition: 'all' },
  { nodeName: '部门审批', nodeType: 'approve', completeCondition: 'all' },
];

export default {
  name: 'ApprovalFlowConfig',
  data() {
    return {
      loading: false,
      saving: false,
      data: [],
      query: {},
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      drawerVisible: false,
      form: {},
      nodes: defaultNodes(),
      businessTypeOptions,
      rules: {
        flowName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
        businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.approval_flow_config_add, false),
        editBtn: this.validData(this.permission.approval_flow_config_edit, false),
        delBtn: this.validData(this.permission.approval_flow_config_delete, false),
        publishBtn: this.validData(this.permission.approval_flow_config_publish, false),
        copyBtn: this.validData(this.permission.approval_flow_config_copy, false),
      };
    },
    drawerTitle() {
      return this.form.flowId ? '编辑审批流程' : '新建审批流程';
    },
  },
  created() {
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getApprovalFlowList(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = Number(result.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    reload() {
      this.onLoad(this.page);
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    handleAdd() {
      this.form = {
        status: '0',
        businessType: 'tenant_entry',
        flowVersion: 1,
      };
      this.nodes = defaultNodes();
      this.drawerVisible = true;
    },
    handleEdit(row) {
      this.drawerVisible = true;
      getApprovalFlowDetail(row.flowId).then(res => {
        const detail = res.data.data || row;
        this.form = { ...detail };
        this.nodes = detail.nodes && detail.nodes.length ? detail.nodes.map(item => ({ ...item })) : defaultNodes();
        getApprovalFlowNodes(row.flowId).then(nodeRes => {
          const nodeList = nodeRes.data.data || [];
          if (nodeList.length) {
            this.nodes = nodeList.map(item => ({ ...item }));
          }
        });
      });
    },
    submitFlow() {
      this.$refs.flowFormRef.validate(valid => {
        if (!valid) return;
        const nodes = this.nodes
          .filter(item => item.nodeName)
          .map((item, index) => ({
            ...item,
            nodeOrder: index + 1,
            completeCondition: item.completeCondition || 'all',
          }));
        if (!nodes.length) {
          this.$message.warning('请至少配置一个审批节点');
          return;
        }
        const payload = {
          ...this.form,
          nodes,
        };
        this.saving = true;
        const action = payload.flowId ? updateApprovalFlow : saveApprovalFlow;
        action(payload)
          .then(() => {
            this.$message.success('保存成功');
            this.drawerVisible = false;
            this.reload();
          })
          .finally(() => {
            this.saving = false;
          });
      });
    },
    handlePublish(row) {
      this.$confirm('发布后该流程可被审批项目引用，确定发布？', '提示', { type: 'warning' })
        .then(() => publishApprovalFlow(row.flowId))
        .then(() => {
          this.$message.success('发布成功');
          this.reload();
        });
    },
    handleCopy(row) {
      copyApprovalFlow(row.flowId).then(() => {
        this.$message.success('复制成功');
        this.reload();
      });
    },
    handleDelete(row) {
      this.$confirm('确定删除该审批流程？', '提示', { type: 'warning' })
        .then(() => removeApprovalFlow(row.flowId))
        .then(() => {
          this.$message.success('删除成功');
          this.reload();
        });
    },
    addNode() {
      this.nodes.push({
        nodeName: '',
        nodeType: 'approve',
        completeCondition: 'all',
      });
    },
    removeNode(index) {
      this.nodes.splice(index, 1);
    },
    displayNodes(row) {
      if (row.nodes && row.nodes.length) return row.nodes;
      if (!row.nodeConfig) return [];
      try {
        const parsed = JSON.parse(row.nodeConfig);
        return Array.isArray(parsed) ? parsed : [];
      } catch (e) {
        return `${row.nodeConfig}`
          .split(',')
          .filter(Boolean)
          .map(name => ({ nodeName: name }));
      }
    },
    businessTypeText(value) {
      const item = this.businessTypeOptions.find(option => option.value === value);
      return item ? item.label : value || '-';
    },
    nodeTypeTag(value) {
      const map = {
        submit: 'info',
        approve: 'primary',
        cc: 'warning',
      };
      return map[value] || 'primary';
    },
  },
};
</script>

<style lang="scss" scoped>
.flow-config-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.flow-search,
.flow-toolbar {
  background: #fff;
}

.flow-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.flow-pagination {
  display: flex;
  justify-content: flex-end;
}

.node-chain {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.node-arrow {
  color: #909399;
}

.flow-form {
  padding-right: 18px;
}

.node-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.node-editor__head,
.node-editor__row {
  display: grid;
  grid-template-columns: 44px minmax(130px, 1.2fr) 100px minmax(150px, 1fr) minmax(120px, 0.8fr) 110px 105px minmax(150px, 1fr) 44px;
  gap: 8px;
  align-items: center;
}

.node-editor__head {
  color: #606266;
  font-size: 13px;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 18px;
}
</style>
