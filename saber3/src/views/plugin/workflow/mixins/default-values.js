import { getUserInfo } from '@/api/system/user';
import { dateFormat } from '../utils/date.js';

export default {
  created() {
    this.extendUserInfo();
  },
  methods: {
    dateFormat,
    // 扩展用户信息
    extendUserInfo() {
      const { userInfo } = this.$store.getters;
      const { dept_name, post_name } = userInfo;
      if (!dept_name || !post_name) {
        getUserInfo().then(res => {
          const data = res.data.data;
          if (data) {
            const { deptName, postName } = data;
            this.$store.commit('SET_USER_INFO', {
              ...userInfo,
              dept_name: deptName,
              post_name: postName,
            });
          }
        });
      }
    },
    // 获取字段默认值
    getDefaultValues(value) {
      let defaultValue = '';
      if (value.includes('${') && value.includes('}')) {
        try {
          defaultValue = eval('`' + value + '`');
        } catch (err) {
          defaultValue = value;
        }
      } else defaultValue = value;

      return defaultValue;
    },
  },
};
