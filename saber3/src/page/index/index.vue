<template>
  <el-watermark
    :content="watermark"
    :font="watermarkFont"
    :gap="[180, 120]"
    :rotate="-18"
    style="height: 100%"
  >
    <div class="avue-contail" :class="{ 'avue--collapse': isCollapse }">
      <div class="avue-layout" :class="{ 'avue-layout--horizontal': isHorizontal }">
        <!-- 顶部一级菜单 -->
        <top ref="top" />
        <div class="avue-body">
          <div class="avue-sidebar" v-show="validSidebar">
            <!-- 左侧二级菜单 -->
            <sidebar />
          </div>
          <div class="avue-main">
            <!-- 顶部标签卡 -->
            <tags />
            <search class="avue-view" v-show="isSearch"></search>
            <!-- 主体视图层 -->
            <div id="avue-view" v-show="!isSearch" v-if="isRefresh">
              <router-view #="{ Component }">
                <keep-alive :include="$store.getters.tagsKeep">
                  <component :is="Component" />
                </keep-alive>
              </router-view>
            </div>
          </div>
        </div>
      </div>
      <!-- <wechat></wechat> -->
      <ai-chat-assistant />
    </div>
  </el-watermark>
</template>

<script>
import index from '@/mixins/index';
import wechat from './wechat.vue';
//import { validatenull } from 'utils/validate';
import { mapGetters } from 'vuex';
import tags from './tags.vue';
import search from './search.vue';
import top from './top/index.vue';
import sidebar from './sidebar/index.vue';
import AiChatAssistant from '@/components/ai-chat-assistant/main.vue';
import website from '@/config/website';
import { validatenull } from '@/utils/validate';

export default {
  mixins: [index],
  components: {
    top,
    tags,
    search,
    sidebar,
    AiChatAssistant,
    wechat,
  },
  name: 'index',
  provide() {
    return {
      index: this,
    };
  },
  computed: {
    ...mapGetters([
      'isHorizontal',
      'isRefresh',
      'isLock',
      'isCollapse',
      'isSearch',
      'menu',
      'menuAll',
      'setting',
      'tagWel',
      'userInfo',
    ]),
    validSidebar() {
      return !(
        (this.$route.meta || {}).menu === false || (this.$route.query || {}).menu === 'false'
      );
    },
    watermark() {
      if (!website.watermark.mode) return '';
      const user = this.userInfo || {};
      const username =
        user.account ||
        user.userName ||
        user.user_name ||
        user.realName ||
        user.real_name ||
        website.watermark.text;
      return `${username}  ${this.watermarkDate}`;
    },
    watermarkFont() {
      return {
        color: 'rgba(16, 89, 198, 0.1)',
        fontSize: 15,
        fontWeight: 400,
        fontFamily: 'Arial, "Microsoft YaHei", sans-serif',
      };
    },
  },
  data() {
    return {
      watermarkDate: this.formatWatermarkDate(),
      watermarkTimer: null,
    };
  },
  mounted() {
    this.scheduleWatermarkDateRefresh();
  },
  beforeUnmount() {
    if (this.watermarkTimer) window.clearTimeout(this.watermarkTimer);
  },
  props: [],
  methods: {
    formatWatermarkDate(date = new Date()) {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    },
    scheduleWatermarkDateRefresh() {
      if (this.watermarkTimer) window.clearTimeout(this.watermarkTimer);
      const now = new Date();
      const nextDay = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
      this.watermarkTimer = window.setTimeout(() => {
        this.watermarkDate = this.formatWatermarkDate();
        this.scheduleWatermarkDateRefresh();
      }, nextDay.getTime() - now.getTime() + 1000);
    },
    //打开菜单
    openMenu(item = {}, skipMainMenu = false) {
      const doOpen = menuItem => {
        this.$store.dispatch('GetMenu', menuItem.id).then(data => {
          if (this.$refs.top && menuItem.id) {
            this.$refs.top.setActiveMenu(menuItem.id);
          }
          if (this.menuAll.length !== 0) {
            this.$router.$avueRouter.formatRoutes(this.menuAll, true);
          }
          if (data.length !== 0) {
            const targetPath = this.findFirstPagePath(data) || menuItem.path;
            if (!validatenull(targetPath)) {
              this.$router.push({ path: targetPath });
            }
          } else if (!validatenull(menuItem.path)) {
            this.$router.push({ path: menuItem.path });
          }
        });
      };
      if (!item.id && !skipMainMenu) {
        if (this.$route.path === this.tagWel.path) {
          this.$store.dispatch('GetMenu').then(data => {
            if (data.length !== 0) {
              this.$router.$avueRouter.formatRoutes(data, true);
            }
            if (this.$refs.top) {
              this.$refs.top.setActiveMenu(0);
            }
          });
          return;
        }
        this.$store
          .dispatch('GetTopMenu')
          .then(topMenus => {
            const activeTopMenu = this.findActiveTopMenu(topMenus);
            this.$store.dispatch('GetMenu', activeTopMenu && activeTopMenu.id).then(data => {
              if (this.$refs.top) {
                this.$refs.top.setActiveMenu(activeTopMenu && activeTopMenu.id);
              }
              if (data.length !== 0) {
                this.$router.$avueRouter.formatRoutes(
                  this.menuAll.length ? this.menuAll : data,
                  true
                );
              }
            });
          })
          .catch(() => {
            this.$store.dispatch('GetMenu').then(data => {
              if (data.length !== 0) {
                this.$router.$avueRouter.formatRoutes(data, true);
              }
            });
          });
      } else {
        doOpen(item);
      }
    },
    findActiveTopMenu(topMenus = []) {
      const path = this.$route.path;
      const matchPath = menuItem => {
        if (!menuItem) return false;
        if (menuItem.path && (path === menuItem.path || path.indexOf(`${menuItem.path}/`) === 0)) {
          return true;
        }
        const children = menuItem.children || [];
        return children.some(child => matchPath(child));
      };
      const matchedByPath = topMenus.find(menuItem => matchPath(menuItem));
      if (matchedByPath) {
        return matchedByPath;
      }
      const topMenuCode = this.topMenuCodeByRoute(path);
      return topMenus.find(menuItem => topMenuCode && menuItem.code === topMenuCode);
    },
    topMenuCodeByRoute(path) {
      const routeMap = [
        { prefix: '/desk/notice', code: 'service' },
        { prefix: '/plugin/workflow', code: 'office' },
        { prefix: '/settlement', code: 'entry' },
        { prefix: '/enterprise', code: 'service' },
        { prefix: '/contract', code: 'contract' },
        { prefix: '/finance', code: 'finance' },
        { prefix: '/park', code: 'park' },
        { prefix: '/system', code: 'system' },
      ];
      return routeMap.find(item => path === item.prefix || path.indexOf(`${item.prefix}/`) === 0)
        ?.code;
    },
    findFirstPagePath(menuList = []) {
      for (const menuItem of menuList) {
        if (menuItem.path === website.fistPage.path) {
          continue;
        }
        const children = menuItem.children || [];
        if (children.length) {
          const childPath = this.findFirstPagePath(children);
          if (!validatenull(childPath)) {
            return childPath;
          }
        } else if (!validatenull(menuItem.path)) {
          return menuItem.path;
        }
      }
      return '';
    },
  },
};
</script>
