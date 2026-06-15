<template>
  <el-watermark :content="watermark" style="height: 100%">
    <div class="avue-contail" :class="{ 'avue--collapse': isCollapse }">
      <div class="avue-layout" :class="{ 'avue-layout--horizontal': isHorizontal }">
        <div class="avue-sidebar" v-show="validSidebar">
          <!-- 左侧导航栏 -->
          <logo />
          <sidebar />
        </div>
        <div class="avue-main">
          <!-- 顶部导航栏 -->
          <top ref="top" />
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
import logo from './logo.vue';
import top from './top/index.vue';
import sidebar from './sidebar/index.vue';
import website from '@/config/website';
import { validatenull } from '@/utils/validate';
import { getMainMenu } from '@/api/system/menu';

export default {
  mixins: [index],
  components: {
    top,
    logo,
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
      'setting',
    ]),
    validSidebar() {
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
          if (data.length !== 0) {
            this.$router.$avueRouter.formatRoutes(data, true);
            if (!validatenull(menuItem.path)) {
              this.$router.push({ path: menuItem.path });
            }
          }
        });
      };
      if (!item.id && !skipMainMenu) {
        getMainMenu()
          .then(res => {
            const mainMenu = res.data.data;
            doOpen(mainMenu && mainMenu.id ? { id: mainMenu.id } : item);
          })
          .catch(() => {
            doOpen(item);
          });
      } else {
        doOpen(item);
      }
    },
  },
};
</script>
