<template>
  <div class="ai-chat-assistant">
    <ai-chat-bubble v-if="!visible" @open="open" />

    <transition name="ai-chat-panel">
      <section
        v-if="visible"
        class="ai-chat-panel"
        :class="{ 'is-minimized': minimized, 'is-maximized': maximized }"
        aria-label="园区 AI 助手"
      >
        <aside class="ai-chat-sidebar">
          <div class="ai-chat-brand">
            <span class="ai-chat-brand__icon"><el-icon><MagicStick /></el-icon></span>
            <div>
              <strong>园区 AI 助手</strong>
              <small>房源与企业数据问答</small>
            </div>
          </div>

          <div class="ai-mode-switch" role="tablist" aria-label="AI 助手切换">
            <button
              type="button"
              :class="{ 'is-active': mode === 'property' }"
              :aria-selected="mode === 'property'"
              role="tab"
              @click="switchMode('property')"
            >
              <el-icon><OfficeBuilding /></el-icon><span>房源问答</span>
            </button>
            <button
              type="button"
              :class="{ 'is-active': mode === 'enterprise' }"
              :aria-selected="mode === 'enterprise'"
              role="tab"
              @click="switchMode('enterprise')"
            >
              <el-icon><Briefcase /></el-icon><span>企业助手</span>
            </button>
          </div>

          <el-button class="ai-chat-new" :icon="Plus" plain @click="createConversation">新建对话</el-button>
          <el-scrollbar class="ai-chat-conversation-scroll">
            <div
              v-for="conversation in conversations"
              :key="conversation.id"
              class="ai-chat-conversation"
              role="button"
              tabindex="0"
              :class="{ 'is-active': String(conversation.id) === String(activeConversationId) }"
              @click="selectConversation(conversation.id)"
              @keydown.enter="selectConversation(conversation.id)"
            >
              <el-icon><ChatLineRound /></el-icon>
              <span>
                <b>{{ conversation.title }}</b>
                <em>{{ formatTime(conversation.lastMessageTime || conversation.createTime) }}</em>
              </span>
              <el-tooltip content="删除会话" placement="right">
                <button class="ai-chat-conversation__delete" type="button" aria-label="删除会话" @click.stop="requestDelete(conversation)">
                  <el-icon><Delete /></el-icon>
                </button>
              </el-tooltip>
            </div>
            <el-empty v-if="!conversations.length" description="暂无历史对话" :image-size="64" />
          </el-scrollbar>
        </aside>

        <main class="ai-chat-main">
          <header class="ai-chat-header">
            <div>
              <h3>{{ modeTitle }}</h3>
              <span><i></i>{{ modeSubtitle }}</span>
            </div>
            <div class="ai-chat-window-actions" role="toolbar" aria-label="窗口操作">
              <button class="ai-chat-window-button" type="button" title="最小化" aria-label="最小化园区 AI 助手" @click="toggleMinimized"><el-icon><Minus /></el-icon></button>
              <button class="ai-chat-window-button" type="button" :title="maximized ? '还原窗口' : '放大窗口'" :aria-label="maximized ? '还原园区 AI 助手窗口' : '放大园区 AI 助手窗口'" @click="toggleMaximized">
                <el-icon><CopyDocument v-if="maximized" /><FullScreen v-else /></el-icon>
              </button>
              <button class="ai-chat-window-button ai-chat-window-button--close" type="button" title="关闭" aria-label="关闭园区 AI 助手" @click="close"><el-icon><Close /></el-icon></button>
            </div>
          </header>

          <el-scrollbar ref="messageScroller" class="ai-chat-messages ai-mode-content">
            <div v-if="!messages.length" class="ai-chat-welcome">
              <div class="ai-chat-welcome__icon">
                <el-icon><OfficeBuilding v-if="mode === 'property'" /><Briefcase v-else /></el-icon>
              </div>
              <h4>{{ mode === 'property' ? '你好，我是房源助手' : '你好，我是企业助手' }}</h4>
              <p>{{ modeWelcome }}</p>
              <div class="ai-chat-suggestions">
                <button v-for="item in currentSuggestions" :key="item" type="button" @click="askSuggestion(item)">{{ item }}</button>
              </div>
            </div>

            <div v-for="message in messages" :key="message.id" class="ai-chat-message" :class="`is-${message.role}`">
              <span class="ai-chat-avatar">
                <el-icon v-if="message.role === 'assistant'"><MagicStick /></el-icon>
                <el-icon v-else><UserFilled /></el-icon>
              </span>
              <div class="ai-chat-bubble">
                <p>{{ message.content }}</p>
                <article v-if="message.role === 'assistant' && hasReport(message.reportId)" class="ai-report-attachment">
                  <span class="ai-report-attachment__icon"><el-icon><Document /></el-icon></span>
                  <div>
                    <strong>企业综合分析报告</strong>
                    <small>HTML 报告</small>
                  </div>
                  <div class="ai-report-attachment__actions">
                    <el-tooltip content="查看报告">
                      <button type="button" aria-label="查看企业报告" @click="viewReport(message.reportId)"><el-icon><View /></el-icon></button>
                    </el-tooltip>
                    <el-tooltip content="下载 HTML">
                      <button type="button" aria-label="下载企业报告 HTML" @click="downloadReport(message.reportId)"><el-icon><Download /></el-icon></button>
                    </el-tooltip>
                  </div>
                </article>
              </div>
            </div>

            <div v-if="sending && !streamingStarted" class="ai-chat-message is-assistant is-pending">
              <span class="ai-chat-avatar"><el-icon><MagicStick /></el-icon></span>
              <div class="ai-chat-bubble"><span></span><span></span><span></span></div>
            </div>
          </el-scrollbar>

          <footer class="ai-chat-input ai-mode-content">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="3"
              resize="none"
              maxlength="500"
              show-word-limit
              :placeholder="inputPlaceholder"
              @keydown.enter.exact.prevent="send"
            />
            <div class="ai-chat-input__footer">
              <span>{{ mode === 'property' ? '房源数据实时查询' : '客户管理数据实时查询' }}</span>
              <el-button type="primary" :icon="Promotion" :loading="sending" :disabled="!draft.trim()" @click="send">发送</el-button>
            </div>
          </footer>
        </main>

        <div v-if="reportViewerOpen" class="ai-report-viewer-overlay ai-mode-content" role="presentation">
          <section class="ai-report-viewer" role="dialog" aria-modal="true" aria-labelledby="ai-report-title">
            <header>
              <div>
                <strong id="ai-report-title">{{ activeReport ? activeReport.title : '企业综合分析报告' }}</strong>
                <span v-if="activeReport">{{ formatTime(activeReport.generatedTime || activeReport.createTime) }}</span>
              </div>
              <div>
                <el-tooltip content="打印报告"><button type="button" aria-label="打印报告" :disabled="!activeReport" @click="printReport"><el-icon><Printer /></el-icon></button></el-tooltip>
                <el-tooltip content="下载 HTML"><button type="button" aria-label="下载报告 HTML" :disabled="!activeReport" @click="downloadActiveReport"><el-icon><Download /></el-icon></button></el-tooltip>
                <el-tooltip content="关闭报告"><button type="button" aria-label="关闭报告" @click="closeReport"><el-icon><Close /></el-icon></button></el-tooltip>
              </div>
            </header>
            <div v-if="reportLoading" class="ai-report-viewer__loading"><el-icon class="is-loading"><Loading /></el-icon><span>报告加载中</span></div>
            <iframe
              v-else-if="activeReport"
              ref="reportFrame"
              class="ai-report-frame"
              :srcdoc="activeReport.htmlContent"
              sandbox="allow-same-origin allow-modals"
              :title="activeReport.title"
            />
          </section>
        </div>

        <div v-if="pendingDelete" class="ai-chat-delete-overlay" role="presentation" @click.self="cancelDelete">
          <section class="ai-chat-delete-dialog" role="alertdialog" aria-modal="true" aria-labelledby="ai-delete-title">
            <header><h4 id="ai-delete-title">删除会话</h4><button type="button" aria-label="关闭删除确认框" @click="cancelDelete"><el-icon><Close /></el-icon></button></header>
            <div class="ai-chat-delete-dialog__body"><span class="ai-chat-delete-dialog__icon"><el-icon><WarningFilled /></el-icon></span><p>确定删除“{{ pendingDelete.title }}”吗？删除后对话内容不可恢复。</p></div>
            <footer>
              <button type="button" class="ai-chat-delete-dialog__cancel" @click="cancelDelete">取消</button>
              <button type="button" class="ai-chat-delete-dialog__confirm" :disabled="deleting" @click="confirmDelete">{{ deleting ? '删除中...' : '删除' }}</button>
            </footer>
          </section>
        </div>
      </section>
    </transition>
  </div>
