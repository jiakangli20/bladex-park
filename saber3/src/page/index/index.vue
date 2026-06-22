<template>
  <el-watermark :content="watermark" style="height: 100%">
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
import website from '@/config/website';
import { validatenull } from '@/utils/validate';

export default {
  mixins: [index],
  components: {
    top,
    tags,
    search,
    sidebar,
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
    ]),
    validSidebar() {
      if (this.$route.path === this.tagWel.path) {
        return false;
      }
      return !(
        (this.$route.meta || {}).menu === false || (this.$route.query || {}).menu === 'false'
      );
    },
    watermark() {
      return website.watermark.mode ? website.watermark.text : '';
    },
  },
  props: [],
  methods: {
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
            if (activeTopMenu) {
              this.$store.dispatch('GetMenu', activeTopMenu.id).then(data => {
                if (this.$refs.top && activeTopMenu.id) {
                  this.$refs.top.setActiveMenu(activeTopMenu.id);
                }
                if (this.menuAll.length !== 0) {
                  this.$router.$avueRouter.formatRoutes(this.menuAll, true);
                }
                if (!data.length && this.$route.path !== this.tagWel.path && !validatenull(activeTopMenu.path)) {
                  this.$router.push({ path: activeTopMenu.path });
                }
              });
              return;
            }
            doOpen(topMenus[0] || item);
          })
          .catch(() => {
            doOpen(item);
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
      return topMenus.find(menuItem => matchPath(menuItem));
    },
    findFirstPagePath(menuList = []) {
      for (const menuItem of menuList) {
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
