<template>
  <nf-container v-if="moduleLoadInit">
    <el-skeleton :loading="waiting" avatar :rows="8">
      <el-affix position="top" :offset="110">
        <div class="header">
          <h3>{{ process.processDefinitionName }}</h3>
          <div v-if="process.status != 'todo'">
            主题：<el-select-v2
              v-model="theme"
              size="default"
              :clearable="false"
              :options="themeList"
            ></el-select-v2>
          </div>
        </div>
      </el-affix>
      <el-tabs v-model="activeName">
        <el-tab-pane label="申请信息" name="first">
          <el-card shadow="never">
            <div id="printBody" :class="process.status != 'todo' ? `nf-theme-${theme}` : ''">
              <!-- 自定义表单区域 -->

              <!-- 自定义表单区域 -->
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
          <!-- 审批意见表单 -->
          <el-card shadow="never" style="margin-top: 20px" v-if="process.status == 'todo'">
            <nf-examine-form
              ref="examineForm"
              v-model:comment="comment"
              :process="process"
              @user-select="handleUserSelect"
            ></nf-examine-form>
          </el-card>
        </el-tab-pane>
        <!-- 流转信息 -->
        <el-tab-pane label="流转信息" name="second">
          <el-card shadow="never" style="margin-top: 5px">
            <nf-flow :flow-list="flow"></nf-flow>
          </el-card>
        </el-tab-pane>
        <!-- 流程图 -->
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
import NfUserSelect from '../../../components/nf-user-select/index.vue';
import exForm from '../../../mixins/ex-form';
import theme from '../../../mixins/theme';

export default {
  mixins: [exForm, theme],
  components: { NfUserSelect, NfExamineForm, NfButton, NfFlow },
  watch: {
    '$route.query.p': {
      handler(val) {
        if (val) {
          const param = JSON.parse(window.atob(val));
          const { taskId, processInsId } = param;
          if (taskId && processInsId) this.getDetail(taskId, processInsId);
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
      option: {}, // 表单配置
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
        console.log('process', process);
        console.log('taskForm', taskForm);
        console.log('variables', variables);
        console.log('status', status); // todo待办 done已办

        // 自行处理相关逻辑

        this.waiting = false; // 关闭骨架屏
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
