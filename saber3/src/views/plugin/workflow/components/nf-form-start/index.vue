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
            this.applyInitialParams();
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
    applyInitialParams() {
      if (this.validatenull(this.params)) return;
      const mappedValues = this.buildMappedParamValues(this.option, this.params);
      this.form = { ...this.form, ...this.params, ...mappedValues };
    },
    buildMappedParamValues(option = {}, params = {}) {
      const values = {};
      this.collectColumns(option).forEach(column => {
        if (!column || !column.prop || column.type === 'title') return;
        if (!this.validatenull(params[column.prop])) {
          values[column.prop] = params[column.prop];
          return;
        }
        const value = this.resolveParamValue(column, params);
        if (!this.validatenull(value)) values[column.prop] = value;
      });
      return values;
    },
    collectColumns(option = {}) {
      const columns = [];
      const visit = list => {
        (list || []).forEach(column => {
          columns.push(column);
          if (column.children && column.children.column) visit(column.children.column);
          if (column.column) visit(column.column);
          if (column.rows) {
            column.rows.forEach(row => {
              (row.cols || []).forEach(col => visit(col.column));
            });
          }
        });
      };
      visit(option.column);
      (option.group || []).forEach(group => visit(group.column));
      return columns;
    },
    resolveParamValue(column, params) {
      const label = String(column.label || column.textValue || '').replace(/[:：\s]/g, '');
      const pick = keys => {
        const key = keys.find(item => !this.validatenull(params[item]));
        return key ? params[key] : undefined;
      };
      if (label.includes('合同编号')) return pick(['contractNo']);
      if (label.includes('合同名称')) return pick(['contractName']);
      if (label.includes('送审部门')) {
        return pick(['applicantDept', 'deptName', 'createDeptName']);
      }
      if (label.includes('合同甲方') || label.includes('甲方')) {
        return pick(['contractPartyA', 'partyA', 'lessorName', 'ownerName', 'parkName']);
      }
      if (
        label.includes('合同乙方') ||
        label.includes('乙方') ||
        label.includes('企业名称') ||
        label.includes('客户名称')
      ) {
        return pick(['contractPartyB', 'partyB', 'lesseeName', 'customerName']);
      }
      if (label.includes('房源') || label.includes('房屋')) {
        return pick(['roomName', 'buildingName']);
      }
      if (label.includes('租赁面积') || label.includes('面积')) return pick(['rentArea']);
      if (label.includes('租赁单价') || label.includes('租金单价')) return pick(['rentPrice']);
      if (label.includes('月租金') || label.includes('租金金额')) return pick(['monthlyRent']);
      if (label.includes('保证金') || label.includes('押金')) return pick(['deposit']);
      if (
        label.includes('实收金额') ||
        label.includes('付款金额') ||
        label.includes('缴费金额') ||
        label.includes('支付金额')
      ) {
        return pick(['amountPaid', 'payAmount', 'paymentAmount', 'amountDue']);
      }
      if (label.includes('开票金额') || label.includes('发票金额')) {
        return pick(['invoiceAmount', 'amountDue', 'unpaidAmount']);
      }
      if (label.includes('退款金额') || label.includes('退还金额') || label.includes('应退押金')) {
        return pick(['refundAmount', 'amountDue', 'deposit']);
      }
      if (label.includes('欠费金额') || label.includes('未缴金额')) {
        return pick(['unpaidAmount', 'amountDue']);
      }
      if (label.includes('费用类型') || label.includes('费用名称')) return pick(['feeName', 'feeType']);
      if (label.includes('账期') || label.includes('所属期')) {
        return pick(['periodText']) || this.buildPeriodText(params);
      }
      if (label.includes('应缴日期') || label.includes('缴费截止') || label.includes('付款截止')) {
        return pick(['payDeadline']);
      }
      if (label.includes('逾期天数')) return pick(['overdueDays']);
      if (label.includes('合同事项') || label.includes('事项')) {
        return pick(['contractMatter', 'contractItem', 'remark']) || this.buildContractMatter(params);
      }
      if (label.includes('开始') || label.includes('起租')) return pick(['startDate']);
      if (label.includes('结束') || label.includes('到期')) return pick(['endDate']);
      if (label.includes('签订')) return pick(['signDate']);
      return undefined;
    },
    buildContractMatter(params = {}) {
      const customer = params.customerName || params.contractName || '';
      const room = params.roomName || params.buildingName || '';
      const dateRange =
        params.startDate || params.endDate
          ? `，期限${params.startDate || '--'}至${params.endDate || '--'}`
          : '';
      const roomText = room ? `，房源${room}` : '';
      return `${customer}租赁合同签订${roomText}${dateRange}`;
    },
    buildPeriodText(params = {}) {
      if (!params.periodStart && !params.periodEnd) return undefined;
      return `${params.periodStart || '--'} 至 ${params.periodEnd || '--'}`;
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
