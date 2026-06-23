<template>
  <nf-container v-if="moduleLoadInit">
    <el-skeleton :loading="waiting" avatar :rows="8">
      <div class="tenant-entry-detail">
        <div class="header">
          <h3>{{ process.processDefinitionName || '企业入驻审批' }}</h3>
          <nf-form-variable v-if="process.processInstanceId" :process-ins-id="process.processInstanceId" />
        </div>
        <el-tabs v-model="activeName">
          <el-tab-pane label="申请信息" name="form">
            <nf-form ref="form" v-model="form" :option="option" />
            <el-card v-if="process.status === 'todo'" shadow="never" class="exam-card">
              <nf-examine-form ref="examineForm" v-model:comment="comment" :process="process" @user-select="handleUserSelect" />
            </el-card>
          </el-tab-pane>
          <el-tab-pane label="流转信息" name="flow">
            <nf-flow :flow-list="flow" />
          </el-tab-pane>
          <el-tab-pane label="流程跟踪" name="diagram">
            <nf-design v-if="activeName === 'diagram'" ref="bpmn" style="height: 500px" :options="bpmnOption" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-skeleton>

    <nf-button
      :loading="submitLoading"
      :button-list="buttonList"
      :process="process"
      :comment="comment"
      @examine="handleExamine"
      @user-select="handleUserSelect"
      @rollback="handleRollbackTask"
      @terminate="handleTerminateProcess"
      @withdraw="handleWithdrawTask"
    />
    <nf-user-select
      ref="user-select"
      :check-type="checkType"
      :default-checked="defaultChecked"
      @onConfirm="handleUserSelectConfirm"
    />
  </nf-container>
</template>

<script>
import NfButton from '../../../components/nf-button/index.vue';
import NfExamineForm from '../../../components/nf-exam-form/index.vue';
import NfFlow from '../../../components/nf-flow/index.vue';
import NfFormVariable from '../../../components/nf-form-variable/index.vue';
import NfUserSelect from '../../../components/nf-user-select/index.vue';
import exForm from '../../../mixins/ex-form';

export default {
  components: { NfButton, NfExamineForm, NfFlow, NfFormVariable, NfUserSelect },
  mixins: [exForm],
  watch: {
    '$route.query.p': {
      handler(val) {
        if (!val) return;
        const param = JSON.parse(window.atob(decodeURIComponent(val)));
        const { taskId, processInsId } = param;
        if (processInsId) this.getDetail(taskId, processInsId);
      },
      immediate: true,
    },
  },
  data() {
    return {
      waiting: true,
      activeName: 'form',
      form: {},
      vars: [
        'businessType',
        'opportunityId',
        'customerId',
        'enterpriseName',
        'shareholderInfo',
        'businessScope',
        'principalName',
        'principalPhone',
        'leaseFloorArea',
        'rentFreePeriod',
        'unitPrice',
        'deposit',
        'contractPeriod',
        'handlerName',
        'handlerDept',
        'approvalMatter',
        'attachment',
      ],
      option: {
        menuBtn: false,
        detail: true,
        labelWidth: 130,
        column: [
          { label: '申请人', prop: 'applicant', type: 'input', span: 8, readonly: true },
          { label: '部门', prop: 'applicantDept', type: 'input', span: 8, readonly: true },
          { label: '申请时间', prop: 'applyTime', type: 'input', span: 8, readonly: true },
          { label: '企业名称', prop: 'enterpriseName', type: 'input', span: 12, readonly: true },
          { label: '股东信息', prop: 'shareholderInfo', type: 'input', span: 24, readonly: true },
          { label: '经营范围', prop: 'businessScope', type: 'textarea', span: 24, readonly: true },
          { label: '负责人', prop: 'principalName', type: 'input', span: 12, readonly: true },
          { label: '联系方式', prop: 'principalPhone', type: 'input', span: 12, readonly: true },
          { label: '租赁楼层、面积', prop: 'leaseFloorArea', type: 'input', span: 12, readonly: true },
          { label: '免租期', prop: 'rentFreePeriod', type: 'input', span: 12, readonly: true },
          { label: '单价（元）', prop: 'unitPrice', type: 'input', span: 12, readonly: true },
          { label: '保证金（元）', prop: 'deposit', type: 'input', span: 12, readonly: true },
          { label: '合同有效期', prop: 'contractPeriod', type: 'input', span: 24, readonly: true },
          { label: '经办人', prop: 'handlerName', type: 'input', span: 12, readonly: true },
          { label: '经办部门', prop: 'handlerDept', type: 'input', span: 12, readonly: true },
          { label: '审批事项', prop: 'approvalMatter', type: 'textarea', span: 24, readonly: true },
          {
            label: '附件',
            prop: 'attachment',
            type: 'upload',
            action: '/blade-resource/oss/endpoint/put-file',
            propsHttp: { res: 'data', url: 'link', name: 'originalName' },
            dataType: 'string',
            span: 24,
            disabled: true,
          },
        ],
      },
      submitLoading: false,
      comment: '',
    };
  },
  methods: {
    getDetail(taskId, processInsId) {
      this.getTaskDetail(taskId, processInsId).then(res => {
        const { process } = res;
        this.form = process.variables || {};
        this.waiting = false;
      }).catch(() => {
        this.waiting = false;
      });
    },
    handleExamine(pass) {
      this.submitLoading = true;
      this.$refs.form.validate((valid, done) => {
        if (!valid) {
          this.submitLoading = false;
          if (typeof done === 'function') done();
          return;
        }
        const variables = {};
        this.vars.forEach(key => {
          if (!this.validatenull(this.form[key])) variables[key] = this.form[key];
        });
        this.handleCompleteTask(pass, variables).then(() => {
          this.$message.success('处理成功');
          this.handleCloseTag('/settlement/project-approval');
        }).catch(() => {
          this.submitLoading = false;
          if (typeof done === 'function') done();
        });
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.tenant-entry-detail {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
}

.header {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.exam-card {
  margin-top: 16px;
}
</style>
