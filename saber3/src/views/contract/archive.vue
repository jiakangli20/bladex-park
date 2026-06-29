<template>
  <basic-container>
    <avue-crud
      class="archive-crud"
      :option="option"
      :table-loading="loading"
      :data="data"
      v-model:page="page"
      ref="crud"
      :permission="permissionList"
      @search-change="searchChange"
      @search-reset="searchReset"
      @current-change="currentChange"
      @size-change="sizeChange"
      @refresh-change="refreshChange"
      @on-load="onLoad"
    >
      <template #customerName="{ row }">
        <el-link
          type="primary"
          :underline="false"
          class="archive-customer-link"
          @click="openArchive(row)"
        >
          {{ row.customerName || '-' }}
        </el-link>
      </template>
      <template #rentPrice="{ row }">
        <span>{{ formatUnitPrice(row.rentPrice) }}</span>
      </template>
      <template #rentArea="{ row }">
        <span>{{ formatArea(row.rentArea) }}</span>
      </template>
      <template #contractNo="{ row }">
        <span class="archive-single-line">{{ row.contractNo || '-' }}</span>
      </template>
      <template #parentContractId="{ row }">
        <span>{{ contractSourceText(row) }}</span>
      </template>
      <template #contractStatus="{ row }">
        <el-tag :type="statusType(row.contractStatus)" effect="plain">
          {{ row.contractStatusName || statusText(row.contractStatus) }}
        </el-tag>
      </template>
      <template #propertyFee="{ row }">
        <span>{{ formatUnitPrice(row.propertyFee) }}</span>
      </template>
      <template #deposit="{ row }">
        <span>{{ formatMoneyWithUnit(row.deposit) }}</span>
      </template>
      <template #menu="{ row }">
        <div class="archive-table-actions">
          <el-button
            type="primary"
            text
            v-if="permission.contract_archive_detail"
            @click="openArchive(row)"
            >查看档案
          </el-button>
          <el-button
            type="primary"
            text
            v-if="permission.contract_archive_export_approval"
            @click="handleExportApproval(row)"
            >导出审批表
          </el-button>
        </div>
      </template>
    </avue-crud>

    <el-drawer v-model="drawerVisible" title="合同档案详情" size="920px" append-to-body>
      <el-skeleton :loading="detailLoading" animated>
        <template #template>
          <el-skeleton-item variant="p" style="width: 90%" />
          <el-skeleton-item variant="p" style="width: 70%" />
          <el-skeleton-item variant="p" style="width: 80%" />
        </template>
        <template #default>
          <el-steps :active="archiveStep" finish-status="success" simple>
            <el-step title="合同签订" />
            <el-step title="租金收缴" />
            <el-step title="补充协议/退租" />
            <el-step title="档案归档" />
          </el-steps>

          <el-tabs v-model="activeTab" class="archive-tabs">
            <el-tab-pane label="合同档案" name="contract">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="合同编号">{{
                  current.contractNo || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="合同名称">{{
                  current.contractName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="客户名称">{{
                  current.customerName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="所属园区">{{
                  current.parkName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="房源">{{
                  current.roomName || current.buildingName || '-'
                }}</el-descriptions-item>
                <el-descriptions-item label="租期">
                  {{ current.startDate || '-' }} 至 {{ current.endDate || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="租赁面积">
                  {{ formatArea(current.rentArea) }}
                </el-descriptions-item>
                <el-descriptions-item label="月租金">
                  {{ formatMoney(current.monthlyRent) }}
                </el-descriptions-item>
                <el-descriptions-item label="押金">
                  {{ formatMoney(current.deposit) }}
                </el-descriptions-item>
                <el-descriptions-item label="合同状态">
                  <el-tag :type="statusType(current.contractStatus)">
                    {{ current.contractStatusName || statusText(current.contractStatus) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="审批状态">
                  <el-tag :type="approvalStatusType(current.approvalStatus)">
                    {{ approvalStatusText(current.approvalStatus) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="当前节点">
                  {{ current.approvalCurrentNodeName || '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{
                  current.remark || '-'
                }}</el-descriptions-item>
              </el-descriptions>
              <div class="archive-actions">
                <el-button
                  type="primary"
                  v-if="permission.contract_archive_export_approval"
                  @click="handleExportApproval(current)"
                  >导出合同审批表
                </el-button>
                <el-button
                  v-if="permission.contract_archive_print"
                  @click="handlePrint(current)"
                  >下载合同正文
                </el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="审批记录" name="workflow">
              <el-table v-loading="workflowLoading" :data="workflowData" border>
                <el-table-column prop="businessType" label="业务类型" width="140" align="center">
                  <template #default="{ row }">
                    {{ workflowBusinessTypeText(row.businessType) }}
                  </template>
                </el-table-column>
                <el-table-column prop="processName" label="流程名称" min-width="160" show-overflow-tooltip />
                <el-table-column prop="processInsId" label="实例ID" min-width="180" show-overflow-tooltip />
                <el-table-column prop="processStatus" label="状态" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag :type="workflowStatusType(row.processStatus)" effect="plain">
                      {{ workflowStatusText(row.processStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="currentNode" label="当前节点" min-width="140" show-overflow-tooltip />
                <el-table-column prop="approvalTime" label="完成时间" width="170" align="center">
                  <template #default="{ row }">
                    {{ row.approvalTime || row.createTime || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="printFileUrl" label="文件" width="110" align="center">
                  <template #default="{ row }">
                    <el-button v-if="row.printFileUrl" type="primary" text @click="openWorkflowFile(row.printFileUrl, row)">
                      查看
                    </el-button>
                    <span v-else>-</span>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="workflowData.length === 0" description="暂无审批记录" />
            </el-tab-pane>

            <el-tab-pane label="缴费记录" name="payment">
              <el-table :data="payments" border>
                <el-table-column prop="feeName" label="费用名称" min-width="120" />
                <el-table-column label="账期" min-width="190">
                  <template #default="{ row }">
                    {{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}
                  </template>
                </el-table-column>
                <el-table-column prop="amountDue" label="应收" width="110" align="right" />
                <el-table-column prop="amountPaid" label="实收" width="110" align="right" />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.payStatus === '1' ? 'success' : 'warning'">
                      {{ dicText(paymentStatusDic, row.payStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="payments.length === 0" description="暂无缴费记录" />
            </el-tab-pane>

            <el-tab-pane label="补充协议" name="supplement">
              <div class="archive-section-header">
                <div>
                  <div class="archive-section-header__title">补充协议归档</div>
                  <div class="archive-section-header__desc">
                    合同变更线下办理后，将盖章补充协议上传到这里归档。
                  </div>
                </div>
                <el-button type="primary" @click="openSupplementDialog">上传补充协议</el-button>
              </div>
              <el-table :data="supplements" border>
                <el-table-column
                  prop="agreementName"
                  label="协议名称"
                  min-width="170"
                  show-overflow-tooltip
                />
                <el-table-column
                  prop="changeItem"
                  label="变更事项"
                  min-width="180"
                  show-overflow-tooltip
                />
                <el-table-column
                  prop="fileName"
                  label="文件名称"
                  min-width="170"
                  show-overflow-tooltip
                />
                <el-table-column prop="createBy" label="归档人" width="110" align="center" />
                <el-table-column prop="createTime" label="归档时间" width="170" align="center" />
                <el-table-column label="操作" width="150" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" text @click="downloadSupplement(row)">下载</el-button>
                    <el-button type="danger" text @click="removeSupplement(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="supplements.length === 0" description="暂无补充协议" />
            </el-tab-pane>

            <el-tab-pane label="退租记录" name="termination">
              <el-table :data="terminations" border>
                <el-table-column prop="terminationNo" label="退租单号" min-width="150" />
                <el-table-column prop="terminationTime" label="退租时间" width="170" />
                <el-table-column label="审批状态" width="120">
                  <template #default="{ row }">
                    {{ dicText(terminationApprovalStatusDic, row.approvalStatus) }}
                  </template>
                </el-table-column>
                <el-table-column label="结算状态" width="120">
                  <template #default="{ row }">
                    {{ dicText(terminationStatusDic, row.status) }}
                  </template>
                </el-table-column>
                <el-table-column
                  prop="approvalOpinion"
                  label="审批意见"
                  min-width="180"
                  show-overflow-tooltip
                />
              </el-table>
              <el-empty v-if="terminations.length === 0" description="暂无退租记录" />
            </el-tab-pane>

            <el-tab-pane label="操作日志" name="log">
              <el-timeline>
                <el-timeline-item
                  v-for="item in logs"
                  :key="item.logId"
                  :timestamp="item.operateTime"
                  placement="top"
                >
                  <div class="archive-log">
                    <div class="archive-log__title">{{ item.actionDesc || item.action }}</div>
                    <div class="archive-log__operator">{{ item.operator || '-' }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-if="logs.length === 0" description="暂无日志" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-skeleton>
    </el-drawer>

    <el-dialog v-model="printVisible" :title="printTitle" width="820px" append-to-body>
      <div class="archive-print" v-html="printHtml"></div>
      <template #footer>
        <el-button @click="printVisible = false">关闭</el-button>
        <el-button type="primary" @click="doBrowserPrint">打印</el-button>
      </template>
    </el-dialog>

    <notice-preview-dialog
      v-model="noticePreview.visible"
      :title="noticePreview.title"
      :html="noticePreview.html"
      :loading="noticePreview.loading"
      :download-url="noticePreview.downloadUrl"
      @download="downloadNoticePreviewFile"
    />

    <el-dialog
      v-model="supplementVisible"
      title="上传补充协议"
      width="620px"
      append-to-body
      @close="resetSupplementForm"
    >
      <div class="supplement-contract">
        <div>
          <span>合同编号</span>
          <strong>{{ current.contractNo || '-' }}</strong>
        </div>
        <div>
          <span>企业名称</span>
          <strong>{{ current.customerName || '-' }}</strong>
        </div>
      </div>
      <el-form
        ref="supplementFormRef"
        :model="supplementForm"
        :rules="supplementRules"
        label-position="top"
      >
        <el-form-item label="协议名称" prop="agreementName">
          <el-input v-model="supplementForm.agreementName" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="变更事项" prop="changeItem">
          <el-input
            v-model="supplementForm.changeItem"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="补充协议文件" prop="fileUrl">
          <el-upload
            drag
            ref="supplementUploadRef"
            action="/api/blade-resource/oss/endpoint/put-file"
            :headers="uploadHeaders"
            :limit="1"
            :file-list="supplementUploadFileList"
            :show-file-list="true"
            accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
            :before-upload="beforeSupplementUpload"
            :on-success="handleSupplementUploadSuccess"
            :on-error="handleSupplementUploadError"
            :on-remove="handleSupplementUploadRemove"
            class="supplement-upload"
          >
            <div class="supplement-upload__text">
              <strong>上传盖章后的补充协议</strong>
              <span>支持 PDF、Word、JPG、PNG，单个文件不超过 20MB</span>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="supplementForm.remark"
            type="textarea"
            :rows="2"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplementVisible = false">取消</el-button>
        <el-button type="primary" :loading="supplementLoading" @click="submitSupplement">
          确定归档
        </el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import {
  contractApprovalPrintUrl,
  contractTextPrintUrl,
  getArchiveDetail,
  getArchiveList,
  getSupplementAgreements,
  getWorkflowRecords,
  removeSupplementAgreement,
  saveSupplementAgreement,
} from '@/api/contract/archive';
import { noticePrintUrl } from '@/api/contract/print';
import {
  approvalStatusDic,
  paymentStatusDic,
  tableOption,
  terminationApprovalStatusDic,
  terminationStatusDic,
} from '@/option/contract/archive';
import { statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import {
  createNoticePreviewState,
  downloadNoticeFile,
  openNoticePreview,
} from '@/utils/contract-notice';

export default {
  components: {
    NoticePreviewDialog,
  },
  data() {
    return {
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      option: tableOption,
      data: [],
      drawerVisible: false,
      detailLoading: false,
      activeTab: 'contract',
      current: {},
      payments: [],
      supplements: [],
      terminations: [],
      logs: [],
      workflowLoading: false,
      workflowData: [],
      archiveStep: 0,
      printVisible: false,
      printTitle: '合同打印',
      printHtml: '',
      noticePreview: createNoticePreviewState(),
      supplementVisible: false,
      supplementLoading: false,
      supplementUploadFileList: [],
      supplementForm: {
        agreementName: '',
        changeItem: '',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      },
      supplementRules: {
        agreementName: [{ required: true, message: '请输入协议名称', trigger: 'blur' }],
        changeItem: [{ required: true, message: '请输入变更事项', trigger: 'blur' }],
        fileUrl: [{ required: true, message: '请上传补充协议文件', trigger: 'change' }],
      },
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      approvalStatusDic,
      paymentStatusDic,
      terminationApprovalStatusDic,
      terminationStatusDic,
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: false,
        viewBtn: this.validData(this.permission.contract_archive_detail, false),
        delBtn: false,
        editBtn: false,
      };
    },
  },
  created() {
    if (this.$route.query.contractId) {
      this.query.contractId = this.$route.query.contractId;
    }
  },
  methods: {
    openArchive(row) {
      const contractId = row.contractId;
      this.drawerVisible = true;
      this.detailLoading = true;
      this.activeTab = 'contract';
      this.workflowData = [];
      getArchiveDetail(contractId)
        .then(res => {
          const data = res.data.data || {};
          this.current = data.contract || row;
          this.payments = data.payments || [];
          this.supplements = data.supplements || [];
          this.terminations = data.terminations || [];
          this.logs = data.logs || [];
          this.archiveStep = data.archiveStep || 0;
          this.loadWorkflow(contractId);
        })
        .finally(() => {
          this.detailLoading = false;
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
    openWorkflowFile(url, row) {
      if (!url || !row) return;
      const noticeType = this.archiveWorkflowNoticeType(row, url);
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          contractId: row.contractId,
          paymentId: row.paymentId,
          formDataJson: row.formDataJson || '',
        },
        noticePrintUrl(noticeType, {
          contractId: row.contractId,
          paymentId: row.paymentId,
        }) || url,
        '合同流程文件',
        row.processName || '审批表预览'
      );
    },
    downloadPrintFile(url, fallbackName = '合同文件') {
      downloadNoticeFile(url, fallbackName);
    },
    archiveWorkflowNoticeType(row, url) {
      const currentUrl = String(url || '');
      if (currentUrl.includes('termination-agreement')) return 'termination-agreement';
      if (currentUrl.includes('room-review')) return 'room-review';
      if (currentUrl.includes('payment-notice')) return 'payment-notice';
      if (currentUrl.includes('invoice-apply')) return 'invoice-apply';
      if (currentUrl.includes('legal-letter')) return 'legal-letter';
      if (currentUrl.includes('overdue-notice')) return 'overdue-notice';
      if (currentUrl.includes('project-approval')) return 'project-approval';
      if (row.businessType === 'contract_payment') {
        return row.processDefKey && String(row.processDefKey).includes('invoice')
          ? 'invoice-apply'
          : 'payment-notice';
      }
      if (row.businessType === 'contract_room_review') return 'room-review';
      if (row.businessType === 'contract_overdue_legal') return 'legal-letter';
      if (row.businessType === 'contract_termination') return 'termination-approval';
      return 'contract-approval';
    },
    downloadNoticePreviewFile() {
      if (!this.noticePreview.downloadUrl) return;
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName);
    },
    loadSupplements(contractId) {
      if (!contractId) return;
      getSupplementAgreements(contractId).then(res => {
        this.supplements = res.data.data || [];
      });
    },
    openSupplementDialog() {
      if (!this.current.contractId) {
        this.$message.warning('请先选择合同档案');
        return;
      }
      const contractNo = this.current.contractNo || '';
      this.supplementForm = {
        agreementName: contractNo ? `${contractNo}补充协议` : '合同补充协议',
        changeItem: '',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      };
      this.supplementUploadFileList = [];
      this.supplementVisible = true;
      this.$nextTick(() => {
        if (this.$refs.supplementFormRef) {
          this.$refs.supplementFormRef.clearValidate();
        }
        if (this.$refs.supplementUploadRef && this.$refs.supplementUploadRef.clearFiles) {
          this.$refs.supplementUploadRef.clearFiles();
        }
      });
    },
    resetSupplementForm() {
      this.supplementLoading = false;
      this.supplementUploadFileList = [];
      this.supplementForm = {
        agreementName: '',
        changeItem: '',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      };
      if (this.$refs.supplementFormRef) {
        this.$refs.supplementFormRef.clearValidate();
      }
    },
    beforeSupplementUpload(file) {
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
    handleSupplementUploadSuccess(response, file) {
      if (!response || response.success === false) {
        this.supplementForm.fileUrl = '';
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      const fileUrl =
        data.link || data.url || data.path || response.link || response.url || response.data || '';
      this.supplementForm.fileUrl = fileUrl || '';
      this.supplementForm.fileName = (file && file.name) || data.name || data.originalName || '';
      this.supplementForm.fileType = this.getFileType(this.supplementForm.fileName || fileUrl);
      this.supplementUploadFileList = file
        ? [{ name: file.name, url: fileUrl }]
        : this.supplementUploadFileList;
      if (this.$refs.supplementFormRef) {
        this.$refs.supplementFormRef.validateField('fileUrl');
      }
      this.$message.success(file && file.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleSupplementUploadError(error) {
      this.supplementForm.fileUrl = '';
      const message = (error && error.message) || '上传失败，请重试';
      this.$message.error(message);
    },
    handleSupplementUploadRemove() {
      this.supplementForm.fileName = '';
      this.supplementForm.fileUrl = '';
      this.supplementForm.fileType = '';
      this.supplementUploadFileList = [];
    },
    submitSupplement() {
      if (!this.current.contractId) {
        this.$message.warning('请先选择合同档案');
        return;
      }
      this.$refs.supplementFormRef.validate(valid => {
        if (!valid) return;
        this.supplementLoading = true;
        saveSupplementAgreement({
          ...this.supplementForm,
          contractId: this.current.contractId,
        })
          .then(() => {
            this.$message.success('补充协议已归档');
            this.supplementVisible = false;
            this.loadSupplements(this.current.contractId);
          })
          .finally(() => {
            this.supplementLoading = false;
          });
      });
    },
    removeSupplement(row) {
      this.$confirm('确认删除该补充协议归档吗？', '提示', {
        type: 'warning',
      })
        .then(() => removeSupplementAgreement(row.agreementId))
        .then(() => {
          this.$message.success('补充协议已删除');
          this.loadSupplements(this.current.contractId);
        })
        .catch(() => {});
    },
    downloadSupplement(row) {
      if (!row.fileUrl) {
        this.$message.warning('暂无补充协议文件');
        return;
      }
      const link = document.createElement('a');
      link.href = row.fileUrl;
      link.target = '_blank';
      link.download = row.fileName || row.agreementName || '补充协议';
      link.click();
    },
    getFileType(value) {
      const match = String(value || '').match(/\.([a-z0-9]+)(?:\?|#|$)/i);
      return match ? match[1].toLowerCase() : '';
    },
    handleExportApproval(row) {
      if (!row || !row.contractId) return;
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType: 'contract-approval',
          contractId: row.contractId,
        },
        contractApprovalPrintUrl(row.contractId),
        `${row.contractNo || '合同'}会签审批表.xlsx`,
        '合同会签审批表'
      );
    },
    handlePrint(row) {
      if (!row || !row.contractId) return;
      this.downloadPrintFile(contractTextPrintUrl(row.contractId, this.resolveRentMode(row)), `${row.contractNo || '合同'}正文.docx`);
    },
    resolveRentMode(row = {}) {
      const increaseNode = String(row.rentIncreaseNode || '');
      const remark = String(row.remark || '');
      if (increaseNode && increaseNode !== 'none') return 'floating';
      if (remark.includes('浮动')) return 'floating';
      return 'fixed';
    },
    doBrowserPrint() {
      const win = window.open('', '_blank');
      if (!win) return;
      win.document.write(
        `<!doctype html><html><head><meta charset="utf-8"><title>${this.printTitle}</title></head><body>${this.printHtml}</body></html>`
      );
      win.document.close();
      win.print();
    },
    searchReset() {
      this.query = {};
      this.reload();
    },
    searchChange(params, done) {
      this.query = params;
      this.page.currentPage = 1;
      this.onLoad(this.page, params);
      done();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
    },
    refreshChange() {
      this.reload();
    },
    reload() {
      this.onLoad(this.page, this.query);
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getArchiveList(page.currentPage, page.pageSize, {
        ...params,
        ...this.query,
      })
        .then(res => {
          const data = res.data.data;
          this.page.total = data.total;
          this.data = data.records || [];
        })
        .finally(() => {
          this.loading = false;
        });
    },
    statusText(value) {
      return this.dicText(statusDic, value);
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
      return map[value] || 'info';
    },
    approvalStatusText(value) {
      return this.dicText(approvalStatusDic, value || '');
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
    contractSourceText(row = {}) {
      return row.parentContractId ? '续签合同' : '新建合同';
    },
    workflowBusinessTypeText(value) {
      const map = {
        contract_approval: '合同审批',
        contract_payment: '付款流程',
        contract_room_review: '房屋验收',
        contract_termination: '退租审批',
        contract_overdue_legal: '逾期律师函',
      };
      return map[String(value || '')] || value || '-';
    },
    workflowStatusText(value) {
      return this.approvalStatusText(value);
    },
    workflowStatusType(value) {
      return this.approvalStatusType(value);
    },
    dicText(dic, value) {
      const item = dic.find(ele => `${ele.value}` === `${value}`);
      return item ? item.label : '-';
    },
    formatMoney(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    formatUnitPrice(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      return `${this.formatMoney(value)}元/㎡·月`;
    },
    formatMoneyWithUnit(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      return `${this.formatMoney(value)}元`;
    },
    formatArea(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      return `${value} ㎡`;
    },
  },
};
</script>

<style lang="scss" scoped>
.archive-crud :deep(.avue-crud__search) {
  padding: 16px 18px 4px;
  margin-bottom: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.archive-crud :deep(.avue-crud__search .el-card) {
  width: 100%;
  border: 0;
  border-radius: 10px;
  background: transparent;
  box-shadow: none;
}

.archive-crud :deep(.avue-crud__search .el-card__body) {
  padding: 0;
}

.archive-crud :deep(.avue-crud__search .el-row) {
  margin-right: 0 !important;
  margin-left: 0 !important;
}

.archive-crud :deep(.avue-crud__search .el-col) {
  flex: 0 0 auto;
  width: auto;
  max-width: none;
  padding-right: 0 !important;
  padding-left: 0 !important;
}

.archive-crud :deep(.avue-crud__search .el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.archive-crud :deep(.avue-crud__search .el-input),
.archive-crud :deep(.avue-crud__search .el-select) {
  width: 190px;
}

.archive-crud :deep(.avue-crud__search .avue-form__menu--left) {
  margin-left: 0;
}

.archive-crud :deep(.avue-crud__search .el-button + .el-button) {
  margin-left: 10px;
}

.archive-crud :deep(.avue-crud__table .el-card__body) {
  padding-bottom: 0;
}

.archive-crud :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.archive-crud :deep(.el-table__body-wrapper) {
  padding-bottom: 0;
}

.archive-crud :deep(.el-scrollbar__bar.is-horizontal) {
  bottom: 0;
}

.archive-tabs {
  margin-top: 16px;
}

.archive-single-line {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
}

.archive-customer-link {
  white-space: nowrap;
}

.archive-table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.archive-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.archive-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.archive-section-header__title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.archive-section-header__desc {
  margin-top: 4px;
  color: #909399;
  font-size: 13px;
}

.archive-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
}

.archive-log__title {
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.archive-log__operator {
  color: #909399;
  font-size: 12px;
}

.archive-print {
  max-height: 60vh;
  overflow: auto;
  border: 1px solid #ebeef5;
}

.supplement-contract {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #f7f9fc;
}

.supplement-contract span,
.supplement-contract strong {
  display: block;
}

.supplement-contract span {
  margin-bottom: 5px;
  color: #909399;
  font-size: 12px;
}

.supplement-contract strong {
  overflow: hidden;
  color: #303133;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.supplement-upload {
  width: 100%;
}

.supplement-upload__text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #606266;
}

.supplement-upload__text strong {
  color: #303133;
  font-size: 14px;
}

.supplement-upload__text span {
  color: #909399;
  font-size: 12px;
}
</style>
