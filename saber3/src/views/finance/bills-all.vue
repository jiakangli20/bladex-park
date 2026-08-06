<template>
  <basic-container>
    <div class="bills-all-page">
      <section class="bill-overview">
        <div class="bill-direction">
          <button
            v-for="item in directionOptions"
            :key="item.value"
            :class="['bill-direction__item', { 'is-active': direction === item.value }]"
            type="button"
            @click="changeDirection(item.value)"
          >
            {{ item.label }}
          </button>
        </div>
        <div class="bill-summary-grid">
          <div v-for="item in summaryCards" :key="item.key" class="bill-summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </section>

      <section class="bill-table-card">
        <div class="bill-toolbar">
          <el-tabs v-model="direction" class="bill-tabs" @tab-change="changeDirection">
            <el-tab-pane label="收款账单" name="receivable" />
            <el-tab-pane label="付款账单" name="payable" />
          </el-tabs>
          <div class="bill-tools">
            <div class="bill-filters">
              <el-input
                v-model="query.customerName"
                class="bill-filter-input"
                clearable
                placeholder="对方名称"
                @keyup.enter="searchChange"
              />
              <el-input
                v-model="query.contractNo"
                class="bill-filter-input"
                clearable
                placeholder="合同编号"
                @keyup.enter="searchChange"
              />
              <el-date-picker
                v-model="query.deadlineRange"
                class="bill-date-picker"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                range-separator="-"
                clearable
              />
            </div>
            <div class="bill-actions">
              <el-button icon="el-icon-refresh-left" @click="searchReset">清空</el-button>
              <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
              <el-button icon="el-icon-download" @click="handleExport">导出</el-button>
              <el-button
                v-if="!isPayable && canConfirmPayment"
                type="warning"
                plain
                @click="openUtilitySubmissionDialog"
              >
                小程序付款凭证
              </el-button>
              <el-dropdown trigger="click" @command="handleCreateCommand">
                <el-button type="primary" icon="el-icon-plus">
                  创建账单
                  <i class="el-icon-arrow-down el-icon--right"></i>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="receivable">添加收款账单</el-dropdown-item>
                    <el-dropdown-item command="payable">添加付款账单</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>

        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="paymentId"
          scrollbar-always-on
          class="bill-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="44" align="center" />
          <el-table-column
            prop="customerName"
            label="对方名称"
            :min-width="customerNameColumnWidth"
            align="center"
            class-name="bill-customer-column"
          >
            <template #default="{ row }">
              <el-button text type="primary" class="bill-link" @click="openBillDetail(row)">
                {{ row.customerName || '-' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column
            prop="buildingName"
            label="楼宇名称"
            width="116"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column prop="payStatus" label="账单状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="payStatusType(row.payStatus)" effect="plain">{{
                payStatusText(row.payStatus)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column
            prop="feeName"
            label="费用类型"
            width="100"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column :label="amountDueLabel" prop="amountDue" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column :label="amountPaidLabel" prop="amountPaid" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column :label="amountPendingLabel" width="116" align="center">
            <template #default="{ row }">{{ formatMoney(pendingAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="periodStart" label="开始日期" width="116" align="center" />
          <el-table-column prop="periodEnd" label="结束时间" width="116" align="center" />
          <el-table-column prop="payDeadline" :label="deadlineLabel" width="116" align="center" />
        </el-table>

        <div class="bill-pagination">
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
      </section>

      <bill-create-drawer
        v-model="createVisible"
        :direction="createDirection"
        @saved="handleBillCreated"
      />

      <el-drawer
        v-model="detailVisible"
        size="76%"
        append-to-body
        class="bill-detail-drawer"
        @closed="closeBillDetail"
      >
        <template #header>
          <div class="bill-drawer-header">
            <div class="bill-drawer-title">
              <el-button text class="bill-drawer-back" @click="detailVisible = false">
                <i class="el-icon-arrow-left"></i>
              </el-button>
              <span>账单详情</span>
              <el-tag :type="payStatusType(detailRow.payStatus)" effect="plain">
                {{ payStatusText(detailRow.payStatus) }}
              </el-tag>
            </div>
            <div class="bill-drawer-actions">
              <el-button :disabled="!detailRow.contractId" @click="openContract(detailRow)"
                >查看合同</el-button
              >
              <el-button
                type="primary"
                plain
                :disabled="!detailRow.paymentId"
                @click="openApplicationWorkflow(detailRow)"
              >
                {{ applicationActionLabel }}
              </el-button>
              <el-button
                v-if="canConfirmPayment && String(detailRow.payStatus) !== '1'"
                type="primary"
                @click="openPaymentConfirm(detailRow)"
              >
                {{ detailIsPayable ? '确认付款' : '确认收款' }}
              </el-button>
            </div>
          </div>
        </template>
        <div v-loading="detailLoading" class="bill-detail-body">
          <template v-if="detailRow && detailRow.paymentId">
            <section class="bill-detail-hero">
              <h2>{{ detailPartyTitle }}</h2>
              <div class="bill-status-strip">
                <div v-for="item in detailMetricItems" :key="item.key" class="bill-status-cell">
                  <span>{{ item.label }}</span>
                  <strong
                    v-if="item.status"
                    :class="['bill-status-value', `is-${item.type || 'info'}`]"
                    >{{ item.value }}</strong
                  >
                  <el-date-picker
                    v-else-if="item.editableDate"
                    v-model="detailDeadline"
                    :disabled="deadlineSaving"
                    type="date"
                    value-format="YYYY-MM-DD"
                    class="bill-status-date"
                    @change="handleDeadlineChange"
                  />
                  <strong v-else>{{ item.value }}</strong>
                </div>
              </div>
            </section>

            <div class="bill-detail-two-col">
              <section class="bill-detail-section">
                <div class="bill-detail-section__title">账单信息</div>
                <div class="bill-detail-info-grid">
                  <div v-for="item in detailBillItems" :key="item.label" class="bill-detail-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </section>

              <section class="bill-detail-section">
                <div class="bill-detail-section__title">房源信息</div>
                <el-table :data="detailRoomRows" border class="bill-detail-table">
                  <el-table-column
                    prop="buildingName"
                    label="所属楼宇"
                    min-width="120"
                    align="center"
                  />
                  <el-table-column
                    prop="roomName"
                    label="楼层/房号"
                    min-width="120"
                    align="center"
                    show-overflow-tooltip
                  />
                  <el-table-column prop="area" label="面积" min-width="120" align="center" />
                </el-table>
              </section>
            </div>

            <section class="bill-detail-section bill-detail-lines">
              <div class="bill-detail-section__title">账单明细</div>
              <el-table :data="detailLineRows" border class="bill-detail-table">
                <el-table-column prop="feeName" label="费用类型" min-width="140" align="center" />
                <el-table-column
                  prop="amountDue"
                  :label="amountDueLabel"
                  min-width="150"
                  align="center"
                />
                <el-table-column prop="taxRate" label="税率" width="110" align="center" />
                <el-table-column prop="taxAmount" label="税额" width="110" align="center" />
                <el-table-column
                  prop="periodStart"
                  label="开始日期"
                  min-width="130"
                  align="center"
                />
                <el-table-column prop="periodEnd" label="结束日期" min-width="130" align="center" />
                <el-table-column
                  prop="remark"
                  label="账单备注"
                  min-width="160"
                  align="center"
                  show-overflow-tooltip
                />
              </el-table>
            </section>

            <section class="bill-detail-section bill-detail-lines bill-application-files">
              <div class="bill-detail-section__title">{{ applicationFilesTitle }}</div>
              <el-table :data="applicationFileRows" border class="bill-detail-table">
                <el-table-column prop="name" label="文件类型" width="180" align="center" />
                <el-table-column prop="status" label="审批状态" width="150" align="center">
                  <template #default="{ row }">
                    <el-tag :type="workflowStatusType(row.status)" effect="plain">
                      {{ workflowStatusText(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column
                  prop="currentNode"
                  label="当前节点"
                  width="180"
                  align="center"
                  class-name="bill-current-node-column"
                >
                  <template #default="{ row }">
                    <span class="bill-current-node">{{ row.currentNode }}</span>
                  </template>
                </el-table-column>
                <el-table-column
                  prop="fileName"
                  label="文件名称"
                  min-width="360"
                  align="center"
                  class-name="bill-file-name-column"
                >
                  <template #default="{ row }">
                    <span class="bill-file-name">{{ row.fileName }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="210" align="center">
                  <template #default="{ row }">
                    <div class="bill-file-actions">
                      <el-button
                        text
                        type="primary"
                        :disabled="!row.fileUrl"
                        @click="previewApplicationFile(row)"
                        >预览</el-button
                      >
                      <el-button
                        text
                        type="primary"
                        :disabled="!row.fileUrl"
                        @click="downloadApplicationFile(row)"
                        >下载</el-button
                      >
                      <el-button
                        v-if="row.paymentRecord && canConfirmPayment"
                        text
                        type="danger"
                        :loading="String(voucherDeletingId) === String(row.recordId)"
                        :disabled="Boolean(voucherDeletingId)"
                        @click="deletePaymentRecord(row)"
                        >删除</el-button
                      >
                    </div>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </template>
          <el-empty v-else description="暂无账单详情" />
        </div>
      </el-drawer>

      <el-dialog
        v-model="paymentConfirmVisible"
        :title="paymentConfirmIsPayable ? '确认付款' : '确认收款'"
        width="620px"
        append-to-body
        destroy-on-close
        @closed="resetPaymentConfirm"
      >
        <section class="bill-workflow-summary payment-confirm-summary">
          <div>
            <span>账单编号</span>
            <strong>{{
              paymentConfirmRow.paymentId ? `ZD${paymentConfirmRow.paymentId}` : '-'
            }}</strong>
          </div>
          <div>
            <span>{{ paymentConfirmIsPayable ? '收款企业' : '付款企业' }}</span>
            <strong>{{ paymentConfirmRow.customerName || '-' }}</strong>
          </div>
          <div>
            <span>费用类型</span>
            <strong>{{ paymentConfirmRow.feeName || '-' }}</strong>
          </div>
          <div v-if="paymentConfirmIsPayable">
            <span>付款审批</span>
            <strong>{{ workflowStatusText(paymentConfirmRow.paymentApprovalStatus) }}</strong>
          </div>
          <div v-else>
            <span>当前已收</span>
            <strong>{{ formatMoney(paymentConfirmRow.amountPaid) }}</strong>
          </div>
        </section>
        <el-form
          ref="paymentConfirmFormRef"
          class="payment-confirm-form"
          :model="paymentConfirmForm"
          :rules="paymentConfirmRules"
          label-width="128px"
        >
          <el-form-item
            :label="paymentConfirmIsPayable ? '本次付款金额' : '本次收款金额'"
            prop="amountPaid"
          >
            <el-input-number
              v-model="paymentConfirmForm.amountPaid"
              :min="0.01"
              :max="pendingAmount(paymentConfirmRow) || undefined"
              :precision="2"
              :step="100"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="paymentConfirmIsPayable ? '付款时间' : '收款时间'" prop="payTime">
            <el-date-picker
              v-model="paymentConfirmForm.payTime"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              :placeholder="paymentConfirmIsPayable ? '请选择付款时间' : '请选择收款时间'"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item
            :label="paymentConfirmIsPayable ? '付款凭证' : '收款凭证'"
            prop="paymentVoucherUrl"
          >
            <el-upload
              ref="paymentVoucherUploadRef"
              action="/api/blade-resource/oss/endpoint/put-file"
              :headers="uploadHeaders"
              :limit="1"
              :file-list="paymentVoucherFileList"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              :before-upload="beforePaymentVoucherUpload"
              :on-success="handlePaymentVoucherSuccess"
              :on-error="handlePaymentVoucherError"
              :on-remove="handlePaymentVoucherRemove"
            >
              <el-button icon="el-icon-upload">
                {{ paymentConfirmIsPayable ? '上传付款凭证' : '上传收款凭证' }}
              </el-button>
            </el-upload>
          </el-form-item>
          <el-form-item :label="paymentConfirmIsPayable ? '付款备注' : '收款备注'">
            <el-input
              v-model="paymentConfirmForm.remark"
              type="textarea"
              :rows="3"
              maxlength="500"
              show-word-limit
              :placeholder="paymentConfirmIsPayable ? '填写付款说明' : '填写收款说明'"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="paymentConfirmVisible = false">取消</el-button>
          <el-button type="primary" :loading="paymentConfirmLoading" @click="submitPaymentConfirm">
            {{ paymentConfirmIsPayable ? '确认付款' : '确认收款' }}
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="workflowVisible"
        :title="workflowDialogTitle"
        width="640px"
        append-to-body
        destroy-on-close
        @closed="resetWorkflowDialog"
      >
        <div class="bill-workflow-dialog">
          <section class="bill-workflow-summary">
            <div v-for="item in workflowSummaryItems" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </section>
          <el-form :model="workflowForm" label-width="96px">
            <el-form-item label="审批流程" required>
              <el-select
                v-model="workflowForm.processDefKey"
                filterable
                :loading="workflowLoading"
                :placeholder="workflowProcessPlaceholder"
                style="width: 100%"
              >
                <el-option
                  v-for="item in workflowProcessOptions"
                  :key="`${item.id || item.key}-${item.version || ''}`"
                  :label="workflowProcessLabel(item)"
                  :value="item.key"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </div>
        <template #footer>
          <el-button @click="workflowVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="workflowLoading"
            :disabled="!workflowForm.processDefKey || workflowLoading"
            @click="goApplicationWorkflow"
          >
            发起申请
          </el-button>
        </template>
      </el-dialog>

      <notice-preview-dialog
        v-model="noticePreview.visible"
        :title="noticePreview.title"
        :html="noticePreview.html"
        :loading="noticePreview.loading"
        :download-url="noticePreview.downloadUrl"
        :preview-type="noticePreview.previewType"
        :document-blob="noticePreview.documentBlob"
        :pdf-blob="noticePreview.pdfBlob"
        :pdf-file-name="noticePreview.pdfFileName"
        :preview-error="noticePreview.previewError"
        @download="downloadNoticePreviewFile"
      />

      <el-dialog
        v-model="utilitySubmissionVisible"
        title="小程序水电付款凭证"
        width="1080px"
        append-to-body
        destroy-on-close
      >
        <div class="utility-submission-toolbar">
          <el-select
            v-model="utilitySubmissionQuery.submitStatus"
            clearable
            placeholder="全部状态"
            @change="searchUtilitySubmissions"
          >
            <el-option label="待确认" value="PENDING" />
            <el-option label="已确认" value="CONFIRMED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
          <el-button icon="el-icon-refresh" @click="loadUtilitySubmissions">刷新</el-button>
        </div>
        <el-table
          v-loading="utilitySubmissionLoading"
          :data="utilitySubmissions"
          border
          row-key="id"
          empty-text="暂无付款凭证"
        >
          <el-table-column
            prop="customerName"
            label="企业"
            min-width="150"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column prop="feeName" label="费用" width="90" align="center" />
          <el-table-column
            prop="roomName"
            label="房源"
            min-width="130"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column label="申报金额" width="120" align="center">
            <template #default="{ row }">{{ formatMoney(row.submitAmount) }}</template>
          </el-table-column>
          <el-table-column
            prop="voucherName"
            label="付款凭证"
            min-width="150"
            align="center"
            show-overflow-tooltip
          >
            <template #default="{ row }"
              ><el-link type="primary" :href="row.voucherUrl" target="_blank">{{
                row.voucherName || '查看凭证'
              }}</el-link></template
            >
          </el-table-column>
          <el-table-column prop="createTime" label="提交时间" width="170" align="center" />
          <el-table-column prop="submitStatus" label="状态" width="100" align="center">
            <template #default="{ row }"
              ><el-tag :type="utilitySubmissionTag(row.submitStatus)" effect="plain">{{
                utilitySubmissionStatusText(row.submitStatus)
              }}</el-tag></template
            >
          </el-table-column>
          <el-table-column
            prop="auditOpinion"
            label="审核意见"
            min-width="150"
            align="center"
            show-overflow-tooltip
          />
          <el-table-column label="操作" width="156" fixed="right" align="center">
            <template #default="{ row }">
              <div class="table-actions" v-if="row.submitStatus === 'PENDING'">
                <el-button type="success" text @click="confirmUtilitySubmission(row)"
                  >确认</el-button
                >
                <el-button type="danger" text @click="rejectUtilitySubmission(row)">驳回</el-button>
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="bill-pagination">
          <el-pagination
            background
            :current-page="utilitySubmissionPage.currentPage"
            :page-sizes="[10, 20, 30, 40, 50, 100]"
            :page-size="utilitySubmissionPage.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="utilitySubmissionPage.total"
            @size-change="changeUtilitySubmissionSize"
            @current-change="changeUtilitySubmissionPage"
          />
        </div>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { Base64 } from 'js-base64';
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import { getWorkflowRecords } from '@/api/contract/contract';
import { noticePrintUrl } from '@/api/contract/print';
import {
  confirmPayment,
  deletePaymentVoucher,
  getPaymentDetail,
  getPaymentPage,
  getPaymentSummary,
  updatePaymentDeadline,
} from '@/api/ics/payment';
import {
  confirmUtilityPaymentSubmission,
  getUtilityPaymentSubmissions,
  rejectUtilityPaymentSubmission,
} from '@/api/ics/utility-billing';
import { payStatusDic } from '@/option/finance/payment';
import {
  createNoticePreviewState,
  downloadNoticeFile,
  openAttachmentPreview,
  openNoticePreview,
} from '@/utils/contract-notice';
import { enrichContractWorkflowContext } from '@/utils/workflow-business-context';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import BillCreateDrawer from './components/bill-create-drawer.vue';

const PAYMENT_WORKFLOW_BUSINESS_TYPE = 'contract_payment';
const PAYMENT_APPLICATION_TEMPLATE = 'payment-notice';
const INVOICE_APPLICATION_TEMPLATE = 'invoice-apply';

export default {
  name: 'FinanceBillsAll',
  components: {
    BillCreateDrawer,
    NoticePreviewDialog,
  },
  data() {
    return {
      direction: 'receivable',
      query: {
        customerName: '',
        contractNo: '',
        deadlineRange: [],
      },
      loading: false,
      detailLoading: false,
      deadlineSaving: false,
      detailVisible: false,
      createVisible: false,
      createDirection: 'receivable',
      detailDeadline: '',
      detailRow: {},
      workflowVisible: false,
      workflowLoading: false,
      workflowRow: {},
      workflowProcessOptions: [],
      workflowExistingRecord: {},
      workflowForm: {
        processDefKey: '',
      },
      paymentConfirmVisible: false,
      paymentConfirmLoading: false,
      paymentConfirmRow: {},
      paymentConfirmForm: {
        amountPaid: null,
        payTime: '',
        paymentVoucherName: '',
        paymentVoucherUrl: '',
        remark: '',
      },
      paymentVoucherFileList: [],
      utilitySubmissionVisible: false,
      utilitySubmissionLoading: false,
      utilitySubmissions: [],
      utilitySubmissionQuery: { submitStatus: 'PENDING' },
      utilitySubmissionPage: { currentPage: 1, pageSize: 10, total: 0 },
      voucherDeletingId: '',
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      paymentConfirmRules: {
        amountPaid: [{ required: true, message: '请输入本次收付款金额', trigger: 'blur' }],
        payTime: [{ required: true, message: '请选择收付款时间', trigger: 'change' }],
        paymentVoucherUrl: [{ required: true, message: '请上传收付款凭证', trigger: 'change' }],
      },
      noticePreview: createNoticePreviewState(),
      data: [],
      selectionList: [],
      summary: {},
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      directionOptions: [
        { label: '收款', value: 'receivable' },
        { label: '付款', value: 'payable' },
      ],
      payStatusOptions: payStatusDic,
    };
  },
  computed: {
    ...mapGetters(['userInfo', 'permission']),
    canConfirmPayment() {
      return this.validData(this.permission.finance_payment_confirm, false);
    },
    customerNameColumnWidth() {
      const longestUnits = this.data.reduce((maxUnits, row) => {
        const name = String(row.customerName || '-');
        const units = Array.from(name).reduce(
          (total, character) => total + (/^[\u0000-\u00ff]$/.test(character) ? 0.6 : 1),
          0
        );
        return Math.max(maxUnits, units);
      }, 0);
      return Math.max(180, Math.ceil(longestUnits * 15 + 48));
    },
    isPayable() {
      return this.direction === 'payable';
    },
    detailIsPayable() {
      return String((this.detailRow || {}).direction || this.direction) === 'payable';
    },
    applicationFilesTitle() {
      return this.detailIsPayable ? '审批与付款文件' : '审批与收款文件';
    },
    paymentConfirmIsPayable() {
      return String((this.paymentConfirmRow || {}).direction || this.direction) === 'payable';
    },
    applicationActionLabel() {
      return this.isPayable ? '付款申请' : '开票申请';
    },
    workflowIsPayable() {
      return String((this.workflowRow || {}).direction || this.direction) === 'payable';
    },
    workflowDialogTitle() {
      return this.workflowIsPayable ? '发起付款申请' : '发起开票申请';
    },
    workflowProcessPlaceholder() {
      return this.workflowIsPayable ? '请选择已部署的付款申请流程' : '请选择已部署的开票申请流程';
    },
    workflowTemplateKey() {
      return this.workflowIsPayable ? PAYMENT_APPLICATION_TEMPLATE : INVOICE_APPLICATION_TEMPLATE;
    },
    workflowSummaryItems() {
      const row = this.workflowRow || {};
      return [
        { label: '对方名称', value: row.customerName || '-' },
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '费用类型', value: row.feeName || '-' },
        {
          label: this.workflowIsPayable ? '申请付款金额' : '申请开票金额',
          value: this.formatMoney(row.amountDue),
        },
        { label: '账单方向', value: this.workflowIsPayable ? '付款' : '收款' },
        {
          label: '申请状态',
          value: this.workflowStatusText((this.workflowExistingRecord || {}).processStatus),
        },
      ];
    },
    amountDueLabel() {
      return this.isPayable ? '应付金额' : '应收金额';
    },
    amountPaidLabel() {
      return this.isPayable ? '实付金额' : '实收金额';
    },
    amountPendingLabel() {
      return this.isPayable ? '需付金额' : '需收金额';
    },
    deadlineLabel() {
      return this.isPayable ? '应付日期' : '应收日期';
    },
    payerTitle() {
      return this.isPayable ? '收款方' : '付款方';
    },
    pendingCount() {
      const total = Number(this.summary.totalCount || 0);
      const paid = Number(this.summary.paidCount || 0);
      return Math.max(total - paid, 0);
    },
    summaryCards() {
      return [
        {
          key: 'due',
          label: `${this.isPayable ? '应付' : '应收'}（${this.summary.totalCount || 0}笔）`,
          value: this.formatMoney(this.summary.amountDue),
        },
        { key: 'lateFee', label: '滞纳金', value: this.formatMoney(0) },
        {
          key: 'paid',
          label: this.isPayable ? '实付' : '实收',
          value: this.formatMoney(this.summary.amountPaid),
        },
        { key: 'increase', label: '调增', value: this.formatMoney(0) },
        { key: 'decrease', label: '调减', value: this.formatMoney(0) },
        {
          key: 'pending',
          label: `${this.isPayable ? '需付' : '需收'}（${this.pendingCount}笔）`,
          value: this.formatMoney(this.summary.amountPending),
        },
      ];
    },
    detailBillItems() {
      const row = this.detailRow || {};
      return [
        { label: '费用类型', value: row.feeName || '-' },
        { label: '计费周期', value: `${row.periodStart || '--'}~${row.periodEnd || '--'}` },
        { label: '账单金额（元）', value: this.formatMoney(row.amountDue) },
        { label: '创建时间', value: row.createTime || '-' },
        { label: this.payerTitle, value: row.customerName || '-' },
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '账单编号', value: row.paymentId ? `ZD${row.paymentId}` : '-' },
        { label: '起算天数', value: '--' },
        { label: '滞纳金比例', value: '--' },
        { label: '滞纳金上限', value: '--' },
        { label: '经办人', value: row.createBy || '-' },
        { label: '账单备注', value: row.remark || '-' },
      ];
    },
    detailPartyTitle() {
      const row = this.detailRow || {};
      const label = this.payerTitle || '';
      const name = row.customerName || '-';
      return label ? `${label}：${name}` : name;
    },
    detailMetricItems() {
      const row = this.detailRow || {};
      return [
        {
          key: 'status',
          label: '账单状态',
          value: this.payStatusText(row.payStatus),
          status: true,
          type: this.payStatusType(row.payStatus),
        },
        {
          key: 'amountDue',
          label: `${this.amountDueLabel}（元）`,
          value: this.formatMoney(row.amountDue),
        },
        {
          key: 'amountPending',
          label: `${this.amountPendingLabel}（元）`,
          value: this.formatMoney(this.pendingAmount(row)),
        },
        { key: 'deadline', label: this.deadlineLabel, editableDate: true },
      ];
    },
    detailRoomRows() {
      const row = this.detailRow || {};
      return [
        {
          buildingName: row.buildingName || '-',
          roomName: row.roomName || '-',
          area: row.rentArea ? `${row.rentArea}㎡` : '-',
        },
      ];
    },
    detailLineRows() {
      const row = this.detailRow || {};
      return [
        {
          feeName: row.feeName || '-',
          amountDue: this.formatMoney(row.amountDue),
          taxRate: '0.00',
          taxAmount: '0.00',
          periodStart: row.periodStart || '-',
          periodEnd: row.periodEnd || '-',
          remark: row.remark || '-',
        },
      ];
    },
    applicationFileRows() {
      const row = this.detailRow || {};
      const payable = String(row.direction || this.direction) === 'payable';
      const status = payable ? row.paymentApprovalStatus : row.invoiceApprovalStatus;
      const fileUrl = payable ? row.paymentFileUrl : row.invoiceFileUrl;
      const fileLabel = payable ? '付款申请单' : '开票申请单';
      // 审批归档文件在流程结束回调中生成，最终节点的历史记录可能尚未落库。
      // 已审批文件统一通过动态接口重新生成，确保分管领导等最后审批节点与预览一致。
      const latestFileUrl =
        status === 'approved'
          ? noticePrintUrl(payable ? PAYMENT_APPLICATION_TEMPLATE : INVOICE_APPLICATION_TEMPLATE, {
              paymentId: row.paymentId,
              contractId: row.contractId,
            })
          : '';
      const resolvedFileUrl = latestFileUrl || fileUrl;
      const rows = [
        {
          name: fileLabel,
          status: status || '',
          currentNode: (payable ? row.paymentCurrentNodeName : row.invoiceCurrentNodeName) || '-',
          fileUrl: resolvedFileUrl,
          fileName: resolvedFileUrl
            ? `${row.contractNo || `ZD${row.paymentId}`}-${fileLabel}.docx`
            : '审批完成后自动生成',
          templateKey: payable ? PAYMENT_APPLICATION_TEMPLATE : INVOICE_APPLICATION_TEMPLATE,
        },
      ];
      const voucherLabel = payable ? '付款凭证' : '收款凭证';
      const confirmText = payable ? '财务已确认付款' : '财务已确认收款';
      const paymentRecords = Array.isArray(row.paymentRecords) ? row.paymentRecords : [];
      paymentRecords.forEach((record, index) => {
        rows.push({
          name: `${voucherLabel}${index + 1}`,
          status: 'approved',
          currentNode: record.paymentTime || confirmText,
          fileUrl: record.voucherUrl,
          fileName:
            record.voucherName ||
            (record.voucherUrl
              ? `${row.contractNo || `ZD${row.paymentId}`}-${voucherLabel}${index + 1}`
              : `未关联${voucherLabel}`),
          attachment: true,
          recordId: record.recordId,
          paymentRecord: true,
          payable,
        });
      });
      if (!paymentRecords.length && row.paymentVoucherUrl) {
        rows.push({
          name: voucherLabel,
          status: row.payStatus === '1' ? 'approved' : '',
          currentNode: confirmText,
          fileUrl: row.paymentVoucherUrl,
          fileName:
            row.paymentVoucherName || `${row.contractNo || `ZD${row.paymentId}`}-${voucherLabel}`,
          attachment: true,
        });
      }
      return rows;
    },
  },
  mounted() {
    this.applyRouteQuery();
    this.reload();
  },
  watch: {
    '$route.query': {
      handler() {
        this.applyRouteQuery();
        this.page.currentPage = 1;
        this.reload();
      },
      deep: true,
    },
  },
  methods: {
    openUtilitySubmissionDialog() {
      this.utilitySubmissionVisible = true;
      this.utilitySubmissionPage.currentPage = 1;
      this.loadUtilitySubmissions();
    },
    loadUtilitySubmissions() {
      this.utilitySubmissionLoading = true;
      getUtilityPaymentSubmissions(
        this.utilitySubmissionPage.currentPage,
        this.utilitySubmissionPage.pageSize,
        this.utilitySubmissionQuery
      )
        .then(res => {
          const data = res.data.data || {};
          this.utilitySubmissions = data.records || [];
          this.utilitySubmissionPage.total = Number(data.total || 0);
        })
        .finally(() => {
          this.utilitySubmissionLoading = false;
        });
    },
    searchUtilitySubmissions() {
      this.utilitySubmissionPage.currentPage = 1;
      this.loadUtilitySubmissions();
    },
    changeUtilitySubmissionPage(current) {
      this.utilitySubmissionPage.currentPage = current;
      this.loadUtilitySubmissions();
    },
    changeUtilitySubmissionSize(size) {
      this.utilitySubmissionPage.pageSize = size;
      this.utilitySubmissionPage.currentPage = 1;
      this.loadUtilitySubmissions();
    },
    confirmUtilitySubmission(row) {
      this.$confirm(
        `确认收到企业付款 ${this.formatMoney(row.submitAmount)} 吗？确认后将更新账单实收金额。`,
        '确认到账',
        { type: 'warning' }
      )
        .then(() =>
          confirmUtilityPaymentSubmission(row.id, {
            payTime: this.formatDateTime(new Date()),
            opinion: '付款凭证核验通过',
          })
        )
        .then(() => {
          this.$message.success('已确认到账');
          this.loadUtilitySubmissions();
          this.reload();
        });
    },
    rejectUtilitySubmission(row) {
      this.$prompt('请输入驳回原因', '驳回付款凭证', {
        confirmButtonText: '确认驳回',
        cancelButtonText: '取消',
        inputValidator: value => (value && value.trim() ? true : '请输入驳回原因'),
      })
        .then(({ value }) => rejectUtilityPaymentSubmission(row.id, { opinion: value.trim() }))
        .then(() => {
          this.$message.success('付款凭证已驳回');
          this.loadUtilitySubmissions();
        });
    },
    utilitySubmissionStatusText(status) {
      return { PENDING: '待确认', CONFIRMED: '已确认', REJECTED: '已驳回' }[status] || '-';
    },
    utilitySubmissionTag(status) {
      return { PENDING: 'primary', CONFIRMED: 'success', REJECTED: 'danger' }[status] || 'info';
    },
    applyRouteQuery() {
      const routeQuery = this.$route.query || {};
      const nextQuery = {
        customerName: routeQuery.customerName || '',
        contractNo: routeQuery.contractNo || '',
        deadlineRange: [],
        settleStatus: routeQuery.settleStatus || '',
      };
      if (routeQuery.direction === 'payable' || routeQuery.direction === 'receivable') {
        this.direction = routeQuery.direction;
      }
      if (
        this.hasRouteDate(routeQuery.deadlineStartDate) &&
        this.hasRouteDate(routeQuery.deadlineEndDate)
      ) {
        nextQuery.deadlineRange = [
          this.normalizeRouteDate(routeQuery.deadlineStartDate),
          this.normalizeRouteDate(routeQuery.deadlineEndDate),
        ];
      }
      this.query = nextQuery;
    },
    reload() {
      this.loadSummary();
      this.loadPage();
    },
    loadPage() {
      this.loading = true;
      getPaymentPage(this.page.currentPage, this.page.pageSize, this.buildQueryParams())
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = result.total || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadSummary() {
      getPaymentSummary(this.buildQueryParams()).then(res => {
        this.summary = res.data.data || {};
      });
    },
    buildQueryParams() {
      const params = {
        direction: this.direction,
        customerName: this.query.customerName,
        contractNo: this.query.contractNo,
        settleStatus: this.query.settleStatus,
      };
      if (Array.isArray(this.query.deadlineRange) && this.query.deadlineRange.length === 2) {
        params.deadlineStartDate = this.toDayStart(this.query.deadlineRange[0]);
        params.deadlineEndDate = this.toDayEnd(this.query.deadlineRange[1]);
      }
      return Object.keys(params).reduce((result, key) => {
        if (params[key] !== '' && params[key] !== null && params[key] !== undefined) {
          result[key] = params[key];
        }
        return result;
      }, {});
    },
    hasRouteDate(value) {
      return typeof value === 'string' && value.trim() !== '';
    },
    normalizeRouteDate(value) {
      if (!this.hasRouteDate(value)) return '';
      return value.trim().slice(0, 10);
    },
    toDayStart(value) {
      const date = this.normalizeRouteDate(value);
      return date ? `${date} 00:00:00` : '';
    },
    toDayEnd(value) {
      const date = this.normalizeRouteDate(value);
      return date ? `${date} 23:59:59` : '';
    },
    changeDirection(value) {
      const nextDirection = value && value.paneName ? value.paneName : value;
      if (nextDirection && this.direction !== nextDirection) {
        this.direction = nextDirection;
      }
      this.page.currentPage = 1;
      this.reload();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {
        customerName: '',
        contractNo: '',
        deadlineRange: [],
        settleStatus: '',
      };
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadPage();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadPage();
    },
    selectionChange(selection) {
      this.selectionList = selection;
    },
    handleCreateCommand(direction) {
      this.createDirection = direction === 'payable' ? 'payable' : 'receivable';
      this.createVisible = true;
    },
    handleBillCreated(payload = {}) {
      this.direction = payload.direction === 'payable' ? 'payable' : 'receivable';
      this.page.currentPage = 1;
      this.reload();
    },
    openBillDetail(row) {
      if (!row || !row.paymentId) return;
      this.detailVisible = true;
      this.detailLoading = true;
      this.detailDeadline = this.dateOnly(row.payDeadline);
      getPaymentDetail(row.paymentId)
        .then(res => {
          this.detailRow = res.data.data || { ...row };
          this.detailDeadline = this.dateOnly(this.detailRow.payDeadline);
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    closeBillDetail() {
      this.detailRow = {};
      this.detailLoading = false;
      this.deadlineSaving = false;
      this.detailDeadline = '';
    },
    handleDeadlineChange(value) {
      const paymentId = this.detailRow && this.detailRow.paymentId;
      if (!paymentId) return;
      if (!value) {
        this.detailDeadline = this.dateOnly(this.detailRow.payDeadline);
        return;
      }
      this.deadlineSaving = true;
      updatePaymentDeadline(paymentId, value)
        .then(() => {
          this.detailRow = {
            ...this.detailRow,
            payDeadline: value,
          };
          this.data = this.data.map(item =>
            item.paymentId === paymentId ? { ...item, payDeadline: value } : item
          );
          this.loadSummary();
          this.$message.success(`${this.deadlineLabel}已更新`);
        })
        .catch(() => {
          this.detailDeadline = this.dateOnly(this.detailRow.payDeadline);
        })
        .finally(() => {
          this.deadlineSaving = false;
        });
    },
    openContract(row) {
      if (!row || !row.contractId) return;
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
        },
      });
    },
    async openApplicationWorkflow(row) {
      if (!row || !row.paymentId || !row.contractId) {
        this.$message.warning('当前账单缺少合同信息，无法发起申请');
        return;
      }
      this.workflowProcessOptions = [];
      this.workflowExistingRecord = {};
      this.workflowForm.processDefKey = '';
      this.workflowVisible = true;
      this.workflowLoading = true;
      Promise.all([
        enrichContractWorkflowContext(row),
        getDeploymentList(1, -1, { status: 1 }),
        getWorkflowRecords(row.contractId),
      ])
        .then(([workflowRow, deploymentRes, workflowRes]) => {
          this.workflowRow = workflowRow;
          const deployments = (deploymentRes.data.data || {}).records || [];
          this.workflowProcessOptions = deployments
            .filter(item => this.isApplicationWorkflowProcess(item))
            .sort((a, b) => Number(b.version || 0) - Number(a.version || 0));
          this.workflowForm.processDefKey = this.resolveApplicationProcessKey(
            this.workflowProcessOptions
          );

          const records = workflowRes.data.data || [];
          this.workflowExistingRecord =
            records
              .filter(item => this.isCurrentPaymentWorkflow(item, row.paymentId))
              .sort((a, b) => Number(b.recordId || 0) - Number(a.recordId || 0))[0] || {};
          if (this.workflowProcessOptions.length === 0) {
            this.$message.warning(
              `未找到可用的${
                this.workflowIsPayable ? '付款申请' : '开票申请'
              }流程，请先在部署管理激活流程`
            );
          }
        })
        .finally(() => {
          this.workflowLoading = false;
        });
    },
    openPaymentConfirm(row) {
      if (!row || !row.paymentId) return;
      const payable = String(row.direction || this.direction) === 'payable';
      if (payable && row.paymentApprovalStatus !== 'approved') {
        this.$alert('请先发起付款申请并完成审批，审批通过后才能由财务确认付款。', '付款前置条件', {
          confirmButtonText: '知道了',
          type: 'warning',
        });
        return;
      }
      this.paymentConfirmRow = { ...row };
      this.paymentConfirmForm = {
        amountPaid: this.pendingAmount(row),
        payTime: this.formatDateTime(new Date()),
        paymentVoucherName: '',
        paymentVoucherUrl: '',
        remark: '',
      };
      this.paymentVoucherFileList = [];
      this.paymentConfirmVisible = true;
    },
    beforePaymentVoucherUpload(file) {
      const allowed =
        /\.(pdf|doc|docx|png|jpg|jpeg)$/i.test(file.name || '') && file.size / 1024 / 1024 < 20;
      if (!allowed) {
        this.$message.error('仅支持 20MB 以内的 PDF、Word、PNG、JPG 收付款凭证');
      }
      return allowed;
    },
    extractUploadUrl(response) {
      const data = response?.data || {};
      return data.link || data.url || data.path || response?.link || response?.url || '';
    },
    handlePaymentVoucherSuccess(response, file) {
      if (!response || response.success === false) {
        this.$message.error(
          (response && response.msg) ||
            `${this.paymentConfirmIsPayable ? '付款' : '收款'}凭证上传失败`
        );
        return;
      }
      const fileUrl = this.extractUploadUrl(response);
      if (!fileUrl) {
        this.$message.error(
          `${this.paymentConfirmIsPayable ? '付款' : '收款'}凭证上传后未返回文件地址`
        );
        return;
      }
      this.paymentConfirmForm.paymentVoucherName = file?.name || '';
      this.paymentConfirmForm.paymentVoucherUrl = fileUrl;
      this.paymentVoucherFileList = [
        {
          name: file?.name || `${this.paymentConfirmIsPayable ? '付款' : '收款'}凭证`,
          url: fileUrl,
        },
      ];
      this.$refs.paymentConfirmFormRef?.validateField('paymentVoucherUrl');
    },
    handlePaymentVoucherError(error) {
      this.$message.error(
        (error && error.message) || `${this.paymentConfirmIsPayable ? '付款' : '收款'}凭证上传失败`
      );
    },
    handlePaymentVoucherRemove() {
      this.paymentConfirmForm.paymentVoucherName = '';
      this.paymentConfirmForm.paymentVoucherUrl = '';
      this.paymentVoucherFileList = [];
    },
    submitPaymentConfirm() {
      this.$refs.paymentConfirmFormRef.validate(valid => {
        if (!valid) return;
        this.paymentConfirmLoading = true;
        const paymentId = this.paymentConfirmRow.paymentId;
        const payload = { ...this.paymentConfirmForm };
        if (!this.paymentConfirmIsPayable) {
          payload.amountPaid =
            Number(this.paymentConfirmRow.amountPaid || 0) + Number(payload.amountPaid || 0);
        }
        confirmPayment(paymentId, payload)
          .then(() => {
            this.$message.success(
              this.paymentConfirmIsPayable ? '付款已确认，退租管理将同步显示付款结果' : '收款已确认'
            );
            this.paymentConfirmVisible = false;
            this.reload();
            return getPaymentDetail(paymentId)
              .then(res => {
                this.detailRow = res.data.data || this.detailRow;
              })
              .catch(() => null);
          })
          .finally(() => {
            this.paymentConfirmLoading = false;
          });
      });
    },
    resetPaymentConfirm() {
      this.paymentConfirmLoading = false;
      this.paymentConfirmRow = {};
      this.paymentConfirmForm = {
        amountPaid: null,
        payTime: '',
        paymentVoucherName: '',
        paymentVoucherUrl: '',
        remark: '',
      };
      this.paymentVoucherFileList = [];
    },
    isApplicationWorkflowProcess(item = {}) {
      const name = String(item.name || '');
      const key = String(item.key || '');
      const formKey = String(item.formKey || '');
      if (this.workflowIsPayable) {
        return formKey === 'pay' || key === 'pay' || name.includes('付款');
      }
      return formKey === 'invoice' || key === 'invoice' || name.includes('开票');
    },
    resolveApplicationProcessKey(options = []) {
      const formKey = this.workflowIsPayable ? 'pay' : 'invoice';
      const preferred =
        options.find(item => item.formKey === formKey) ||
        options.find(item => item.key === formKey) ||
        options[0];
      return preferred ? preferred.key : '';
    },
    isCurrentPaymentWorkflow(record, paymentId) {
      if (!record || String(record.paymentId || '') !== String(paymentId || '')) return false;
      if (record.businessType !== PAYMENT_WORKFLOW_BUSINESS_TYPE) return false;
      const marker = `${record.templateKey || ''}|${record.formKey || ''}|${
        record.processDefKey || ''
      }`.toLowerCase();
      return this.workflowIsPayable
        ? marker.includes('payment-notice') || marker.includes('|pay|') || marker.endsWith('|pay')
        : marker.includes('invoice');
    },
    workflowStatusText(status) {
      const map = {
        running: '审批中',
        approved: '已完成',
        rejected: '被驳回',
        canceled: '已撤回',
        deleted: '已撤回',
      };
      return map[status] || '未发起';
    },
    workflowStatusType(status) {
      const map = {
        running: 'primary',
        approved: 'success',
        rejected: 'danger',
        canceled: 'info',
        deleted: 'info',
      };
      return map[status] || 'info';
    },
    previewApplicationFile(file) {
      if (!file || !file.fileUrl || !this.detailRow.paymentId) return;
      if (file.attachment) {
        openAttachmentPreview(this.noticePreview, file, `${file.name}预览`);
        return;
      }
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType: file.templateKey,
          paymentId: this.detailRow.paymentId,
          contractId: this.detailRow.contractId,
        },
        file.fileUrl,
        file.fileName,
        `${file.name}预览`
      );
    },
    downloadApplicationFile(file) {
      if (!file || !file.fileUrl) return;
      downloadNoticeFile(file.fileUrl, file.fileName);
    },
    deletePaymentRecord(file) {
      if (!file || !file.recordId || !this.detailRow.paymentId) return;
      const payable = Boolean(file.payable);
      const actionName = payable ? '付款' : '收款';
      this.$confirm(
        `删除后将撤回这一笔${actionName}，累计已${
          payable ? '付' : '收'
        }金额和缴费状态会同步回退。是否继续？`,
        `删除${actionName}记录`,
        {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
        .then(() => {
          this.voucherDeletingId = file.recordId;
          return deletePaymentVoucher(this.detailRow.paymentId, file.recordId);
        })
        .then(() => {
          this.$message.success(`该笔${actionName}已撤回`);
          this.reload();
          return getPaymentDetail(this.detailRow.paymentId);
        })
        .then(res => {
          this.detailRow = res.data.data || this.detailRow;
        })
        .catch(error => {
          if (error === 'cancel' || error === 'close') return;
        })
        .finally(() => {
          this.voucherDeletingId = '';
        });
    },
    downloadNoticePreviewFile() {
      if (!this.noticePreview.downloadUrl) return;
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName);
    },
    workflowProcessLabel(item = {}) {
      const name = item.name || item.key || '-';
      const version = item.version ? ` v${item.version}` : '';
      const key = item.key ? ` / ${item.key}` : '';
      return `${name}${version}${key}`;
    },
    goApplicationWorkflow() {
      const row = this.workflowRow || {};
      const existingStatus = (this.workflowExistingRecord || {}).processStatus;
      if (existingStatus === 'running') {
        this.$message.warning(
          `该账单${this.workflowIsPayable ? '付款申请' : '开票申请'}正在审批中`
        );
        return;
      }
      if (existingStatus === 'approved') {
        this.$message.warning(
          `该账单${
            this.workflowIsPayable ? '付款申请' : '开票申请'
          }已审批完成，可在当前账单详情的“${
            this.workflowIsPayable ? '审批与付款文件' : '审批与收款文件'
          }”中预览和下载`
        );
        return;
      }
      if (!row.paymentId || !row.contractId || !this.workflowForm.processDefKey) {
        this.$message.warning('请选择审批流程');
        return;
      }
      const selectedProcess =
        this.workflowProcessOptions.find(item => item.key === this.workflowForm.processDefKey) ||
        {};
      const invoiceAmountParams = this.workflowIsPayable ? {} : this.buildInvoiceAmountParams(row);
      const payload = {
        processDefKey: this.workflowForm.processDefKey,
        params: {
          processDefKey: this.workflowForm.processDefKey,
          formKey: selectedProcess.formKey || (this.workflowIsPayable ? 'pay' : 'invoice'),
          businessType: PAYMENT_WORKFLOW_BUSINESS_TYPE,
          businessTable: 'biz_contract_payment',
          businessKey: String(row.paymentId),
          contractId: row.contractId,
          paymentId: row.paymentId,
          contractNo: row.contractNo,
          contractName: row.contractName,
          customerId: row.customerId,
          customerName: row.customerName,
          enterpriseName: row.enterpriseName || row.customerName,
          tenantName: row.tenantName || row.customerName,
          lesseeName: row.lesseeName || row.customerName,
          contactName: row.contactName,
          contactPhone: row.contactPhone,
          customerPhone: row.customerPhone || row.contactPhone,
          applicantContactPhone: row.applicantContactPhone || row.contactPhone,
          creditCode: row.creditCode,
          registeredAddress: row.registeredAddress,
          contactEmail: row.contactEmail,
          roomName: row.roomName,
          buildingName: row.buildingName,
          parkId: row.parkId,
          feeType: row.feeType,
          feeName: row.feeName,
          direction: this.workflowIsPayable ? 'payable' : 'receivable',
          periodStart: row.periodStart,
          periodEnd: row.periodEnd,
          amountDue: row.amountDue,
          amountPaid: row.amountPaid,
          invoiceAmount: row.amountDue,
          payDeadline: row.payDeadline,
          templateKey: this.workflowTemplateKey,
          applicant:
            (this.userInfo || {}).real_name ||
            (this.userInfo || {}).realName ||
            (this.userInfo || {}).nick_name ||
            (this.userInfo || {}).user_name ||
            '',
          applicantDept: (this.userInfo || {}).dept_name || '',
          applyTime: this.formatDateTime(new Date()),
          ...invoiceAmountParams,
        },
      };
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      this.workflowVisible = false;
      this.$router.push(`/plugin/workflow/pages/process/form/start/${encodedParam}`);
    },
    buildInvoiceAmountParams(row = {}) {
      const marker = `${row.feeType || ''}|${row.feeName || ''}`.toLowerCase();
      const amount = Number(row.amountDue || 0).toFixed(2);
      const zero = '0.00';
      const isRent = marker.includes('rent') || marker.includes('房租') || marker.includes('租金');
      const isProperty = marker.includes('property') || marker.includes('物业');
      const isDeposit =
        marker.includes('deposit') || marker.includes('押金') || marker.includes('保证金');
      return {
        a178229043562386124: isRent ? amount : zero,
        a178229053048579216: isProperty ? amount : zero,
        a178229053161649966: isDeposit ? amount : zero,
      };
    },
    resetWorkflowDialog() {
      this.workflowRow = {};
      this.workflowProcessOptions = [];
      this.workflowExistingRecord = {};
      this.workflowForm.processDefKey = '';
    },
    handleExport() {
      this.$message.info('导出接口预留中，后续接入账单导出服务');
    },
    pendingAmount(row) {
      return Number(row.amountDue || 0) - Number(row.amountPaid || 0);
    },
    payStatusText(value) {
      const item = this.payStatusOptions.find(option => option.value === value);
      return item ? item.label : '未知';
    },
    payStatusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'danger',
        3: 'warning',
      };
      return map[value] || 'info';
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    dateOnly(value) {
      if (!value) return '';
      return String(value).slice(0, 10);
    },
    formatDateTime(value) {
      const date = value instanceof Date ? value : new Date(value);
      const pad = number => String(number).padStart(2, '0');
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
        date.getHours()
      )}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.bills-all-page {
  min-width: 0;
}

.bill-overview {
  display: flex;
  min-height: 112px;
  margin-bottom: 16px;
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #ebeef5;
}

.bill-direction {
  width: 64px;
  padding: 14px 0;
  border-right: 1px solid #edf0f5;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: center;
  gap: 12px;
}

.bill-direction__item {
  position: relative;
  height: 34px;
  border: 0;
  background: transparent;
  color: #303133;
  font-size: 14px;
  line-height: 34px;
  cursor: pointer;
}

.bill-direction__item.is-active {
  color: #1677ff;
  font-weight: 600;
}

.bill-direction__item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 3px;
  width: 3px;
  height: 28px;
  background: #1677ff;
  border-radius: 0 3px 3px 0;
}

.bill-summary-grid {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
}

.bill-summary-card {
  min-width: 0;
  padding: 22px 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.bill-summary-card span {
  color: #909399;
  font-size: 14px;
  line-height: 20px;
}

.bill-summary-card strong {
  margin-top: 8px;
  color: #303133;
  font-size: 22px;
  line-height: 30px;
  font-weight: 600;
  white-space: nowrap;
}

.bill-table-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
}

.bill-toolbar {
  min-height: 74px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #ebeef5;
}

.bill-tabs {
  flex: 0 0 auto;
  min-width: 180px;
}

.bill-tools {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.bill-filters,
.bill-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.bill-filters {
  flex: 1 1 620px;
}

.bill-actions {
  flex: 0 0 auto;
  justify-content: flex-end;
}

.bill-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.bill-filter-input {
  width: 150px;
}

.bill-date-picker {
  width: 250px;
}

.bill-table {
  width: 100%;
}

.bill-table :deep(.el-table__cell) {
  text-align: center;
}

.bill-table :deep(.bill-customer-column .cell) {
  padding: 0 12px;
  white-space: nowrap;
}

.bill-link {
  min-width: 0;
  max-width: none;
  padding: 0;
  overflow: visible;
  font-weight: 500;
  text-align: center;
  white-space: nowrap;
  vertical-align: middle;
}

.bill-link :deep(span) {
  white-space: nowrap;
}

.bill-pagination {
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
}

.bills-all-page :deep(.el-tabs__header) {
  margin: 0;
}

.bills-all-page :deep(.el-tabs__nav-wrap::after) {
  height: 0;
}

.bills-all-page :deep(.el-button),
.bills-all-page :deep(.el-input__wrapper),
.bills-all-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

.bills-all-page :deep(.el-table th.el-table__cell) {
  background: #fafafa;
  color: #606266;
  font-weight: 600;
}

.bills-all-page :deep(.el-table .cell) {
  white-space: nowrap;
}

.bill-detail-body {
  min-height: 280px;
  padding: 0 0 24px;
  background: #f4f4f6;
}

.bill-drawer-header {
  width: 100%;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.bill-drawer-title,
.bill-drawer-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bill-drawer-title {
  min-width: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.bill-drawer-back {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #606266;
}

.bill-drawer-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.bill-detail-hero {
  min-height: 128px;
  padding: 18px 24px 22px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.bill-detail-hero h2 {
  margin: 0 0 22px;
  color: #303133;
  font-size: 22px;
  line-height: 30px;
  font-weight: 600;
}

.bill-status-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 168px));
  gap: 30px;
  align-items: end;
  max-width: 760px;
}

.bill-status-cell {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bill-status-cell span,
.bill-detail-item span {
  color: #909399;
  font-size: 13px;
  line-height: 18px;
}

.bill-status-cell strong {
  color: #303133;
  font-size: 22px;
  line-height: 28px;
  font-weight: 500;
  white-space: nowrap;
}

.bill-status-value {
  display: inline-flex;
  align-items: center;
  width: fit-content;
}

.bill-status-value.is-success {
  color: #16a34a;
}

.bill-status-value.is-danger {
  color: #dc2626;
}

.bill-status-value.is-warning {
  color: #d97706;
}

.bill-status-value.is-info {
  color: #303133;
}

.bill-status-date {
  width: 150px;
}

.bill-status-date :deep(.el-input__wrapper) {
  min-height: 30px;
  padding: 0;
  box-shadow: none;
  background: transparent;
}

.bill-status-date :deep(.el-input__inner) {
  color: #303133;
  font-size: 22px;
  line-height: 28px;
  font-weight: 500;
}

.bill-status-date :deep(.el-input__prefix) {
  order: 2;
  margin-left: 6px;
  margin-right: 0;
}

.bill-detail-two-col {
  padding: 18px 20px 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
}

.bill-detail-section {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.bill-detail-section {
  min-height: 260px;
}

.bill-detail-section__title {
  height: 58px;
  margin: 0;
  padding: 0 20px;
  border-bottom: 1px solid #e5e7eb;
  color: #303133;
  font-size: 16px;
  line-height: 58px;
  font-weight: 600;
}

.bill-detail-info-grid {
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  column-gap: 24px;
  row-gap: 18px;
}

.bill-detail-item {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bill-detail-item strong {
  color: #303133;
  font-size: 14px;
  line-height: 22px;
  font-weight: 500;
  word-break: break-word;
}

.bill-detail-table {
  width: calc(100% - 40px);
  margin: 20px;
}

.bill-detail-lines {
  margin: 18px 20px 0;
  min-height: 0;
}

.bill-application-files {
  margin-bottom: 20px;
}

.bill-file-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
}

.bill-file-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.bill-file-name {
  display: inline-block;
  max-width: none;
  white-space: nowrap;
}

.bill-current-node {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  white-space: nowrap;
}

.bill-application-files :deep(.bill-current-node-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.bill-application-files :deep(.bill-file-name-column .cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
  text-align: center;
  white-space: nowrap;
}

.bill-detail-drawer :deep(.el-drawer__header) {
  margin: 0;
  padding: 14px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.bill-detail-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f4f4f6;
}

.bill-detail-table :deep(.el-table__cell) {
  text-align: center;
}

.bill-workflow-dialog {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bill-workflow-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border: 1px solid #e5e7eb;
}

.bill-workflow-summary > div {
  min-width: 0;
  min-height: 72px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
}

.bill-workflow-summary > div:nth-child(2n) {
  border-right: 0;
}

.bill-workflow-summary > div:nth-last-child(-n + 2) {
  border-bottom: 0;
}

.bill-workflow-summary span {
  color: #909399;
  font-size: 13px;
}

.bill-workflow-summary strong {
  overflow: hidden;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-confirm-summary {
  grid-template-rows: repeat(2, 88px);
  margin-bottom: 22px;
}

.payment-confirm-summary > div {
  min-height: 0;
  padding: 16px 18px;
  align-items: flex-start;
  justify-content: center;
}

.payment-confirm-summary span {
  width: 100%;
  line-height: 20px;
}

.payment-confirm-summary strong {
  width: 100%;
  line-height: 22px;
}

.payment-confirm-form :deep(.el-form-item__label) {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  height: 32px;
  line-height: 32px;
  white-space: nowrap;
}

.utility-submission-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 14px;
}

.utility-submission-toolbar :deep(.el-select) {
  width: 160px;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

@media (max-width: 1560px) {
  .bill-toolbar {
    padding: 10px 16px 14px;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }

  .bill-tabs {
    width: 100%;
    min-width: 0;
  }

  .bill-tools,
  .bill-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .bill-filter-input {
    width: calc(50% - 5px);
    min-width: 180px;
    flex: 1 1 180px;
  }

  .bill-date-picker {
    width: 100%;
    flex: 1 1 100%;
  }

  .bill-actions {
    width: 100%;
  }
}

@media (max-width: 1100px) {
  .bill-detail-two-col,
  .bill-status-strip {
    grid-template-columns: 1fr;
  }

  .bill-detail-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