</template>

<script>
import { nextTick } from 'vue';
import {
  Briefcase,
  ChatLineRound,
  Close,
  CopyDocument,
  Delete,
  Document,
  Download,
  FullScreen,
  Loading,
  MagicStick,
  Minus,
  OfficeBuilding,
  Plus,
  Printer,
  Promotion,
  UserFilled,
  View,
  WarningFilled,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { deleteConversation, listConversations, listMessages, streamMessage } from '@/api/ai/chat';
import { getEnterpriseReport } from '@/api/ai/report';
import AiChatBubble from './bubble.vue';

export default {
  name: 'AiChatAssistant',
  components: {
    AiChatBubble,
    Briefcase,
    ChatLineRound,
    Close,
    CopyDocument,
    Delete,
    Document,
    Download,
    FullScreen,
    Loading,
    MagicStick,
    Minus,
    OfficeBuilding,
    Plus,
    Printer,
    Promotion,
    UserFilled,
    View,
    WarningFilled,
  },
  data() {
    return {
      visible: false,
      minimized: false,
      maximized: false,
      mode: 'property',
      pendingDelete: null,
      deleting: false,
      sending: false,
      streamingStarted: false,
      draft: '',
      activeConversationId: null,
      conversations: [],
      messages: [],
      suggestions: {
        property: ['我现在租了哪些房源？', '我的出租率是多少？', '我的空置房间有多少？'],
        enterprise: ['查询 XX 科技有限公司的经营状态', 'XX 科技有限公司有哪些风险？', '为 XX 科技有限公司生成企业分析报告'],
      },
      reportViewerOpen: false,
      reportLoading: false,
      activeReport: null,
      reportCache: {},
    };
  },
  computed: {
    modeTitle() {
      return this.mode === 'property' ? '房源助手' : '企业助手';
    },
    modeSubtitle() {
      return this.mode === 'property' ? '实时连接园区房源数据' : '实时连接客户管理数据';
    },
    modeWelcome() {
      return this.mode === 'property' ? '查询出租率、空置房间和当前在租房源。' : '查询企业基本信息与风险，或在对话中生成报告。';
    },
    currentSuggestions() {
      return this.suggestions[this.mode];
    },
    inputPlaceholder() {
      return this.mode === 'property' ? '请输入房源相关问题' : '请输入企业名称和需要查询或分析的内容';
    },
  },
  methods: {
    async open() {
      this.visible = true;
      this.minimized = false;
      await this.loadConversations();
    },
    close() {
      this.visible = false;
      this.minimized = false;
      this.maximized = false;
      this.closeReport();
    },
    toggleMinimized() {
      this.minimized = !this.minimized;
      if (this.minimized) this.closeReport();
    },
    toggleMaximized() {
      this.maximized = !this.maximized;
      this.minimized = false;
    },
    async switchMode(mode) {
      if (this.mode === mode) return;
      this.mode = mode;
      this.minimized = false;
      this.closeReport();
      this.createConversation();
      await this.loadConversations();
    },
    async loadConversations() {
      try {
        const { data } = await listConversations(this.mode);
        this.conversations = data.data || [];
      } catch (error) {
        this.conversations = [];
        ElMessage.error(error.message || '加载历史对话失败');
      }
    },
    async selectConversation(conversationId) {
      this.closeReport();
      this.activeConversationId = conversationId;
      try {
        const { data } = await listMessages(conversationId);
        this.messages = data.data || [];
        this.scrollToBottom();
      } catch (error) {
        ElMessage.error(error.message || '加载对话失败');
      }
    },
    createConversation() {
      this.activeConversationId = null;
      this.messages = [];
      this.draft = '';
      this.closeReport();
    },
    askSuggestion(question) {
      this.draft = question;
      this.send();
    },
    async send() {
      const content = this.draft.trim();
      if (!content || this.sending) return;
      this.sending = true;
      this.streamingStarted = false;
      this.draft = '';
      const localMessage = { id: `local-${Date.now()}`, role: 'user', domain: this.mode, content };
      this.messages.push(localMessage);
      this.scrollToBottom();
      try {
        let assistantMessage = null;
        await streamMessage({ domain: this.mode, conversationId: this.activeConversationId, content }, (event, payload) => {
          if (event === 'meta') {
            this.activeConversationId = payload.conversationId;
            if (payload.userMessage) Object.assign(localMessage, payload.userMessage);
          } else if (event === 'delta') {
            this.streamingStarted = true;
            if (!assistantMessage) {
              assistantMessage = { id: `stream-${Date.now()}`, role: 'assistant', domain: this.mode, content: '' };
              this.messages.push(assistantMessage);
            }
            assistantMessage.content += payload.content || '';
            this.scrollToBottom();
          } else if (event === 'done' && payload.assistantMessage) {
            if (assistantMessage) Object.assign(assistantMessage, payload.assistantMessage);
            else this.messages.push(payload.assistantMessage);
          } else if (event === 'error') {
            throw new Error(payload.message || 'AI 问答失败');
          }
        });
        await this.loadConversations();
      } catch (error) {
        ElMessage.error(error.message || 'AI 问答失败');
      } finally {
        this.sending = false;
        this.streamingStarted = false;
        this.scrollToBottom();
      }
    },
    async fetchReport(reportId) {
      const key = String(reportId);
      if (this.reportCache[key]) return this.reportCache[key];
      const { data } = await getEnterpriseReport(reportId);
      const report = data.data;
      if (!report) throw new Error('报告不存在或已删除');
      this.reportCache[key] = report;
      return report;
    },
    hasReport(reportId) {
      return Number.isFinite(Number(reportId)) && Number(reportId) > 0;
    },
    async viewReport(reportId) {
      this.reportViewerOpen = true;
      this.reportLoading = true;
      this.activeReport = null;
      try {
        this.activeReport = await this.fetchReport(reportId);
      } catch (error) {
        this.reportViewerOpen = false;
        ElMessage.error(error.message || '加载报告失败');
      } finally {
        this.reportLoading = false;
      }
    },
    closeReport() {
      this.reportViewerOpen = false;
      this.reportLoading = false;
      this.activeReport = null;
    },
    printReport() {
      const frame = this.$refs.reportFrame;
      if (frame && frame.contentWindow) frame.contentWindow.print();
    },
    async downloadReport(reportId) {
      try {
        this.triggerReportDownload(await this.fetchReport(reportId));
      } catch (error) {
        ElMessage.error(error.message || '下载报告失败');
      }
    },
    downloadActiveReport() {
      if (this.activeReport) this.triggerReportDownload(this.activeReport);
    },
    triggerReportDownload(report) {
      const blob = new Blob([report.htmlContent], { type: 'text/html;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${(report.enterpriseName || '企业').replace(/[\\/:*?"<>|]/g, '_')}_综合信息报告.html`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    },
    requestDelete(conversation) {
      this.pendingDelete = conversation;
    },
    cancelDelete() {
      if (!this.deleting) this.pendingDelete = null;
    },
    async confirmDelete() {
      if (!this.pendingDelete || this.deleting) return;
      this.deleting = true;
      const conversation = this.pendingDelete;
      try {
        await deleteConversation(conversation.id);
        if (String(this.activeConversationId) === String(conversation.id)) this.createConversation();
        await this.loadConversations();
      } catch (error) {
        ElMessage.error(error.message || '删除会话失败');
      } finally {
        this.deleting = false;
        this.pendingDelete = null;
      }
    },
    scrollToBottom() {
      nextTick(() => {
        const scrollbar = this.$refs.messageScroller;
        if (scrollbar && scrollbar.wrapRef) scrollbar.wrapRef.scrollTop = scrollbar.wrapRef.scrollHeight;
      });
    },
    formatTime(value) {
      if (!value) return '';
      const date = new Date(String(value).replace(/-/g, '/'));
      if (Number.isNaN(date.getTime())) return '';
      return `${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    },
  },
};
</script>

<style lang="scss" scoped>
.ai-chat-assistant { position: static; }
.ai-chat-panel { position: fixed; z-index: 3000; isolation: isolate; right: 28px; bottom: 28px; width: min(1040px, calc(100vw - 48px)); height: min(720px, calc(100vh - 110px)); min-height: 500px; display: flex; overflow: hidden; background: #fff; border: 1px solid #dfe4ed; border-radius: 10px; box-shadow: 0 18px 44px rgba(20, 44, 82, .22); transition: width .2s ease, height .2s ease, right .2s ease, bottom .2s ease; }
.ai-chat-panel.is-maximized { right: 16px; bottom: 16px; width: calc(100vw - 32px); height: calc(100vh - 92px); max-height: none; }
.ai-chat-panel.is-minimized { width: min(620px, calc(100vw - 48px)); height: 64px; min-height: 64px; }
.ai-chat-panel.is-minimized .ai-chat-sidebar,.ai-chat-panel.is-minimized .ai-mode-content { display: none; }
.ai-chat-panel.is-minimized .ai-chat-main { width: 100%; }
.ai-chat-sidebar { width: 260px; flex: 0 0 260px; display: flex; flex-direction: column; padding: 18px 12px; border-right: 1px solid #e7ebf2; background: #f7f9fc; }
.ai-chat-brand { display: flex; align-items: center; gap: 10px; padding: 0 7px 14px; }
.ai-chat-brand__icon,.ai-chat-avatar { display: inline-flex; align-items: center; justify-content: center; color: #fff; background: #1059c6; }
.ai-chat-brand__icon { width: 32px; height: 32px; flex: 0 0 32px; border-radius: 8px; }
.ai-chat-brand strong,.ai-chat-brand small { display: block; }
.ai-chat-brand strong { color: #202b3d; font-size: 14px; }
.ai-chat-brand small { margin-top: 4px; color: #8a94a6; font-size: 11px; }
.ai-mode-switch { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 3px; margin-bottom: 14px; padding: 3px; border: 1px solid #dfe5ee; border-radius: 6px; background: #edf1f6; }
.ai-mode-switch button { min-width: 0; height: 32px; display: inline-flex; align-items: center; justify-content: center; gap: 5px; padding: 0 7px; border: 0; border-radius: 4px; color: #6e7b8d; background: transparent; font-size: 12px; cursor: pointer; }
.ai-mode-switch button.is-active { color: #1059c6; background: #fff; box-shadow: 0 1px 3px rgba(23, 42, 70, .12); }
.ai-chat-new { width: 100%; margin-bottom: 14px; border-color: #bed2f2; color: #1059c6; background: #fff; }
.ai-chat-conversation-scroll { flex: 1; min-height: 0; }
.ai-chat-conversation { box-sizing: border-box; width: 100%; min-height: 64px; display: flex; align-items: center; gap: 9px; padding: 10px 8px; border: 0; border-radius: 6px; color: #7e8898; background: transparent; text-align: left; cursor: pointer; }
.ai-chat-conversation span { min-width: 0; flex: 1; }
.ai-chat-conversation b,.ai-chat-conversation em { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-chat-conversation b { color: #4a5567; font-size: 13px; font-weight: 500; }
.ai-chat-conversation em { margin-top: 4px; color: #a0a8b6; font-size: 11px; font-style: normal; }
.ai-chat-conversation:hover,.ai-chat-conversation.is-active { background: #e8f0fc; }
.ai-chat-conversation.is-active b,.ai-chat-conversation.is-active > .el-icon { color: #1059c6; }
.ai-chat-conversation__delete { width: 28px; height: 28px; flex: 0 0 28px; margin-left: auto; display: inline-flex; align-items: center; justify-content: center; padding: 0; border: 1px solid transparent; border-radius: 4px; color: #8b98aa; background: transparent; opacity: 0; cursor: pointer; }
.ai-chat-conversation:hover .ai-chat-conversation__delete,.ai-chat-conversation.is-active .ai-chat-conversation__delete { opacity: 1; }
.ai-chat-conversation__delete:hover { border-color: #f5cccc; color: #d64545; background: #feecec; }
.ai-chat-main { min-width: 0; flex: 1; display: flex; flex-direction: column; }
.ai-chat-header { min-height: 64px; padding: 0 18px 0 22px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #edf0f4; }
.ai-chat-header h3 { margin: 0; color: #273248; font-size: 16px; }
.ai-chat-header span { display: flex; align-items: center; gap: 5px; margin-top: 5px; color: #8590a1; font-size: 12px; }
.ai-chat-header span i { width: 6px; height: 6px; border-radius: 50%; background: #31a56b; }
.ai-chat-window-actions { flex: 0 0 auto; display: flex; align-items: center; gap: 4px; margin-left: 16px; }
.ai-chat-window-button { width: 32px; height: 28px; display: inline-flex; align-items: center; justify-content: center; padding: 0; border: 1px solid #dce3ee; border-radius: 4px; color: #64748b; background: #fff; cursor: pointer; }
.ai-chat-window-button:hover { color: #1059c6; border-color: #a9c4ec; background: #eef4fd; }
.ai-chat-window-button--close:hover { color: #d64545; border-color: #f0b8b8; background: #fff3f3; }
.ai-chat-messages { flex: 1; min-height: 0; padding: 20px 22px; background: #fbfcfe; }
.ai-chat-welcome { max-width: 480px; margin: 70px auto; text-align: center; }
.ai-chat-welcome__icon { width: 52px; height: 52px; display: inline-flex; align-items: center; justify-content: center; border-radius: 10px; color: #1059c6; background: #e7f0fd; font-size: 25px; }
.ai-chat-welcome h4 { margin: 14px 0 8px; color: #263148; font-size: 17px; }
.ai-chat-welcome p { margin: 0; color: #7b8697; font-size: 13px; }
.ai-chat-suggestions { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 20px; }
.ai-chat-suggestions button { padding: 7px 10px; border: 1px solid #d4e1f5; border-radius: 5px; color: #376aaf; background: #fff; font-size: 12px; cursor: pointer; }
.ai-chat-suggestions button:hover { border-color: #1059c6; color: #1059c6; }
.ai-chat-message { display: flex; gap: 9px; margin-bottom: 16px; }
.ai-chat-message.is-user { flex-direction: row-reverse; }
.ai-chat-avatar { width: 28px; height: 28px; flex: 0 0 28px; border-radius: 50%; font-size: 14px; }
.ai-chat-message.is-user .ai-chat-avatar { background: #74839a; }
.ai-chat-bubble { max-width: 76%; padding: 10px 12px; border: 1px solid #e4e9f0; border-radius: 7px; color: #354052; background: #fff; font-size: 13px; line-height: 1.7; white-space: pre-wrap; }
.ai-chat-bubble p { margin: 0; }
.ai-chat-message.is-user .ai-chat-bubble { color: #fff; border-color: #1059c6; background: #1059c6; }
.is-pending .ai-chat-bubble { display: flex; align-items: center; gap: 4px; }
.is-pending .ai-chat-bubble span { width: 5px; height: 5px; border-radius: 50%; background: #8a99ae; animation: ai-chat-pulse 1.1s infinite ease-in-out; }
.is-pending .ai-chat-bubble span:nth-child(2) { animation-delay: .15s; }
.is-pending .ai-chat-bubble span:nth-child(3) { animation-delay: .3s; }
.ai-report-attachment { min-width: 330px; display: flex; align-items: center; gap: 10px; margin-top: 10px; padding: 10px; border: 1px solid #d8e3f2; border-radius: 6px; background: #f7faff; white-space: normal; }
.ai-report-attachment__icon { width: 34px; height: 34px; flex: 0 0 34px; display: inline-flex; align-items: center; justify-content: center; border-radius: 5px; color: #fff; background: #1059c6; font-size: 17px; }
.ai-report-attachment > div:nth-child(2) { min-width: 0; flex: 1; }
.ai-report-attachment strong,.ai-report-attachment small { display: block; }
.ai-report-attachment strong { overflow: hidden; color: #334155; font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.ai-report-attachment small { margin-top: 2px; color: #8b96a6; font-size: 11px; }
.ai-report-attachment__actions { flex: 0 0 auto; display: flex; gap: 5px; }
.ai-report-attachment__actions button,.ai-report-viewer header button { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; padding: 0; border: 1px solid #d6dfeb; border-radius: 4px; color: #607089; background: #fff; cursor: pointer; }
.ai-report-attachment__actions button:hover,.ai-report-viewer header button:hover { color: #1059c6; border-color: #a9c4ec; background: #eef4fd; }
.ai-chat-input { padding: 12px 16px 14px; border-top: 1px solid #e9edf3; background: #fff; }
.ai-chat-input :deep(.el-textarea__inner) { box-shadow: none; color: #354052; }
.ai-chat-input__footer { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.ai-chat-input__footer span { color: #9aa3b0; font-size: 12px; }
.ai-chat-input__footer .el-button { min-width: 72px; }
.ai-report-viewer-overlay { position: absolute; inset: 0; z-index: 3008; display: flex; padding: 18px; background: rgba(15, 28, 48, .6); }
.ai-report-viewer { min-width: 0; min-height: 0; flex: 1; display: flex; flex-direction: column; overflow: hidden; border: 1px solid #dce3ed; border-radius: 8px; background: #f4f4f6; box-shadow: 0 18px 48px rgba(15, 31, 56, .3); }
.ai-report-viewer header { min-height: 56px; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 8px 12px 8px 18px; border-bottom: 1px solid #dde2ea; background: #fff; }
.ai-report-viewer header > div { min-width: 0; display: flex; align-items: center; gap: 8px; }
.ai-report-viewer header > div:first-child { display: block; }
.ai-report-viewer header strong,.ai-report-viewer header span { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-report-viewer header strong { max-width: 520px; color: #334155; font-size: 14px; }
.ai-report-viewer header span { margin-top: 3px; color: #929dab; font-size: 11px; }
.ai-report-viewer header button:disabled { opacity: .45; cursor: not-allowed; }
.ai-report-viewer__loading { flex: 1; display: flex; align-items: center; justify-content: center; gap: 9px; color: #718096; font-size: 13px; }
.ai-report-frame { width: 100%; min-height: 0; flex: 1; border: 0; background: #f4f4f6; }
.ai-chat-delete-overlay { position: absolute; inset: 0; z-index: 3010; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(15, 28, 48, .56); }
.ai-chat-delete-dialog { width: min(440px, 100%); overflow: hidden; border: 1px solid #e0e6ef; border-radius: 8px; background: #fff; box-shadow: 0 18px 48px rgba(15, 31, 56, .28); }
.ai-chat-delete-dialog header { display: flex; align-items: center; justify-content: space-between; padding: 18px 20px 12px; }
.ai-chat-delete-dialog h4 { margin: 0; color: #273248; font-size: 18px; font-weight: 600; }
.ai-chat-delete-dialog header button { width: 30px; height: 30px; display: inline-flex; align-items: center; justify-content: center; padding: 0; border: 0; color: #8b96a6; background: transparent; cursor: pointer; }
.ai-chat-delete-dialog__body { display: flex; align-items: flex-start; gap: 12px; padding: 12px 20px 22px; }
.ai-chat-delete-dialog__icon { width: 28px; height: 28px; flex: 0 0 28px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; color: #fff; background: #e6a23c; }
.ai-chat-delete-dialog__body p { margin: 2px 0 0; color: #5d6878; font-size: 14px; line-height: 1.65; word-break: break-all; }
.ai-chat-delete-dialog footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px 18px; border-top: 1px solid #edf0f4; }
.ai-chat-delete-dialog footer button { min-width: 72px; height: 34px; padding: 0 16px; border-radius: 4px; font-size: 13px; cursor: pointer; }
.ai-chat-delete-dialog__cancel { border: 1px solid #d8e0eb; color: #536174; background: #fff; }
.ai-chat-delete-dialog__confirm { border: 1px solid #d64545; color: #fff; background: #d64545; }
.ai-chat-delete-dialog__confirm:disabled { opacity: .6; cursor: not-allowed; }
.ai-chat-panel-enter-active,.ai-chat-panel-leave-active { transition: opacity .2s ease, transform .2s ease; }
.ai-chat-panel-enter-from,.ai-chat-panel-leave-to { opacity: 0; transform: translateY(12px) scale(.98); }
@keyframes ai-chat-pulse { 0%,80%,100% { transform: scale(.7); opacity: .45; } 40% { transform: scale(1); opacity: 1; } }
@media (max-width: 760px) {
  .ai-chat-panel,.ai-chat-panel.is-maximized { right: 12px; bottom: 12px; width: calc(100vw - 24px); height: calc(100vh - 24px); min-height: 0; }
  .ai-chat-sidebar { width: 78px; flex-basis: 78px; padding: 14px 8px; }
  .ai-chat-brand { justify-content: center; padding: 0 0 12px; }
  .ai-chat-brand > div { display: none; }
  .ai-mode-switch { grid-template-columns: 1fr; }
  .ai-mode-switch button span,.ai-chat-new :deep(span),.ai-chat-conversation span { display: none; }
  .ai-chat-new { padding: 7px; }
  .ai-chat-conversation { justify-content: center; padding: 11px 0; }
  .ai-chat-conversation__delete { display: none; }
  .ai-chat-messages { padding: 16px; }
  .ai-chat-bubble { max-width: 82%; }
  .ai-report-attachment { min-width: min(280px, calc(100vw - 150px)); }
  .ai-report-viewer-overlay { padding: 8px; }
  .ai-report-viewer header strong { max-width: calc(100vw - 260px); }
}
</style>
