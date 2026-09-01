<template>
  <button
    ref="trigger"
    class="ai-chat-trigger"
    type="button"
    aria-label="打开房源智能问答"
    :style="positionStyle"
    @pointerdown="startDrag"
    @pointermove="moveDrag"
    @pointerup="endDrag"
    @pointercancel="endDrag"
    @click="open"
  >
    <el-icon><ChatDotRound /></el-icon>
    <span>AI</span>
  </button>
</template>

<script>
import { ChatDotRound } from '@element-plus/icons-vue';

const STORAGE_KEY = 'ai-chat-bubble-position';
const BUBBLE_MARGIN = 16;

export default {
  name: 'AiChatBubble',
  components: { ChatDotRound },
  emits: ['open'],
  data() {
    return {
      left: null,
      top: null,
      dragging: false,
      moved: false,
      clickBlocked: false,
      pointerId: null,
      startX: 0,
      startY: 0,
      startLeft: 0,
      startTop: 0,
    };
  },
  computed: {
    positionStyle() {
      if (this.left === null || this.top === null) {
        return { right: '28px', bottom: '28px' };
      }
      return { left: `${this.left}px`, top: `${this.top}px` };
    },
  },
  mounted() {
    this.restorePosition();
    window.addEventListener('resize', this.keepInViewport);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.keepInViewport);
  },
  methods: {
    restorePosition() {
      try {
        const saved = JSON.parse(window.localStorage.getItem(STORAGE_KEY));
        if (saved && Number.isFinite(saved.left) && Number.isFinite(saved.top)) {
          this.left = saved.left;
          this.top = saved.top;
          this.$nextTick(this.keepInViewport);
        }
      } catch (e) {
        // Ignore unavailable or malformed browser storage.
      }
    },
    savePosition() {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ left: this.left, top: this.top }));
    },
    setInitialPosition() {
      const element = this.$refs.trigger;
      if (!element) return;
      const rect = element.getBoundingClientRect();
      this.left = rect.left;
      this.top = rect.top;
    },
    keepInViewport() {
      if (this.left === null || this.top === null) return;
      const element = this.$refs.trigger;
      const width = element ? element.offsetWidth : 58;
      const height = element ? element.offsetHeight : 58;
      const maxLeft = Math.max(BUBBLE_MARGIN, window.innerWidth - width - BUBBLE_MARGIN);
      const maxTop = Math.max(BUBBLE_MARGIN, window.innerHeight - height - BUBBLE_MARGIN);
      const nextLeft = Math.min(Math.max(this.left, BUBBLE_MARGIN), maxLeft);
      const nextTop = Math.min(Math.max(this.top, BUBBLE_MARGIN), maxTop);
      if (nextLeft !== this.left || nextTop !== this.top) {
        this.left = nextLeft;
        this.top = nextTop;
        this.savePosition();
      }
    },
    startDrag(event) {
      if (event.button !== undefined && event.button !== 0) return;
      if (this.left === null || this.top === null) this.setInitialPosition();
      this.dragging = true;
      this.moved = false;
      this.pointerId = event.pointerId;
      this.startX = event.clientX;
      this.startY = event.clientY;
      this.startLeft = this.left;
      this.startTop = this.top;
      event.currentTarget.setPointerCapture?.(event.pointerId);
      event.preventDefault();
    },
    moveDrag(event) {
      if (!this.dragging || event.pointerId !== this.pointerId) return;
      const deltaX = event.clientX - this.startX;
      const deltaY = event.clientY - this.startY;
      if (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4) this.moved = true;
      this.left = this.startLeft + deltaX;
      this.top = this.startTop + deltaY;
      this.keepInViewport();
      event.preventDefault();
    },
    endDrag(event) {
      if (!this.dragging || event.pointerId !== this.pointerId) return;
      this.dragging = false;
      event.currentTarget.releasePointerCapture?.(event.pointerId);
      if (this.moved) {
        this.clickBlocked = true;
        this.savePosition();
        window.setTimeout(() => { this.clickBlocked = false; }, 0);
      }
      this.pointerId = null;
    },
    open(event) {
      if (this.clickBlocked || this.moved) {
        event.preventDefault();
        this.moved = false;
        return;
      }
      this.$emit('open');
    },
  },
};
</script>

<style lang="scss" scoped>
.ai-chat-trigger { position: fixed; z-index: 3001; width: 58px; height: 58px; padding: 0; border: 0; border-radius: 50%; color: #fff; background: #1059c6; box-shadow: 0 8px 20px rgba(16, 89, 198, .32); cursor: grab; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px; font-size: 11px; touch-action: none; user-select: none; transition: background .2s ease, transform .2s ease; }
.ai-chat-trigger:active { cursor: grabbing; }
.ai-chat-trigger .el-icon { font-size: 20px; }
.ai-chat-trigger:hover { background: #0c4eae; transform: translateY(-1px); }
@media (max-width: 720px) {
  .ai-chat-trigger { width: 54px; height: 54px; }
}
</style>
