<template>
  <div class="top-menu-shell">
    <button
      class="top-menu-shell__arrow"
      :class="{ 'is-disabled': !canScrollLeft }"
      type="button"
      @click="scrollMenu(-1)"
    >
      <span>‹</span>
    </button>
    <div
      ref="menuScroll"
      class="top-menu-shell__scroll"
      @scroll="updateScrollState"
      @wheel.prevent="handleMenuWheel"
    >
      <el-menu class="top-menu" :default-active="activeIndex" mode="horizontal" :ellipsis="false">
        <el-menu-item index="0" @click="openHome">
          <template #title>
            <i :class="itemHome.source" style="padding-right: 5px"></i>
            <span>{{ itemHome.name }}</span>
          </template>
        </el-menu-item>

        <template v-for="(item, index) in items" :key="index">
          <el-menu-item :index="item.id + ''" @click="openMenu(item)">
            <template #title>
              <i :class="item.source" style="padding-right: 5px"></i>
              <span>{{ item.name }}</span>
            </template>
          </el-menu-item>
        </template>
      </el-menu>
    </div>
    <button
      class="top-menu-shell__arrow"
      :class="{ 'is-disabled': !canScrollRight }"
      type="button"
      @click="scrollMenu(1)"
    >
      <span>›</span>
    </button>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'top-menu',
  data() {
    return {
      itemHome: {
        name: '首页',
        source: 'iconfont iconicon_work',
      },
      activeIndex: '0',
      items: [],
      canScrollLeft: false,
      canScrollRight: false,
    };
  },
  inject: ['index'],
  created() {
    this.getMenu();
  },
  mounted() {
    this.updateScrollState();
    window.addEventListener('resize', this.updateScrollState);
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.updateScrollState);
  },
  computed: {
    ...mapGetters(['tagCurrent', 'topMenu', 'tagWel']),
  },
  watch: {
    topMenu: {
      handler(list = []) {
        this.items = this.normalizeTopMenu(list);
        this.syncActiveByRoute();
        this.$nextTick(this.updateScrollState);
      },
      deep: true,
      immediate: true,
    },
    $route: {
      handler() {
        this.syncActiveByRoute();
      },
      immediate: true,
    },
  },
  methods: {
    openMenu(item) {
      this.setActive(item.id);
      this.index.openMenu(item);
    },
    openHome() {
      this.setActive(0);
      this.$router.push(this.tagWel);
    },
    setActive(id) {
      this.activeIndex = String(id || 0);
      this.$nextTick(this.scrollActiveIntoView);
    },
    setActiveMenu(id) {
      this.setActive(id);
    },
    normalizeTopMenu(list = []) {
      return list.filter(Boolean);
    },
    matchPath(menuItem, path) {
      if (!menuItem || !path) return false;
      if (menuItem.path && (path === menuItem.path || path.indexOf(`${menuItem.path}/`) === 0)) {
        return true;
      }
      return (menuItem.children || []).some(child => this.matchPath(child, path));
    },
    syncActiveByRoute() {
      const path = this.$route.path;
      if (path === this.tagWel.path) {
        this.setActive(0);
        return;
      }
      const activeMenu = this.items.find(item => this.matchPath(item, path));
      if (activeMenu) {
        this.setActive(activeMenu.id);
      }
    },
    getMenu() {
      this.$store.dispatch('GetTopMenu').then(res => {
        this.items = this.normalizeTopMenu(res);
        this.$nextTick(this.updateScrollState);
      });
    },
    scrollMenu(direction) {
      const el = this.$refs.menuScroll;
      if (!el) return;
      el.scrollBy({
        left: direction * 320,
        behavior: 'smooth',
      });
    },
    handleMenuWheel(event) {
      const el = this.$refs.menuScroll;
      if (!el) return;
      const distance = Math.abs(event.deltaX) > Math.abs(event.deltaY) ? event.deltaX : event.deltaY;
      el.scrollBy({
        left: distance,
        behavior: 'auto',
      });
    },
    updateScrollState() {
      const el = this.$refs.menuScroll;
      if (!el) return;
      this.canScrollLeft = el.scrollLeft > 0;
      this.canScrollRight = el.scrollLeft + el.clientWidth < el.scrollWidth - 1;
    },
    scrollActiveIntoView() {
      const el = this.$refs.menuScroll;
      if (!el) return;
      const active = el.querySelector('.el-menu-item.is-active');
      if (active && active.scrollIntoView) {
        active.scrollIntoView({ inline: 'nearest', block: 'nearest', behavior: 'smooth' });
      }
      this.updateScrollState();
    },
  },
};
</script>
