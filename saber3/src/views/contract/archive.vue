<template>
  <basic-container>
    <div v-if="detailMode" class="archive-detail-page">
      <el-skeleton :loading="detailLoading" animated>
        <template #template>
          <el-skeleton-item variant="p" style="width: 40%" />
          <el-skeleton-item variant="p" style="width: 70%" />
          <el-skeleton-item variant="p" style="width: 80%" />
        </template>
        <template #default>
          <header class="archive-detail-header">
            <div class="archive-title-row">
              <el-button class="archive-back" text icon="el-icon-arrow-left" @click="closeArchiveDetail" />
              <h2>{{ customerTitle }}</h2>
            </div>
            <div class="archive-header-actions">
              <el-button
                v-if="permission.contract_archive_export_approval"
                type="primary"
                plain
                @click="handleExportApproval(current)"
              >
                合同审批表
              </el-button>
              <el-button
                v-if="permission.contract_archive_print"
                type="primary"
                @click="handlePrint(current)"
              >
                合同正文
              </el-button>
            </div>
            <div class="archive-company-grid">
              <div>
                <span>联系人：</span>
                <strong>{{ detailValue(customerDetail.contactName || current.followUser) }}</strong>
              </div>
              <div>
                <span>行业分类：</span>
                <strong>{{ detailValue(customerDetail.industry) }}</strong>
              </div>
              <div>
                <span>租客标签：</span>
                <template v-if="customerTags.length">
                  <el-tag
                    v-for="tag in customerTags"
                    :key="tag"
                    size="small"
                    effect="plain"
                    class="archive-tag"
                  >
                    {{ tag }}
                  </el-tag>
                </template>
                <strong v-else>--</strong>
              </div>
              <div>
                <span>邮箱：</span>
                <strong>{{ detailValue(customerDetail.contactEmail) }}</strong>
              </div>
              <div>
                <span>租客编码：</span>
                <strong>{{ detailValue(current.customerId || customerDetail.customerId) }}</strong>
              </div>
            </div>
          </header>

          <el-tabs v-model="activeTab" class="archive-detail-tabs">
            <el-tab-pane label="详情" name="detail">
              <section class="archive-detail-section">
                <div class="archive-section-title">企业信息</div>
                <div class="archive-info-grid">
                  <div v-for="item in enterpriseInfoItems" :key="item.label" class="archive-info-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </section>
              <section class="archive-detail-section">
                <div class="archive-section-title">合同概要</div>
                <div class="archive-info-grid">
                  <div v-for="item in contractInfoItems" :key="item.label" class="archive-info-item">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                </div>
              </section>
            </el-tab-pane>

            <el-tab-pane label="合同" name="contract">
              <section class="archive-detail-section archive-table-section">
                <el-table :data="contractRows" class="archive-flat-table">
                  <el-table-column prop="contractNo" label="合同编号" min-width="160" align="center">
                    <template #default="{ row }">
                      <el-link type="primary" underline="never" @click="handlePrint(row)">
                        {{ row.contractNo || '-' }}
                      </el-link>
                    </template>
                  </el-table-column>
                  <el-table-column prop="contractStatus" label="合同状态" width="140" align="center">
                    <template #default="{ row }">
                      <el-tag :type="statusType(row.contractStatus)" effect="plain">
                        {{ row.contractStatusName || statusText(row.contractStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="合同类型" width="140" align="center">
                    <template #default="{ row }">{{ contractTypeText(row) }}</template>
                  </el-table-column>
                  <el-table-column label="租赁单价" width="160" align="center">
                    <template #default="{ row }">{{ formatRentUnitPrice(row.rentPrice) }}</template>
                  </el-table-column>
                  <el-table-column prop="signDate" label="签订日期" width="150" align="center">
                    <template #default="{ row }">{{ row.signDate || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="合同来源" width="140" align="center">
                    <template #default="{ row }">{{ contractSourceText(row) }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="180" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" text @click="handlePrint(row)">预览正文</el-button>
                      <el-button
                        v-if="permission.contract_archive_export_approval"
                        type="primary"
                        text
                        @click="handleExportApproval(row)"
                      >
                        审批表
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </section>
            </el-tab-pane>

            <el-tab-pane label="账单" name="bill">
              <section class="archive-detail-section archive-table-section">
                <el-table :data="payments" class="archive-flat-table">
                  <el-table-column prop="contractNo" label="合同编号" min-width="150" show-overflow-tooltip />
                  <el-table-column prop="feeName" label="账单名称" min-width="150" />
                  <el-table-column label="账期" min-width="200" align="center">
                    <template #default="{ row }">
                      {{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="amountDue" label="应收金额" width="130" align="right">
                    <template #default="{ row }">{{ formatMoneyWithUnit(row.amountDue) }}</template>
                  </el-table-column>
                  <el-table-column prop="amountPaid" label="实收金额" width="130" align="right">
                    <template #default="{ row }">{{ formatMoneyWithUnit(row.amountPaid) }}</template>
                  </el-table-column>
                  <el-table-column prop="payDeadline" label="缴费期限" width="150" align="center">
                    <template #default="{ row }">{{ row.payDeadline || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="账单状态" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag :type="billStatusType(row.payStatus)" effect="plain">
                        {{ billStatusText(row.payStatus) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="payments.length === 0" description="暂无账单" />
              </section>
            </el-tab-pane>

            <el-tab-pane label="附件" name="attachment">
              <section class="archive-detail-section archive-table-section">
                <div class="archive-attachment-bar">
                  <el-button type="primary" icon="el-icon-plus" @click="openSupplementDialog">
                    新增附件
                  </el-button>
                </div>
                <el-table :data="attachmentRows" class="archive-flat-table">
                  <el-table-column prop="fileName" label="文件名称" width="240" show-overflow-tooltip>
                    <template #default="{ row }">{{ row.fileName || row.agreementName || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="createBy" label="上传人" width="150" align="center">
                    <template #default="{ row }">{{ row.createBy || row.updateBy || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="上传时间" width="180" align="center">
                    <template #default="{ row }">{{ row.createTime || row.updateTime || '-' }}</template>
                  </el-table-column>
                  <el-table-column label="操作" width="230" align="center" fixed="right">
                    <template #default="{ row }">
                      <div class="attachment-table-actions">
                        <el-button type="primary" text @click="previewAttachment(row)">预览</el-button>
                        <el-button type="primary" text @click="downloadSupplement(row)">下载</el-button>
                        <el-button type="danger" text @click="removeSupplement(row)">删除</el-button>
                      </div>
                    </template>
                  </el-table-column>
                </el-table>
                <el-empty v-if="attachmentRows.length === 0" description="暂无附件" />
              </section>
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-skeleton>
    </div>

    <avue-crud
      v-else
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
          underline="never"
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
        </div>
      </template>
    </avue-crud>

    <el-drawer
      v-model="drawerVisible"
      title="合同档案详情"
      size="920px"
      append-to-body
      class="archive-detail-drawer"
    >
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
                  width="220"
                  show-overflow-tooltip
                />
                <el-table-column prop="createBy" label="归档人" width="110" align="center" />
                <el-table-column prop="createTime" label="归档时间" width="170" align="center" />
                <el-table-column label="操作" width="230" align="center" fixed="right">
                  <template #default="{ row }">
                    <div class="attachment-table-actions">
                      <el-button type="primary" text @click="previewAttachment(row)">预览</el-button>
                      <el-button type="primary" text @click="downloadSupplement(row)">下载</el-button>
                      <el-button type="danger" text @click="removeSupplement(row)">删除</el-button>
                    </div>
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
      :download-label="noticePreview.downloadLabel"
      @download="downloadNoticePreviewFile"
    />

    <el-dialog
      v-model="supplementVisible"
      title="新增附件"
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
        <el-form-item label="附件名称" prop="agreementName">
          <el-input
            v-model="supplementForm.agreementName"
            maxlength="100"
            show-word-limit
            placeholder="请输入附件名称"
          />
        </el-form-item>
        <el-form-item label="附件文件" prop="fileUrl">
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
              <strong>上传合同归档附件</strong>
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
          确定上传
        </el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import {
  contractApprovalPrintUrl,
  getArchiveDetail,
  getArchiveList,
  getSupplementAgreements,
  getWorkflowRecords,
  removeSupplementAgreement,
  saveSupplementAgreement,
} from '@/api/contract/archive';
import { noticePrintUrl } from '@/api/contract/print';
import { getCustomerDetail } from '@/api/business/customer';
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
  openAttachmentPreview,
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
      detailMode: false,
      drawerVisible: false,
      detailLoading: false,
      activeTab: 'detail',
      current: {},
      customerDetail: {},
      customerContracts: [],
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
      archiveAutoOpened: false,
      supplementVisible: false,
      supplementLoading: false,
      supplementUploadFileList: [],
      supplementForm: {
        agreementName: '',
        changeItem: '合同档案附件',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      },
      supplementRules: {
        agreementName: [{ required: true, message: '请输入附件名称', trigger: 'blur' }],
        fileUrl: [{ required: true, message: '请上传附件文件', trigger: 'change' }],
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
    customerTitle() {
      return this.customerDetail.enterpriseName || this.current.customerName || '客户详情';
    },
    customerTags() {
      const tags = this.customerDetail.tags || [];
      if (!Array.isArray(tags)) return [];
      return tags
        .map(item => item.tagName || item.name || item.label || item.tagValue || '')
        .filter(Boolean);
    },
    enterpriseInfoItems() {
      const customer = this.customerDetail || {};
      return [
        { label: '企业名称', value: this.detailValue(customer.enterpriseName || this.current.customerName) },
        { label: '统一社会信用代码', value: this.detailValue(customer.creditCode) },
        { label: '企业类型', value: this.detailValue(customer.enterpriseType) },
        { label: '所属行业', value: this.detailValue(customer.industry) },
        { label: '联系人', value: this.detailValue(customer.contactName || this.current.followUser) },
        { label: '联系电话', value: this.detailValue(customer.contactPhone) },
        { label: '联系邮箱', value: this.detailValue(customer.contactEmail) },
        { label: '企业地址', value: this.detailValue(customer.address || customer.registeredAddress) },
      ];
    },
    contractInfoItems() {
      return [
        { label: '合同编号', value: this.detailValue(this.current.contractNo) },
        { label: '合同名称', value: this.detailValue(this.current.contractName) },
        { label: '所属园区', value: this.detailValue(this.current.parkName) },
        { label: '房源信息', value: this.detailValue(this.current.roomName || this.current.buildingName) },
        { label: '租赁面积', value: this.formatArea(this.current.rentArea) },
        { label: '月租金', value: this.formatMoneyWithUnit(this.current.monthlyRent) },
        { label: '租期', value: `${this.current.startDate || '-'} 至 ${this.current.endDate || '-'}` },
        { label: '合同状态', value: this.current.contractStatusName || this.statusText(this.current.contractStatus) },
      ];
    },
    contractRows() {
      if (this.customerContracts.length) {
        return this.customerContracts;
      }
      return this.current && this.current.contractId ? [this.current] : [];
    },
    attachmentRows() {
      return this.supplements || [];
    },
  },
  created() {
    if (this.$route.query.contractId) {
      this.query.contractId = this.$route.query.contractId;
    }
    if (this.$route.query.contractNo) {
      this.query.contractNo = this.$route.query.contractNo;
    }
  },
  methods: {
    openArchive(row) {
      if (!row || !row.contractId) return;
      const contractId = row.contractId;
      this.detailMode = true;
      this.drawerVisible = false;
      this.detailLoading = true;
      this.activeTab = 'detail';
      this.workflowData = [];
      this.customerDetail = {};
      this.customerContracts = [];
      getArchiveDetail(contractId)
        .then(res => {
          const data = res.data.data || {};
          this.current = data.contract || row;
          this.customerContracts = this.current.contractId ? [this.current] : [];
          this.payments = data.payments || [];
          this.supplements = data.supplements || [];
          this.terminations = data.terminations || [];
          this.logs = data.logs || [];
          this.archiveStep = data.archiveStep || 0;
          this.loadCustomerDetail(this.current.customerId);
          this.loadCustomerArchiveRows(this.current, data.payments || []);
        })
        .finally(() => {
          this.detailLoading = false;
        });
    },
    closeArchiveDetail() {
      this.detailMode = false;
      this.current = {};
      this.customerDetail = {};
      this.customerContracts = [];
      this.payments = [];
      this.supplements = [];
      this.terminations = [];
      this.logs = [];
      this.activeTab = 'detail';
    },
    loadCustomerDetail(customerId) {
      if (!customerId) return;
      getCustomerDetail(customerId)
        .then(res => {
          this.customerDetail = res.data.data || {};
        })
        .catch(() => {
          this.customerDetail = {};
        });
    },
    loadCustomerArchiveRows(contract = {}, currentPayments = []) {
      if (!contract.customerId && !contract.customerName) return;
      getArchiveList(1, 200, { customerName: contract.customerName || '' })
        .then(res => {
          const records = ((res.data.data || {}).records || []).filter(item =>
            this.isSameCustomerArchive(item, contract)
          );
          const contracts = records.length ? records : [contract];
          this.customerContracts = contracts;
          return Promise.all(
            contracts.map(item => {
              if (String(item.contractId) === String(contract.contractId)) {
                return Promise.resolve({
                  contract: item,
                  payments: currentPayments,
                });
              }
              return getArchiveDetail(item.contractId)
                .then(detailRes => ({
                  contract: item,
                  payments: (detailRes.data.data || {}).payments || [],
                }))
                .catch(() => ({
                  contract: item,
                  payments: [],
                }));
            })
          );
        })
        .then(groups => {
          if (!Array.isArray(groups)) return;
          this.payments = groups.flatMap(group =>
            (group.payments || []).map(payment => ({
              ...payment,
              contractNo: payment.contractNo || group.contract.contractNo,
              contractName: payment.contractName || group.contract.contractName,
              customerName: payment.customerName || group.contract.customerName,
            }))
          );
        })
        .catch(() => {});
    },
    isSameCustomerArchive(row = {}, contract = {}) {
      if (row.customerId && contract.customerId) {
        return String(row.customerId) === String(contract.customerId);
      }
      return Boolean(row.customerName && row.customerName === contract.customerName);
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
        agreementName: contractNo ? `${contractNo}附件` : '合同附件',
        changeItem: '合同档案附件',
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
        changeItem: '合同档案附件',
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
          changeItem: this.supplementForm.changeItem || '合同档案附件',
        })
          .then(() => {
            this.$message.success('附件已上传');
            this.supplementVisible = false;
            this.loadSupplements(this.current.contractId);
          })
          .finally(() => {
            this.supplementLoading = false;
          });
      });
    },
    removeSupplement(row) {
      this.$confirm('确认删除该附件吗？', '提示', {
        type: 'warning',
      })
        .then(() => removeSupplementAgreement(row.agreementId))
        .then(() => {
          this.$message.success('附件已删除');
          this.loadSupplements(this.current.contractId);
        })
        .catch(() => {});
    },
    downloadSupplement(row) {
      if (!row.fileUrl) {
        this.$message.warning('暂无附件文件');
        return;
      }
      downloadNoticeFile(row.fileUrl, row.fileName || row.agreementName || '附件').catch(() => {
        this.$message.error('附件下载失败，请稍后重试');
      });
    },
    previewAttachment(row, title = '附件预览') {
      if (!row || !row.fileUrl) {
        this.$message.warning('暂无附件文件');
        return;
      }
      openAttachmentPreview(this.noticePreview, row, title);
    },
    getFileType(value) {
      const match = String(value || '').match(/\.([a-z0-9]+)(?:\?|#|$)/i);
      return match ? match[1].toLowerCase() : '';
    },
    handleExportApproval(row) {
      if (!row || !row.contractId) return;
      this.openContractApprovalPreview(row);
    },
    openContractApprovalPreview(row) {
      const contractId = row && row.contractId;
      if (!contractId) return;
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType: 'contract-approval',
          contractId,
        },
        contractApprovalPrintUrl(contractId),
        `${row.contractNo || '合同'}会签审批表.docx`,
        '合同会签审批表'
      );
    },
    handlePrint(row) {
      if (!row || !row.contractId) return;
      const rentMode = this.resolveRentMode(row);
      const noticeType = rentMode === 'floating' ? 'contract-floating' : 'contract-fixed';
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          contractId: row.contractId,
        },
        noticePrintUrl(noticeType, { contractId: row.contractId }),
        `${row.contractNo || '合同'}正文.docx`,
        rentMode === 'floating' ? '合同正文浮动租金版' : '合同正文固定租金版'
      );
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
          if (
            this.$route.query.openArchive === '1' &&
            !this.archiveAutoOpened &&
            this.data.length
          ) {
            this.archiveAutoOpened = true;
            this.openArchive(this.data[0]);
          }
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
    contractTypeText(row = {}) {
      return this.resolveRentMode(row) === 'floating' ? '浮动租金合同' : '普通合同';
    },
    billStatusText(value) {
      return this.dicText(paymentStatusDic, value);
    },
    billStatusType(value) {
      return String(value) === '1' ? 'success' : 'warning';
    },
    detailValue(value) {
      if (value === null || value === undefined || value === '') {
        return '--';
      }
      return value;
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
    formatRentUnitPrice(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      return `${Number(value).toLocaleString('zh-CN', {
        maximumFractionDigits: 2,
      })}元/㎡·天`;
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

.archive-crud :deep(.el-table__header th),
.archive-crud :deep(.el-table__cell),
.archive-detail-page :deep(.el-table__header th),
.archive-detail-page :deep(.el-table__cell),
.archive-detail-drawer :deep(.el-table__header th),
.archive-detail-drawer :deep(.el-table__cell) {
  text-align: center;
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

.archive-detail-page {
  min-height: calc(100vh - 180px);
  padding: 0;
  overflow: hidden;
  border-radius: 10px;
  background: #f4f4f6;
}

.archive-detail-header {
  position: relative;
  padding: 22px 24px 24px;
  border: 1px solid #e6eaf2;
  border-radius: 10px 10px 0 0;
  background: #fff;
  box-shadow: 0 2px 10px rgba(16, 89, 198, 0.04);
}

.archive-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
}

.archive-title-row h2 {
  max-width: 520px;
  margin: 0;
  overflow: hidden;
  color: #303133;
  font-size: 22px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.archive-back {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #606266;
}

.archive-header-actions {
  position: absolute;
  top: 22px;
  right: 24px;
  display: flex;
  gap: 12px;
}

.archive-company-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px 80px;
  margin-top: 18px;
  color: #303133;
  font-size: 14px;
}

.archive-company-grid > div {
  min-width: 0;
}

.archive-company-grid span {
  color: #303133;
}

.archive-company-grid strong {
  color: #303133;
  font-weight: 400;
}

.archive-tag {
  margin-right: 6px;
}

.archive-detail-tabs {
  margin-top: 0;
  background: transparent;
}

.archive-detail-tabs :deep(.el-tabs__header) {
  padding: 0 24px;
  margin: 0;
  border-right: 1px solid #e6eaf2;
  border-left: 1px solid #e6eaf2;
  background: #fff;
}

.archive-detail-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #edf0f5;
}

.archive-detail-tabs :deep(.el-tabs__item) {
  height: 54px;
  padding: 0 18px;
  color: #303133;
  font-size: 15px;
  font-weight: 500;
}

.archive-detail-tabs :deep(.el-tabs__item.is-active) {
  color: #2f80ff;
}

.archive-detail-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background-color: #2f80ff;
}

.archive-detail-tabs :deep(.el-tabs__content) {
  padding: 16px 18px 18px;
  background: #f4f4f6;
}

.archive-detail-section {
  padding: 22px 24px 26px;
  margin-bottom: 16px;
  border: 1px solid #e6eaf2;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(16, 89, 198, 0.04);
}

.archive-detail-section:last-child {
  margin-bottom: 0;
}

.archive-table-section {
  padding: 18px;
}

.archive-section-title {
  margin-bottom: 16px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.archive-info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.archive-info-item {
  min-height: 74px;
  padding: 12px 14px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fafbfc;
}

.archive-info-item span,
.archive-info-item strong {
  display: block;
}

.archive-info-item span {
  color: #8c98aa;
  font-size: 12px;
}

.archive-info-item strong {
  margin-top: 8px;
  color: #303133;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  overflow-wrap: anywhere;
}

.archive-flat-table {
  width: 100%;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  overflow: hidden;
}

.archive-flat-table :deep(.el-table__header th) {
  height: 48px;
  background: #fff;
  color: #8c98aa;
  font-weight: 600;
  text-align: center;
}

.archive-flat-table :deep(.el-table__cell) {
  text-align: center;
}

.archive-flat-table :deep(.el-table__row) {
  height: 54px;
}

.archive-flat-table :deep(.el-table__inner-wrapper::before) {
  background-color: #edf0f5;
}

.attachment-table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  white-space: nowrap;
}

.attachment-table-actions :deep(.el-button) {
  min-width: 0;
  padding: 0;
  margin-left: 0;
}

.archive-attachment-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
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

.archive-table-actions :deep(.el-button) {
  min-width: 64px;
  padding: 0 3px;
  margin-left: 0;
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

@media (max-width: 1200px) {
  .archive-company-grid,
  .archive-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .archive-detail-page {
    padding: 18px 14px 28px;
  }

  .archive-header-actions {
    position: static;
    margin-top: 14px;
  }

  .archive-company-grid,
  .archive-info-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
}
</style>
