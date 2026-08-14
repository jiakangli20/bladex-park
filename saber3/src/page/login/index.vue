<template>
  <div class="login-container" ref="login" @keyup.enter="handleLogin">
    <section class="login-visual" :style="visualStyle" aria-label="平台品牌信息">
      <div class="login-brand">
        <span class="login-brand__mark">
          <img src="/img/wzjk-logo.png" alt="吴中金控" />
        </span>
        <span class="login-brand__text">
          <strong>吴中金控</strong>
          <small>WUZHONG FINANCIAL HOLDINGS</small>
        </span>
      </div>

      <div class="login-visual__content animate__animated animate__fadeInLeft">
        <p class="login-eyebrow">园区数字化运营平台</p>
        <h1>{{ website.title }}</h1>
        <p class="login-visual__description">
          连接招商、合同、物业与企业服务，构建规范、高效、协同的园区运营体系。
        </p>
      </div>

      <div class="login-visual__footer">
        <div class="login-date" aria-label="当前日期时间">
          <strong>{{ clock }}</strong>
          <span>{{ date }} · {{ weekday }}</span>
        </div>
        <p>园区运营管理 · 企业服务协同</p>
      </div>
    </section>

    <main class="login-panel">
      <div class="login-panel__tools" title="切换语言">
        <top-lang></top-lang>
      </div>

      <div class="login-mobile-brand">
        <span class="login-mobile-brand__mark">
          <img src="/img/wzjk-logo.png" alt="" />
        </span>
        <strong>{{ website.title }}</strong>
      </div>

      <div class="login-main animate__animated animate__fadeInRight">
        <header class="login-heading">
          <p>{{ formEyebrow }}</p>
          <h2>{{ formTitle }}</h2>
          <span>{{ formDescription }}</span>
        </header>

        <div v-if="activeName !== 'register' && activeName !== 'third'" class="login-tabs">
          <button
            type="button"
            :class="{ 'is-active': activeName === 'user' }"
            :aria-selected="activeName === 'user'"
            @click="activeName = 'user'"
          >
            {{ $t('login.userLogin') }}
          </button>
          <button
            type="button"
            :class="{ 'is-active': activeName === 'code' }"
            :aria-selected="activeName === 'code'"
            @click="activeName = 'code'"
          >
            {{ $t('login.phoneLogin') }}
          </button>
        </div>

        <userLogin v-if="activeName === 'user'"></userLogin>
        <codeLogin v-else-if="activeName === 'code'"></codeLogin>
        <thirdLogin v-else-if="activeName === 'third'"></thirdLogin>
        <registerLogin v-else-if="activeName === 'register'"></registerLogin>

        <div v-if="activeName !== 'register'" class="login-menu">
          <button
            v-if="activeName === 'third'"
            type="button"
            class="login-menu__back"
            @click="activeName = 'user'"
          >
            返回账号登录
          </button>
          <template v-else>
            <span>其他登录方式</span>
            <button type="button" @click="activeName = 'third'">
              {{ $t('login.thirdLogin') }}
            </button>
            <a
              :href="
                website.oauth2.ssoBaseUrl + website.oauth2.ssoAuthUrl + website.oauth2.redirectUri
              "
            >
              {{ $t('login.ssoLogin') }}
            </a>
          </template>
        </div>
      </div>

      <p class="login-copyright">© {{ currentYear }} 吴中金控企业服务平台</p>
    </main>
  </div>
</template>
<script>
import userLogin from './userlogin.vue';
import registerLogin from './registerlogin.vue';
import codeLogin from './codelogin.vue';
import thirdLogin from './thirdlogin.vue';
import { mapGetters } from 'vuex';
import { validatenull } from '@/utils/validate';
import topLang from '@/page/index/top/top-lang.vue';
import { getQueryString, getTopUrl } from '@/utils/util';
import website from '@/config/website';

