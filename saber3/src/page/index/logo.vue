<template>
  <div class="avue-logo">
    <transition name="fade">
      <span v-if="getScreen(isCollapse)" class="avue-logo_subtitle" key="0">
        <img class="logo-image logo-image--mini" src="/img/wzjk-logo.png" alt="吴中金控企业服务平台" />
      </span>
    </transition>
    <transition-group name="fade">
      <template v-if="getScreen(!isCollapse)">
        <img class="logo-image" key="0" src="/img/wzjk-logo.png" alt="吴中金控企业服务平台" />
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
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1;
  white-space: nowrap;
}

.logo-image {
  width: 24px;
  height: 24px;
  margin-right: 12px;
  object-fit: contain;
  flex: 0 0 auto;
}

.logo-image--mini {
  margin-right: 0;
}
</style>
