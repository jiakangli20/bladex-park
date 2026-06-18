<template>
  <nf-container v-if="moduleLoadInit">
    <el-skeleton :loading="waiting" avatar :rows="8">
      <h3 style="margin-bottom: 20px">{{ process.name }}</h3>
      <el-card shadow="never" style="margin-top: 20px">
        <!-- 自定义表单区域 -->
        <nf-form
          v-if="
            option &&
            ((option.column && option.column.length > 0) ||
              (option.group && option.group.length > 0))
          "
          v-model="form"
          ref="form"
          :option="option"
          @submit="handleSubmit"
          @error="loading = false"
          :upload-preview="handleUploadPreview"
        >
        </nf-form>
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
import { submit } from '../../../api/demo/leave.js';
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
          const { processId, processDefKey, params } = param;
          if (processId || processDefKey) this.getForm(processId, processDefKey);
          if (params) this.params = params;
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
      form: {},
      option: {
        menuBtn: false,
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
                this.form.day = undefined;
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
      process: {},
      loading: false,
      params: null,
    };
  },
  methods: {
    getForm(processId, processDefKey) {
      let param;
      let method;
      if (processId) {
        param = processId;
        method = 'getStartForm';
      } else if (processDefKey) {
        param = processDefKey;
        method = 'getStartFormByProcessDefKey';
      }
      this[method](param).then(res => {
        let { process, startForm } = res;
        this.form.processId = process.id;
        const option = this.option;
        const { column, group } = option;

        const groupArr = [];
        const columnArr = this.filterAvueColumn(column, startForm, true).column;
        if (group && group.length > 0) {
          // 处理group
          group.forEach(gro => {
            gro.column = this.filterAvueColumn(gro.column, startForm, true).column;
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
          this.initDraft({ processDefId: process.id }).then(data => {
            this.$confirm('是否恢复之前保存的草稿？', '提示', {})
              .then(() => {
                this.form = JSON.parse(data);
              })
              .catch(() => {});
          });
        }
        this.waiting = false;
      });
    },
    handleSubmit() {
      let form = this.deepClone(this.form);
      form = {
        ...form,
        processDefId: form.processId,
        formKey: this.process.formKey,
        formUrl: this.process.formUrl,
      };
      submit(form).then(res => {
        const data = res.data.data;
        this.form.processDefKey = this.process.key;
        this.form.businessKey = data.id;
        this.handleStartProcessByKey(true)
          .then((res, done) => {
            const processInsId = res.data.data;
            submit({ id: data.id, processInsId }).then(() => {
              this.$message.success('发起成功');
              this.handleCloseTag('/plugin/workflow/pages/process/send');
              if (typeof done == 'function') done();
            });
          })
          .catch(() => {
            this.loading = false;
          });
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
