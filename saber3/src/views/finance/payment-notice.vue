<template>
  <basic-container>
    <div class="notice-center-page">
      <section class="notice-header">
        <div>
          <h2>通知管理</h2>
          <span>统一管理收款、催款和逾期通知</span>
        </div>
        <el-segmented v-model="activeCategory" :options="categoryOptions" @change="categoryChange" />
      </section>

      <section class="notice-summary">
        <div v-for="item in summaryCards" :key="item.key" class="notice-summary__item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="notice-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="租客名称">
            <el-input v-model="query.customerName" clearable placeholder="请输入租客名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="账单编号">
            <el-input v-model="query.paymentNo" clearable placeholder="请输入账单编号" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="楼宇名称">
            <el-select v-model="query.buildingName" clearable filterable placeholder="全部楼宇">
              <el-option v-for="item in buildingOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="notice-table-wrap">
        <el-table v-loading="loading" :data="data" border row-key="paymentId" class="notice-table">
          <el-table-column prop="customerName" label="租客名称" min-width="180" align="center" show-overflow-tooltip />
          <el-table-column prop="contactPhone" label="联系电话" width="130" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.contactPhone || '--' }}</template>
          </el-table-column>
          <el-table-column prop="contactEmail" label="联系邮箱" min-width="190" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.contactEmail || '--' }}</template>
          </el-table-column>
          <el-table-column prop="contractNo" label="合同编号" min-width="160" align="center" show-overflow-tooltip />
          <el-table-column label="房源信息" min-width="170" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ row.roomName || row.buildingName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="paymentNo" label="账单编号" width="130" align="center" />
          <el-table-column prop="feeName" label="费用类型" width="110" align="center" show-overflow-tooltip />
          <el-table-column label="未收金额" width="130" align="center">
            <template #default="{ row }">{{ formatMoney(unpaidAmount(row)) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="120" align="center" />
          <el-table-column v-if="activeCategory !== 'payment'" label="逾期工作日" width="116" align="center">
            <template #default="{ row }">{{ businessDaysOverdue(row) }}个</template>
          </el-table-column>
          <el-table-column v-if="activeCategory !== 'payment'" label="催款记录" width="126" align="center">
            <template #default="{ row }">
              <el-tag :type="Number(row.reminderCount || 0) ? 'success' : 'info'" effect="plain">
                {{ Number(row.reminderCount || 0) }}次
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="通知状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="noticeSendStatusType(row.noticeSendStatus)" effect="plain">{{ noticeSendStatusText(row.noticeSendStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="账单状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="billStatusType(row.billStatus)" effect="plain">{{ billStatusText(row.billStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" :width="activeCategory === 'reminder' ? 156 : 96" align="center" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="openDrawer(row)">处理</el-button>
              <el-button v-if="activeCategory === 'reminder'" text type="warning" @click="handleReminder(row)">{{ Number(row.reminderCount || 0) > 0 ? '再次催款' : '催款' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="notice-pagination">
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

      <el-drawer v-model="drawerVisible" :title="drawerTitle" size="720px" append-to-body>
        <div v-if="drawerRow" class="notice-drawer">
          <section class="drawer-profile">
            <div><span>租客名称</span><strong>{{ drawerRow.customerName || '-' }}</strong></div>
            <div><span>合同编号</span><strong>{{ drawerRow.contractNo || '-' }}</strong></div>
            <div><span>未收金额</span><strong>{{ formatMoney(unpaidAmount(drawerRow)) }}</strong></div>
            <div><span>应缴日期</span><strong>{{ drawerRow.payDeadline || '-' }}</strong></div>
            <div><span>联系电话</span><strong>{{ drawerRow.contactPhone || '--' }}</strong></div>
            <div><span>联系邮箱</span><strong>{{ drawerRow.contactEmail || '--' }}</strong></div>
          </section>

          <section v-if="activeCategory !== 'payment'" class="node-strip">
            <div><span>逾期工作日</span><strong>{{ businessDaysOverdue(drawerRow) }}个</strong></div>
            <div><span>累计催款</span><strong>{{ Number(drawerRow.reminderCount || 0) }}次</strong></div>
            <div><span>最近催款</span><strong>{{ drawerRow.latestReminderTime || '-' }}</strong></div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">通知文书</div>
            <div class="document-list">
              <div v-for="item in drawerDocuments" :key="item.value" class="document-row">
                <div class="document-row__name">
                  <strong>{{ item.label }}</strong>
                  <span>{{ documentTimingText(item) }}</span>
                </div>
                <div class="document-row__actions">
                  <el-button text type="primary" @click="previewNotice(item)">预览</el-button>
                  <el-button text type="primary" @click="generateNotice(item)">下载</el-button>
                </div>
              </div>
            </div>
          </section>

          <section v-if="activeCategory === 'reminder'" class="drawer-section">
            <div class="drawer-section-title">催款操作</div>
            <div class="channel-actions">
              <el-button plain type="warning" @click="handleReminder(drawerRow)">{{ Number(drawerRow.reminderCount || 0) > 0 ? '再次催款' : '催款' }}</el-button>
              <span class="drawer-hint">催款不限制次数，未满 5 个工作日会先提示</span>
            </div>
          </section>

          <section v-if="activeCategory === 'overdue'" class="drawer-section">
            <div class="drawer-section-title">律师函审批</div>
            <div class="legal-approval-action">
              <div>
                <strong>{{ legalStatusText(drawerRow) }}</strong>
                <span>{{ legalApprovalTimingText(drawerRow) }}</span>
              </div>
              <el-button
                type="warning"
                plain
                :disabled="drawerRow.overdueApprovalStatus === 'running'"
                @click="handleLegalApproval(drawerRow)"
              >
                {{ drawerRow.overdueApprovalStatus === 'running' ? '审批中' : '律师函审批' }}
              </el-button>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">发送通道</div>
            <div class="channel-actions">
              <el-button plain type="primary" @click="sendSms">短信发送</el-button>
              <el-button plain type="primary" @click="sendEmail">邮件发送</el-button>
              <el-button plain type="primary" @click="sendPaymentMiniApp">小程序发送</el-button>
            </div>
          </section>

          <section class="drawer-section send-record-section">
            <div class="drawer-section-title-row">
              <div class="drawer-section-title">发送记录</div>
              <el-button text type="primary" :loading="sendRecordLoading" @click="loadSendRecords">刷新</el-button>
            </div>
            <div v-loading="sendRecordLoading" class="send-record-list">
              <el-empty v-if="!sendRecordLoading && !sendRecords.length" description="暂无发送记录" :image-size="72" />
              <div
                v-for="record in sendRecords"
                :key="record.recordId"
                class="send-record-item"
                :class="`send-record-item--${record.sendStatus || 'pending'}`"
              >
                <div class="send-record-item__top">
                  <div class="send-record-item__tags">
                    <el-tag size="small" effect="plain">{{ channelText(record.channel) }}</el-tag>
                    <el-tag size="small" :type="sendStatusType(record.sendStatus)" effect="plain">
                      {{ sendStatusText(record.sendStatus) }}
                    </el-tag>
                  </div>
                  <time>{{ record.sentTime || record.createTime || '--' }}</time>
                </div>
                <div class="send-record-item__title">
                  <strong>{{ record.subject || '无主题' }}</strong>
                  <span>发送人：{{ record.senderName || '--' }}</span>
                </div>
                <div class="send-record-item__mailbox">
                  <div>
                    <span>发件邮箱</span>
                    <strong :title="record.senderEmail || '--'">{{ record.senderEmail || '--' }}</strong>
                  </div>
                  <div>
                    <span>收件邮箱</span>
                    <strong :title="record.recipientEmail || '--'">{{ record.recipientEmail || '--' }}</strong>
                  </div>
                </div>
                <span v-if="record.failureReason" class="send-record-item__error">{{ record.failureReason }}</span>
              </div>
            </div>
          </section>

        </div>
      </el-drawer>

      <el-dialog
        v-model="emailDialogVisible"
        title="邮件发送"
        width="720px"
        append-to-body
        destroy-on-close
        :close-on-click-modal="false"
      >
        <el-form
          ref="emailFormRef"
          v-loading="emailComposeLoading"
          :model="emailForm"
          :rules="emailRules"
          label-width="88px"
          class="email-compose-form"
        >
          <el-form-item label="发件人">
            <el-input :model-value="emailForm.senderEmail || '未绑定QQ邮箱'" disabled />
          </el-form-item>
          <el-form-item label="收件人" prop="recipientEmail">
            <el-input v-model="emailForm.recipientEmail" disabled placeholder="客户尚未维护邮箱" />
          </el-form-item>
          <el-form-item label="邮件主题" prop="subject">
            <el-input v-model="emailForm.subject" maxlength="255" show-word-limit placeholder="请输入邮件主题" />
          </el-form-item>
          <el-form-item label="邮件正文" prop="content">
            <el-input
              v-model="emailForm.content"
              type="textarea"
              :rows="10"
              maxlength="5000"
              show-word-limit
              resize="vertical"
              placeholder="请输入邮件正文"
            />
          </el-form-item>
          <el-form-item label="附件">
            <div class="email-attachment-row">
              <span>{{ emailForm.attachmentName || '通知文书' }}</span>
              <div>
                <el-button text type="primary" @click="previewEmailAttachment">预览</el-button>
                <el-button text type="primary" @click="downloadEmailAttachment">下载</el-button>
              </div>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="emailDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="emailSending"
            :disabled="!emailForm.senderConfigured || emailComposeLoading"
            @click="submitEmail"
          >
            发送邮件
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="miniAppDialogVisible"
        title="发送小程序通知"
        width="680px"
        append-to-body
        destroy-on-close
        :close-on-click-modal="false"
      >
        <el-form v-loading="miniAppComposeLoading" label-width="96px" class="miniapp-compose-form">
          <el-form-item label="接收客户">
            <el-input :model-value="miniAppForm.customerName || '--'" disabled />
          </el-form-item>
          <el-form-item label="客户联系人">
            <el-input :model-value="miniAppForm.contactText || '--'" disabled />
          </el-form-item>
          <el-form-item label="通知标题">
            <el-input :model-value="miniAppForm.noticeTitle" disabled />
          </el-form-item>
          <el-form-item label="发送内容">
            <el-input :model-value="miniAppForm.content" type="textarea" :rows="7" resize="none" disabled />
          </el-form-item>
          <el-form-item label="通知文书">
            <div class="email-attachment-row">
              <span>{{ miniAppForm.fileName || '通知文书' }}</span>
              <div>
                <el-button text type="primary" @click="previewMiniAppAttachment">预览</el-button>
                <el-button text type="primary" @click="downloadMiniAppAttachment">下载</el-button>
              </div>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="miniAppDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="miniAppSending"
            :disabled="miniAppComposeLoading || !miniAppForm.paymentId"
            @click="confirmMiniAppSend"
          >
            确认发送
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
    </div>
  </basic-container>
</template>

<script>
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import { noticePrintUrl } from '@/api/contract/print';
import {
  generatePaymentNotice,
  getPaymentNoticeEmailCompose,
  getPaymentNoticeMiniAppCompose,
  getPaymentNoticeBuildings,
  getPaymentNoticePage,
  getPaymentNoticeSendRecords,
  getPaymentNoticeSummary,
  remindOverduePayment,
  sendPaymentNoticeEmail,
  sendPaymentNoticeMiniApp,
  sendPaymentNoticeSms,
} from '@/api/ics/payment';
import { createNoticePreviewState, downloadNoticeFile, openNoticePreview } from '@/utils/contract-notice';

const DOCUMENTS = {
  payment: [{ label: '收款通知', value: 'payment-notice' }],
  reminder: [{ label: '催款通知书', value: 'reminder-notice', recommendedBusinessDays: 5 }],
  overdue: [{ label: '租金逾期处理通知书', value: 'overdue-notice', recommendedBusinessDays: 5 }],
};

export default {
  name: 'FinancePaymentNotice',
  components: { NoticePreviewDialog },
  data() {
    return {
      activeCategory: 'payment',
      categoryOptions: [
        { label: '收款', value: 'payment' },
        { label: '催款', value: 'reminder' },
        { label: '逾期', value: 'overdue' },
      ],
      query: {},
      summary: {},
      buildingOptions: [],
      loading: false,
      data: [],
      page: { currentPage: 1, pageSize: 10, total: 0 },
      drawerVisible: false,
      drawerRow: null,
      noticePreview: createNoticePreviewState(),
      previewDocument: null,
      emailDialogVisible: false,
      emailComposeLoading: false,
      emailSending: false,
      miniAppDialogVisible: false,
      miniAppComposeLoading: false,
      miniAppSending: false,
      sendRecordLoading: false,
      sendRecords: [],
      emailForm: {
        paymentId: null,
        noticeType: 'payment-notice',
        senderEmail: '',
        senderConfigured: false,
        recipientEmail: '',
        subject: '',
        content: '',
        attachmentName: '',
        attachmentUrl: '',
      },
      emailRules: {
        recipientEmail: [
          { required: true, message: '请输入客户邮箱', trigger: 'blur' },
          { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] },
        ],
        subject: [{ required: true, message: '请输入邮件主题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入邮件正文', trigger: 'blur' }],
      },
      miniAppForm: {
        paymentId: null,
        noticeType: 'payment-notice',
        customerName: '',
        contactText: '',
        noticeTitle: '',
        content: '',
        fileName: '',
        fileUrl: '',
      },
    };
  },
  computed: {
    summaryCards() {
      if (this.activeCategory === 'payment') {
        return [
          { key: 'total', label: '收款账单', value: this.page.total || 0 },
          { key: 'generated', label: '已生成文书', value: this.summary.generatedCount || 0 },
          { key: 'send', label: '累计发送', value: this.summary.sendCount || 0 },
        ];
      }
      if (this.activeCategory === 'reminder') {
        return [
          { key: 'total', label: '催款账单', value: this.page.total || 0 },
          { key: 'five', label: '达到5个工作日', value: this.summary.fiveBusinessDayCount || 0 },
          { key: 'reminded', label: '已催款账单', value: this.summary.remindedBillCount || 0 },
          { key: 'count', label: '累计催款次数', value: this.summary.reminderCount || 0 },
        ];
      }
      return [
        { key: 'total', label: '逾期账单', value: this.page.total || 0 },
        { key: 'five', label: '达到5个工作日', value: this.summary.fiveBusinessDayCount || 0 },
        { key: 'twenty', label: '达到20个工作日', value: this.summary.twentyBusinessDayCount || 0 },
        { key: 'approval', label: '律师函审批通过', value: this.summary.approvedLegalCount || 0 },
      ];
    },
    drawerTitle() {
      const label = (this.categoryOptions.find(item => item.value === this.activeCategory) || {}).label || '通知';
      return `${label}处理`;
    },
    drawerDocuments() {
      return DOCUMENTS[this.activeCategory] || [];
    },
  },
  mounted() {
    const routeCategory = this.$route.query && this.$route.query.category;
    if (['payment', 'reminder', 'overdue'].includes(routeCategory)) this.activeCategory = routeCategory;
    if (routeCategory === 'legal') this.activeCategory = 'overdue';
    this.reload();
    this.loadBuildingOptions();
  },
  methods: {
    categoryChange() { this.page.currentPage = 1; this.drawerVisible = false; this.reload(); this.loadBuildingOptions(); },
    reload() { this.loadPage(); this.loadSummary(); },
    loadPage() {
      this.loading = true;
      getPaymentNoticePage(this.page.currentPage, this.page.pageSize, this.buildQueryParams())
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = result.total || 0;
          if (this.drawerVisible && this.drawerRow) {
            const latestRow = this.data.find(item => String(item.paymentId) === String(this.drawerRow.paymentId));
            if (latestRow) this.drawerRow = { ...latestRow };
          }
        })
        .finally(() => { this.loading = false; });
    },
    loadSummary() { getPaymentNoticeSummary(this.buildQueryParams()).then(res => { this.summary = res.data.data || {}; }); },
    loadBuildingOptions() { getPaymentNoticeBuildings(this.buildQueryParams()).then(res => { this.buildingOptions = res.data.data || []; }); },
    buildQueryParams() { return { ...this.query, categoryQuery: this.activeCategory }; },
    searchChange() { this.page.currentPage = 1; this.reload(); },
    searchReset() { this.query = {}; this.page.currentPage = 1; this.reload(); this.loadBuildingOptions(); },
    currentChange(value) { this.page.currentPage = value; this.loadPage(); },
    sizeChange(value) { this.page.pageSize = value; this.page.currentPage = 1; this.loadPage(); },
    openDrawer(row) {
      this.drawerRow = { ...row };
      this.drawerVisible = true;
      this.loadSendRecords();
    },
    previewNotice(item) {
      const row = this.drawerRow;
      if (!row) return;
      this.previewDocument = item;
      openNoticePreview(this, this.noticePreview, { noticeType: item.value, paymentId: row.paymentId, contractId: row.contractId }, noticePrintUrl(item.value, { paymentId: row.paymentId, contractId: row.contractId }), this.fileName(row, item), `${item.label}预览`);
    },
    async generateNotice(item) {
      const row = this.drawerRow;
      if (!row) return;
      const elapsed = this.businessDaysOverdue(row);
      if (item.recommendedBusinessDays && elapsed < item.recommendedBusinessDays) {
        try {
          await this.$confirm(`当前逾期${elapsed}个工作日，尚未达到${item.recommendedBusinessDays}个工作日的建议节点，是否继续生成？`, '提前生成确认', { type: 'warning', confirmButtonText: '继续生成' });
        } catch (error) { return; }
      }
      generatePaymentNotice(row.paymentId, item.value).then(res => {
        const file = res.data.data || {};
        const url = file.fileUrl || noticePrintUrl(item.value, { paymentId: row.paymentId, contractId: row.contractId });
        return downloadNoticeFile(url, this.fileName(row, item)).then(() => this.$message.success(`${item.label}已生成`));
      });
    },
    async handleReminder(row) {
      if (!row || !row.paymentId) return;
      const elapsed = this.businessDaysOverdue(row);
      if (elapsed < 5) {
        try {
          await this.$confirm(`当前逾期${elapsed}个工作日，尚未达到5个工作日的建议催款节点，是否继续催款？`, '提前催款提示', { type: 'warning', confirmButtonText: '继续催款', cancelButtonText: '取消' });
        } catch (error) { return; }
      }
      remindOverduePayment(row.paymentId, 'notice_management').then(() => {
        this.$message.success('催款已发送，可继续重复催款');
        this.reload();
      });
    },
    async handleLegalApproval(row) {
      if (!row || !row.paymentId) return;
      if (row.overdueApprovalStatus === 'running') {
        this.$message.warning('该账单律师函审批正在进行中');
        return;
      }
      const elapsed = this.businessDaysOverdue(row);
      if (elapsed < 20) {
        await this.$alert(`当前逾期${elapsed}个工作日，律师函审批需满20个工作日后才可发起。`, '律师函审批限制', {
          confirmButtonText: '知道了',
          type: 'warning',
        });
        return;
      }
      this.drawerVisible = false;
      this.$router.push({ path: '/finance/overdue-reminder', query: { paymentId: row.paymentId } });
    },
    sendSms() {
      const row = this.drawerRow;
      if (!row) return;
      sendPaymentNoticeSms(row.paymentId, this.activeNoticeType()).then(() => {
        this.$message.warning('短信发送结果已记录');
        this.reload();
        this.loadSendRecords();
      });
    },
    sendEmail() {
      const row = this.drawerRow;
      if (!row) return;
      const noticeType = this.activeNoticeType();
      this.emailForm = {
        paymentId: row.paymentId,
        noticeType,
        senderEmail: '',
        senderConfigured: false,
        recipientEmail: '',
        subject: '',
        content: '',
        attachmentName: '',
        attachmentUrl: '',
      };
      this.emailDialogVisible = true;
      this.emailComposeLoading = true;
      getPaymentNoticeEmailCompose(row.paymentId, noticeType)
        .then(res => {
          const compose = res.data.data || {};
          this.emailForm = {
            paymentId: compose.paymentId || row.paymentId,
            noticeType: compose.noticeType || noticeType,
            senderEmail: compose.senderEmail || '',
            senderConfigured: Boolean(compose.senderConfigured),
            recipientEmail: compose.recipientEmail || '',
            subject: compose.subject || '',
            content: compose.content || '',
            attachmentName: compose.attachmentName || '',
            attachmentUrl: compose.attachmentUrl || '',
          };
          if (!compose.senderConfigured) this.$message.warning('请先到个人中心绑定并启用QQ邮箱');
        })
        .catch(() => { this.emailDialogVisible = false; })
        .finally(() => { this.emailComposeLoading = false; });
    },
    submitEmail() {
      this.$refs.emailFormRef.validate(valid => {
        if (!valid) return;
        this.emailSending = true;
        sendPaymentNoticeEmail({
          paymentId: this.emailForm.paymentId,
          noticeType: this.emailForm.noticeType,
          recipientEmail: this.emailForm.recipientEmail,
          subject: this.emailForm.subject,
          content: this.emailForm.content,
        })
          .then(res => {
            const result = res.data.data || {};
            if (result.emailStatus === 'success') {
              this.$message.success('邮件发送成功');
              this.emailDialogVisible = false;
            } else {
              this.$message.error(result.remark || '邮件发送失败');
            }
            this.reload();
            this.loadSendRecords();
          })
          .finally(() => { this.emailSending = false; });
      });
    },
    loadSendRecords() {
      const row = this.drawerRow;
      if (!row || !row.paymentId) return;
      this.sendRecordLoading = true;
      getPaymentNoticeSendRecords(row.paymentId, this.activeNoticeType())
        .then(res => { this.sendRecords = res.data.data || []; })
        .finally(() => { this.sendRecordLoading = false; });
    },
    previewEmailAttachment() {
      const row = this.drawerRow;
      if (!row) return;
      const item = this.drawerDocuments.find(document => document.value === this.emailForm.noticeType) || this.drawerDocuments[0];
      if (item) this.previewNotice(item);
    },
    downloadEmailAttachment() {
      if (this.emailForm.attachmentUrl) {
        downloadNoticeFile(this.emailForm.attachmentUrl, this.emailForm.attachmentName || '通知文书.docx');
      }
    },
    channelText(value) { return { email: '邮件', sms: '短信', miniapp: '小程序' }[value] || value || '通知'; },
    sendStatusText(value) { return { pending: '发送中', success: '发送成功', failed: '发送失败', reserved: '通道待接入' }[value] || '未知'; },
    sendStatusType(value) { return { pending: 'primary', success: 'success', failed: 'danger', reserved: 'warning' }[value] || 'info'; },
    sendPaymentMiniApp() {
      const row = this.drawerRow;
      if (!row) return;
      this.miniAppDialogVisible = true;
      this.miniAppComposeLoading = true;
      this.miniAppForm = {
        paymentId: null,
        noticeType: this.activeNoticeType(),
        customerName: '',
        contactText: '',
        noticeTitle: '',
        content: '',
        fileName: '',
        fileUrl: '',
      };
      getPaymentNoticeMiniAppCompose(row.paymentId, this.activeNoticeType())
        .then(res => {
          const compose = res.data.data || {};
          const receiver = compose.receiver || {};
          const payload = compose.payload || {};
          this.miniAppForm = {
            paymentId: compose.paymentId || row.paymentId,
            noticeType: compose.noticeType || this.activeNoticeType(),
            customerName: receiver.customerName || payload.customerName || row.customerName || '',
            contactText: [receiver.contactName, receiver.contactPhone, receiver.contactEmail].filter(Boolean).join(' / '),
            noticeTitle: compose.noticeTitle || '',
            content: this.buildMiniAppContent(payload),
            fileName: compose.fileName || (compose.document || {}).fileName || '',
            fileUrl: compose.fileUrl || (compose.document || {}).fileUrl || '',
          };
        })
        .finally(() => { this.miniAppComposeLoading = false; });
    },
    buildMiniAppContent(payload) {
      return [
        `租客名称：${payload.customerName || '-'}`,
        `合同号：${payload.contractNo || '-'}`,
        `费用类型：${payload.feeName || '-'}`,
        `账期：${payload.periodText || '-'}`,
        `应缴日期：${payload.payDeadline || '-'}`,
        `未收金额：¥${payload.unpaidAmount || '0.00'}`,
      ].join('\n');
    },
    confirmMiniAppSend() {
      if (!this.miniAppForm.paymentId) return;
      this.miniAppSending = true;
      sendPaymentNoticeMiniApp(this.miniAppForm.paymentId, this.miniAppForm.noticeType)
        .then(() => {
          this.$message.warning(`${this.activeCategory === 'payment' ? '收款' : this.activeCategory === 'reminder' ? '催款' : '逾期'}小程序发送内容已记录，通道待接入`);
          this.miniAppDialogVisible = false;
          this.reload();
          this.loadSendRecords();
        })
        .finally(() => { this.miniAppSending = false; });
    },
    previewMiniAppAttachment() {
      const item = this.drawerDocuments.find(document => document.value === this.miniAppForm.noticeType) || this.drawerDocuments[0];
      if (item) this.previewNotice(item);
    },
    downloadMiniAppAttachment() {
      if (this.miniAppForm.fileUrl) {
        downloadNoticeFile(this.miniAppForm.fileUrl, this.miniAppForm.fileName || '通知文书.docx');
      }
    },
    activeNoticeType() { return { payment: 'payment-notice', reminder: 'reminder-notice', overdue: 'overdue-notice' }[this.activeCategory] || 'payment-notice'; },
    noticeSendStatusText(value) { return { pending: '未发送', sent: '已发送', failed: '发送失败' }[value] || '未发送'; },
    noticeSendStatusType(value) { return { pending: 'info', sent: 'success', failed: 'danger' }[value] || 'info'; },
    billStatusText(value) { return { pending: '待处理', paid: '已交款', reminded: '已催款', overdue: '已逾期', legal: '已律师函' }[value] || '待处理'; },
    billStatusType(value) { return { pending: 'info', paid: 'success', reminded: 'warning', overdue: 'danger', legal: 'danger' }[value] || 'info'; },
    documentTimingText(item) {
      if (this.activeCategory === 'payment') return '账单生成后可发送';
      const elapsed = this.businessDaysOverdue(this.drawerRow);
      return elapsed >= item.recommendedBusinessDays ? `已达到${item.recommendedBusinessDays}个工作日建议节点` : `建议满${item.recommendedBusinessDays}个工作日，当前${elapsed}个`;
    },
    legalStatusText(row) {
      if (Number(row.legalSendCount || 0) > 0) return `律师函已发送${row.legalSendCount}次`;
      const map = { running: '律师函审批中', approved: '律师函审批通过', rejected: '律师函审批驳回', canceled: '律师函审批取消' };
      return map[row.overdueApprovalStatus] || '律师函未发起';
    },
    legalStatusType(row) { if (Number(row.legalSendCount || 0) > 0) return 'success'; return { running: 'warning', approved: 'primary', rejected: 'danger' }[row.overdueApprovalStatus] || 'info'; },
    legalApprovalTimingText(row) {
      const elapsed = this.businessDaysOverdue(row);
      return elapsed >= 20 ? '已满20个工作日，可进入逾期处理发起审批' : `需满20个工作日，当前${elapsed}个工作日`;
    },
    businessDaysOverdue(row) {
      if (!row || !row.payDeadline) return 0;
      const parts = String(row.payDeadline).slice(0, 10).split('-').map(Number);
      if (parts.length !== 3 || parts.some(Number.isNaN)) return 0;
      const deadline = new Date(parts[0], parts[1] - 1, parts[2]);
      const today = new Date(); today.setHours(0, 0, 0, 0);
      let count = 0; const cursor = new Date(deadline); cursor.setDate(cursor.getDate() + 1);
      while (cursor <= today) { if (![0, 6].includes(cursor.getDay())) count += 1; cursor.setDate(cursor.getDate() + 1); }
      return count;
    },
    unpaidAmount(row) { return Math.max(Number(row.amountDue || 0) - Number(row.amountPaid || 0), 0); },
    formatMoney(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`; },
    fileName(row, item) { return `${row.paymentNo || `ZD${row.paymentId}`}-${item.label}.docx`; },
    downloadNoticePreviewFile() { if (this.noticePreview.downloadUrl) downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName); },
  },
};
</script>

<style lang="scss" scoped>
.notice-center-page { display: flex; flex-direction: column; gap: 16px; min-width: 0; }
.notice-header, .notice-summary, .notice-search, .notice-table-wrap { border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; }
.notice-header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px; }
.notice-header h2 { margin: 0 0 4px; color: #1f2937; font-size: 20px; }
.notice-header span { color: #909399; font-size: 13px; }
.notice-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); padding: 16px; }
.notice-summary__item { min-height: 58px; display: flex; flex-direction: column; align-items: center; justify-content: center; border-right: 1px solid #ebeef5; }
.notice-summary__item:last-child { border-right: 0; }
.notice-summary__item span { color: #909399; font-size: 13px; }
.notice-summary__item strong { margin-top: 6px; color: #1f2937; font-size: 23px; }
.notice-search { padding: 16px 18px 0; }
.notice-table-wrap { padding: 16px; }
.notice-pagination { display: flex; justify-content: flex-end; padding-top: 16px; }
.notice-drawer { display: flex; flex-direction: column; gap: 16px; }
.drawer-profile, .node-strip { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.node-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.drawer-profile > div, .node-strip > div { min-width: 0; padding: 12px; border: 1px solid #ebeef5; border-radius: 8px; background: #fafafa; }
.drawer-profile span, .drawer-profile strong, .node-strip span, .node-strip strong { display: block; }
.drawer-profile span, .node-strip span { color: #909399; font-size: 12px; }
.drawer-profile strong, .node-strip strong { margin-top: 5px; overflow: hidden; color: #1f2937; text-overflow: ellipsis; white-space: nowrap; }
.drawer-section { padding-top: 4px; }
.drawer-section-title, .drawer-section-title-row { margin-bottom: 12px; color: #1f2937; font-size: 15px; font-weight: 600; }
.drawer-section-title-row { display: flex; align-items: center; justify-content: space-between; }
.drawer-section-title-row .drawer-section-title { margin-bottom: 0; }
.document-list, .send-record-list { display: flex; flex-direction: column; gap: 10px; }
.document-row, .send-record-list > div { min-height: 58px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 10px 12px; border: 1px solid #ebeef5; border-radius: 8px; background: #fafafa; }
.document-row__name { min-width: 0; }
.document-row__name strong, .document-row__name span { display: block; }
.document-row__name strong { color: #1f2937; font-size: 14px; line-height: 20px; }
.document-row__name span { margin-top: 3px; color: #909399; font-size: 12px; }
.document-row__actions, .channel-actions { display: inline-flex; align-items: center; gap: 8px; white-space: nowrap; }
.document-row__actions :deep(.el-button) { margin-left: 0; }
.legal-approval-action { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 13px 14px; border: 1px solid #ebeef5; border-radius: 8px; }
.legal-approval-action strong, .legal-approval-action span { display: block; }
.legal-approval-action span { margin-top: 4px; color: #909399; font-size: 12px; }
.drawer-hint { color: #909399; font-size: 12px; }
.send-record-list { margin-top: 8px; }
.send-record-list > div { align-items: stretch; flex-direction: column; }
.send-record-section { min-height: 260px; }
.send-record-item { position: relative; width: 100%; box-sizing: border-box; overflow: hidden; padding: 14px 16px !important; border-color: #e5e7eb !important; background: #fff !important; }
.send-record-item::before { position: absolute; top: 0; bottom: 0; left: 0; width: 3px; background: #409eff; content: ''; }
.send-record-item--success::before { background: #67c23a; }
.send-record-item--failed::before { background: #f56c6c; }
.send-record-item--reserved::before { background: #e6a23c; }
.send-record-item__top { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.send-record-item__tags { display: inline-flex; align-items: center; gap: 8px; }
.send-record-item__top time { color: #8a93a3; font-size: 12px; white-space: nowrap; }
.send-record-item__title { display: flex; min-width: 0; align-items: baseline; gap: 10px; }
.send-record-item__title strong { min-width: 0; overflow: hidden; color: #1f2937; font-size: 14px; line-height: 22px; text-overflow: ellipsis; white-space: nowrap; }
.send-record-item__title span { flex: none; color: #8a93a3; font-size: 12px; }
.send-record-item__mailbox { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); overflow: hidden; border: 1px solid #edf0f5; border-radius: 6px; background: #fafbfc; }
.send-record-item__mailbox > div { min-width: 0; padding: 9px 12px; }
.send-record-item__mailbox > div + div { border-left: 1px solid #edf0f5; }
.send-record-item__mailbox span { display: block; margin-bottom: 3px; color: #8a93a3; font-size: 12px; }
.send-record-item__mailbox strong { display: block; overflow: hidden; color: #4b5563; font-size: 13px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }
.send-record-item__error { padding: 8px 10px; border-radius: 6px; background: #fef0f0; color: #f56c6c; font-size: 12px; line-height: 18px; }
.email-compose-form { padding-right: 12px; }
.miniapp-compose-form { padding-right: 12px; }
.email-attachment-row { width: 100%; min-height: 44px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 0 12px; border: 1px solid #ebeef5; border-radius: 8px; background: #fafafa; }
.email-attachment-row > span { min-width: 0; overflow: hidden; color: #303133; text-overflow: ellipsis; white-space: nowrap; }
.email-attachment-row > div { display: inline-flex; align-items: center; white-space: nowrap; }
.email-attachment-row :deep(.el-button) { margin-left: 8px; }
@media (max-width: 900px) { .notice-header { align-items: flex-start; flex-direction: column; gap: 14px; } .notice-summary { grid-template-columns: repeat(2, 1fr); } .drawer-profile, .node-strip { grid-template-columns: 1fr; } .send-record-item__title { align-items: flex-start; flex-direction: column; gap: 2px; } .send-record-item__mailbox { grid-template-columns: 1fr; } .send-record-item__mailbox > div + div { border-top: 1px solid #edf0f5; border-left: 0; } }
</style>
