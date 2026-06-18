<template>
  <nf-container v-if="moduleLoadInit">
    <el-skeleton :loading="waiting" avatar :rows="8">
      <el-affix position="top" :offset="110">
        <div class="header">
          <h3>{{ process.processDefinitionName }}</h3>
          <div style="display: flex">
            <nf-theme
              v-if="process.status != 'todo'"
              v-model="theme"
              :theme-list="themeList"
            ></nf-theme>
            <nf-form-variable :process-ins-id="process.processInstanceId"></nf-form-variable>
          </div>
        </div>
      </el-affix>
      <el-tabs v-model="activeName">
        <el-tab-pane label="申请信息" name="first">
          <el-card shadow="never">
            <div id="printBody" :class="process.status != 'todo' ? `nf-theme-custom` : ''">
              <nf-form
                v-if="
                  option &&
                  ((option.column && option.column.length > 0) ||
                    (option.group && option.group.length > 0))
                "
                v-model="form"
                :style="themeCustomStyle"
                ref="form"
                v-model:defaults="defaults"
                :option="option"
                :upload-preview="handleUploadPreview"
              >
              </nf-form>
              <el-card
                shadow="never"
                style="margin-top: 10px"
                header="流转信息"
                :body-style="{ padding: '20px 0' }"
                v-if="isPrintShow"
              >
                <nf-flow :flow-list="flow"></nf-flow>
              </el-card>
            </div>
          </el-card>
          <el-card shadow="never" style="margin-top: 20px" v-if="process.status == 'todo'">
            <nf-examine-form
              ref="examineForm"
              v-model:comment="comment"
              :process="process"
              @user-select="handleUserSelect"
            ></nf-examine-form>
          </el-card>
        </el-tab-pane>
        <el-tab-pane label="流转信息" name="second">
          <el-card shadow="never" style="margin-top: 5px">
            <nf-flow :flow-list="flow"></nf-flow>
          </el-card>
        </el-tab-pane>
        <el-tab-pane label="流程跟踪" name="third">
          <template v-if="activeName == 'third'">
            <el-card shadow="never" style="margin-top: 5px">
              <nf-design ref="bpmn" style="height: 500px" :options="bpmnOption"></nf-design>
            </el-card>
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-skeleton>

    <!-- 底部按钮 -->
    <nf-button
      :loading="submitLoading"
      :button-list="buttonList"
      :process="process"
      :comment="comment"
      @examine="handleExamine"
      @user-select="handleUserSelect"
      @print="handlePrint"
      @rollback="handleRollbackTask"
      @terminate="handleTerminateProcess"
      @withdraw="handleWithdrawTask"
    ></nf-button>
    <!-- 人员选择弹窗 -->
    <nf-user-select
      ref="user-select"
      :check-type="checkType"
      :default-checked="defaultChecked"
      @onConfirm="handleUserSelectConfirm"
    ></nf-user-select>
  </nf-container>
</template>

<script>
import NfExamineForm from '../../../components/nf-exam-form/index.vue';
import NfButton from '../../../components/nf-button/index.vue';
import NfFlow from '../../../components/nf-flow/index.vue';
import NfTheme from '../../../components/nf-theme/index.vue';
import NfFormVariable from '../../../components/nf-form-variable/index.vue';
import NfUserSelect from '../../../components/nf-user-select/index.vue';

import exForm from '../../../mixins/ex-form';
import theme from '../../../mixins/theme';

