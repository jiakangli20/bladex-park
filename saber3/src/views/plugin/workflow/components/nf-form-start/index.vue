<template>
  <component
    :is="isDialog ? (dialogType == 'drawer' ? 'el-drawer' : 'el-dialog') : 'nf-container'"
    v-if="moduleLoadInit && visible"
    v-model="visible"
    :title="process.name"
    :width="dialogWidth"
    :size="dialogWidth"
    align-center
    destroy-on-close
    class="nf-form-start"
  >
    <el-skeleton :loading="waiting" :rows="8">
      <nf-title
        v-if="!isDialog"
        :text-value="process.name"
        :style="{ fontSize: '20px', fontWeight: 'bold' }"
      ></nf-title>
      <el-card shadow="never" style="margin-top: 20px">
        <nf-form
          v-if="
            option &&
            ((option.column && option.column.length > 0) ||
              (option.group && option.group.length > 0))
          "
          v-model="form"
          ref="form"
          :option="option"
          :upload-preview="handleUploadPreview"
        >
        </nf-form>
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
          :class="isDialog && dialogType == 'dialog' ? 'foot-item-dialog' : 'foot-item'"
          :style="{
            width: isDialog
              ? dialogType == 'dialog'
                ? '100%'
                : `calc(${dialogWidth} - 10px)`
              : isCollapse
              ? 'calc(100% - 86px)'
              : 'calc(100% - 256px)',
          }"
        >
          <el-button type="primary" size="default" v-loading="loading" @click="handleStartProcess">
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
      v-if="showExamForm"
      ref="user-select"
      :check-type="checkType"
      :default-checked="defaultChecked"
      @onConfirm="handleUserSelectConfirm"
    ></nf-user-select>
  </component>
</template>

<script>
import NfExamineForm from '../nf-exam-form/index.vue';
import NfUserSelect from '../nf-user-select/index.vue';
import exForm from '../../mixins/ex-form';
import draft from '../../mixins/draft';

export default {
  components: {
    NfUserSelect,
    NfExamineForm,
  },
  mixins: [exForm, draft],
  expose: ['show', 'hide'],
  props: {
    props: {
      type: Object,
      default: () => ({}),
    },
    isDialog: {
      type: Boolean,
      default: false,
    },
    dialogType: {
      type: String,
      default: 'drawer',
    },
    dialogWidth: {
      type: String,
      default: '80%',
    },
  },
  watch: {
    props: {
      handler(val) {
        if (!val || Object.keys(val).length === 0) return;
        const { processDefId, processDefKey, params } = val;
        if (processDefId || processDefKey) this.getForm(processDefId, processDefKey);
        if (params) this.params = params;
      },
      deep: true,
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
      form: {},
      option: {},
      process: {},
      loading: false,
      params: null,
      visible: false,
    };
  },
  methods: {
    getForm(processId, processDefKey) {
      const _this = this;

      let param;
      let method;
      if (processId) {
        param = processId;
        method = 'getStartForm';
      } else if (processDefKey) {
        param = processDefKey;
        method = 'getStartFormByProcessDefKey';
      }
      _this[method](param).then(res => {
        let { process, form, startForm } = res;
        this.form.processId = process.id;
        if (form) {
          const option = { ...Function('"use strict";return (' + form + ')')(), menuBtn: false };
          const { column, group } = option;

          const groupArr = [];
          const columnArr = this.filterAvueColumn(column, startForm).column;
          if (group && group.length > 0) {
            // 处理group
            group.forEach(gro => {
              gro.column = this.filterAvueColumn(gro.column, startForm).column;
              if (gro.column.length > 0) groupArr.push(gro);
            });
          }
          if (option.initFunction) {
            option.initFunction = eval((option.initFunction + '').replace(/this/g, '_this'));
          }
          option.column = columnArr;
          option.group = groupArr;
          this.option = option;

          setTimeout(() => {
            if (!this.validatenull(this.params)) {
              this.form = { ...this.form, ...this.params };
              console.log('外部参数赋值', this.params);
            }
          }, 500);

          if (this.permission.wf_process_draft) {
            // 查询是否有草稿箱
            this.initDraft({ processDefId: process.id })
              .then(data => {
                this.$confirm('是否恢复之前保存的草稿？', '提示', {})
                  .then(() => {
                    this.form = JSON.parse(data);
                  })
                  .catch(() => {});
              })
              .catch(() => {});
          }
        }
        this.waiting = false;
      });
    },
    show() {
      this.visible = true;
    },
    hide() {
      this.visible = false;
    },
  },
};
</script>

<style lang="scss" scoped>
.nf-form-start {
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
  .foot-item-dialog {
    z-index: 101;
    height: 66px;
    background-color: #fff;
    display: flex;
    justify-content: center;
    align-items: center;
    -webkit-box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
}
</style>
