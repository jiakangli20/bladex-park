import { downloadBlob } from '@/api/common';
import { getNoticePreview } from '@/api/contract/print';
import { downloadFile } from '@/utils/util';

export const createNoticePreviewState = () => ({
  visible: false,
  loading: false,
  title: '审批表预览',
  html: '',
  downloadUrl: '',
  fallbackName: '',
});

export const resolveDownloadFilename = (disposition, fallbackName) => {
  if (!disposition) return fallbackName;
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match && utf8Match[1]) {
    return decodeURIComponent(utf8Match[1]);
  }
  const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
  return filenameMatch && filenameMatch[1] ? decodeURIComponent(filenameMatch[1]) : fallbackName;
};

export const downloadNoticeFile = (url, fallbackName) => {
  if (!url) return Promise.resolve();
  return downloadBlob(url).then(res => {
    const disposition = res.headers && res.headers['content-disposition'];
    const filename = resolveDownloadFilename(disposition, fallbackName);
    const contentType = (res.headers && res.headers['content-type']) || 'application/octet-stream';
    downloadFile(res.data, filename, contentType);
  });
};

export const openNoticePreview = (vm, state, params, downloadUrl, fallbackName, title) => {
  state.loading = true;
  state.visible = true;
  state.title = title || '审批表预览';
  state.html = '';
  state.downloadUrl = downloadUrl || '';
  state.fallbackName = fallbackName || '审批文件';
  return getNoticePreview(params)
    .then(res => {
      const data = res.data.data || {};
      state.title = data.noticeName || title || '审批表预览';
      state.html = data.html || '';
      if (data.fileName) {
        state.fallbackName = data.fileName;
      }
    })
    .catch(error => {
      state.visible = false;
      throw error;
    })
    .finally(() => {
      state.loading = false;
    });
};

export const closeNoticePreview = state => {
  state.visible = false;
  state.loading = false;
  state.title = '审批表预览';
  state.html = '';
  state.downloadUrl = '';
  state.fallbackName = '';
};