export default {
  mixins: [exForm, theme],
  components: { NfUserSelect, NfExamineForm, NfButton, NfFlow, NfTheme, NfFormVariable },
  watch: {
    '$route.query.p': {
      handler(val) {
        if (val) {
          const param = JSON.parse(window.atob(val));
          const { taskId, processInsId } = param;
          if ((taskId && processInsId) || processInsId) this.getDetail(taskId, processInsId);
        }
      },
      immediate: true,
    },
  },
  data() {
    return {
      activeName: 'first',
      defaults: {},
      form: {},
      option: {
        column: [
          {
            type: 'input',
            label: '创建人',
            span: 12,
            display: true,
            prop: 'creator',
            value: '${this.$store.getters.userInfo.nick_name}',
            readonly: true,
          },
          {
            type: 'input',
            label: '创建部门',
            span: 12,
            display: true,
            prop: 'creatorDept',
            value: '${this.$store.getters.userInfo.dept_name}',
            readonly: true,
          },
          {
            type: 'datetimerange',
            label: '请假时间',
            span: 12,
            display: true,
            format: 'YYYY-MM-DD HH:mm:ss',
            valueFormat: 'YYYY-MM-DD HH:mm:ss',
            prop: 'datetime',
            dataType: 'string',
            required: true,
            rules: [
              {
                required: true,
                message: '开始时间必须填写',
              },
            ],
            change: ({ value }) => {
              if (!value || value.length == 0) {
                this.form.days = undefined;
              } else {
                const d1 = Date.parse(value.split(',')[0]);
                const d2 = Date.parse(value.split(',')[1]);
                const day = (d2 - d1) / (1 * 24 * 60 * 60 * 1000);
                this.form.days = Number(day.toFixed(2));
              }
            },
          },
          {
            type: 'number',
            label: '请假天数',
            span: 12,
            display: true,
            prop: 'days',
            required: true,
            rules: [
              {
                required: true,
                message: '请假天数必须填写',
              },
            ],
            controls: true,
            controlsPosition: 'right',
            change: ({ value }) => {
              if (value) this.form.reason = '请假' + value + '天';
              else this.form.reason = '';
            },
          },
          {
            type: 'textarea',
            label: '请假理由',
            span: 24,
            display: true,
            prop: 'reason',
            required: true,
            rules: [
              {
                required: true,
                message: '请假理由必须填写',
              },
            ],
          },
          {
            label: '附件',
            type: 'upload',
            propsHttp: {
              res: 'data',
              url: 'link',
              name: 'originalName',
            },
            action: '/blade-resource/oss/endpoint/put-file',
            display: true,
            span: 24,
            showFileList: true,
            multiple: true,
            limit: 10,
            prop: 'attachment',
            dataType: 'string',
          },
        ],
      },
      vars: [], // 需要提交的字段
      submitLoading: false, // 提交时按钮loading
    };
  },
  methods: {
    // 获取任务详情
    getDetail(taskId, processInsId) {
      this.getTaskDetail(taskId, processInsId).then(res => {
        const { process, form } = res;
        const { variables, status } = process;

        let { taskForm } = form;
        const option = this.option;
        option.menuBtn = false;
        let { column, group } = option;
        if (taskForm && taskForm.length > 0) {
          const columnFilter = this.filterAvueColumn(column, taskForm, true);
          column = columnFilter.column;
          let vars = columnFilter.vars || [];

          const groupArr = [];
          if (group && group.length > 0) {
            // 处理group
            group.forEach(gro => {
              const groupFilter = this.filterAvueColumn(gro.column, taskForm, true);
              gro.column = groupFilter.column;
              vars = vars.concat(groupFilter.vars);
              if (gro.column.length > 0) groupArr.push(gro);
            });
          }
          group = groupArr;
          this.vars = vars;
        }

        if (status != 'todo') {
          // 已办，删除字段默认值
          option.detail = true;
          if (column && column.length > 0) {
            // 处理column
            column.forEach(col => {
              if (col.type == 'dynamic')
                col.children.column.forEach(cc => {
                  delete cc.value;
                });
              delete col.value;
            });
          }

          if (group && group.length > 0) {
            // 处理group
            group.forEach(gro => {
              if (gro.column && gro.column.length > 0) {
                gro.column.forEach(col => {
                  if (col.type == 'dynamic')
                    col.children.column.forEach(cc => {
                      delete cc.value;
                    });
                  delete col.value;
                });
              }
            });
          }
        }
        if (option.initFunction) {
          option.initFunction = eval((option.initFunction + '').replace(/this/g, '_this'));
        }
        option.column = column;
        option.group = group;

        for (let key in variables) {
          if (!variables[key]) delete variables[key];
        }

        if (option.column && process.variables && process.variables.serialNumber) {
          option.column.unshift({
            label: '流水号',
            prop: 'serialNumber',
            span: 24,
            detail: true,
          });
        }

        this.option = option;
        this.form = variables;
        this.waiting = false;
      });
    },
    // 审核
    handleExamine(pass) {
      this.submitLoading = true;
      this.$refs.form.validate((valid, done) => {
        if (valid) {
          const variables = {};
          this.vars.forEach(v => {
            if (!this.validatenull(this.form[v])) {
              variables[v] = this.form[v];
              if (this.form[`$${v}`]) variables[`$${v}`] = this.form[`$${v}`];
            }
          });

          this.handleCompleteTask(pass, variables)
            .then(() => {
              this.$message.success('处理成功');
              this.handleCloseTag('/plugin/workflow/pages/process/todo');
            })
            .catch(() => {
              done();
              this.submitLoading = false;
            });
        } else {
          done();
          this.submitLoading = false;
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.header {
  width: 100%;
  height: 50px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px 10px 0;
}
</style>
