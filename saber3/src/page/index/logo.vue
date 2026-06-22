<template>
  <div class="avue-logo">
    <transition name="fade">
      <span v-if="getScreen(isCollapse)" class="avue-logo_subtitle" key="0">
        <span class="logo-mark">企</span>
      </span>
    </transition>
    <transition-group name="fade">
      <template v-if="getScreen(!isCollapse)">
        <span class="logo-mark" key="0">企</span>
        <span class="logo-title" key="1">{{ website.indexTitle }} </span>
      </template>
    </transition-group>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'logo',
  data() {
    return {};
  },
  created() {},
  computed: {
    ...mapGetters(['isCollapse']),
  },
  methods: {},
};
</script>
<style scoped>
.logo-title {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-right: 12px;
  border-radius: 4px;
  background: #fff;
  color: #1059c6;
  font-size: 14px;
  font-weight: 700;
}
</style>
