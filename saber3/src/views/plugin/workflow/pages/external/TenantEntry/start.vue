<template>
  <nf-container v-if="moduleLoadInit">
    <el-skeleton :loading="waiting" avatar :rows="8">
      <div class="tenant-entry-form">
        <h3>{{ process.name || '企业入驻审批' }}</h3>
        <nf-form ref="form" v-model="form" :option="option" @submit="handleSubmit" @error="loading = false" />
      </div>
      <div style="height: 80px"></div>
      <el-affix position="bottom" :offset="20">
        <el-row class="foot-item" :style="{ width: isCollapse ? 'calc(100% - 71px)' : 'calc(100% - 241px)' }">
          <el-button type="primary" size="default" v-loading="loading" @click="handleSubmit">发起</el-button>
        </el-row>
      </el-affix>
    </el-skeleton>
  </nf-container>
</template>

<script>
import { mapGetters } from 'vuex';
import exForm from '../../../mixins/ex-form';
import { getOpportunityDetail } from '@/api/business/opportunity';
import { getCustomerDetail } from '@/api/business/customer';

const DEFAULT_PROCESS_KEY = 'tenant_entry-1';
const BUSINESS_TYPE = 'tenant_entry';

export default {
  mixins: [exForm],
  computed: {
    ...mapGetters(['isCollapse', 'userInfo']),
  },
  watch: {
    '$route.query.p': {
      handler(val) {
        if (!val) return;
        const param = JSON.parse(window.atob(decodeURIComponent(val)));
        const { processId, processDefKey, params } = param;
        if (processId || processDefKey) this.getForm(processId, processDefKey);
        if (params) this.initParams(params);
      },
      immediate: true,
    },
  },
  mounted() {
    if (!this.$route.query.p) {
      this.waiting = false;
    }
  },
  data() {
    return {
      waiting: true,
      loading: false,
      process: {},
      form: {
        processDefKey: DEFAULT_PROCESS_KEY,
        businessType: BUSINESS_TYPE,
        applicant: '',
        applicantDept: '',
      },
      option: {
        menuBtn: false,
        labelWidth: 130,
        column: [
          { label: '企业名称', prop: 'enterpriseName', type: 'input', span: 12, rules: [{ required: true, message: '请输入企业名称', trigger: 'blur' }] },
          { label: '申请时间', prop: 'applyTime', type: 'date', format: 'YYYY-MM-DD', valueFormat: 'YYYY-MM-DD', span: 12 },
          { label: '股东信息', prop: 'shareholderInfo', type: 'input', span: 24, placeholder: '如：自然人、社会团体、企业法人等' },
          { label: '经营范围', prop: 'businessScope', type: 'textarea', span: 24, minRows: 4, rules: [{ required: true, message: '请输入经营范围', trigger: 'blur' }] },
          { label: '负责人', prop: 'principalName', type: 'input', span: 12, rules: [{ required: true, message: '请输入负责人', trigger: 'blur' }] },
          { label: '联系方式', prop: 'principalPhone', type: 'input', span: 12, rules: [{ required: true, message: '请输入联系方式', trigger: 'blur' }] },
          { label: '租赁楼层、面积', prop: 'leaseFloorArea', type: 'input', span: 12, placeholder: '如：2111,141平' },
          { label: '免租期', prop: 'rentFreePeriod', type: 'input', span: 12 },
          { label: '单价（元）', prop: 'unitPrice', type: 'number', span: 12, precision: 2, controls: false },
          { label: '保证金（元）', prop: 'deposit', type: 'number', span: 12, precision: 2, controls: false },
          { label: '合同有效期', prop: 'contractPeriod', type: 'input', span: 24, placeholder: '如：自2026年2月1日至2027年1月31日止' },
          { label: '经办人', prop: 'handlerName', type: 'input', span: 12 },
          { label: '部门', prop: 'handlerDept', type: 'input', span: 12 },
          { label: '审批事项', prop: 'approvalMatter', type: 'textarea', span: 24, minRows: 3 },
          {
            label: '附件',
            prop: 'attachment',
            type: 'upload',
            action: '/blade-resource/oss/endpoint/put-file',
            propsHttp: { res: 'data', url: 'link', name: 'originalName' },
            dataType: 'string',
            span: 24,
            showFileList: true,
            multiple: true,
          },
        ],
      },
    };
  },
  methods: {
    getForm(processId, processDefKey) {
      const method = processId ? 'getStartForm' : 'getStartFormByProcessDefKey';
      this[method](processId || processDefKey).then(res => {
        if (!res || !res.process) {
          this.$message.warning(`未读取到入驻审批流程配置，请确认 ${processDefKey || DEFAULT_PROCESS_KEY} 已部署`);
        }
        this.form.processDefKey = processDefKey || this.form.processDefKey || DEFAULT_PROCESS_KEY;
        this.form.businessType = BUSINESS_TYPE;
        this.waiting = false;
      }).catch(() => {
        this.$message.warning(`未读取到入驻审批流程配置，请确认 ${processDefKey || DEFAULT_PROCESS_KEY} 已部署`);
        this.waiting = false;
      });
    },
    initParams(params) {
      this.form = {
        ...this.form,
        businessType: BUSINESS_TYPE,
        processDefKey: params.processDefKey || this.form.processDefKey || DEFAULT_PROCESS_KEY,
        applicant: this.form.applicant || this.userInfo.nick_name,
        applicantDept: this.form.applicantDept || this.userInfo.dept_name,
        applyTime: this.form.applyTime || this.formatDate(new Date()),
        ...params,
      };
      if (params.opportunityId) {
        getOpportunityDetail(params.opportunityId).then(res => {
          this.mergeBusinessData(res.data.data || {});
        });
      } else if (params.customerId) {
        getCustomerDetail(params.customerId).then(res => {
          this.mergeBusinessData(res.data.data || {});
        });
      }
    },
    mergeBusinessData(data) {
      const enterpriseName = data.enterpriseName || this.form.enterpriseName;
      const floorArea = [
        data.leaseFloor,
        this.formatArea(data.intentArea),
      ].filter(Boolean).join('，');
      this.form = {
        ...this.form,
        businessKey: data.opportunityId || this.form.businessKey,
        opportunityId: data.opportunityId || this.form.opportunityId,
        customerId: data.customerId || this.form.customerId,
        enterpriseName,
        creditCode: data.creditCode || this.form.creditCode,
        shareholderInfo: data.equityStructure || data.enterpriseType || this.form.shareholderInfo,
        businessScope: data.businessScope || this.form.businessScope,
        principalName: data.contactName || this.form.principalName,
        principalPhone: data.contactPhone || this.form.principalPhone,
        leaseFloorArea: floorArea || this.form.leaseFloorArea,
        contractPeriod: data.leaseTermLabel || (data.leaseTermYears ? `${data.leaseTermYears}年` : this.form.contractPeriod),
        handlerName: data.followUser || this.form.handlerName || this.userInfo.nick_name,
        handlerDept: this.form.handlerDept || this.userInfo.dept_name,
        approvalMatter: data.remark || data.mainBusiness || this.form.approvalMatter,
      };
    },
    formatArea(value) {
      if (value === undefined || value === null || value === '') return '';
      return `${value}平`;
    },
    formatDate(date) {
      const y = date.getFullYear();
      const m = date.getMonth() + 1;
      const d = date.getDate();
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    },
    handleSubmit() {
      this.form.processDefKey = this.form.processDefKey || DEFAULT_PROCESS_KEY;
      this.form.businessType = BUSINESS_TYPE;
      this.form.applicant = this.form.applicant || this.userInfo.nick_name;
      this.form.applicantDept = this.form.applicantDept || this.userInfo.dept_name;
      this.form.businessKey = this.form.opportunityId || this.form.customerId || this.form.businessKey;
      this.handleStartProcessByKey(true).then((res, done) => {
        this.$message.success('发起成功');
        this.handleCloseTag('/settlement/project-approval?scope=send');
        if (typeof done === 'function') done();
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.tenant-entry-form {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
}

.tenant-entry-form h3 {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
}

.foot-item {
  position: fixed;
  bottom: 5px;
  margin-left: -20px;
  z-index: 101;
  height: 66px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
