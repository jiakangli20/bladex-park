<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="980px"
    top="8vh"
    append-to-body
    destroy-on-close
    class="background-investigation-dialog"
  >
    <div v-loading="loading" class="background-investigation-content">
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane :label="`涉诉信息（${count('litigationList')}）`" name="litigation">
          <el-table :data="data.litigationList" border height="280" class="risk-table">
            <el-table-column
              prop="caseNo"
              label="案号"
              min-width="160"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              prop="caseReason"
              label="案由"
              min-width="150"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column prop="caseType" label="案件类型" width="120" align="center" />
            <el-table-column prop="caseStatus" label="案件状态" width="120" align="center" />
            <el-table-column prop="litigationStatus" label="诉讼地位" width="120" align="center" />
            <el-table-column
              prop="filingAmount"
              label="立案标的（元）"
              width="140"
              align="center"
            />
            <template #empty>
              <el-empty :image-size="64" description="暂无涉诉记录" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`被执行人记录（${count('executorList')}）`" name="executor">
          <el-table :data="data.executorList" border height="280" class="risk-table">
            <el-table-column prop="publishDate" label="发布日期" width="140" align="center" />
            <el-table-column
              prop="court"
              label="执行法院"
              min-width="190"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              prop="executionCaseNo"
              label="执行案号"
              min-width="190"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              prop="dishonestBehavior"
              label="失信行为"
              min-width="280"
              align="center"
              show-overflow-tooltip
            />
            <template #empty>
              <el-empty :image-size="64" description="暂无被执行人记录" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`行政处罚记录（${count('penaltyList')}）`" name="penalty">
          <el-table :data="data.penaltyList" border height="280" class="risk-table">
            <el-table-column
              prop="penaltyDepartment"
              label="处罚部门"
              min-width="160"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              prop="penaltyDecisionNo"
              label="处罚决定书号"
              min-width="180"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column prop="penaltyDate" label="处罚日期" width="130" align="center" />
            <el-table-column
              prop="illegalAct"
              label="违法行为"
              min-width="180"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column
              prop="illegalPeriod"
              label="违法行为所属期间"
              width="160"
              align="center"
            />
            <el-table-column
              prop="penaltyContent"
              label="处罚内容"
              min-width="220"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column prop="updateDate" label="信息更新日期" width="140" align="center" />
            <template #empty>
              <el-empty :image-size="64" description="暂无行政处罚记录" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane
          :label="`股东/高管关联风险记录（${count('relatedRiskList')}）`"
          name="relatedRisk"
        >
          <el-table :data="data.relatedRiskList" border height="280" class="risk-table">
            <el-table-column prop="name" label="姓名" width="120" align="center" />
            <el-table-column prop="identity" label="身份" width="140" align="center" />
            <el-table-column
              prop="riskType"
              label="关联风险类型"
              min-width="180"
              align="center"
              show-overflow-tooltip
            />
            <el-table-column prop="riskTime" label="风险发生时间" width="150" align="center" />
            <el-table-column
              prop="riskSummary"
              label="风险简要说明"
              min-width="300"
              align="center"
              show-overflow-tooltip
            />
            <template #empty>
              <el-empty :image-size="64" description="暂无股东/高管关联风险记录" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-dialog>
</template>

<script>
import { getOpportunityBackgroundByName } from '@/api/business/opportunity';

const emptyData = () => ({
  litigationList: [],
  executorList: [],
  penaltyList: [],
  relatedRiskList: [],
});

export default {
  name: 'BackgroundInvestigationDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    enterpriseName: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue'],
  data() {
    return {
      loading: false,
      activeTab: 'litigation',
      data: emptyData(),
      requestSequence: 0,
    };
  },
  computed: {
    visible: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      },
    },
    dialogTitle() {
      const name = (this.enterpriseName || '').trim();
      return name ? `背景调查 - ${name}` : '背景调查';
    },
  },
  watch: {
    modelValue(value) {
      if (value) {
        this.loadData();
      }
    },
  },
  methods: {
    count(key) {
      const list = this.data[key];
      return Array.isArray(list) ? list.length : 0;
    },
    normalizeData(value = {}) {
      return {
        litigationList: Array.isArray(value.litigationList) ? value.litigationList : [],
        executorList: Array.isArray(value.executorList) ? value.executorList : [],
        penaltyList: Array.isArray(value.penaltyList) ? value.penaltyList : [],
        relatedRiskList: Array.isArray(value.relatedRiskList) ? value.relatedRiskList : [],
      };
    },
    loadData() {
      const name = (this.enterpriseName || '').trim();
      this.activeTab = 'litigation';
      this.data = emptyData();
      if (!name) {
        this.$message.warning('缺少企业名称，无法查询背景调查');
        return;
      }

      const sequence = ++this.requestSequence;
      this.loading = true;
      getOpportunityBackgroundByName(name)
        .then(res => {
          if (sequence !== this.requestSequence) return;
          const payload = res && res.data ? res.data.data || res.data : {};
          this.data = this.normalizeData(payload);
        })
        .catch(() => {
          if (sequence === this.requestSequence) {
            this.data = emptyData();
          }
        })
        .finally(() => {
          if (sequence === this.requestSequence) {
            this.loading = false;
          }
        });
    },
  },
};
</script>

<style lang="scss">
.background-investigation-dialog {
  max-width: calc(100vw - 48px);
}

.background-investigation-dialog .el-dialog__header {
  margin-right: 0;
  padding: 18px 22px 14px;
  border-bottom: 1px solid #ebeef5;
}

.background-investigation-dialog .el-dialog__title {
  color: #1f2937;
  font-size: 17px;
  font-weight: 600;
}

.background-investigation-dialog .el-dialog__body {
  padding: 4px 22px 22px;
}

.background-investigation-content {
  min-height: 330px;
}

.background-investigation-content .el-tabs__header {
  margin-bottom: 12px;
}

.background-investigation-content .el-tabs__item {
  height: 50px;
  padding: 0 18px;
  color: #606266;
}

.background-investigation-content .el-tabs__item.is-active {
  color: #1059c6;
}

.background-investigation-content .el-tabs__active-bar {
  background-color: #1059c6;
}

.risk-table {
  width: 100%;
}

.risk-table .el-table__cell {
  text-align: center;
}

.risk-table .el-empty {
  padding: 30px 0;
}
</style>
