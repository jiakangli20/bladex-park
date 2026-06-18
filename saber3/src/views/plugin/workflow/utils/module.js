// Nutflow高德地图配置
const nfFormOption = {
  amap: {
    key: '483aec727f3b4ee2bc262440340c8a12',
    secret: '61a0e1c24a86f563caaf8c1a6160ae77'
  }
}

import { ElLoading } from 'element-plus';
import NfCustomFields from "@/views/plugin/workflow/components/custom-fields"; // 自定义业务字段
import NfDesignEnLocale from '@nutflow/nf-design-elp/lib/locale/lang/en'
import NfDesignZhLocale from '@nutflow/nf-design-elp/lib/locale/lang/zh'
import { language } from '@/lang/';

const option = {
  lock: true,
  fullscreen: true,
  background: 'rgba(255, 255, 255, 0.1)'
}

export function loadFlowDesignModule(app) {
  return new Promise((resolve) => {
    const nfDesign = app.component("nf-design") || app.component("NfDesign");
    if (!nfDesign) {
      option.text = "正在加载流程设计器, 请稍等..."
      const loading = ElLoading.service(option);

      import('@nutflow/nf-design-elp')
        .then(module => {
          app.use(module.default, { locale: language == 'en' ? NfDesignEnLocale : NfDesignZhLocale });
          loading.close();
          resolve();
        })
        .catch(error => {
          loading.close();
          console.error('failed to load module:', error)
        });
    } else {
      resolve()
    }
  })
}

export function loadFormModule(app) {
  return new Promise((resolve) => {
    const nfForm = app.component("nf-form") || app.component("NfForm");
    if (!nfForm) {
      app.use(NfCustomFields);

      option.text = "正在加载表单组件, 请稍等..."
      const loading = ElLoading.service(option);
      import('@saber/nf-form-elp')
        .then(module => {
          app.use(module.default, nfFormOption);
          loading.close();
          resolve();
        })
        .catch(error => {
          loading.close();
          console.error('failed to load module:', error)
        });
    } else {
      resolve()
    }
  })
}

export function loadFormDesignModule(app) {
  return new Promise((resolve) => {
    const nfFormDesign = app.component("nf-form-design") || app.component("NfFormDesign");
    if (!nfFormDesign) {
      option.text = "正在加载表单设计器, 请稍等..."
      const loading = ElLoading.service(option);
      import('@saber/nf-form-design-elp')
        .then(module => {
          app.use(module.default);
          loading.close();
          resolve();
        })
        .catch(error => {
          loading.close();
          console.error('failed to load module:', error)
        });
    } else {
      resolve()
    }
  })
}
