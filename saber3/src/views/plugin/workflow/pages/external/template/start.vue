<template>
  <nf-container v-if="moduleLoadInit">
    <el-skeleton :loading="waiting" :rows="8">
      <h3 style="margin-bottom: 20px">{{ process.name }}</h3>
      <el-card shadow="never" style="margin-top: 20px">
        <!-- 自定义表单区域 -->

        <!-- 自定义表单区域 -->
      </el-card>

      <el-card shadow="never" style="margin-top: 20px" v-if="showExamForm">
        <nf-examine-form
          ref="examineForm"
          :process="process"
          @user-select="handleUserSelect"
        ></nf-examine-form>
      </el-card>
      <div style="height: 80px"></div>
      <el-affix position="bottom" :offset="20">
        <el-row
          class="foot-item"
          :style="{ width: isCollapse ? 'calc(100% - 71px)' : 'calc(100% - 241px)' }"
        >
          <el-button type="primary" size="default" v-loading="loading" @click="handleSubmit">
            发起
          </el-button>
          <el-button
            v-if="permission.wf_process_draft"
            type="success"
            size="default"
            v-loading="loading"
            @click="
              handleDraft({ processDefId: process.id, formKey: process.formKey, variables: form })
            "
          >
            存为草稿
          </el-button>
        </el-row>
      </el-affix>
    </el-skeleton>

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
import NfUserSelect from '../../../components/nf-user-select/index.vue';

import exForm from '../../../mixins/ex-form';
import draft from '../../../mixins/draft';

export default {
  components: {
    NfUserSelect,
    NfExamineForm,
  },
  mixins: [exForm, draft],
  watch: {
    '$route.query.p': {
      handler(val) {
        if (val) {
          const param = JSON.parse(window.atob(val));
          const { processId } = param;
          if (processId) this.getForm(processId);
        }
      },
      immediate: true,
    },
  },
  computed: {
    showExamForm() {
      const { hideComment, hideCopy, hideExamine } = this.process;
      return !hideComment || !hideCopy || !hideExamine;
    },
  },
  data() {
    return {
      defaults: {},
      form: {},
      option: {
        menuBtn: false,
        column: [],
      },
      process: {},
      loading: false,
    };
  },
  methods: {
    getForm(processId) {
      this.getStartForm(processId).then(res => {
        let { process, startForm } = res;
        console.log('process', process);
        console.log('startForm', startForm);
        // 自行处理相关逻辑
        this.waiting = false;
      });
    },
    handleSubmit() {
      this.handleStartProcess(true)
        .then(done => {
          this.$message.success('发起成功');
          this.handleCloseTag('/plugin/workflow/pages/process/send');
          done();
        })
        .catch(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style lang="scss" scoped>
.foot-item {
  position: fixed;
  bottom: 5px;
  margin-left: -20px;
  // right: 0;
  z-index: 101;
  height: 66px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  -webkit-transition: 0.3s;
  transition: 0.3s;
  -webkit-box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
