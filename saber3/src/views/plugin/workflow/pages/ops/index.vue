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
      <template #menu-left>
        <el-button
          v-if="permission.wf_ops_pass"
          :disabled="loading"
          size="default"
          type="success"
          icon="el-icon-check"
          @click="handleCompleteTask(null, true)"
        >
          通过
        </el-button>
        <el-button
          v-if="permission.wf_ops_reject"
          :disabled="loading"
          size="default"
          type="danger"
          icon="el-icon-close"
          @click="handleCompleteTask(null, false)"
        >
          驳回
        </el-button>
        <el-button
          v-if="permission.wf_ops_change_assignee"
          :disabled="loading"
          size="default"
          type="warning"
          icon="el-icon-user"
          @click="handleUserSelect({ type: 'assignee', checkType: 'radio' })"
        >
          变更审核人
        </el-button>
        <el-button
          v-if="permission.wf_ops_transfer"
          :disabled="loading"
          size="default"
          type="primary"
          icon="el-icon-user"
          @click="handleUserSelect({ type: 'transfer', checkType: 'radio' })"
        >
          转办
        </el-button>
        <el-button
          v-if="permission.wf_ops_delegate"
          :disabled="loading"
          size="default"
          type="success"
          icon="el-icon-user"
          @click="handleUserSelect({ type: 'delegate', checkType: 'radio' })"
        >
          委托
        </el-button>
        <el-button
          v-if="permission.wf_ops_copy"
          :disabled="loading"
          size="default"
          type="info"
          icon="el-icon-promotion"
          @click="handleUserSelect({ type: 'copy', checkType: 'checkbox' })"
        >
          抄送
        </el-button>
        <el-button
          v-if="permission.wf_ops_urge"
          :disabled="loading"
          size="default"
          type="warning"
          icon="el-icon-warning-filled"
          @click="handleUrgeTask(null)"
        >
          催办
        </el-button>
        <el-button
          v-if="permission.wf_ops_terminate"
          :disabled="loading"
          size="default"
          type="danger"
          icon="el-icon-opportunity"
          @click="handleTerminateProcess(null)"
        >
          终止
        </el-button>
      </template>
      <template #processDefinitionName="{ row }">
        <el-link
          v-if="permission.wf_ops_detail"
          style="font-size: 12px"
          type="primary"
          @click="handleDetail(row)"
        >
          {{ row.processDefinitionName }}
        </el-link>
        <span v-else>{{ row.processDefinitionName }}</span>
      </template>
      <template #isSuspended="{ row }">
        <el-tag v-if="row.isSuspended" size="default" type="danger"> 挂起 </el-tag>
        <el-tag v-else size="default"> 激活 </el-tag>
      </template>
      <template #menu="{ row }">
        <el-button
          v-if="permission.wf_ops_follow"
          text
          type="primary"
          size="default"
          icon="el-icon-search"
          @click="handleFlow(row)"
        >
          流程图
        </el-button>
        <el-dropdown style="margin-left: 5px">
          <el-button size="default" text type="primary">
            更多操作 <el-icon class="el-icon-right"> <el-icon-arrow-down /> </el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-if="permission.wf_ops_pass"
                @click="handleCompleteTask(row.taskId, true)"
              >
                通过
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_reject"
                @click="handleCompleteTask(row.taskId, false)"
              >
                驳回
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_transfer"
                @click="handleUserSelect({ type: 'transfer', checkType: 'radio' }, row)"
              >
                转办
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_delegate"
                @click="handleUserSelect({ type: 'delegate', checkType: 'radio' }, row)"
              >
                委托
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_dispatch"
                @click="getProcessNodes(row.taskId, row.processInstanceId, 'dispatch')"
              >
                调度
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_rollback"
                @click="getProcessNodes(row.taskId, row.processInstanceId, 'rollback')"
              >
                指定 回退</el-dropdown-item
              >
              <el-dropdown-item
                v-if="permission.wf_ops_terminate"
                @click="handleTerminateProcess(row.taskId)"
              >
                终止
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_add_multi_instance && row.isMultiInstance"
                @click="handleUserSelect({ type: 'addMultiInstance', checkType: 'checkbox' }, row)"
              >
                加签
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_delete_multi_instance && row.isMultiInstance"
                @click="handleDeleteMultiInstance(row)"
              >
                减签
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_copy"
                @click="handleUserSelect({ type: 'copy', checkType: 'checkbox' }, row)"
              >
                抄送
              </el-dropdown-item>
              <el-dropdown-item v-if="permission.wf_ops_urge" @click="handleUrgeTask(row.taskId)">
                催办
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_active && row.isSuspended"
                @click="handleChangeStatus(row, false)"
              >
                激活
              </el-dropdown-item>
              <el-dropdown-item
                v-if="permission.wf_ops_suspend && !row.isSuspended"
                @click="handleChangeStatus(row, true)"
              >
                挂起
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </nf-crud>

    <el-dialog v-model="bpmnVisible" append-to-body destroy-on-close title="流程图">
      <nf-design ref="bpmn" style="height: 500px" :options="bpmnOption"></nf-design>
    </el-dialog>
    <!-- 人员选择弹窗 -->
    <nf-user-select
      ref="user-select"
      :check-type="checkType"
      @onConfirm="handleUserSelectConfirm"
    ></nf-user-select>
    <!-- 选择节点弹窗 -->
    <el-dialog v-model="nodeVisible" append-to-body title="选择节点">
      <nf-form
        v-if="nodeVisible"
        v-model="nodeForm"
        :option="nodeOption"
        @submit="handleNodeSubmit"
      ></nf-form>
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
import { detail, userList } from '../../api/process/process';
import {
  getList,
  completeTask,
  changeTaskStatus,
  changeTaskAssignee,
  transferTask,
  delegateTask,
  copyTask,
  urgeTask,
  terminateProcess,
  processNodes,
  rollbackTask,
  dispatchTask,
  addMultiInstance,
  deleteMultiInstance,
} from '../../api/ops/ops';
import { mapGetters } from 'vuex';
import NfUserSelect from '../../components/nf-user-select/index.vue';
import NfFormDetail from '../../components/nf-form-detail/index.vue';
import exForm from '../../mixins/ex-form';

