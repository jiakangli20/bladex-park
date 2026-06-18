import { getList } from '../api/design/form-theme.js';
export default {
  data() {
    return {
      theme: '',
      themeList: [],
      themeCustomStyle: {},
    };
  },
  created() {
    this.initTheme();
    const theme = localStorage.getItem('nf-form-theme');
    if (theme || theme == '') this.theme = theme || 'default';
    else this.theme = 'default';
  },
  watch: {
    theme: {
      handler(val) {
        if (val) {
          import(`../assets/styles/theme.scss`);
          localStorage.setItem('nf-form-theme', val);
          this.setThemeStyle();
        }
      },
      immediate: true,
    },
  },
  methods: {
    initTheme() {
      getList(1, -1).then(res => {
        this.themeList = res.data.data.records;
        this.themeList.forEach(e => {
          e.label = e.name;
          e.value = e.themeKey;
        });
        this.setThemeStyle();
      });
    },
    setThemeStyle() {
      let obj = this.themeList.find(item => item.themeKey == this.theme);
      for (let i in obj) {
        this.themeCustomStyle[`--${i}`] = obj[i];
      }
    },
  },
};
