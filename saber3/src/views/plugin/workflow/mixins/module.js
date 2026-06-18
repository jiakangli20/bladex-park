import { loadFlowDesignModule, loadFormModule, loadFormDesignModule } from "../utils/module"

export default function (module = []) {
  return {
    data() {
      return {
        moduleLoadInit: false
      }
    },
    beforeCreate() {
      if (this.$app) {
        loadFormModule(this.$app).then(() => {
          if (!module || module.length == 0) {
            this.moduleLoadInit = true;
          }
          // 加载流程设计器
          if (module.includes("d")) {
            loadFlowDesignModule(this.$app).then(() => {
              this.moduleLoadInit = true;
            });
          }

          // 加载表单设计器
          if (module.includes("fd")) {
            loadFormDesignModule(this.$app).then(() => {
              this.moduleLoadInit = true;
            });
          }
        })
      } else {
        this.$message.error("请在main.js中添加 app.config.globalProperties.$app = app;")
      }
    }
  }
}