export default {
  mixins: [exForm],
  components: {
    NfUserSelect,
    NfFormDetail,
  },
  data() {
    return {
      checkType: 'radio',
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
        rowKey: 'taskId',
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
        searchMenuSpan: 6,
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
            label: '当前节点',
            prop: 'taskName',
            search: true,
          },
          {
            label: '审核人',
            prop: 'assignee',
            component: 'wf-user-select',
            params: {
              checkType: 'radio',
            },
            overHidden: true,
            search: true,
          },
          {
            label: '申请人',
            prop: 'startUsername',
            search: true,
          },
          {
            label: '创建时间',
            prop: 'createTime',
            type: 'datetime',
            format: 'YYYY-MM-DD HH:mm',
            width: 165,
          },
          {
            label: '状态',
            prop: 'isSuspended',
            type: 'select',
            dicData: [
              {
                label: '挂起',
                value: true,
              },
              {
                label: '激活',
                value: false,
              },
            ],
            search: true,
          },
          {
            label: '时间范围',
            prop: 'startTimeRange',
            type: 'datetimerange',
            dataType: 'string',
            format: 'YYYY-MM-DD HH:mm:ss',
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            hide: true,
            search: true,
            searchRange: true,
          },
        ],
      },
      data: [],
      bpmnVisible: false,
      bpmnOption: {},
      nodeVisible: false,
      nodeForm: {},
      nodeOption: {
        column: [
          {
            label: '节点',
            prop: 'nodeId',
            type: 'select',
            props: {
              label: 'nodeName',
              value: 'nodeId',
            },
            span: 24,
            rules: [{ required: true, message: '请选择节点', trigger: 'change' }],
          },
        ],
      },
      nodeType: '',
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
    handleDeleteMultiInstance(row) {
      const { taskId } = row;
      this.$confirm(`确定要将选中的任务减签吗？`, '警告', {
        type: 'warning',
      })
        .then(() => {
          this.loading = true;
          deleteMultiInstance({ taskId })
            .then(() => {
              this.$message.success('减签成功');
              this.onLoad(this.page, this.query);
            })
            .catch(() => {
              this.loading = false;
            });
        })
        .catch(() => {});
    },
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
    rollbackTask,
    dispatchTask,
    handleNodeSubmit(form, done) {
      const { nodeId, taskId } = form;
      const param = { nodeId, taskId };
      this.loading = true;
      this[`${this.nodeType}Task`](param)
        .then(() => {
          this.$message.success('操作成功');
          this.form = {};
          done();
          this.nodeVisible = false;
          this.onLoad(this.page, this.query);
        })
        .catch(() => {
          done();
          this.loading = false;
        });
    },
    getProcessNodes(taskId, processInstanceId, type) {
      this.nodeType = type;
      const param = { processInstanceId };
      if (type == 'rollback') param.taskId = taskId;
      processNodes(param).then(res => {
        this.findObject(this.nodeOption.column, 'nodeId').dicData = res.data.data;
        this.nodeVisible = true;
        this.nodeForm = {
          ...this.nodeForm,
          taskId,
          processInstanceId,
        };
      });
    },
    handleCompleteTask(taskId, pass) {
      if (!taskId) {
        if (this.selectionList.length === 0) {
          this.$message.warning('请选择至少一条数据');
          return;
        }
        taskId = this.ids;
      }
      this.$confirm(
        `确定要将选中的任务全部<span style='color: red;'> ${pass ? '通过' : '驳回'} </span>吗？`,
        '警告',
        {
          type: 'warning',
          dangerouslyUseHTMLString: true,
        }
      )
        .then(() => {
          this.loading = true;
          completeTask({ taskId, pass })
            .then(() => {
              this.$message.success('操作成功');
              this.onLoad(this.page, this.query);
            })
            .catch(() => {
              this.loading = false;
            });
        })
        .catch(() => {});
    },
    handleUrgeTask(taskId) {
      if (!taskId) {
        if (this.selectionList.length === 0) {
          this.$message.warning('请选择至少一条数据');
          return;
        }
        taskId = this.ids;
      }
      this.$confirm(`确定要将催办选中的任务吗？若任务没有审核人此操作无效`, '警告', {
        type: 'warning',
      })
        .then(() => {
          this.loading = true;
          urgeTask({ taskId })
            .then(() => {
              this.$message.success('催办成功');
              this.onLoad(this.page, this.query);
            })
            .catch(() => {
              this.loading = false;
            });
        })
        .catch(() => {});
    },
    handleTerminateProcess(taskId) {
      if (!taskId) {
        if (this.selectionList.length === 0) {
          this.$message.warning('请选择至少一条数据');
          return;
        }
        taskId = this.ids;
      }
      this.$confirm(`确定要将选中的任务流程终止吗？`, '警告', {
        type: 'warning',
      })
        .then(() => {
          this.loading = true;
          terminateProcess({ taskId })
            .then(() => {
              this.$message.success('终止成功');
              this.onLoad(this.page, this.query);
            })
            .catch(() => {
              this.loading = false;
            });
        })
        .catch(() => {});
    },
    handleChangeStatus(row, isSuspended) {
      this.loading = true;
      changeTaskStatus({ taskId: row.taskId, isSuspended })
        .then(() => {
          this.$message.success('操作成功');
          this.onLoad(this.page, this.query);
        })
        .catch(() => {
          this.loading = false;
        });
    },
    // 人员选择弹窗
    handleUserSelect({ type, checkType }, row) {
      if (row) this.selectionList = [row];
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$refs['user-select'].visible = true;
      this.userSelectType = type;
      this.checkType = checkType;
    },
    handleUserSelectConfirm(id, name) {
      switch (this.userSelectType) {
        case 'assignee':
          this.$confirm(
            `确定要将选中的任务审核人变更为<span style='color: red;'> ${name} </span>吗？`,
            '提示',
            {
              type: 'warning',
              dangerouslyUseHTMLString: true,
            }
          )
            .then(() => {
              this.$refs['user-select'].visible = false;
              this.loading = true;
              changeTaskAssignee({ taskId: this.ids, assignee: id })
                .then(() => {
                  this.$message.success('操作成功');
                  this.onLoad(this.page, this.query);
                  this.selectionList = [];
                })
                .catch(() => {
                  this.loading = false;
                });
            })
            .catch(() => {});
          break;
        case 'transfer':
          this.$confirm(
            `确定要将选中的任务转办给<span style='color: red;'> ${name} </span>吗？`,
            '提示',
            {
              type: 'warning',
              dangerouslyUseHTMLString: true,
            }
          )
            .then(() => {
              this.$refs['user-select'].visible = false;
              this.loading = true;
              transferTask({ taskId: this.ids, assignee: id })
                .then(() => {
                  this.$message.success('转办成功');
                  this.onLoad(this.page, this.query);
                  this.selectionList = [];
                })
                .catch(() => {
                  this.loading = false;
                });
            })
            .catch(() => {});
          break;
        case 'delegate':
          this.$confirm(
            `确定要将选中的任务委托给<span style='color: red;'> ${name} </span>吗？`,
            '提示',
            {
              type: 'warning',
              dangerouslyUseHTMLString: true,
            }
          )
            .then(() => {
              this.$refs['user-select'].visible = false;
              this.loading = true;
              delegateTask({ taskId: this.ids, assignee: id })
                .then(() => {
                  this.$message.success('委托成功');
                  this.onLoad(this.page, this.query);
                  this.selectionList = [];
                })
                .catch(() => {
                  this.loading = false;
                });
            })
            .catch(() => {});
          break;
        case 'copy':
          this.$confirm(
            `确定要将选中的任务抄送给<span style='color: red;'> ${name} </span>吗？`,
            '提示',
            {
              type: 'warning',
              dangerouslyUseHTMLString: true,
            }
          )
            .then(() => {
              this.$refs['user-select'].visible = false;
              copyTask({ taskId: this.ids, assignee: id })
                .then(() => {
                  this.$refs.crud.toggleSelection();
                  this.$message.success('抄送成功');
                })
                .catch(() => {
                  this.loading = false;
                });
            })
            .catch(() => {});
          break;
        case 'addMultiInstance':
          this.$confirm(`确定要将选中的人员加签吗？`, '提示', {
            type: 'warning',
          })
            .then(() => {
              this.$refs['user-select'].visible = false;
              addMultiInstance({ taskId: this.ids, assignee: id })
                .then(() => {
                  this.$message.success('加签成功');
                  this.onLoad(this.page, this.query);
                  this.selectionList = [];
                })
                .catch(() => {
                  this.loading = false;
                });
            })
            .catch(() => {});
          break;
      }
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
      const param = Object.assign(params, this.query);
      const { assignee } = param;
      if (assignee) {
        const res = await userList(1, -1, { name: assignee });
        if (res.data.data.records && res.data.data.records.length > 0) {
          param.assignee = res.data.data.records.map(d => d.id).join(',');
        } else {
          this.page.total = 0;
          this.data = [];
          this.loading = false;
        }
      }
      getList(page.currentPage, page.pageSize, param)
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