export default {
  name: 'login',
  components: {
    userLogin,
    registerLogin,
    codeLogin,
    thirdLogin,
    topLang,
  },
  data() {
    return {
      website: website,
      date: '',
      clock: '',
      weekday: '',
      timer: null,
      activeName: 'user',
      tenantBackground: '',
      socialForm: {
        tenantId: '000000',
        source: '',
        code: '',
        state: '',
      },
    };
  },
  watch: {
    $route() {
      this.handleLogin();
    },
  },
  created() {
    this.handleLogin();
    this.getTime();
  },
  beforeUnmount() {
    window.clearInterval(this.timer);
  },
  mounted() {},
  computed: {
    ...mapGetters(['tagWel']),
    currentYear() {
      return this.$dayjs().format('YYYY');
    },
    visualStyle() {
      return {
        backgroundImage: `url(${this.tenantBackground || '/img/bg/login.png'})`,
      };
    },
    formEyebrow() {
      if (this.activeName === 'register') return '创建平台账号';
      if (this.activeName === 'third') return '便捷访问';
      return '欢迎使用';
    },
    formTitle() {
      if (this.activeName === 'register') return '账号注册';
      if (this.activeName === 'third') return '第三方账号登录';
      return '登录园区工作台';
    },
    formDescription() {
      if (this.activeName === 'register') return '请填写完整信息并提交注册申请';
      if (this.activeName === 'third') return '请选择已绑定的平台账号继续';
      return '请使用已分配的账号进入系统';
    },
  },
  props: [],
  methods: {
    getTime() {
      const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
      const updateTime = () => {
        const now = this.$dayjs();
        this.date = now.format('YYYY年MM月DD日');
        this.clock = now.format('HH:mm:ss');
        this.weekday = weekdays[now.day()];
      };
      updateTime();
      this.timer = window.setInterval(updateTime, 1000);
    },
    setTenantBackground(url) {
      this.tenantBackground = url || '';
    },
    handleLogin() {
      const topUrl = getTopUrl();
      const redirectUrl = '/oauth/redirect/';
      const ssoCode = '?code=';
      this.socialForm.source = getQueryString('source');
      this.socialForm.code = getQueryString('code');
      this.socialForm.state = getQueryString('state');
      if (validatenull(this.socialForm.source) && topUrl.includes(redirectUrl)) {
        let source = topUrl.split('?')[0];
        source = source.split(redirectUrl)[1];
        this.socialForm.source = source;
      }
      if (
        topUrl.includes(redirectUrl) &&
        !validatenull(this.socialForm.source) &&
        !validatenull(this.socialForm.code) &&
        !validatenull(this.socialForm.state)
      ) {
        const loading = this.$loading({
          lock: true,
          text: '第三方系统登录中,请稍后',
          background: 'rgba(0, 0, 0, 0.7)',
        });
        this.$store
          .dispatch('LoginBySocial', this.socialForm)
          .then(() => {
            window.location.href = topUrl.split(redirectUrl)[0];
            //加载工作流路由集
            this.loadFlowRoutes();
            this.$router.push(this.tagWel);
            loading.close();
          })
          .catch(() => {
            loading.close();
          });
      } else if (
        !topUrl.includes(redirectUrl) &&
        !validatenull(this.socialForm.code) &&
        !validatenull(this.socialForm.state)
      ) {
        const loading = this.$loading({
          lock: true,
          text: '单点系统登录中,请稍后',
          background: 'rgba(0, 0, 0, 0.7)',
        });
        this.$store
          .dispatch('LoginBySso', this.socialForm)
          .then(() => {
            window.location.href = topUrl.split(ssoCode)[0];
            //加载工作流路由集
            this.loadFlowRoutes();
            this.$router.push(this.tagWel);
            loading.close();
          })
          .catch(() => {
            loading.close();
          });
      }
    },
    loadFlowRoutes() {
      this.$store.dispatch('FlowRoutes').then(() => {});
    },
  },
};
</script>
