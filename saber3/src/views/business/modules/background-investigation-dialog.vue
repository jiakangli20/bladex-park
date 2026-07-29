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
      <el-alert
        title="第三方工商及司法风险查询待接入，当前页面仅展示和保存人工核验结果。"
        type="warning"
        :closable="false"
        show-icon
        class="background-investigation-alert"
      />

      <el-form :model="form" label-width="104px" class="manual-investigation-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="核验状态">
              <el-select v-model="form.verifyStatus" style="width: 100%">
                <el-option label="已核验" value="1" />
                <el-option label="需补充" value="2" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险等级">
              <el-select v-model="form.riskLevel" style="width: 100%">
                <el-option label="未发现风险" value="0" />
                <el-option label="低风险" value="1" />
                <el-option label="中风险" value="2" />
                <el-option label="高风险" value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="风险标记" class="risk-flag-item">
              <el-checkbox v-model="riskFlags.legal">法律</el-checkbox>
              <el-checkbox v-model="riskFlags.executive">高管</el-checkbox>
              <el-checkbox v-model="riskFlags.shareholder">股东</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险摘要">
              <el-input
                v-model="form.riskSummary"
                maxlength="1000"
                show-word-limit
                placeholder="请填写人工核验结论"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="信息来源">
              <el-input
                v-model="form.sourceRemark"
                maxlength="500"
                show-word-limit
                placeholder="例如：国家企业信用信息公示系统、线下材料"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="manual-investigation-meta">
          <span>最近核验人：{{ valueText(latest.createBy) }}</span>
          <span>最近核验时间：{{ valueText(latest.createTime) }}</span>
          <el-button type="primary" :loading="saving" @click="saveInvestigation"
            >保存核验结果</el-button
          >
        </div>
      </el-form>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane :label="`历史核验记录（${history.length}）`" name="history">
          <el-table :data="history" border max-height="280" class="risk-table">
            <el-table-column prop="createTime" label="核验时间" width="170" align="center">
              <template #default="{ row }">{{ valueText(row.createTime) }}</template>
            </el-table-column>
            <el-table-column prop="createBy" label="核验人" width="110" align="center">
              <template #default="{ row }">{{ valueText(row.createBy) }}</template>
            </el-table-column>
            <el-table-column prop="verifyStatus" label="核验状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="verifyType(row.verifyStatus)" effect="plain">
                  {{ verifyText(row.verifyStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险等级" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="riskType(row.riskLevel)" effect="plain">
                  {{ riskText(row.riskLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="风险标记" min-width="150" align="center">
              <template #default="{ row }">{{ riskFlagsText(row) }}</template>
            </el-table-column>
            <el-table-column
              prop="riskSummary"
              label="风险摘要"
              min-width="190"
              align="center"
              show-overflow-tooltip
            >
              <template #default="{ row }">{{ valueText(row.riskSummary) }}</template>
            </el-table-column>
            <el-table-column
              prop="sourceRemark"
              label="信息来源"
              min-width="170"
              align="center"
              show-overflow-tooltip
            >
              <template #default="{ row }">{{ valueText(row.sourceRemark) }}</template>
            </el-table-column>
            <template #empty>
              <el-empty :image-size="64" description="暂无历史核验记录" />
            </template>
          </el-table>
        </el-tab-pane>

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
import {
  getOpportunityBackgroundByName,
  saveOpportunityBackground,
} from '@/api/business/opportunity';

const emptyData = () => ({
  litigationList: [],
  executorList: [],
  penaltyList: [],
  relatedRiskList: [],
});

const emptyForm = () => ({
  verifyStatus: '1',
  riskLevel: '0',
  riskSummary: '',
  sourceRemark: '',
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
  emits: ['update:modelValue', 'saved'],
  data() {
    return {
      loading: false,
      activeTab: 'history',
      data: emptyData(),
      history: [],
      latest: {},
      form: emptyForm(),
      riskFlags: {
        legal: false,
        executive: false,
        shareholder: false,
      },
      saving: false,
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
    normalizeHistory(value = {}) {
      return Array.isArray(value.history) ? value.history : [];
    },
    applyLatest(value = {}) {
      const latest = value.latest || {};
      this.latest = latest;
      this.form = {
        verifyStatus: latest.verifyStatus || '1',
        riskLevel: latest.riskLevel || '0',
        riskSummary: latest.riskSummary || '',
        sourceRemark: latest.sourceRemark || '',
      };
      this.riskFlags = {
        legal: latest.legalRiskFlag === '1',
        executive: latest.executiveRiskFlag === '1',
        shareholder: latest.shareholderRiskFlag === '1',
      };
    },
    loadData() {
      const name = (this.enterpriseName || '').trim();
      this.activeTab = 'history';
      this.data = emptyData();
      this.history = [];
      this.latest = {};
      this.form = emptyForm();
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
          this.history = this.normalizeHistory(payload);
          this.applyLatest(payload);
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
    saveInvestigation() {
      const enterpriseName = (this.enterpriseName || '').trim();
      if (!enterpriseName) return;
      this.saving = true;
      saveOpportunityBackground({
        enterpriseName,
        ...this.form,
        legalRiskFlag: this.riskFlags.legal ? '1' : '0',
        executiveRiskFlag: this.riskFlags.executive ? '1' : '0',
        shareholderRiskFlag: this.riskFlags.shareholder ? '1' : '0',
      })
        .then(res => {
          const payload = res && res.data ? res.data.data || res.data : {};
          this.data = this.normalizeData(payload);
          this.history = this.normalizeHistory(payload);
          this.applyLatest(payload);
          this.$message.success('人工核验结果已保存');
          this.$emit('saved');
        })
        .finally(() => {
          this.saving = false;
        });
    },
    valueText(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    verifyText(value) {
      return String(value) === '2' ? '需补充' : '已核验';
    },
    verifyType(value) {
      return String(value) === '2' ? 'warning' : 'success';
    },
    riskText(value) {
      const map = {
        0: '未发现风险',
        1: '低风险',
        2: '中风险',
        3: '高风险',
      };
      return map[String(value)] || '未发现风险';
    },
    riskType(value) {
      const map = {
        0: 'success',
        1: 'success',
        2: 'warning',
        3: 'danger',
      };
      return map[String(value)] || 'info';
    },
    riskFlagsText(row = {}) {
      const labels = [];
      if (String(row.legalRiskFlag) === '1') labels.push('法律');
      if (String(row.executiveRiskFlag) === '1') labels.push('高管');
      if (String(row.shareholderRiskFlag) === '1') labels.push('股东');
      return labels.length ? labels.join('、') : '无';
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

.background-investigation-alert {
  margin: 14px 0 16px;
}

.manual-investigation-form {
  padding: 16px 16px 8px;
  margin-bottom: 14px;
  border: 1px solid #e5e7eb;
  background: #f8fafc;
}

.manual-investigation-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
  min-height: 40px;
  color: #606266;
  font-size: 13px;
}

.manual-investigation-meta .el-button {
  margin-left: auto;
}

.risk-flag-item .el-form-item__content {
  white-space: nowrap;
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
