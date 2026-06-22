<template>
  <el-scrollbar class="avue-menu">
    <div v-if="showMenuTip" class="avue-sidebar--tip">
      {{ $t('menuTip') }}
    </div>
    <el-menu
      unique-opened
      :default-active="activeMenu"
      :mode="setting.sidebar"
      :collapse="getScreen(isCollapse)"
    >
      <sidebar-item :menu="menu"></sidebar-item>
    </el-menu>
  </el-scrollbar>
</template>

<script>
import { mapGetters } from 'vuex';
import sidebarItem from './sidebarItem.vue';

export default {
  name: 'sidebar',
  components: { sidebarItem },
  inject: ['index'],
  created() {
    this.index.openMenu();
  },
  computed: {
    ...mapGetters(['isHorizontal', 'setting', 'menu', 'tag', 'tagWel', 'isCollapse', 'menuId']),
    showMenuTip() {
      return this.menu && this.menu.length === 0 && !this.isHorizontal && this.$route.path !== this.tagWel.path;
    },
    activeMenu() {
      const route = this.$route;
      const { meta, path } = route;
      if (meta.activeMenu) {
        return meta.activeMenu;
      }
      return path;
    },
  },
};
</script>
<style lang="scss" scoped></style>
