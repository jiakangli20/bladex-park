<template>
  <basic-container>
    <div class="contract-page">
      <el-alert
        v-if="expiringData.length"
        type="warning"
        show-icon
        :closable="false"
        :description="expiringSummary"
        class="contract-alert"
      >
        <template #title>
          合同到期提醒：当前有 {{ expiringData.length }} 份合同进入续签提醒周期
        </template>
      </el-alert>

      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="contract-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input
              v-model="query.contractNo"
              clearable
              placeholder="请输入合同编号"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="客户名称">
            <el-input
              v-model="query.customerName"
              clearable
              placeholder="请输入客户名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="合同状态">
            <el-select v-model="query.contractStatus" clearable placeholder="请选择">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="contract-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建合同</el-button>
          <el-button
            type="success"
            plain
            icon="el-icon-s-promotion"
            :disabled="!singleSelectedContract || !canStartApproval(singleSelectedContract)"
            @click="handleStartApprovalFromSelection"
          >
            发起合同审批
          </el-button>
          <el-button
            v-if="permission.contract_contract_delete"
            type="danger"
            plain
            icon="el-icon-delete"
            :disabled="selectionList.length === 0"
            @click="handleDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <el-table
        v-loading="loading"
        :data="data"
        border
        row-key="contractId"
        class="contract-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column
          prop="customerName"
          label="租客名称"
          min-width="180"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-button text type="primary" class="tenant-name-btn" @click="openDetail(row)">
              {{ row.customerName || '-' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          prop="contractNo"
          label="合同编号"
          min-width="170"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column prop="contractStatus" label="合同状态" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.contractStatus)" effect="plain">
              {{ row.contractStatusName || statusText(row.contractStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="approvalStatus"
          label="审批状态"
          width="120"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-tag :type="approvalStatusType(row.approvalStatus)" effect="plain">
              {{ approvalStatusText(row.approvalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="parkName"
          label="园区名称"
          width="130"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          prop="roomName"
          label="房源信息"
          width="130"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span>{{ row.roomName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日" width="115" align="center" />
        <el-table-column prop="endDate" label="结束日" width="145" align="center">
          <template #default="{ row }">
            <span>{{ row.endDate || '-' }}</span>
            <el-tag v-if="isExpiringSoon(row)" type="warning" effect="plain" class="inline-tag"
              >即将到期</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="rentPrice" label="租赁单价" width="120" align="center">
          <template #default="{ row }">{{ formatUnitPrice(row.rentPrice) }}</template>
        </el-table-column>
        <el-table-column prop="signDate" label="签订日期" width="120" align="center">
          <template #default="{ row }">{{ row.signDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" @click="openDetail(row)">详情</el-button>
              <el-button text type="primary" @click="handleArchive(row)">归档</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="contract-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-sizes="[10, 20, 30, 40, 50, 100]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>

      <el-drawer v-model="detailVisible" :title="detailTitle" size="980px" append-to-body>
        <section class="detail-head">
          <div>
            <h3>{{ detailContract.contractName || '合同详情' }}</h3>
            <span>{{ detailContract.contractNo || '-' }}</span>
          </div>
          <div class="detail-actions">
            <el-button
              v-if="canStartApproval(detailContract)"
              type="success"
              plain
              @click="handleStartApproval(detailContract)"
            >
              发起合同审批
            </el-button>
            <el-button type="primary" plain @click="handleArchive(detailContract)"
              >合同归档</el-button
            >
            <el-button
              v-if="detailContract.contractId"
              type="primary"
              plain
              @click="handleRenew(detailContract)"
            >
              合同续租
            </el-button>
            <el-button
              v-if="canUploadSignedContract(detailContract)"
              type="primary"
              @click="handleSignedUpload(detailContract)"
            >
              上传盖章合同
            </el-button>
            <el-button
              v-if="canStartTermination(detailContract)"
              type="warning"
              plain
              @click="handleStartTermination(detailContract)"
            >
              发起退租审批
            </el-button>
            <el-button
              v-if="canStartRoomReview(detailContract)"
              type="primary"
              plain
              @click="handleStartRoomReview(detailContract)"
            >
              发起房屋验收
            </el-button>
            <el-button
              v-if="permission.contract_contract_terminate && detailContract.contractStatus !== '4'"
              type="danger"
              @click="handleTerminate(detailContract)"
            >
              作废
            </el-button>
          </div>
        </section>

        <el-alert
          class="detail-alert"
          type="info"
          show-icon
          :closable="false"
          :title="contractSummary"
        />

        <el-tabs v-model="detailTab">
          <el-tab-pane label="合同信息" name="info">
            <section class="detail-section">
              <div class="detail-section-title">基本信息</div>
              <div class="contract-field-grid">
                <div v-for="item in basicDetailItems" :key="item.label" class="contract-field">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-title">房源信息</div>
              <div class="contract-field-grid">
                <div class="contract-field">
                  <span>房源信息</span>
                  <strong>{{ detailContract.roomName || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>面积</span>
                  <strong>{{ formatAreaValue(detailContract.rentArea) }}</strong>
                </div>
                <div class="contract-field">
                  <span>建筑名称</span>
                  <strong>{{ detailContract.buildingName || '-' }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-title">租客信息</div>
              <div class="contract-field-grid">
                <div class="contract-field">
                  <span>租客</span>
                  <strong>{{ detailContract.customerName || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>跟进人</span>
                  <strong>{{ detailContract.followUser || detailContract.createBy || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>备注</span>
                  <strong>{{ detailContract.remark || '-' }}</strong>
                </div>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="账单列表" name="payment">
            <el-table v-loading="paymentLoading" :data="paymentData" border>
              <el-table-column prop="feeName" label="费用类型" width="110" align="center" />
              <el-table-column label="费用期间" min-width="210" align="center">
                <template #default="{ row }"
                  >{{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}</template
                >
              </el-table-column>
              <el-table-column prop="amountDue" label="应缴金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
              </el-table-column>
              <el-table-column prop="amountPaid" label="已缴金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
              </el-table-column>
              <el-table-column prop="payDeadline" label="到期日" width="120" align="center" />
              <el-table-column prop="payStatus" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="paymentTagType(row)" effect="plain">{{
                    paymentStatusText(row.payStatus)
                  }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="overdueApprovalStatus"
                label="逾期审批"
                width="110"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    v-if="row.overdueApprovalStatus"
                    :type="approvalStatusType(row.overdueApprovalStatus)"
                    effect="plain"
                  >
                    {{ approvalStatusText(row.overdueApprovalStatus) }}
                  </el-tag>
                  <span v-else class="muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.payStatus !== '1'"
                    text
                    type="primary"
                    @click="handleConfirmPayment(row)"
                    >确认缴费</el-button
                  >
                  <el-button
                    v-if="row.payStatus !== '1'"
                    text
                    type="primary"
                    @click="handleRemind(row)"
                    >催缴</el-button
                  >
                  <el-button
                    v-if="isPaymentOverdue(row)"
                    text
                    type="danger"
                    :disabled="row.overdueApprovalStatus === 'running'"
                    @click="handleStartOverdueApproval(row)"
                  >
                    逾期处理
                  </el-button>
                  <span v-else class="muted">已完成</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="审批记录" name="workflow">
            <el-table v-loading="workflowLoading" :data="workflowData" border>
              <el-table-column prop="businessType" label="业务类型" width="140" align="center">
                <template #default="{ row }">
                  {{ workflowBusinessTypeText(row.businessType) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="processName"
                label="流程名称"
                min-width="160"
                show-overflow-tooltip
              />
              <el-table-column
                prop="processInsId"
                label="实例ID"
                min-width="180"
                show-overflow-tooltip
              />
              <el-table-column prop="processStatus" label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="workflowStatusType(row.processStatus)" effect="plain">
                    {{ workflowStatusText(row.processStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="currentNode"
                label="当前节点"
                min-width="140"
                show-overflow-tooltip
              />
              <el-table-column prop="approvalTime" label="完成时间" width="170" align="center">
                <template #default="{ row }">
                  {{ row.approvalTime || row.createTime || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="printFileUrl" label="文件" width="110" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.printFileUrl"
                    text
                    type="primary"
                    @click="openWorkflowFile(row.printFileUrl)"
                  >
                    查看
                  </el-button>
                  <span v-else class="muted">-</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="workflowData.length === 0" description="暂无审批记录" />
          </el-tab-pane>

          <el-tab-pane label="操作日志" name="log">
            <el-timeline>
              <el-timeline-item
                v-for="item in logData"
                :key="item.logId"
                :timestamp="item.operateTime"
                placement="top"
              >
                <div class="contract-log">
                  <div class="contract-log__title">{{ item.actionDesc || item.action }}</div>
                  <div class="contract-log__operator">{{ item.operator }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="logData.length === 0" description="暂无操作记录" />
          </el-tab-pane>
        </el-tabs>
      </el-drawer>

      <el-drawer v-model="paymentBox" title="缴费计划" size="760px" append-to-body>
        <el-table v-loading="paymentLoading" :data="paymentData" border>
          <el-table-column prop="feeName" label="费用" width="110" />
          <el-table-column prop="periodStart" label="账期开始" width="110" />
          <el-table-column prop="periodEnd" label="账期结束" width="110" />
          <el-table-column prop="amountDue" label="应收" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="实收" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="paymentTagType(row)" effect="plain">{{
                paymentStatusText(row.payStatus)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.payStatus !== '1'"
                type="primary"
                text
                @click="handleConfirmPayment(row)"
                >确认</el-button
              >
              <el-button v-if="row.payStatus !== '1'" type="primary" text @click="handleRemind(row)"
                >催缴</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </el-drawer>

      <el-dialog
        v-model="approvalVisible"
        :title="approvalDialogTitle"
        width="760px"
        append-to-body
        @close="resetApprovalDialog"
      >
        <div class="approval-dialog">
          <section class="detail-section">
            <div class="detail-section-title">{{ approvalSummaryTitle }}</div>
            <div class="contract-field-grid">
              <div v-for="item in approvalSummaryItems" :key="item.label" class="contract-field">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title">审批配置</div>
            <el-form :model="approvalForm" label-width="108px">
              <el-form-item label="审批流程" required>
                <el-select
                  v-model="approvalForm.processDefKey"
                  filterable
                  :loading="approvalLoading"
                  :placeholder="approvalProcessPlaceholder"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in approvalProcessOptions"
                    :key="item.id || item.key"
                    :label="approvalProcessLabel(item)"
                    :value="item.key"
                  >
                    <div class="approval-option">
                      <span>{{ item.name || item.key }}</span>
                      <em
                        >{{ item.key
                        }}<template v-if="item.version"> / v{{ item.version }}</template></em
                      >
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-form>
          </section>

          <section v-if="isTerminationWorkflow" class="detail-section">
            <div class="detail-section-title">退租信息</div>
            <el-form :model="approvalExtra" label-width="108px">
              <el-form-item label="退租类型">
                <el-radio-group v-model="approvalExtra.terminationType">
                  <el-radio-button label="normal">正常退租</el-radio-button>
                  <el-radio-button label="early">客户提前退租</el-radio-button>
                  <el-radio-button label="special">特殊情况</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="退租日期">
                <el-date-picker
                  v-model="approvalExtra.expectedTerminationDate"
                  value-format="YYYY-MM-DD"
                  type="date"
                  placeholder="请选择退租日期"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item v-if="approvalExtra.terminationType !== 'normal'" label="违约金">
                <el-input-number
                  v-model="approvalExtra.breachPenalty"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="退租原因">
                <el-input
                  v-model="approvalExtra.terminationReason"
                  type="textarea"
                  :rows="3"
                  placeholder="请填写退租原因"
                />
              </el-form-item>
            </el-form>
          </section>
        </div>
        <template #footer>
          <el-button @click="approvalVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!approvalForm.processDefKey || approvalLoading"
            @click="goWorkflowApproval"
          >
            下一步
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="renewVisible"
        title="合同续租"
        width="552px"
        append-to-body
        class="renew-dialog"
        @close="resetRenewDialog"
      >
        <el-alert
          class="renew-tip"
          title="提示"
          type="info"
          show-icon
          :closable="false"
          description="合同续租，将使用原有条款新建合同，新合同将在租期起日开始执行"
        />
        <el-form ref="renewFormRef" :model="renewForm" :rules="renewRules" label-position="top">
          <el-form-item label="租期起始日期" prop="dateRange" required>
            <el-date-picker
              v-model="renewForm.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="renewVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRenew">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="signedUploadVisible"
        title="上传盖章合同"
        width="560px"
        append-to-body
        @close="resetSignedUpload"
      >
        <section class="signed-upload-summary">
          <div>
            <span>合同编号</span>
            <strong>{{ signedUploadContract.contractNo || '-' }}</strong>
          </div>
          <div>
            <span>企业名称</span>
            <strong>{{ signedUploadContract.customerName || '-' }}</strong>
          </div>
        </section>
        <el-upload
          drag
          ref="signedUploadRef"
          action="/api/blade-resource/oss/endpoint/put-file"
          :headers="uploadHeaders"
          :limit="1"
          :show-file-list="true"
          accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
          :before-upload="beforeSignedFileUpload"
          :on-success="handleSignedFileSuccess"
          :on-error="handleSignedFileError"
          :on-remove="handleSignedFileRemove"
          class="signed-upload-box"
        >
          <div class="signed-upload-text">
            <strong>上传已盖章合同扫描件</strong>
            <span>支持 PDF、Word、图片等合同归档文件</span>
          </div>
        </el-upload>
        <template #footer>
          <el-button @click="signedUploadVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="signedUploadLoading"
            :disabled="!signedUploadForm.contractFileUrl"
            @click="submitSignedUpload"
          >
            确认生效
          </el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { Base64 } from 'js-base64';
import {
  confirmPayment,
  getDetail,
  getExpiring,
  getList,
  getLogs,
  getPayment,
  getStats,
  getWorkflowRecords,
  remindPayment,
  remove,
  terminate,
  uploadSignedContract,
} from '@/api/contract/contract';
import { downloadBlob } from '@/api/common';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { paymentStatusDic, statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import { downloadFile } from '@/utils/util';

const CONTRACT_APPROVAL_BUSINESS_TYPE = 'contract_approval';
const CONTRACT_PAYMENT_BUSINESS_TYPE = 'contract_payment';
const CONTRACT_ROOM_REVIEW_BUSINESS_TYPE = 'contract_room_review';
const CONTRACT_TERMINATION_BUSINESS_TYPE = 'contract_termination';
const CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE = 'contract_overdue_legal';
const WORKFLOW_TYPES = {
  contract_approval: {
    title: '发起合同审批',
    summaryTitle: '合同信息',
    processPlaceholder: '请选择已部署的合同审批流程',
    formKeys: ['contract', 'wf_ex_contract'],
    defaultKeys: ['contract_sign'],
    nameKeywords: ['合同审批', '合同'],
  },
  contract_payment: {
    title: '发起付款/开票流程',
    summaryTitle: '账单信息',
    processPlaceholder: '请选择已部署的付款/开票流程',
    formKeys: ['invoice'],
    defaultKeys: ['pay'],
    nameKeywords: ['开票', '付款'],
  },
  contract_room_review: {
    title: '发起房屋验收',
    summaryTitle: '退租交接信息',
    processPlaceholder: '请选择已部署的房屋验收流程',
    formKeys: ['return'],
    defaultKeys: ['roomreview'],
    nameKeywords: ['房屋验收', '交接验收', '归还载体'],
  },
  contract_termination: {
    title: '发起退租审批',
    summaryTitle: '退租信息',
    processPlaceholder: '请选择已部署的退租审批流程',
    formKeys: ['terminate'],
    defaultKeys: ['termination'],
    nameKeywords: ['退租审批', '退租'],
  },
  contract_overdue_legal: {
    title: '发起逾期律师函审批',
    summaryTitle: '逾期账单信息',
    processPlaceholder: '请选择已部署的逾期律师函流程',
    formKeys: ['project-approval-law'],
    defaultKeys: ['law'],
    nameKeywords: ['律师函', '逾期', '项目审批'],
  },
};

export default {
  name: 'ContractList',
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        contractStatus: '',
      },
      loading: false,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      data: [],
      stats: {},
      expiringData: [],
      detailVisible: false,
      detailTab: 'info',
      detailContract: {},
      paymentBox: false,
      paymentLoading: false,
      paymentData: [],
      workflowLoading: false,
      workflowData: [],
      logData: [],
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      signedUploadVisible: false,
      signedUploadLoading: false,
      signedUploadContract: {},
      signedUploadForm: {
        contractFileUrl: '',
      },
      renewVisible: false,
      renewContract: {},
      renewForm: {
        dateRange: [],
      },
      renewRules: {
        dateRange: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
                callback(new Error('请选择租期起始日期'));
                return;
              }
              if (new Date(value[1]).getTime() <= new Date(value[0]).getTime()) {
                callback(new Error('结束时间必须晚于开始时间'));
                return;
              }
              callback();
            },
            trigger: 'change',
          },
        ],
      },
      routeDetailOpenedContractId: '',
      approvalVisible: false,
      approvalLoading: false,
      approvalType: CONTRACT_APPROVAL_BUSINESS_TYPE,
      approvalContract: {},
      approvalPayment: {},
      approvalProcessOptions: [],
      approvalForm: {
        processDefKey: '',
      },
      approvalExtra: {
        terminationType: 'normal',
        expectedTerminationDate: '',
        breachPenalty: 0,
        terminationReason: '',
      },
    };
  },
  computed: {
    ...mapGetters(['permission', 'userInfo']),
    statusOptions() {
      return statusDic;
    },
    singleSelectedContract() {
      return this.selectionList.length === 1 ? this.selectionList[0] : null;
    },
    summaryCards() {
      return [
        { key: 'total', label: '合同总数', value: this.stats.totalCount || 0 },
        { key: 'active', label: '履约中', value: this.stats.activeCount || 0 },
        { key: 'pending', label: '待签约', value: this.stats.pendingCount || 0 },
        { key: 'rent', label: '月租金合计', value: this.formatMoney(this.stats.monthlyRentTotal) },
      ];
    },
    ids() {
      return this.selectionList.map(item => item.contractId).join(',');
    },
    expiringSummary() {
      return this.expiringData
        .slice(0, 3)
        .map(item => `${item.customerName || item.contractName || '-'}：${item.endDate || '-'}`)
        .join('；');
    },
    detailTitle() {
      return this.detailContract.contractName || '合同详情';
    },
    approvalConfig() {
      return WORKFLOW_TYPES[this.approvalType] || WORKFLOW_TYPES[CONTRACT_APPROVAL_BUSINESS_TYPE];
    },
    approvalDialogTitle() {
      return this.approvalConfig.title;
    },
    approvalSummaryTitle() {
      return this.approvalConfig.summaryTitle;
    },
    approvalProcessPlaceholder() {
      return this.approvalConfig.processPlaceholder;
    },
    isTerminationWorkflow() {
      return this.approvalType === CONTRACT_TERMINATION_BUSINESS_TYPE;
    },
    approvalSummaryItems() {
      const contract = this.approvalContract || {};
      const payment = this.approvalPayment || {};
      if (this.approvalType === CONTRACT_PAYMENT_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || payment.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || payment.customerName || '-' },
          { label: '费用类型', value: payment.feeName || '-' },
          {
            label: '费用期间',
            value: `${payment.periodStart || '--'} 至 ${payment.periodEnd || '--'}`,
          },
          { label: '应缴金额', value: this.formatMoney(payment.amountDue) },
          { label: '缴费截止日', value: payment.payDeadline || '-' },
        ];
      }
      if (this.approvalType === CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || payment.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || payment.customerName || '-' },
          { label: '逾期费用', value: payment.feeName || '-' },
          {
            label: '欠费期间',
            value: `${payment.periodStart || '--'} 至 ${payment.periodEnd || '--'}`,
          },
          { label: '欠费金额', value: this.formatMoney(this.unpaidAmount(payment)) },
          { label: '逾期天数', value: `${this.overdueDays(payment)}天` },
        ];
      }
      if (this.approvalType === CONTRACT_TERMINATION_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || '-' },
          { label: '租赁房源', value: contract.roomName || contract.buildingName || '-' },
          {
            label: '合同期限',
            value: `${contract.startDate || '--'} 至 ${contract.endDate || '--'}`,
          },
          { label: '保证金', value: this.formatMoney(contract.deposit) },
          { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
        ];
      }
      if (this.approvalType === CONTRACT_ROOM_REVIEW_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || '-' },
          { label: '交接房源', value: contract.roomName || contract.buildingName || '-' },
          { label: '租赁面积', value: this.formatAreaValue(contract.rentArea) },
          { label: '保证金', value: this.formatMoney(contract.deposit) },
          {
            label: '退租状态',
            value: contract.contractStatusName || this.statusText(contract.contractStatus),
          },
        ];
      }
      return [
        { label: '合同编号', value: contract.contractNo || '-' },
        { label: '合同名称', value: contract.contractName || '-' },
        { label: '企业名称', value: contract.customerName || '-' },
        { label: '房源信息', value: contract.roomName || contract.buildingName || '-' },
        {
          label: '合同期限',
          value: `${contract.startDate || '--'} 至 ${contract.endDate || '--'}`,
        },
        { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
      ];
    },
    contractSummary() {
      const contract = this.detailContract || {};
      return `合同摘要：起租日 ${contract.startDate || '--'}，租赁面积 ${this.formatAreaValue(
        contract.rentArea
      )}，租金单价 ${this.formatUnitPrice(contract.rentPrice)}，月租金 ${this.formatMoney(
        contract.monthlyRent
      )}。`;
    },
    basicDetailItems() {
      const contract = this.detailContract || {};
      return [
        { label: '合同编号', value: contract.contractNo || '-' },
        {
          label: '合同状态',
          value: contract.contractStatusName || this.statusText(contract.contractStatus),
        },
        { label: '审批状态', value: this.approvalStatusText(contract.approvalStatus) },
        { label: '当前节点', value: contract.approvalCurrentNodeName || '-' },
        { label: '所属园区', value: contract.parkName || '-' },
        { label: '签订日', value: contract.signDate || '-' },
        { label: '合同开始日', value: contract.startDate || '-' },
        { label: '合同结束日', value: contract.endDate || '-' },
        { label: '租赁面积', value: this.formatAreaValue(contract.rentArea) },
        { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
        { label: '租金单价', value: this.formatUnitPrice(contract.rentPrice) },
        { label: '物业费', value: this.formatUnitPrice(contract.propertyFee) },
        { label: '押金', value: this.formatMoney(contract.deposit) },
        { label: '滞纳金', value: this.lateFeeText(contract) },
        { label: '租金递增', value: contract.rentIncreaseNode || '-' },
        {
          label: '续签提醒',
          value: contract.renewalRemindDays ? `${contract.renewalRemindDays}天` : '-',
        },
      ];
    },
  },
  created() {
    this.applyRouteQuery();
    this.reload();
  },
  watch: {
    '$route.query'() {
      this.applyRouteQuery();
      this.reload();
    },
  },
  methods: {
    applyRouteQuery() {
      const routeQuery = this.$route.query || {};
      if (routeQuery.customerId) {
        this.query.customerId = routeQuery.customerId;
      }
      if (routeQuery.customerName) {
        this.query.customerName = routeQuery.customerName;
      }
      if (routeQuery.contractId) {
        this.query.contractId = routeQuery.contractId;
        this.routeDetailOpenedContractId = '';
      } else {
        delete this.query.contractId;
        this.routeDetailOpenedContractId = '';
      }
    },
    defaultApprovalExtra() {
      return {
        terminationType: 'normal',
        expectedTerminationDate: this.formatDate(this.addDays(new Date(), 30)),
        breachPenalty: 0,
        terminationReason: '',
      };
    },
    terminationTypeText(value) {
      const map = {
        normal: '正常退租',
        early: '客户提前退租',
        special: '特殊情况',
      };
      return map[value] || '正常退租';
    },
    handleAdd() {
      this.$router.push({ path: '/contract/create-template', query: { mode: 'create' } });
    },
    handleStartApprovalFromSelection() {
      if (!this.singleSelectedContract) {
        this.$message.warning('请先选择一条待签约合同');
        return;
      }
      this.handleStartApproval(this.singleSelectedContract);
    },
    handleStartApproval(row) {
      if (!this.canStartApproval(row)) {
        this.$message.warning('仅待签约合同可以发起合同审批');
        return;
      }
      this.handleStartWorkflow(CONTRACT_APPROVAL_BUSINESS_TYPE, row);
    },
    handleStartOverdueApproval(row) {
      if (!row || !row.paymentId) {
        this.$message.warning('请选择需要发起逾期处理的账单');
        return;
      }
      if (!this.isPaymentOverdue(row)) {
        this.$message.warning('仅逾期账单可以发起逾期处理');
        return;
      }
      if (row.overdueApprovalStatus === 'running') {
        this.$message.warning('该账单逾期处理流程正在进行中');
        return;
      }
      this.handleStartWorkflow(CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE, this.detailContract, row);
    },
    handleStartTermination(row) {
      if (!this.canStartTermination(row)) {
        this.$message.warning('该合同已有进行中的退租审批');
        return;
      }
      this.handleStartWorkflow(CONTRACT_TERMINATION_BUSINESS_TYPE, row);
    },
    handleStartRoomReview(row) {
      if (!this.canStartRoomReview(row)) {
        this.$message.warning('退租审批通过后才可以发起房屋验收');
        return;
      }
      this.handleStartWorkflow(CONTRACT_ROOM_REVIEW_BUSINESS_TYPE, row);
    },
    handleRenew(row) {
      if (!row || !row.contractId) {
        this.$message.warning('请选择需要续租的合同');
        return;
      }
      this.renewContract = { ...(row || {}) };
      const startDate = this.nextDate(row.endDate) || this.formatDate(new Date());
      const endDate = this.defaultRenewEndDate(startDate, row.startDate, row.endDate);
      this.renewForm.dateRange = [startDate, endDate];
      this.renewVisible = true;
      this.$nextTick(() => {
        if (this.$refs.renewFormRef) {
          this.$refs.renewFormRef.clearValidate();
        }
      });
    },
    confirmRenew() {
      this.$refs.renewFormRef.validate(valid => {
        if (!valid) return;
        const [startDate, endDate] = this.renewForm.dateRange || [];
        this.renewVisible = false;
        this.$router.push({
          path: '/contract/create-template',
          query: {
            mode: 'renew',
            sourceContractId: this.renewContract.contractId,
            renewStartDate: startDate,
            renewEndDate: endDate,
          },
        });
      });
    },
    resetRenewDialog() {
      this.renewContract = {};
      this.renewForm = {
        dateRange: [],
      };
    },
    handleStartWorkflow(type, contract = {}, payment = {}) {
      this.approvalType = type || CONTRACT_APPROVAL_BUSINESS_TYPE;
      this.approvalContract = { ...(contract || {}) };
      this.approvalPayment = { ...(payment || {}) };
      this.approvalExtra = this.defaultApprovalExtra();
      this.approvalForm.processDefKey = '';
      this.approvalVisible = true;
      this.loadApprovalProcessOptions();
    },
    loadApprovalProcessOptions() {
      this.approvalLoading = true;
      getDeploymentList(1, -1, { status: 1 })
        .then(res => {
          const records = (res.data.data || {}).records || [];
          this.approvalProcessOptions = records
            .filter(item => this.isWorkflowProcess(item))
            .sort((a, b) => {
              const versionDiff = Number(b.version || 0) - Number(a.version || 0);
              if (versionDiff !== 0) return versionDiff;
              const timeA = new Date(a.deployTime || a.createTime || 0).getTime();
              const timeB = new Date(b.deployTime || b.createTime || 0).getTime();
              return timeB - timeA;
            });
          this.approvalForm.processDefKey = this.resolveApprovalProcessKey(
            this.approvalProcessOptions,
            this.approvalForm.processDefKey
          );
          if (this.approvalProcessOptions.length === 0) {
            this.$message.warning(
              `未找到可用的${this.approvalDialogTitle.replace('发起', '')}，请先在部署管理激活流程`
            );
          }
        })
        .finally(() => {
          this.approvalLoading = false;
        });
    },
    isWorkflowProcess(item = {}) {
      const name = item.name || '';
      const key = item.key || '';
      const formKey = item.formKey || '';
      const config = this.approvalConfig;
      return (
        (config.formKeys || []).includes(formKey) ||
        (config.defaultKeys || []).includes(key) ||
        (config.nameKeywords || []).some(keyword => name.includes(keyword) || key.includes(keyword))
      );
    },
    resolveApprovalProcessKey(processOptions = [], currentKey = '') {
      if (currentKey && processOptions.some(item => item.key === currentKey)) {
        return currentKey;
      }
      const config = this.approvalConfig;
      const preferred =
        processOptions.find(item => (config.formKeys || []).includes(item.formKey)) ||
        processOptions.find(item => (config.defaultKeys || []).includes(item.key)) ||
        processOptions.find(item =>
          (config.nameKeywords || []).some(keyword => (item.name || '').includes(keyword))
        ) ||
        processOptions[0];
      return preferred ? preferred.key : '';
    },
    approvalProcessLabel(item = {}) {
      const name = item.name || item.key || '-';
      const key = item.key ? ` / ${item.key}` : '';
      const version = item.version ? ` v${item.version}` : '';
      return `${name}${version}${key}`;
    },
    canStartApproval(row) {
      const approvalStatus = String((row && row.approvalStatus) || '');
      return Boolean(
        row &&
          row.contractId &&
          String(row.contractStatus) === '0' &&
          !['running', 'approved'].includes(approvalStatus)
      );
    },
    canStartTermination(row) {
      return Boolean(
        row && row.contractId && !this.hasRunningWorkflow(CONTRACT_TERMINATION_BUSINESS_TYPE)
      );
    },
    canStartRoomReview(row) {
      const status = String((row && row.contractStatus) || '');
      return Boolean(
        row &&
          row.contractId &&
          status === '7' &&
          !this.hasRunningWorkflow(CONTRACT_ROOM_REVIEW_BUSINESS_TYPE)
      );
    },
    hasRunningWorkflow(businessType) {
      return (this.workflowData || []).some(
        item => item.businessType === businessType && item.processStatus === 'running'
      );
    },
    buildContractApprovalPayload(contract = {}) {
      const selectedContract = contract || {};
      return {
        processDefKey: this.approvalForm.processDefKey,
        params: {
          processDefKey: this.approvalForm.processDefKey,
          businessType: CONTRACT_APPROVAL_BUSINESS_TYPE,
          businessTable: 'biz_contract',
          businessKey: String(selectedContract.contractId || ''),
          contractId: selectedContract.contractId,
          contractNo: selectedContract.contractNo,
          contractName: selectedContract.contractName,
          customerId: selectedContract.customerId,
          customerName: selectedContract.customerName,
          roomId: selectedContract.roomId,
          roomName: selectedContract.roomName,
          buildingId: selectedContract.buildingId,
          buildingName: selectedContract.buildingName,
          parkId: selectedContract.parkId,
          parkName: selectedContract.parkName,
          rentArea: selectedContract.rentArea,
          rentPrice: selectedContract.rentPrice,
          monthlyRent: selectedContract.monthlyRent,
          propertyFee: selectedContract.propertyFee,
          deposit: selectedContract.deposit,
          followUser: selectedContract.followUser,
          signDate: selectedContract.signDate,
          startDate: selectedContract.startDate,
          endDate: selectedContract.endDate,
          paymentCycle: selectedContract.paymentCycle,
          renewalRemindDays: selectedContract.renewalRemindDays,
          contractStatus: selectedContract.contractStatus,
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    buildPaymentWorkflowPayload(type, contract = {}, payment = {}) {
      const selectedContract = contract || {};
      const selectedPayment = payment || {};
      return {
        processDefKey: this.approvalForm.processDefKey,
        params: {
          processDefKey: this.approvalForm.processDefKey,
          businessType: type,
          businessTable: 'biz_contract_payment',
          businessKey: String(selectedPayment.paymentId || ''),
          contractId: selectedPayment.contractId || selectedContract.contractId,
          paymentId: selectedPayment.paymentId,
          contractNo: selectedContract.contractNo || selectedPayment.contractNo,
          contractName: selectedContract.contractName,
          customerId: selectedContract.customerId,
          customerName: selectedContract.customerName || selectedPayment.customerName,
          roomId: selectedContract.roomId,
          roomName: selectedContract.roomName,
          buildingId: selectedContract.buildingId,
          buildingName: selectedContract.buildingName,
          parkId: selectedPayment.parkId || selectedContract.parkId,
          parkName: selectedContract.parkName,
          feeType: selectedPayment.feeType,
          feeName: selectedPayment.feeName,
          periodStart: selectedPayment.periodStart,
          periodEnd: selectedPayment.periodEnd,
          amountDue: selectedPayment.amountDue,
          amountPaid: selectedPayment.amountPaid,
          unpaidAmount: this.unpaidAmount(selectedPayment),
          payDeadline: selectedPayment.payDeadline,
          overdueDays: this.overdueDays(selectedPayment),
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    buildContractFollowupWorkflowPayload(type, contract = {}) {
      const selectedContract = contract || {};
      const unpaidAmount = (this.paymentData || []).reduce(
        (total, item) => total + this.unpaidAmount(item),
        0
      );
      const overdueAmount = (this.paymentData || [])
        .filter(item => this.isPaymentOverdue(item))
        .reduce((total, item) => total + this.unpaidAmount(item), 0);
      const templateKeyMap = {
        [CONTRACT_TERMINATION_BUSINESS_TYPE]: 'termination-approval',
        [CONTRACT_ROOM_REVIEW_BUSINESS_TYPE]: 'room-review',
      };
      const params = {
        processDefKey: this.approvalForm.processDefKey,
        businessType: type,
        businessTable: 'biz_contract',
        businessKey: String(selectedContract.contractId || ''),
        contractId: selectedContract.contractId,
        contractNo: selectedContract.contractNo,
        contractName: selectedContract.contractName,
        customerId: selectedContract.customerId,
        customerName: selectedContract.customerName,
        roomId: selectedContract.roomId,
        roomIds: selectedContract.roomIds,
        roomName: selectedContract.roomName,
        buildingId: selectedContract.buildingId,
        buildingIds: selectedContract.buildingIds,
        buildingName: selectedContract.buildingName,
        parkId: selectedContract.parkId,
        parkName: selectedContract.parkName,
        rentArea: selectedContract.rentArea,
        rentPrice: selectedContract.rentPrice,
        monthlyRent: selectedContract.monthlyRent,
        deposit: selectedContract.deposit,
        startDate: selectedContract.startDate,
        endDate: selectedContract.endDate,
        followUser: selectedContract.followUser,
        unpaidAmount,
        overdueAmount,
        expectedTerminationDate: this.formatDate(new Date()),
        templateKey: templateKeyMap[type] || type,
        applicant: this.userInfo.nick_name,
        applicantDept: this.userInfo.dept_name,
        applyTime: this.formatDate(new Date()),
      };
      if (type === CONTRACT_TERMINATION_BUSINESS_TYPE) {
        Object.assign(params, {
          terminationType: this.approvalExtra.terminationType,
          terminationTypeName: this.terminationTypeText(this.approvalExtra.terminationType),
          expectedTerminationDate:
            this.approvalExtra.expectedTerminationDate || params.expectedTerminationDate,
          breachPenalty: this.approvalExtra.breachPenalty,
          terminationReason: this.approvalExtra.terminationReason,
          offlineSpecialProcess: this.approvalExtra.terminationType === 'special',
        });
      }
      return {
        processDefKey: this.approvalForm.processDefKey,
        params,
      };
    },
    buildWorkflowPayload() {
      if (
        this.approvalType === CONTRACT_PAYMENT_BUSINESS_TYPE ||
        this.approvalType === CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE
      ) {
        return this.buildPaymentWorkflowPayload(
          this.approvalType,
          this.approvalContract,
          this.approvalPayment
        );
      }
      if (
        this.approvalType === CONTRACT_TERMINATION_BUSINESS_TYPE ||
        this.approvalType === CONTRACT_ROOM_REVIEW_BUSINESS_TYPE
      ) {
        return this.buildContractFollowupWorkflowPayload(this.approvalType, this.approvalContract);
      }
      return this.buildContractApprovalPayload(this.approvalContract);
    },
    goWorkflowApproval() {
      if (!this.approvalContract.contractId) {
        this.$message.warning('请选择合同后再发起流程');
        return;
      }
      if (this.isTerminationWorkflow && !this.validateTerminationExtra()) {
        return;
      }
      if (
        [CONTRACT_PAYMENT_BUSINESS_TYPE, CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE].includes(
          this.approvalType
        ) &&
        !this.approvalPayment.paymentId
      ) {
        this.$message.warning('请选择账单后再发起流程');
        return;
      }
      if (!this.approvalForm.processDefKey) {
        this.$message.warning('请选择审批流程');
        return;
      }
      const payload = this.buildWorkflowPayload();
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      this.approvalVisible = false;
      this.$router.push(`/plugin/workflow/pages/process/form/start/${encodedParam}`);
    },
    validateTerminationExtra() {
      if (!this.approvalExtra.expectedTerminationDate) {
        this.$message.warning('请选择退租日期');
        return false;
      }
      if (!this.approvalExtra.terminationReason) {
        this.$message.warning('请填写退租原因');
        return false;
      }
      if (
        this.approvalExtra.terminationType === 'early' &&
        Number(this.approvalExtra.breachPenalty || 0) <= 0
      ) {
        this.$message.warning('客户提前退租请填写违约金');
        return false;
      }
      return true;
    },
    resetApprovalDialog() {
      this.approvalType = CONTRACT_APPROVAL_BUSINESS_TYPE;
      this.approvalContract = {};
      this.approvalPayment = {};
      this.approvalForm.processDefKey = '';
      this.approvalExtra = this.defaultApprovalExtra();
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(this.ids))
        .then(() => {
          this.reload();
          this.$message.success('操作成功!');
        });
    },
    handleTerminate(row) {
      if (!row || !row.contractId) return;
      this.$confirm('确定作废该合同?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => terminate(row.contractId))
        .then(() => {
          this.reload();
          this.detailVisible = false;
          this.$message.success('操作成功!');
        });
    },
    handleSignedUpload(row) {
      if (!row || !row.contractId) {
        return;
      }
      if (!this.canUploadSignedContract(row)) {
        this.$message.warning('仅审批通过待盖章的合同可以上传盖章文件');
        return;
      }
      this.signedUploadContract = { ...row };
      this.signedUploadForm = {
        contractFileUrl: row.contractFileUrl || '',
      };
      this.signedUploadVisible = true;
      this.$nextTick(() => {
        const upload = this.$refs.signedUploadRef;
        if (upload && upload.clearFiles) {
          upload.clearFiles();
        }
      });
    },
    beforeSignedFileUpload(file) {
      const allowTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'image/jpeg',
        'image/png',
      ];
      const isAllowType =
        allowTypes.includes(file.type) || /\.(pdf|doc|docx|jpg|jpeg|png)$/i.test(file.name || '');
      const isLt20M = file.size / 1024 / 1024 < 20;
      if (!isAllowType) {
        this.$message.error('仅支持上传 PDF、Word、JPG、PNG 格式文件');
      }
      if (!isLt20M) {
        this.$message.error('文件大小不能超过 20MB');
      }
      return isAllowType && isLt20M;
    },
    handleSignedFileSuccess(response, file) {
      if (!response || response.success === false) {
        this.signedUploadForm.contractFileUrl = '';
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      const fileUrl =
        data.link || data.url || data.path || response.link || response.url || response.data || '';
      this.signedUploadForm.contractFileUrl = fileUrl || '';
      if (this.signedUploadContract) {
        this.signedUploadContract.contractFileUrl = fileUrl || '';
      }
      this.$message.success(file && file.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleSignedFileError(error) {
      this.signedUploadForm.contractFileUrl = '';
      const message = (error && error.message) || '上传失败，请重试';
      this.$message.error(message);
    },
    handleSignedFileRemove() {
      this.signedUploadForm.contractFileUrl = '';
      if (this.signedUploadContract) {
        this.signedUploadContract.contractFileUrl = '';
      }
    },
    submitSignedUpload() {
      if (!this.signedUploadContract.contractId) {
        this.$message.warning('请选择需要盖章的合同');
        return;
      }
      if (!this.signedUploadForm.contractFileUrl) {
        this.$message.warning('请先上传盖章合同文件');
        return;
      }
      this.signedUploadLoading = true;
      uploadSignedContract(this.signedUploadContract.contractId, {
        contractFileUrl: this.signedUploadForm.contractFileUrl,
      })
        .then(() => {
          this.$message.success('盖章合同已生效');
          const contractId = this.signedUploadContract.contractId;
          this.signedUploadVisible = false;
          this.resetSignedUpload();
          this.reload();
          if (this.detailVisible && this.detailContract.contractId === contractId) {
            this.openDetail({ contractId });
          }
        })
        .finally(() => {
          this.signedUploadLoading = false;
        });
    },
    resetSignedUpload() {
      this.signedUploadLoading = false;
      this.signedUploadContract = {};
      this.signedUploadForm = {
        contractFileUrl: '',
      };
      this.$nextTick(() => {
        const upload = this.$refs.signedUploadRef;
        if (upload && upload.clearFiles) {
          upload.clearFiles();
        }
      });
    },
    openDetail(row) {
      if (!row || !row.contractId) return;
      this.detailVisible = true;
      this.detailTab = 'info';
      this.paymentData = [];
      this.workflowData = [];
      this.logData = [];
      getDetail(row.contractId).then(res => {
        this.detailContract = res.data.data || {};
      });
      this.loadPayment(row.contractId);
      this.loadWorkflow(row.contractId);
      this.loadLogs(row.contractId);
    },
    handlePayment(row) {
      this.detailContract = row || {};
      this.paymentBox = true;
      this.loadPayment(row.contractId);
    },
    loadPayment(contractId) {
      if (!contractId) return;
      this.paymentLoading = true;
      getPayment(contractId)
        .then(res => {
          this.paymentData = res.data.data || [];
        })
        .finally(() => {
          this.paymentLoading = false;
        });
    },
    loadLogs(contractId) {
      if (!contractId) return;
      getLogs(contractId).then(res => {
        this.logData = res.data.data || [];
      });
    },
    loadWorkflow(contractId) {
      if (!contractId) return;
      this.workflowLoading = true;
      getWorkflowRecords(contractId)
        .then(res => {
          this.workflowData = res.data.data || [];
        })
        .finally(() => {
          this.workflowLoading = false;
        });
    },
    openWorkflowFile(url) {
      if (!url) return;
      downloadBlob(url).then(res => {
        const disposition = res.headers && res.headers['content-disposition'];
        const filename = this.resolveDownloadFilename(disposition, '合同流程文件');
        const contentType = (res.headers && res.headers['content-type']) || 'application/octet-stream';
        downloadFile(res.data, filename, contentType);
      });
    },
    resolveDownloadFilename(disposition, fallbackName) {
      if (!disposition) return fallbackName;
      const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
      if (utf8Match && utf8Match[1]) {
        return decodeURIComponent(utf8Match[1]);
      }
      const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
      return filenameMatch && filenameMatch[1] ? decodeURIComponent(filenameMatch[1]) : fallbackName;
    },
    handleConfirmPayment(row) {
      this.$prompt('请输入实收金额', '确认缴费', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: row.amountDue,
      })
        .then(({ value }) =>
          confirmPayment(row.paymentId, {
            amountPaid: value,
          })
        )
        .then(() => {
          this.loadPayment(this.currentPaymentContractId(row));
          this.$message.success('操作成功!');
        });
    },
    handleRemind(row) {
      remindPayment(row.paymentId).then(() => {
        this.$message.success('操作成功!');
      });
    },
    handleArchive(row) {
      if (!row || !row.contractId) return;
      this.$confirm('确定进入该合同档案归档?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        this.$router.push({
          path: '/contract/archive',
          query: {
            contractId: row.contractId,
          },
        });
      });
    },
    searchReset() {
      this.query = {
        contractNo: '',
        customerName: '',
        contractStatus: '',
      };
      this.page.currentPage = 1;
      this.reload();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadData();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadData();
    },
    reload() {
      this.loadData();
      this.loadStats();
      this.loadExpiring();
    },
    loadData() {
      this.loading = true;
      getList(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          this.data = result.records || [];
          this.openRouteContractDetail();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadStats() {
      getStats({}).then(res => {
        this.stats = res.data.data || {};
      });
    },
    loadExpiring() {
      getExpiring(1, 20, {}).then(res => {
        const data = res.data.data || {};
        this.expiringData = data.records || [];
      });
    },
    openRouteContractDetail() {
      if (!this.query.contractId) return;
      if (String(this.routeDetailOpenedContractId) === String(this.query.contractId)) return;
      const target = this.data.find(
        item => String(item.contractId) === String(this.query.contractId)
      );
      if (target) {
        this.routeDetailOpenedContractId = String(this.query.contractId);
        this.openDetail(target);
      }
    },
    currentPaymentContractId(row) {
      return (
        row.contractId ||
        this.detailContract.contractId ||
        (this.paymentData[0] && this.paymentData[0].contractId)
      );
    },
    cleanParams(params) {
      return Object.keys(params || {}).reduce((result, key) => {
        const value = params[key];
        if (value !== '' && value !== undefined && value !== null) {
          result[key] = value;
        }
        return result;
      }, {});
    },
    isExpiringSoon(row) {
      if (!row || !row.endDate) return false;
      const end = new Date(row.endDate).getTime();
      if (Number.isNaN(end)) return false;
      const days = Math.ceil((end - Date.now()) / 86400000);
      const remindDays = Number(row.renewalRemindDays || 30);
      return days >= 0 && days <= remindDays;
    },
    statusText(value) {
      const item = statusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '未知';
    },
    statusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'primary',
        4: 'danger',
        5: 'warning',
        6: 'warning',
        7: 'primary',
        8: 'warning',
      };
      return map[String(value)] || 'info';
    },
    approvalStatusText(value) {
      const map = {
        running: '审批中',
        approved: '已通过',
        rejected: '已驳回',
        canceled: '已取消',
        deleted: '已删除',
      };
      return map[String(value || '')] || '未发起';
    },
    approvalStatusType(value) {
      const map = {
        running: 'warning',
        approved: 'success',
        rejected: 'danger',
        canceled: 'info',
        deleted: 'info',
      };
      return map[String(value || '')] || 'info';
    },
    canUploadSignedContract(row) {
      return String((row && row.contractStatus) || '') === '5';
    },
    workflowBusinessTypeText(value) {
      const map = {
        contract_approval: '合同审批',
        contract_payment: '付款/开票流程',
        contract_room_review: '房屋验收',
        contract_termination: '退租审批',
        contract_overdue_legal: '逾期律师函',
      };
      return map[String(value || '')] || value || '-';
    },
    workflowStatusText(value) {
      const map = {
        running: '审批中',
        approved: '已通过',
        rejected: '已驳回',
        canceled: '已取消',
        deleted: '已删除',
      };
      return map[String(value || '')] || value || '-';
    },
    workflowStatusType(value) {
      const map = {
        running: 'warning',
        approved: 'success',
        rejected: 'danger',
        canceled: 'info',
        deleted: 'info',
      };
      return map[String(value || '')] || 'info';
    },
    paymentStatusText(value) {
      const item = paymentStatusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '未知';
    },
    paymentTagType(row) {
      if (row.payStatus === '1') return 'success';
      if (this.isPaymentOverdue(row)) return 'danger';
      return 'warning';
    },
    isPaymentOverdue(row) {
      if (!row.payDeadline || row.payStatus === '1') return false;
      return new Date(row.payDeadline).getTime() < Date.now();
    },
    overdueDays(row) {
      if (!row || !row.payDeadline || row.payStatus === '1') return 0;
      const deadline = new Date(row.payDeadline).getTime();
      if (Number.isNaN(deadline) || deadline >= Date.now()) return 0;
      return Math.ceil((Date.now() - deadline) / 86400000);
    },
    unpaidAmount(row) {
      const due = Number((row && row.amountDue) || 0);
      const paid = Number((row && row.amountPaid) || 0);
      return Math.max(due - paid, 0);
    },
    lateFeeText(contract) {
      if (!contract || !contract.lateFeeRatio) return '-';
      const unitMap = {
        percent_day: '%/天',
        yuan_day: '元/天',
        percentPerDay: '%/日',
        percentPerMonth: '%/月',
        amountPerDay: '元/日',
      };
      return `${contract.lateFeeRatio}${
        unitMap[contract.lateFeeUnit] || contract.lateFeeUnit || ''
      }`;
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    formatUnitPrice(value) {
      if (value === null || value === undefined || value === '') return '-';
      return `${this.formatMoney(value)}元/㎡/月`;
    },
    formatAreaValue(value) {
      const number = Number(value || 0);
      return `${number.toLocaleString('zh-CN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      })}㎡`;
    },
    formatDate(date) {
      const y = date.getFullYear();
      const m = date.getMonth() + 1;
      const d = date.getDate();
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    },
    parseDate(value) {
      if (!value) return null;
      const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);
      return Number.isNaN(date.getTime()) ? null : date;
    },
    nextDate(value) {
      const date = this.parseDate(value);
      if (!date) return '';
      date.setDate(date.getDate() + 1);
      return this.formatDate(date);
    },
    defaultRenewEndDate(startDate, oldStartDate, oldEndDate) {
      const start = this.parseDate(startDate);
      if (!start) return '';
      const oldStart = this.parseDate(oldStartDate);
      const oldEnd = this.parseDate(oldEndDate);
      const months =
        oldStart && oldEnd
          ? Math.max(
              (oldEnd.getFullYear() - oldStart.getFullYear()) * 12 +
                oldEnd.getMonth() -
                oldStart.getMonth(),
              1
            )
          : 12;
      const end = new Date(start);
      end.setMonth(end.getMonth() + months);
      return this.formatDate(end);
    },
    addDays(date, days) {
      const next = new Date(date);
      next.setDate(next.getDate() + Number(days || 0));
      return next;
    },
  },
};
</script>

<style lang="scss" scoped>
.contract-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contract-alert,
.contract-search,
.contract-toolbar {
  border-radius: 10px;
}

.contract-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.contract-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.contract-search :deep(.el-input),
.contract-search :deep(.el-select) {
  width: 190px;
}

.contract-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.contract-table {
  width: 100%;
  border-radius: 0;
}

.tenant-name-btn {
  max-width: 100%;
  display: inline-flex;
  overflow: hidden;
  vertical-align: middle;
}

.tenant-name-btn :deep(span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.inline-tag {
  margin-left: 6px;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.detail-head span {
  display: block;
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.approval-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.approval-option em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.renew-dialog :deep(.el-dialog__body) {
  min-height: 640px;
  padding-top: 0;
}

.renew-tip {
  margin-bottom: 20px;
}

.renew-dialog :deep(.el-form-item__label) {
  color: #606266;
  font-weight: 500;
}

.renew-dialog :deep(.el-range-editor.el-input__wrapper) {
  width: 100%;
}

.detail-alert {
  margin-bottom: 12px;
}

.detail-section {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-weight: 600;
}

.contract-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.contract-field {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}

.contract-field span {
  flex: 0 0 104px;
  color: #8c98aa;
}

.contract-field strong {
  min-width: 0;
  color: #303133;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.contract-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.contract-log__title {
  color: #303133;
  font-weight: 600;
}

.contract-log__operator,
.muted {
  color: #909399;
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-card span {
  color: #606266;
  font-size: 13px;
}

.summary-card strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

.contract-page :deep(.el-button),
.contract-page :deep(.el-input__wrapper),
.contract-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .contract-field-grid {
    grid-template-columns: 1fr;
  }
}
</style>
