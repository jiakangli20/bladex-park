import { loadFlowDesignModule, loadFormModule, loadFormDesignModule } from '../utils/module';
import { getCurrentInstance, onBeforeMount, ref } from 'vue';
import { ElMessage } from 'element-plus';

const moduleLoadInit = ref(false);

export const useLoadModule = (module: string[] = []) => {
  const { proxy } = getCurrentInstance() as any;
  onBeforeMount(() => {
    if (proxy.$app) {
      loadFormModule(proxy.$app).then(() => {
        if (!module || module.length == 0) {
          moduleLoadInit.value = true;
        }
        // 加载流程设计器
        if (module.includes("d")) {
          loadFlowDesignModule(proxy.$app).then(() => {
            moduleLoadInit.value = true;
          });
        }

        // 加载表单设计器
        if (module.includes("fd")) {
          loadFormDesignModule(proxy.$app).then(() => {
            moduleLoadInit.value = true;
          });
        }
      })
    } else {
      ElMessage.error("请在main.js中添加 app.config.globalProperties.$app = app;")
    }
  });

  return {
    moduleLoadInit,
  };
};
