<template>
  <basic-container>
    <div class="termination-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="termination-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input
              v-model="query.contractNo"
              clearable
              placeholder="请输入合同编号"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="租客名称">
            <el-input
              v-model="query.customerName"
              clearable
              placeholder="请输入租客名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="流程状态">
            <el-select v-model="query.processStatus" clearable placeholder="请选择">
              <el-option
                v-for="item in approvalStatusOptions"
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

      <el-table
        v-loading="loading"
        :data="data"
        border
        row-key="recordId"
        class="termination-table"
      >
        <el-table-column
          prop="customerName"
          label="租客名称"
          min-width="150"
          align="center"
          class-name="termination-full-cell"
        />
        <el-table-column
          prop="roomName"
          label="房源信息"
          min-width="150"
          align="center"
          class-name="termination-full-cell"
        >
          <template #default="{ row }">
            {{ row.roomName || row.buildingName || '-' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="processName"
          label="流程名称"
          min-width="140"
          align="center"
          class-name="termination-full-cell"
        />
        <el-table-column prop="processStatus" label="审批状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="approvalStatusType(row.processStatus)" effect="plain">
              {{ approvalStatusText(row.processStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="退租阶段" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="terminationStageType(row)" effect="plain">{{
              terminationStageText(row)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="currentNode"
          label="当前节点"
          min-width="130"
          align="center"
          class-name="termination-full-cell"
        />
        <el-table-column prop="contractStatus" label="合同状态" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="contractStatusType(row.contractStatus)" effect="plain">
              {{ row.contractStatusName || statusText(row.contractStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deposit" label="保证金" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.deposit) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="发起时间" width="170" align="center" />
        <el-table-column prop="approvalTime" label="完成时间" width="170" align="center">
          <template #default="{ row }">{{ row.approvalTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" @click="openTerminationDetail(row)">查看</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="termination-pagination">
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

      <el-drawer
        v-model="detailVisible"
        title="退租详情"
        size="860px"
        append-to-body
        class="termination-detail-drawer"
      >
        <div v-if="detailRecord.recordId" class="termination-detail">
          <section class="detail-section">
            <div class="detail-section-title">基础信息</div>
            <div class="field-grid">
              <div v-for="item in detailSummaryItems" :key="item.label" class="field-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title">流程文件</div>
            <div class="drawer-actions">
              <el-button type="primary" @click="openArchive(detailRecord)">合同归档</el-button>
              <el-button type="primary" plain @click="handleOfflineRoomReview(detailRecord)">
                登记房屋验收
              </el-button>
              <el-button type="primary" plain @click="handleStartDepositRefund(detailRecord)">
                付款申请
              </el-button>
              <el-button type="primary" plain @click="handleOfflineDepositRefund(detailRecord)">
                上传支付凭证
              </el-button>
              <el-button
                v-if="detailRecord.printFileUrl"
                type="primary"
                plain
                @click="openWorkflowFile(detailRecord.printFileUrl, detailRecord)"
              >
                审批表
              </el-button>
              <el-button
                v-if="terminationAgreementUrl(detailRecord)"
                type="primary"
                plain
                @click="openWorkflowFile(terminationAgreementUrl(detailRecord), detailRecord)"
              >
                解除协议
              </el-button>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title-row">
              <div class="detail-section-title">付款申请前置</div>
              <el-tag
                :type="isDepositRefundCompleted(detailRecord) || paymentReady ? 'success' : 'warning'"
                effect="plain"
              >
                {{
                  isDepositRefundCompleted(detailRecord)
                    ? '已完成'
                    : paymentReady
                    ? '可发起'
                    : '待补齐'
                }}
              </el-tag>
            </div>
            <div class="payment-check-list">
              <div
                v-for="item in paymentPrerequisiteItems"
                :key="item.label"
                :class="['payment-check-item', { 'is-done': item.done }]"
              >
                <span>{{ item.label }}</span>
                <strong>{{ item.done ? '已完成' : item.pendingText }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title-row">
              <div class="detail-section-title">退租资料</div>
              <el-button type="primary" icon="el-icon-upload" @click="openMaterialUpload">
                上传资料
              </el-button>
            </div>
            <div v-if="materialGroups.length" class="material-group-list">
              <div v-for="group in materialGroups" :key="group.name" class="material-group">
                <div class="material-group-title">{{ group.name }}</div>
                <div class="material-file-list">
                  <div
                    v-for="file in group.files"
                    :key="file.fileUrl + file.fileName + file.uploadTime"
                    class="material-file-card"
                  >
                    <div class="material-file-info">
                      <span>{{ file.fileName || group.name }}</span>
                      <em v-if="materialFileMeta(file)">{{ materialFileMeta(file) }}</em>
                      <p v-if="file.remark">{{ file.remark }}</p>
                    </div>
                    <div class="material-file-actions">
                      <el-button text type="primary" @click="previewMaterial(file)">预览</el-button>
                      <el-button text type="primary" @click="downloadMaterial(file)">下载</el-button>
                      <el-button
                        v-if="file.deletable"
                        text
                        type="danger"
                        @click="removeMaterial(file)"
                      >
                        删除
                      </el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else :image-size="54" description="暂无退租资料" />
          </section>
        </div>
      </el-drawer>

      <el-dialog
        v-model="precheckVisible"
        :title="precheckTitle"
        width="560px"
        append-to-body
        class="precheck-dialog"
      >
        <div class="precheck-list">
          <div
            v-for="item in precheckItems"
            :key="item.label"
            :class="['precheck-item', { 'is-done': item.done }]"
          >
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.done ? '已满足' : item.pendingText }}</span>
            </div>
            <el-tag :type="item.done ? 'success' : 'warning'" effect="plain">
              {{ item.done ? '完成' : '待处理' }}
            </el-tag>
          </div>
        </div>
        <template #footer>
          <el-button @click="precheckVisible = false">关闭</el-button>
          <el-button v-if="precheckCanUpload" type="primary" @click="openMaterialUploadFromPrecheck">
            去上传资料
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="materialUploadVisible"
        title="上传退租资料"
        width="560px"
        append-to-body
        @close="resetMaterialUpload"
      >
        <el-form :model="materialUploadForm" label-width="100px" class="offline-form">
          <el-form-item label="资料类型" required>
            <el-select
              v-model="materialUploadForm.materialName"
              filterable
              allow-create
              clearable
              default-first-option
              placeholder="选择或输入资料类型"
              style="width: 100%"
              @change="handleMaterialNameChange"
            >
              <el-option
                v-for="item in materialTypeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.label"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="上传文件" required>
            <el-upload
              ref="materialUploadRef"
              action="/api/blade-resource/oss/endpoint/put-file"
              :headers="uploadHeaders"
              :limit="1"
              :show-file-list="true"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              :before-upload="beforeOfflineFileUpload"
              :on-success="handleMaterialUploadSuccess"
              :on-error="handleOfflineUploadError"
              :on-remove="handleMaterialUploadRemove"
            >
              <el-button icon="el-icon-upload">选择文件</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="materialUploadForm.remark"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="填写资料说明"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="materialUploadVisible = false">取消</el-button>
          <el-button type="primary" :loading="materialUploading" @click="submitMaterialUpload">
            保存资料
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="workflowVisible"
        :title="workflowDialogTitle"
        width="720px"
        append-to-body
        @close="resetWorkflowDialog"
      >
        <div class="workflow-dialog">
          <section class="detail-section">
            <div class="detail-section-title">{{ workflowSummaryTitle }}</div>
            <div class="field-grid">
              <div v-for="item in workflowSummaryItems" :key="item.label" class="field-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>
          <section class="detail-section">
            <div class="detail-section-title">审批配置</div>
            <el-form :model="workflowForm" label-width="108px">
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
                    :key="item.id || item.key"
                    :label="workflowProcessLabel(item)"
                    :value="item.key"
                  >
                    <div class="workflow-option">
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
        </div>
        <template #footer>
          <el-button @click="workflowVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!workflowForm.processDefKey || workflowLoading"
            @click="goWorkflow"
          >
            下一步
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="offlineReviewVisible"
        title="登记房屋验收"
        width="680px"
        append-to-body
        @close="resetOfflineReview"
      >
        <section class="offline-summary">
          <div>
            <span>合同编号</span>
            <strong>{{ offlineReviewRecord.contractNo || '-' }}</strong>
          </div>
          <div>
            <span>企业名称</span>
            <strong>{{ offlineReviewRecord.customerName || '-' }}</strong>
          </div>
          <div>
            <span>交接房源</span>
            <strong>{{ offlineReviewRecord.roomName || offlineReviewRecord.buildingName || '-' }}</strong>
          </div>
        </section>
        <el-form :model="offlineReviewForm" label-width="96px" class="offline-form">
          <el-form-item label="验收日期" required>
            <el-date-picker
              v-model="offlineReviewForm.acceptanceDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择验收日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="验收结果" required>
            <el-radio-group v-model="offlineReviewForm.acceptanceResult">
              <el-radio-button label="验收通过" />
              <el-radio-button label="需整改" />
            </el-radio-group>
          </el-form-item>
          <el-form-item label="验收说明" required>
            <el-input
              v-model="offlineReviewForm.acceptanceSituation"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="填写线下房屋验收情况、交接说明、遗留问题等"
            />
          </el-form-item>
          <el-form-item label="验收附件">
            <el-upload
              ref="offlineReviewUploadRef"
              action="/api/blade-resource/oss/endpoint/put-file"
              :headers="uploadHeaders"
              :limit="1"
              :show-file-list="true"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              :before-upload="beforeOfflineFileUpload"
              :on-success="handleOfflineReviewUploadSuccess"
              :on-error="handleOfflineUploadError"
              :on-remove="handleOfflineReviewUploadRemove"
            >
              <el-button icon="el-icon-upload">上传附件</el-button>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="offlineReviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="offlineReviewLoading" @click="submitOfflineReview">
            确认登记
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="depositVoucherVisible"
        title="上传支付凭证"
        width="640px"
        append-to-body
        @close="resetDepositVoucher"
      >
        <section class="offline-summary">
          <div>
            <span>合同编号</span>
            <strong>{{ depositVoucherRecord.contractNo || '-' }}</strong>
          </div>
          <div>
            <span>企业名称</span>
            <strong>{{ depositVoucherRecord.customerName || '-' }}</strong>
          </div>
          <div>
            <span>退款金额</span>
            <strong>{{ formatMoney(depositVoucherForm.amountPaid) }}</strong>
          </div>
        </section>
        <el-form :model="depositVoucherForm" label-width="96px" class="offline-form">
          <el-form-item label="支付日期" required>
            <el-date-picker
              v-model="depositVoucherForm.payDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择支付日期"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="支付金额" required>
            <el-input-number
              v-model="depositVoucherForm.amountPaid"
              :min="0"
              :precision="2"
              :step="100"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="支付凭证" required>
            <el-upload
              ref="depositVoucherUploadRef"
              action="/api/blade-resource/oss/endpoint/put-file"
              :headers="uploadHeaders"
              :limit="1"
              :show-file-list="true"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              :before-upload="beforeOfflineFileUpload"
              :on-success="handleDepositVoucherUploadSuccess"
              :on-error="handleOfflineUploadError"
              :on-remove="handleDepositVoucherUploadRemove"
            >
              <el-button icon="el-icon-upload">上传凭证</el-button>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="depositVoucherForm.remark"
              type="textarea"
              :rows="3"
              maxlength="300"
              show-word-limit
              placeholder="填写线下退款说明"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="depositVoucherVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="depositVoucherLoading"
            @click="submitDepositVoucher"
          >
            确认上传
          </el-button>
        </template>
      </el-dialog>

      <notice-preview-dialog
        v-model="noticePreview.visible"
        :title="noticePreview.title"
        :html="noticePreview.html"
        :loading="noticePreview.loading"
        :download-url="noticePreview.downloadUrl"
        :download-label="noticePreview.downloadLabel"
        @download="downloadNoticePreviewFile"
      />
    </div>
  </basic-container>
</template>

<script>
import { Base64 } from 'js-base64';
import {
  ensureDepositRefundPayment,
  getDepositRefundPayment,
  getLatestWorkflowRecord,
  getWorkflowRecordPage,
  offlineDepositRefund,
  offlineRoomReview,
  removeWorkflowRecordAttachment,
  uploadWorkflowRecordAttachment,
} from '@/api/contract/contract';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { approvalStatusDic } from '@/option/contract/archive';
import { statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import { noticePrintUrl } from '@/api/contract/print';
import {
  createNoticePreviewState,
  downloadNoticeFile,
  openAttachmentPreview,
  openNoticePreview,
} from '@/utils/contract-notice';

const TERMINATION_BUSINESS_TYPE = 'contract_termination';
const ROOM_REVIEW_BUSINESS_TYPE = 'contract_room_review';
const PAYMENT_BUSINESS_TYPE = 'contract_payment';
const TERMINATION_AGREEMENT_KEY = 'termination-agreement';
const MATERIAL_TYPE_OPTIONS = [
  { value: 'approval', label: '审批资料' },
  { value: 'room_acceptance', label: '房屋验收资料' },
  { value: 'deposit_refund', label: '押金退还' },
  { value: 'termination_agreement', label: '租赁合同解除补充协议' },
  { value: 'signed_termination_agreement', label: '已盖章解除补充协议' },
  { value: 'handover_file', label: '退租交接资料' },
  { value: 'other', label: '其他资料' },
];
const PAYMENT_REQUIRED_MATERIALS = [
  {
    value: 'room_acceptance',
    label: '房屋验收资料',
    types: ['room_acceptance'],
    names: ['房屋验收', '房屋验收资料'],
    attachmentKeys: ['acceptanceFileUrl', 'roomAcceptanceFiles', 'roomReviewFileUrl', 'room-review'],
  },
  {
    value: 'termination_agreement',
    label: '租赁合同解除补充协议',
    types: ['termination_agreement', 'signed_termination_agreement'],
    names: ['租赁合同解除补充协议', '已盖章解除补充协议'],
    attachmentKeys: ['termination-agreement', 'terminationAgreementUrl'],
  },
  {
    value: 'handover_file',
    label: '退租交接资料',
    types: ['handover_file'],
    names: ['退租交接资料'],
    attachmentKeys: ['fileList', 'handoverFileUrl'],
  },
];
const WORKFLOW_CONFIGS = {
  [ROOM_REVIEW_BUSINESS_TYPE]: {
    title: '发起房屋验收流程',
    summaryTitle: '退租交接信息',
    placeholder: '请选择已部署的房屋验收流程',
    formKeys: ['return'],
    defaultKeys: ['roomreview'],
    nameKeywords: ['房屋验收', '交接验收', '归还载体'],
  },
  [PAYMENT_BUSINESS_TYPE]: {
    title: '发起押金退还',
    summaryTitle: '押金退还信息',
    placeholder: '请选择已部署的付款审批流程',
    formKeys: ['pay', 'invoice'],
    defaultKeys: ['pay'],
    nameKeywords: ['付款', '缴费', '押金退还', '押金退款'],
  },
};

export default {
  name: 'ContractTermination',
  components: {
    NoticePreviewDialog,
  },
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        processStatus: '',
      },
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      loading: false,
      data: [],
      detailVisible: false,
      detailRecord: {},
      workflowVisible: false,
      workflowLoading: false,
      workflowType: ROOM_REVIEW_BUSINESS_TYPE,
      workflowRecord: {},
      workflowPayment: {},
      workflowProcessOptions: [],
      workflowForm: {
        processDefKey: '',
      },
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      materialUploadVisible: false,
      materialUploading: false,
      materialUploadForm: {
        materialType: '',
        materialName: '',
        fileUrl: '',
        fileName: '',
        remark: '',
      },
      offlineReviewVisible: false,
      offlineReviewLoading: false,
      offlineReviewRecord: {},
      offlineReviewForm: {
        acceptanceDate: '',
        acceptanceResult: '验收通过',
        acceptanceSituation: '',
        acceptanceFileUrl: '',
        acceptanceFileName: '',
      },
      depositVoucherVisible: false,
      depositVoucherLoading: false,
      depositVoucherRecord: {},
      depositVoucherPayment: {},
      depositVoucherForm: {
        payDate: '',
        amountPaid: 0,
        paymentVoucherUrl: '',
        paymentVoucherName: '',
        remark: '',
      },
      precheckVisible: false,
      precheckTitle: '',
      precheckItems: [],
      noticePreview: createNoticePreviewState(),
    };
  },
  computed: {
    ...mapGetters(['userInfo']),
    approvalStatusOptions() {
      return approvalStatusDic.filter(item => item.value !== '');
    },
    materialTypeOptions() {
      return MATERIAL_TYPE_OPTIONS;
    },
    materialGroups() {
      const groups = this.allMaterialFiles.reduce((result, file) => {
        const name = file.materialName || file.categoryName || file.category || '退租资料';
        if (!result[name]) {
          result[name] = [];
        }
        result[name].push(file);
        return result;
      }, {});
      return Object.keys(groups).map(name => ({
        name,
        files: groups[name],
      }));
    },
    allMaterialFiles() {
      const files = [];
      const append = (
        value,
        fallbackName,
        sourceName = '',
        sourceType = 'termination',
        recordId = this.detailRecord.recordId,
        deletable = false
      ) => {
        const list = Array.isArray(value) ? value : value ? [value] : [];
        list.forEach(item => {
          if (typeof item === 'string') {
            files.push({
              fileUrl: item,
              fileName: fallbackName,
              materialName: fallbackName,
              remark: sourceName,
              sourceName,
              sourceType,
              recordId,
              deletable,
            });
            return;
          }
          if (item && item.fileUrl) {
            files.push({
              materialName: item.materialName || item.categoryName || item.category || fallbackName,
              fileName: item.fileName || fallbackName,
              fileUrl: item.fileUrl,
              materialType: item.materialType || item.category || '',
              uploadBy: item.uploadBy || '',
              uploadTime: item.uploadTime || '',
              remark: item.remark || sourceName,
              sourceName,
              sourceType,
              recordId,
              deletable,
            });
          }
        });
      };
      const appendDirect = (attachment, key, fallbackName, sourceName = '', sourceType = 'generated', recordId = '') => {
        if (!attachment || !attachment[key]) return;
        append(
          {
            fileUrl: attachment[key],
            fileName: attachment[`${key}Name`] || attachment.fileName || fallbackName,
            materialName: fallbackName,
            remark: sourceName,
          },
          fallbackName,
          sourceName,
          sourceType,
          recordId,
          false
        );
      };
      this.recordAttachmentList(this.detailRecord).forEach(({ attachment, sourceName, sourceType, recordId }) => {
        const isTerminationSource = sourceType === 'termination';
        append(attachment.materials, '退租资料', sourceName, sourceType, recordId, isTerminationSource);
        append(attachment.approvalMaterials, '审批资料', sourceName, sourceType, recordId, false);
        append(attachment.roomAcceptanceFiles, '房屋验收资料', sourceName, sourceType, recordId, false);
        append(attachment.depositRefundFiles, '押金退还', sourceName, sourceType, recordId, false);
        append(attachment.fileList, '退租交接资料', sourceName, sourceType, recordId, false);
        appendDirect(attachment, 'acceptanceFileUrl', '房屋验收资料', sourceName, sourceType, recordId);
        appendDirect(attachment, 'roomReviewFileUrl', '房屋验收流程文件', sourceName, sourceType, recordId);
        appendDirect(attachment, 'paymentVoucherUrl', '押金退还支付凭证', sourceName, sourceType, recordId);
        appendDirect(attachment, 'terminationAgreementUrl', '租赁合同解除补充协议', sourceName, sourceType, recordId);
        appendDirect(attachment, 'termination-approval', '退租审批表', sourceName, 'generated', recordId);
        appendDirect(attachment, 'termination-agreement', '租赁合同解除补充协议', sourceName, 'generated', recordId);
        appendDirect(attachment, 'room-review', '房屋验收流程文件', sourceName, 'generated', recordId);
      });
      const seen = new Set();
      return files.filter(file => {
        const key = `${file.materialName || ''}|${file.fileUrl || ''}`;
        if (!file.fileUrl || seen.has(key)) return false;
        seen.add(key);
        return true;
      });
    },
    paymentReady() {
      return this.paymentApplicationBlockedReason(this.detailRecord) === '';
    },
    paymentPrerequisiteItems() {
      return this.paymentApplicationPrerequisites(this.detailRecord);
    },
    precheckCanUpload() {
      return this.precheckItems.some(item => item.uploadable && !item.done);
    },
    detailSummaryItems() {
      const row = this.detailRecord || {};
      return [
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '企业名称', value: row.customerName || '-' },
        { label: '房源信息', value: row.roomName || row.buildingName || '-' },
        { label: '审批状态', value: this.approvalStatusText(row.processStatus) },
        { label: '退租阶段', value: this.terminationStageText(row) },
        { label: '当前节点', value: row.currentNode || '-' },
        { label: '合同状态', value: row.contractStatusName || this.statusText(row.contractStatus) },
        { label: '保证金', value: this.formatMoney(row.deposit) },
        { label: '发起时间', value: row.createTime || '-' },
        { label: '完成时间', value: row.approvalTime || '-' },
      ];
    },
    workflowConfig() {
      return WORKFLOW_CONFIGS[this.workflowType] || WORKFLOW_CONFIGS[ROOM_REVIEW_BUSINESS_TYPE];
    },
    workflowDialogTitle() {
      return this.workflowConfig.title;
    },
    workflowSummaryTitle() {
      return this.workflowConfig.summaryTitle;
    },
    workflowProcessPlaceholder() {
      return this.workflowConfig.placeholder;
    },
    workflowSummaryItems() {
      const row = this.workflowRecord || {};
      const payment = this.workflowPayment || {};
      if (this.workflowType === PAYMENT_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: row.contractNo || payment.contractNo || '-' },
          { label: '企业名称', value: row.customerName || payment.customerName || '-' },
          { label: '退款事项', value: payment.feeName || '押金退还' },
          { label: '退款金额', value: this.formatMoney(payment.amountDue || row.deposit) },
          {
            label: '合同状态',
            value: row.contractStatusName || this.statusText(row.contractStatus),
          },
          {
            label: '审批状态',
            value: payment.paymentApprovalStatus
              ? this.approvalStatusText(payment.paymentApprovalStatus)
              : '未发起',
          },
        ];
      }
      return [
        { label: '合同编号', value: row.contractNo || '-' },
        { label: '企业名称', value: row.customerName || '-' },
        { label: '交接房源', value: row.roomName || row.buildingName || '-' },
        { label: '合同期限', value: `${row.startDate || '--'} 至 ${row.endDate || '--'}` },
        { label: '保证金', value: this.formatMoney(row.deposit) },
        { label: '当前状态', value: row.contractStatusName || this.statusText(row.contractStatus) },
      ];
    },
    summaryCards() {
      const running = this.data.filter(item => item.processStatus === 'running').length;
      const approved = this.data.filter(item => item.processStatus === 'approved').length;
      const handover = this.data.filter(item => String(item.contractStatus) === '7').length;
      return [
        { key: 'total', label: '退租申请', value: this.page.total || 0 },
        { key: 'running', label: '审批中', value: running },
        { key: 'approved', label: '已通过', value: approved },
        { key: 'handover', label: '待交接', value: handover },
      ];
    },
  },
  created() {
    this.loadData();
  },
  methods: {
    searchChange() {
      this.page.currentPage = 1;
      this.loadData();
    },
    searchReset() {
      this.query = {
        contractNo: '',
        customerName: '',
        processStatus: '',
      };
      this.page.currentPage = 1;
      this.loadData();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadData();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadData();
    },
    loadData() {
      this.loading = true;
      getWorkflowRecordPage(
        this.page.currentPage,
        this.page.pageSize,
        this.cleanParams({
          ...this.query,
          businessType: TERMINATION_BUSINESS_TYPE,
        })
      )
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          return this.enrichTerminationRecords(result.records || []);
        })
        .then(records => {
          this.data = records || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    enrichTerminationRecords(records = []) {
      const contractRecords = records.filter(item => item.contractId);
      const paymentTargets = records.filter(item => String(item.contractStatus) === '4' && item.contractId);
      if (!contractRecords.length && !paymentTargets.length) {
        return Promise.resolve(records);
      }
      const paymentPromise = Promise.all(
        paymentTargets.map(item =>
          getDepositRefundPayment(item.contractId)
            .then(res => {
              const payment = res.data.data || {};
              return {
                contractId: item.contractId,
                depositRefundPayStatus: String(payment.payStatus || ''),
                depositRefundPaymentId: payment.paymentId || '',
                depositRefundApprovalStatus: payment.paymentApprovalStatus || '',
              };
            })
            .catch(() => ({
              contractId: item.contractId,
              depositRefundPayStatus: '',
              depositRefundPaymentId: '',
              depositRefundApprovalStatus: '',
            }))
        )
      );
      const roomReviewPromise = Promise.all(
        contractRecords.map(item =>
          getLatestWorkflowRecord(item.contractId, ROOM_REVIEW_BUSINESS_TYPE)
            .then(res => {
              const record = res.data.data || {};
              return {
                contractId: item.contractId,
                roomReviewRecordId: record.recordId || '',
                roomReviewAttachmentJson: record.attachmentJson || '',
                roomReviewPrintFileUrl: record.printFileUrl || '',
                roomReviewProcessStatus: record.processStatus || '',
              };
            })
            .catch(() => ({
              contractId: item.contractId,
              roomReviewRecordId: '',
              roomReviewAttachmentJson: '',
              roomReviewPrintFileUrl: '',
              roomReviewProcessStatus: '',
            }))
        )
      );
      return Promise.all([paymentPromise, roomReviewPromise]).then(([paymentStates, roomReviewStates]) => {
        const paymentStateMap = paymentStates.reduce((result, item) => {
          result[item.contractId] = item;
          return result;
        }, {});
        const roomReviewStateMap = roomReviewStates.reduce((result, item) => {
          result[item.contractId] = item;
          return result;
        }, {});
        return records.map(item => ({
          ...item,
          ...(paymentStateMap[item.contractId] || {}),
          ...(roomReviewStateMap[item.contractId] || {}),
        }));
      });
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
    openTerminationDetail(row) {
      this.detailRecord = { ...(row || {}) };
      this.detailVisible = true;
    },
    parseAttachment(row = {}) {
      return this.parseAttachmentJson(row.attachmentJson);
    },
    parseAttachmentJson(attachmentJson) {
      if (!attachmentJson) return {};
      try {
        return typeof attachmentJson === 'string' ? JSON.parse(attachmentJson) || {} : attachmentJson || {};
      } catch (error) {
        return {};
      }
    },
    recordAttachmentList(row = {}) {
      const list = [
        {
          attachment: this.parseAttachment(row),
          sourceName: '退租审批',
          sourceType: 'termination',
          recordId: row.recordId,
        },
      ];
      const roomReviewAttachment = this.parseAttachmentJson(row.roomReviewAttachmentJson);
      if (Object.keys(roomReviewAttachment).length) {
        list.push({
          attachment: roomReviewAttachment,
          sourceName: '房屋验收流程',
          sourceType: 'roomReview',
          recordId: row.roomReviewRecordId,
        });
      }
      if (row.roomReviewPrintFileUrl) {
        list.push({
          sourceName: '房屋验收流程',
          sourceType: 'roomReviewPrint',
          recordId: row.roomReviewRecordId,
          attachment: {
            roomReviewFileUrl: row.roomReviewPrintFileUrl,
          },
        });
      }
      return list;
    },
    openArchive(row) {
      if (!row || !row.contractId) {
        this.$message.warning('当前退租记录未关联合同');
        return;
      }
      this.$confirm('确定进入该合同档案归档?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => {
          this.$router.push({
            path: '/contract/archive',
            query: {
              contractId: row.contractId,
              contractNo: row.contractNo,
              openArchive: '1',
            },
          });
        })
        .catch(() => {});
    },
    openMaterialUpload() {
      if (!this.detailRecord.recordId) {
        this.$message.warning('请选择退租记录');
        return;
      }
      this.materialUploadForm = {
        materialType: '',
        materialName: '',
        fileUrl: '',
        fileName: '',
        remark: '',
      };
      this.materialUploadVisible = true;
      this.$nextTick(() => {
        const upload = this.$refs.materialUploadRef;
        if (upload && upload.clearFiles) upload.clearFiles();
      });
    },
    handleMaterialNameChange(value) {
      const match = this.materialTypeOptions.find(item => item.label === value);
      this.materialUploadForm.materialType = match ? match.value : 'custom';
      this.materialUploadForm.materialName = value || '';
    },
    handleMaterialUploadSuccess(response, file) {
      if (!response || response.success === false) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const fileUrl = this.extractUploadUrl(response);
      if (!fileUrl) {
        this.$message.error('上传成功但未返回文件地址');
        return;
      }
      this.materialUploadForm.fileUrl = fileUrl;
      this.materialUploadForm.fileName = file?.name || '';
      this.$message.success(file?.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleMaterialUploadRemove() {
      this.materialUploadForm.fileUrl = '';
      this.materialUploadForm.fileName = '';
    },
    submitMaterialUpload() {
      if (!this.detailRecord.recordId) {
        this.$message.warning('请选择退租记录');
        return;
      }
      if (!this.materialUploadForm.materialName) {
        this.$message.warning('请选择或输入资料类型');
        return;
      }
      if (!this.materialUploadForm.fileUrl) {
        this.$message.warning('请先上传文件');
        return;
      }
      const materialName = String(this.materialUploadForm.materialName || '').trim();
      const match = this.materialTypeOptions.find(item => item.label === materialName);
      this.materialUploading = true;
      uploadWorkflowRecordAttachment(this.detailRecord.recordId, {
        ...this.materialUploadForm,
        materialName,
        materialType: match ? match.value : this.materialUploadForm.materialType || 'custom',
      })
        .then(res => {
          const updated = res.data.data || {};
          const attachmentJson = updated.attachmentJson || this.detailRecord.attachmentJson;
          this.detailRecord = {
            ...this.detailRecord,
            attachmentJson,
          };
          this.data = this.data.map(item =>
            String(item.recordId) === String(this.detailRecord.recordId)
              ? { ...item, attachmentJson }
              : item
          );
          this.materialUploadVisible = false;
          this.$message.success('资料已保存');
        })
        .finally(() => {
          this.materialUploading = false;
        });
    },
    resetMaterialUpload() {
      this.materialUploadForm = {
        materialType: '',
        materialName: '',
        fileUrl: '',
        fileName: '',
        remark: '',
      };
    },
    materialFileMeta(file = {}) {
      return [file.sourceName, file.uploadBy, file.uploadTime].filter(Boolean).join(' / ');
    },
    previewMaterial(file) {
      if (!file || !file.fileUrl) {
        this.$message.warning('暂无资料文件');
        return;
      }
      openAttachmentPreview(this.noticePreview, file, '退租资料预览');
    },
    downloadMaterial(file) {
      if (!file || !file.fileUrl) {
        this.$message.warning('暂无资料文件');
        return;
      }
      downloadNoticeFile(file.fileUrl, file.fileName || file.materialName || '退租资料').catch(() => {
        this.$message.error('资料下载失败，请稍后重试');
      });
    },
    removeMaterial(file) {
      if (!file || !file.deletable || !file.recordId) {
        this.$message.warning('该资料由流程自动生成，不能删除');
        return;
      }
      this.$confirm('确认删除该资料吗？', '提示', {
        type: 'warning',
      })
        .then(() =>
          removeWorkflowRecordAttachment(file.recordId, {
            fileUrl: file.fileUrl,
            materialName: file.materialName,
          })
        )
        .then(res => {
          const updated = res.data.data || {};
          const attachmentJson = updated.attachmentJson || '';
          this.detailRecord = {
            ...this.detailRecord,
            attachmentJson,
          };
          this.data = this.data.map(item =>
            String(item.recordId) === String(file.recordId)
              ? { ...item, attachmentJson }
              : item
          );
          this.$message.success('资料已删除');
        })
        .catch(() => {});
    },
    openContract(row) {
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
          customerName: row.customerName,
        },
      });
    },
    handleStartRoomReview(row) {
      if (!this.ensurePrerequisites('发起房屋验收流程前置条件', this.roomReviewPrerequisites(row))) {
        return;
      }
      this.workflowType = ROOM_REVIEW_BUSINESS_TYPE;
      this.workflowRecord = { ...(row || {}) };
      this.workflowPayment = {};
      this.workflowForm.processDefKey = '';
      this.workflowVisible = true;
      this.loadWorkflowProcessOptions();
    },
    handleStartDepositRefund(row) {
      if (!this.ensurePrerequisites('付款申请前置条件', this.paymentApplicationPrerequisites(row))) {
        return;
      }
      ensureDepositRefundPayment(row.contractId).then(res => {
        const payment = res.data.data || {};
        if (payment.paymentApprovalStatus === 'running') {
          this.$message.warning('该押金退还付款审批正在进行中');
          return;
        }
        if (payment.payStatus === '1') {
          this.$message.warning('该押金退还已完成');
          return;
        }
        this.workflowType = PAYMENT_BUSINESS_TYPE;
        this.workflowRecord = { ...(row || {}) };
        this.workflowPayment = payment;
        this.workflowForm.processDefKey = '';
        this.workflowVisible = true;
        this.loadWorkflowProcessOptions();
      });
    },
    handleOfflineRoomReview(row) {
      if (!this.ensurePrerequisites('登记房屋验收前置条件', this.offlineRoomReviewPrerequisites(row))) {
        return;
      }
      this.offlineReviewRecord = { ...(row || {}) };
      this.offlineReviewForm = {
        acceptanceDate: this.formatDate(new Date()),
        acceptanceResult: '验收通过',
        acceptanceSituation: '',
        acceptanceFileUrl: '',
        acceptanceFileName: '',
      };
      this.offlineReviewVisible = true;
      this.$nextTick(() => {
        const upload = this.$refs.offlineReviewUploadRef;
        if (upload && upload.clearFiles) upload.clearFiles();
      });
    },
    handleOfflineDepositRefund(row) {
      if (!this.ensurePrerequisites('上传支付凭证前置条件', this.depositVoucherPrerequisites(row))) {
        return;
      }
      getDepositRefundPayment(row.contractId).then(res => {
        const payment = res.data.data || {};
        if (!payment.paymentId) {
          this.$message.warning('请先发起付款申请');
          return;
        }
        if (payment.paymentApprovalStatus !== 'approved') {
          this.$message.warning('付款申请审批完成后才可以上传支付凭证');
          return;
        }
        this.depositVoucherRecord = { ...(row || {}) };
        this.depositVoucherPayment = { ...(payment || {}) };
        this.depositVoucherForm = {
          payDate: this.formatDate(new Date()),
          amountPaid: Number(payment.amountDue || row.deposit || 0),
          paymentVoucherUrl: '',
          paymentVoucherName: '',
          remark: '',
        };
        this.depositVoucherVisible = true;
        this.$nextTick(() => {
          const upload = this.$refs.depositVoucherUploadRef;
          if (upload && upload.clearFiles) upload.clearFiles();
        });
      });
    },
    beforeOfflineFileUpload(file) {
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
    extractUploadUrl(response) {
      const data = response?.data || {};
      return data.link || data.url || data.path || response?.link || response?.url || '';
    },
    handleOfflineReviewUploadSuccess(response, file) {
      if (!response || response.success === false) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      this.offlineReviewForm.acceptanceFileUrl = this.extractUploadUrl(response);
      this.offlineReviewForm.acceptanceFileName = file?.name || '';
      this.$message.success(file?.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleOfflineReviewUploadRemove() {
      this.offlineReviewForm.acceptanceFileUrl = '';
      this.offlineReviewForm.acceptanceFileName = '';
    },
    handleDepositVoucherUploadSuccess(response, file) {
      if (!response || response.success === false) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      this.depositVoucherForm.paymentVoucherUrl = this.extractUploadUrl(response);
      this.depositVoucherForm.paymentVoucherName = file?.name || '';
      this.$message.success(file?.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleDepositVoucherUploadRemove() {
      this.depositVoucherForm.paymentVoucherUrl = '';
      this.depositVoucherForm.paymentVoucherName = '';
    },
    handleOfflineUploadError(error) {
      const message = (error && error.message) || '上传失败，请重试';
      this.$message.error(message);
    },
    submitOfflineReview() {
      if (!this.offlineReviewRecord.contractId) {
        this.$message.warning('请选择退租记录');
        return;
      }
      if (!this.offlineReviewForm.acceptanceDate) {
        this.$message.warning('请选择验收日期');
        return;
      }
      if (!this.offlineReviewForm.acceptanceSituation) {
        this.$message.warning('请填写房屋验收情况');
        return;
      }
      this.offlineReviewLoading = true;
      offlineRoomReview(this.offlineReviewRecord.contractId, this.offlineReviewForm)
        .then(() => {
          this.$message.success('房屋验收情况已登记');
          this.offlineReviewVisible = false;
          this.loadData();
        })
        .finally(() => {
          this.offlineReviewLoading = false;
        });
    },
    submitDepositVoucher() {
      if (!this.depositVoucherRecord.contractId) {
        this.$message.warning('请选择退租记录');
        return;
      }
      if (!this.depositVoucherForm.payDate) {
        this.$message.warning('请选择支付日期');
        return;
      }
      if (!this.depositVoucherForm.amountPaid || this.depositVoucherForm.amountPaid <= 0) {
        this.$message.warning('请填写支付金额');
        return;
      }
      if (!this.depositVoucherForm.paymentVoucherUrl) {
        this.$message.warning('请上传支付凭证');
        return;
      }
      this.depositVoucherLoading = true;
      offlineDepositRefund(this.depositVoucherRecord.contractId, {
        ...this.depositVoucherForm,
        paymentId: this.depositVoucherPayment.paymentId,
      })
        .then(() => {
          this.$message.success('支付凭证已上传');
          this.depositVoucherVisible = false;
          this.loadData();
        })
        .finally(() => {
          this.depositVoucherLoading = false;
        });
    },
    resetOfflineReview() {
      this.offlineReviewRecord = {};
      this.offlineReviewForm = {
        acceptanceDate: '',
        acceptanceResult: '验收通过',
        acceptanceSituation: '',
        acceptanceFileUrl: '',
        acceptanceFileName: '',
      };
    },
    resetDepositVoucher() {
      this.depositVoucherRecord = {};
      this.depositVoucherPayment = {};
      this.depositVoucherForm = {
        payDate: '',
        amountPaid: 0,
        paymentVoucherUrl: '',
        paymentVoucherName: '',
        remark: '',
      };
    },
    loadWorkflowProcessOptions() {
      this.workflowLoading = true;
      getDeploymentList(1, -1, { status: 1 })
        .then(res => {
          const records = (res.data.data || {}).records || [];
          this.workflowProcessOptions = records
            .filter(item => this.isWorkflowProcess(item))
            .sort((a, b) => {
              const versionDiff = Number(b.version || 0) - Number(a.version || 0);
              if (versionDiff !== 0) return versionDiff;
              const timeA = new Date(a.deployTime || a.createTime || 0).getTime();
              const timeB = new Date(b.deployTime || b.createTime || 0).getTime();
              return timeB - timeA;
            });
          this.workflowForm.processDefKey = this.resolveWorkflowProcessKey(
            this.workflowProcessOptions
          );
          if (this.workflowProcessOptions.length === 0) {
            this.$message.warning(
              `未找到可用的${this.workflowDialogTitle.replace('发起', '')}，请先在部署管理激活流程`
            );
          }
        })
        .finally(() => {
          this.workflowLoading = false;
        });
    },
    isWorkflowProcess(item = {}) {
      const name = item.name || '';
      const key = item.key || '';
      const formKey = item.formKey || '';
      const config = this.workflowConfig;
      return (
        (config.formKeys || []).includes(formKey) ||
        (config.defaultKeys || []).includes(key) ||
        (config.nameKeywords || []).some(keyword => name.includes(keyword) || key.includes(keyword))
      );
    },
    resolveWorkflowProcessKey(processOptions = []) {
      const config = this.workflowConfig;
      const preferred =
        processOptions.find(item => (config.formKeys || []).includes(item.formKey)) ||
        processOptions.find(item => (config.defaultKeys || []).includes(item.key)) ||
        processOptions.find(item =>
          (config.nameKeywords || []).some(keyword => (item.name || '').includes(keyword))
        ) ||
        processOptions[0];
      return preferred ? preferred.key : '';
    },
    workflowProcessLabel(item = {}) {
      const name = item.name || item.key || '-';
      const key = item.key ? ` / ${item.key}` : '';
      const version = item.version ? ` v${item.version}` : '';
      return `${name}${version}${key}`;
    },
    canStartRoomReview(row) {
      return this.roomReviewBlockedReason(row) === '';
    },
    canStartDepositRefund(row) {
      return this.paymentApplicationBlockedReason(row) === '';
    },
    isDepositRefundCompleted(row) {
      return String(row?.depositRefundPayStatus || '') === '1';
    },
    ensurePrerequisites(title, items = []) {
      if ((items || []).every(item => item.done)) {
        return true;
      }
      this.precheckTitle = title;
      this.precheckItems = items || [];
      this.precheckVisible = true;
      return false;
    },
    openMaterialUploadFromPrecheck() {
      this.precheckVisible = false;
      this.openMaterialUpload();
    },
    basicRecordPrerequisites(row, options = {}) {
      return [
        {
          label: '已选择退租记录',
          done: Boolean(row && row.contractId),
          pendingText: '请先选择退租记录',
        },
        {
          label: '退租审批已完成',
          done: row?.processStatus === 'approved',
          pendingText: '待退租审批完成',
        },
        ...(options.requireHandover
          ? [
              {
                label: '进入退租交接阶段',
                done: ['7', '8', '4'].includes(String(row?.contractStatus || '')),
                pendingText: '待退租审批完成后进入交接',
              },
            ]
          : []),
        ...(options.requireTerminated
          ? [
              {
                label: '房屋验收流程已完成',
                done: String(row?.contractStatus || '') === '4',
                pendingText: '待房屋验收流程完成',
              },
            ]
          : []),
      ];
    },
    roomReviewPrerequisites(row) {
      return this.basicRecordPrerequisites(row, { requireHandover: true });
    },
    offlineRoomReviewPrerequisites(row) {
      return this.basicRecordPrerequisites(row, { requireHandover: true });
    },
    paymentApplicationPrerequisites(row) {
      return [
        ...this.basicRecordPrerequisites(row, { requireTerminated: true }),
        {
          label: '合同保证金已配置',
          done: Number(row?.deposit || 0) > 0,
          pendingText: '该合同未配置可退保证金',
        },
        {
          label: '付款审批未进行中',
          done: this.paymentApprovalStatus(row) !== 'running',
          pendingText: '当前付款审批正在进行中',
        },
        {
          label: '押金退还未完成',
          done: !this.isDepositRefundCompleted(row),
          pendingText: '该押金退还已完成',
        },
        ...PAYMENT_REQUIRED_MATERIALS.map(item => ({
          label: item.label,
          done: this.hasPaymentMaterial(row, item),
          pendingText: '待上传',
          uploadable: true,
        })),
      ];
    },
    depositVoucherPrerequisites(row) {
      return [
        ...this.paymentApplicationPrerequisites(row),
        {
          label: '付款申请审批已完成',
          done: this.paymentApprovalStatus(row) === 'approved',
          pendingText: '待付款申请审批完成',
        },
      ];
    },
    paymentApprovalStatus(row) {
      return String(row?.depositRefundApprovalStatus || row?.paymentApprovalStatus || '');
    },
    roomReviewBlockedReason(row) {
      const pending = this.roomReviewPrerequisites(row).find(item => !item.done);
      return pending ? pending.pendingText : '';
    },
    paymentApplicationBlockedReason(row) {
      const pending = this.paymentApplicationPrerequisites(row).find(item => !item.done);
      return pending ? pending.pendingText : '';
    },
    missingPaymentMaterials(row) {
      return PAYMENT_REQUIRED_MATERIALS.filter(item => !this.hasPaymentMaterial(row, item)).map(
        item => item.label
      );
    },
    hasPaymentMaterial(row, requirement) {
      return this.recordAttachmentList(row).some(({ attachment }) => {
        if ((requirement.attachmentKeys || []).some(key => this.hasAttachmentValue(attachment[key]))) {
          return true;
        }
        return this.attachmentFiles(attachment.materials).some(file => {
          const materialType = String(file.materialType || file.category || '');
          const materialName = String(file.materialName || file.categoryName || file.category || '');
          return (
            (requirement.types || []).includes(materialType) ||
            (requirement.names || []).some(name => materialName.includes(name))
          );
        });
      });
    },
    attachmentFiles(value) {
      if (Array.isArray(value)) return value;
      if (value && typeof value === 'object') return [value];
      return [];
    },
    hasAttachmentValue(value) {
      if (Array.isArray(value)) return value.length > 0;
      if (value && typeof value === 'object') return Object.keys(value).length > 0;
      return Boolean(value);
    },
    goWorkflow() {
      if (!this.workflowRecord.contractId) {
        this.$message.warning('请选择退租记录');
        return;
      }
      if (this.workflowType === PAYMENT_BUSINESS_TYPE && !this.workflowPayment.paymentId) {
        this.$message.warning('请先生成押金退还付款单');
        return;
      }
      if (!this.workflowForm.processDefKey) {
        this.$message.warning('请选择审批流程');
        return;
      }
      const payload =
        this.workflowType === PAYMENT_BUSINESS_TYPE
          ? this.buildDepositRefundPayload()
          : this.buildRoomReviewPayload();
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      this.workflowVisible = false;
      this.$router.push(`/plugin/workflow/pages/process/form/start/${encodedParam}`);
    },
    buildRoomReviewPayload() {
      const row = this.workflowRecord || {};
      return {
        processDefKey: this.workflowForm.processDefKey,
        params: {
          processDefKey: this.workflowForm.processDefKey,
          businessType: ROOM_REVIEW_BUSINESS_TYPE,
          businessTable: 'biz_contract',
          businessKey: String(row.contractId || ''),
          contractId: row.contractId,
          contractNo: row.contractNo,
          contractName: row.contractName,
          customerId: row.customerId,
          customerName: row.customerName,
          roomIds: row.roomIds,
          roomName: row.roomName,
          buildingName: row.buildingName,
          parkId: row.parkId,
          monthlyRent: row.monthlyRent,
          deposit: row.deposit,
          startDate: row.startDate,
          endDate: row.endDate,
          sourceTerminationRecordId: row.recordId,
          templateKey: 'room-review',
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    buildDepositRefundPayload() {
      const row = this.workflowRecord || {};
      const payment = this.workflowPayment || {};
      const processKey = this.workflowForm.processDefKey || '';
      const isInvoiceWorkflow = processKey.includes('invoice');
      return {
        processDefKey: this.workflowForm.processDefKey,
        params: {
          processDefKey: this.workflowForm.processDefKey,
          businessType: PAYMENT_BUSINESS_TYPE,
          businessTable: 'biz_contract_payment',
          businessKey: String(payment.paymentId || ''),
          contractId: row.contractId || payment.contractId,
          paymentId: payment.paymentId,
          contractNo: row.contractNo || payment.contractNo,
          contractName: row.contractName || payment.contractName,
          customerId: row.customerId,
          customerName: row.customerName || payment.customerName,
          roomIds: row.roomIds,
          roomName: row.roomName,
          buildingName: row.buildingName,
          parkId: row.parkId || payment.parkId,
          feeType: payment.feeType || 'deposit_refund',
          feeName: payment.feeName || '押金退还',
          periodStart: payment.periodStart,
          periodEnd: payment.periodEnd,
          amountDue: payment.amountDue || row.deposit,
          amountPaid: payment.amountPaid,
          payDeadline: payment.payDeadline,
          templateKey: isInvoiceWorkflow ? 'invoice-apply' : 'payment-notice',
          refundType: 'deposit_refund',
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    resetWorkflowDialog() {
      this.workflowType = ROOM_REVIEW_BUSINESS_TYPE;
      this.workflowRecord = {};
      this.workflowPayment = {};
      this.workflowProcessOptions = [];
      this.workflowForm.processDefKey = '';
    },
    openWorkflowFile(url, row) {
      if (!url || !row) return;
      const noticeType = this.noticeTypeByUrl(url, row);
      if (!noticeType) {
        downloadNoticeFile(url, '退租流程文件');
        return;
      }
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          contractId: row.contractId,
          formDataJson: row.formDataJson || '',
        },
        noticePrintUrl(noticeType, { contractId: row.contractId }),
        '退租流程文件',
        '审批表预览'
      );
    },
    noticeTypeByUrl(url, row) {
      if (url === this.terminationAgreementUrl(row) || String(url).includes('termination-agreement')) {
        return 'termination-agreement';
      }
      if (String(url).includes('room-review')) {
        return 'room-review';
      }
      if (String(url).includes('payment-notice')) {
        return 'payment-notice';
      }
      if (String(url).includes('invoice-apply')) {
        return 'invoice-apply';
      }
      if (row.businessType === PAYMENT_BUSINESS_TYPE) {
        return row.processDefKey && String(row.processDefKey).includes('invoice')
          ? 'invoice-apply'
          : 'payment-notice';
      }
      return 'termination-approval';
    },
    downloadNoticePreviewFile() {
      if (!this.noticePreview.downloadUrl) return;
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName);
    },
    terminationAgreementUrl(row) {
      return this.attachmentUrl(row, TERMINATION_AGREEMENT_KEY);
    },
    attachmentUrl(row, key) {
      if (!row || !row.attachmentJson || !key) return '';
      try {
        const attachment = JSON.parse(row.attachmentJson);
        return attachment && attachment[key] ? attachment[key] : '';
      } catch (error) {
        return '';
      }
    },
    terminationStageText(row) {
      if (!row) return '-';
      if (row.processStatus === 'running') return '退租中';
      if (row.processStatus === 'rejected') return '退租驳回';
      if (row.processStatus === 'canceled') return '退租取消';
      if (String(row.contractStatus) === '8') return '房屋验收流程中';
      if (this.isDepositRefundCompleted(row)) return '退租已完成';
      if (String(row.contractStatus) === '4') return '待押金退还';
      if (String(row.contractStatus) === '7') return '待登记房屋验收';
      if (row.processStatus === 'approved') return '退租已通过';
      return this.approvalStatusText(row.processStatus);
    },
    terminationStageType(row) {
      if (!row) return 'info';
      if (row.processStatus === 'rejected') return 'danger';
      if (row.processStatus === 'running' || String(row.contractStatus) === '8') return 'warning';
      if (this.isDepositRefundCompleted(row)) return 'success';
      if (String(row.contractStatus) === '4') return 'success';
      if (String(row.contractStatus) === '7') return 'primary';
      return 'info';
    },
    statusText(value) {
      const item = statusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '-';
    },
    contractStatusType(value) {
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
      return map[String(value || '')] || 'info';
    },
    approvalStatusText(value) {
      const item = approvalStatusDic.find(ele => String(ele.value) === String(value || ''));
      return item ? item.label : '-';
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
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    formatDate(date) {
      const y = date.getFullYear();
      const m = date.getMonth() + 1;
      const d = date.getDate();
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.termination-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
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

.termination-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.termination-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.termination-search :deep(.el-input),
.termination-search :deep(.el-select) {
  width: 190px;
}

.termination-table {
  width: 100%;
  border-radius: 10px;
  overflow: hidden;
}

.termination-table :deep(.el-table__header th),
.termination-table :deep(.el-table__cell) {
  text-align: center;
}

.termination-table :deep(.termination-full-cell .cell) {
  white-space: normal;
  word-break: break-all;
  line-height: 20px;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  min-width: 40px;
  padding: 0 3px;
  margin-left: 0;
}

.workflow-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
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

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.field-item {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}

.field-item span {
  flex: 0 0 104px;
  color: #8c98aa;
}

.field-item strong {
  min-width: 0;
  color: #303133;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.termination-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.payment-check-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.payment-check-item {
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.payment-check-item span {
  min-width: 0;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.payment-check-item strong {
  flex: 0 0 auto;
  color: #b88230;
  font-weight: 500;
}

.payment-check-item.is-done {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.payment-check-item.is-done strong {
  color: #529b2e;
}

.precheck-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.precheck-item {
  min-height: 54px;
  padding: 10px 12px;
  border: 1px solid #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.precheck-item div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.precheck-item strong {
  color: #303133;
  font-weight: 600;
}

.precheck-item span {
  color: #8c98aa;
  font-size: 12px;
}

.precheck-item.is-done {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.detail-section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.detail-section-title-row .detail-section-title {
  margin-bottom: 0;
}

.material-group-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.material-group {
  padding: 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}

.material-group-title {
  margin-bottom: 10px;
  color: #1f2937;
  font-weight: 600;
}

.material-file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.material-file-card {
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 6px;
  background: #f8fafc;
  color: #1059c6;
}

.material-file-info {
  min-width: 0;
}

.material-file-info span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-file-info em {
  display: block;
  margin-top: 3px;
  color: #8c98aa;
  font-size: 12px;
  font-style: normal;
}

.material-file-info p {
  margin: 4px 0 0;
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
}

.material-file-actions {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.material-file-actions :deep(.el-button) {
  padding: 0;
  margin-left: 0;
}

.workflow-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.workflow-option em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.offline-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.offline-summary div {
  min-height: 60px;
  padding: 10px 12px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #f8fafc;
}

.offline-summary span {
  display: block;
  color: #8c98aa;
  font-size: 12px;
}

.offline-summary strong {
  display: block;
  margin-top: 6px;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.offline-form :deep(.el-upload) {
  width: 100%;
}

.termination-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.termination-page :deep(.el-button),
.termination-page :deep(.el-input__wrapper),
.termination-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field-grid {
    grid-template-columns: 1fr;
  }

  .payment-check-list {
    grid-template-columns: 1fr;
  }

  .offline-summary {
    grid-template-columns: 1fr;
  }
}
</style>
