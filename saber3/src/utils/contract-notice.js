import { downloadBlob } from '@/api/common';
import { getNoticePreview } from '@/api/contract/print';
import { baseUrl } from '@/config/env';
import { downloadFile } from '@/utils/util';

export const createNoticePreviewState = () => ({
  visible: false,
  loading: false,
  title: '审批表预览',
  html: '',
  downloadUrl: '',
  fallbackName: '',
  downloadLabel: '下载Word',
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

export const normalizeNoticeDownloadUrl = url => {
  if (!url || typeof window === 'undefined') return url;
  try {
    const parsed = new URL(url, window.location.origin);
    const localHost = ['localhost', '127.0.0.1', '::1'].includes(parsed.hostname);
    const currentHost = parsed.hostname === window.location.hostname;
    if (
      parsed.pathname.startsWith('/upload/') &&
      (localHost || currentHost || parsed.port === '8080')
    ) {
      return `${String(baseUrl || '').replace(/\/$/, '')}${parsed.pathname}${parsed.search}`;
    }
  } catch {
    return url;
  }
  return url;
};

export const downloadNoticeFile = (url, fallbackName) => {
  if (!url) return Promise.resolve();
  return downloadBlob(normalizeNoticeDownloadUrl(url)).then(res => {
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
  state.downloadLabel = '下载Word';
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

const escapeHtml = value =>
  String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

export const fileExtension = file => {
  const value = String((file && (file.fileName || file.name || file.fileUrl || file.url)) || '');
  const match = value.match(/\.([a-z0-9]+)(?:\?|#|$)/i);
  return match ? match[1].toLowerCase() : '';
};

export const buildAttachmentPreviewHtml = file => {
  const url = String((file && (file.fileUrl || file.url)) || '');
  const fileName = String((file && (file.fileName || file.name || file.agreementName)) || '附件');
  const ext = fileExtension(file);
  const safeUrl = escapeHtml(url);
  const safeName = escapeHtml(fileName);
  const sourceText = escapeHtml(file && (file.sourceName || file.remark || file.materialName || ''));
  const baseStyle = `
    <style>
      * { box-sizing: border-box; }
      body { margin: 0; padding: 24px; background: #f6f8fb; color: #303133; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
      .preview-wrap { min-height: calc(100vh - 48px); display: flex; align-items: center; justify-content: center; }
      .preview-card { width: 100%; min-height: 520px; border: 1px solid #e6eaf2; border-radius: 10px; background: #fff; box-shadow: 0 10px 30px rgba(16, 89, 198, .08); overflow: hidden; }
      .preview-head { padding: 16px 18px; border-bottom: 1px solid #edf0f5; background: #fff; }
      .preview-title { margin: 0; color: #1f2937; font-size: 16px; font-weight: 600; line-height: 1.5; word-break: break-all; }
      .preview-meta { margin-top: 6px; color: #8c98aa; font-size: 12px; }
      .preview-body { min-height: 450px; display: flex; align-items: center; justify-content: center; background: #f9fbff; }
      img { display: block; max-width: 100%; max-height: 68vh; object-fit: contain; }
      iframe { width: 100%; height: 68vh; border: 0; background: #fff; }
      .file-panel { width: min(520px, 92%); padding: 34px 28px; border: 1px dashed #c9d7ef; border-radius: 10px; background: #fff; text-align: center; }
      .file-icon { width: 54px; height: 54px; margin: 0 auto 16px; border-radius: 12px; background: #eaf2ff; color: #1059c6; display: flex; align-items: center; justify-content: center; font-weight: 700; }
      .file-name { margin: 0 0 8px; color: #303133; font-size: 16px; font-weight: 600; line-height: 1.5; word-break: break-all; }
      .file-desc { margin: 0; color: #8c98aa; font-size: 13px; line-height: 1.7; }
    </style>
  `;
  if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'].includes(ext)) {
    return `<!doctype html><html><head><meta charset="utf-8">${baseStyle}</head><body><div class="preview-wrap"><div class="preview-card"><div class="preview-head"><h1 class="preview-title">${safeName}</h1><div class="preview-meta">${sourceText}</div></div><div class="preview-body"><img src="${safeUrl}" alt="${safeName}" /></div></div></div></body></html>`;
  }
  if (ext === 'pdf') {
    return `<!doctype html><html><head><meta charset="utf-8">${baseStyle}</head><body><div class="preview-wrap"><div class="preview-card"><div class="preview-head"><h1 class="preview-title">${safeName}</h1><div class="preview-meta">${sourceText}</div></div><div class="preview-body"><iframe src="${safeUrl}" title="${safeName}"></iframe></div></div></div></body></html>`;
  }
  const typeText = ext ? ext.toUpperCase() : 'FILE';
  return `<!doctype html><html><head><meta charset="utf-8">${baseStyle}</head><body><div class="preview-wrap"><div class="file-panel"><div class="file-icon">${escapeHtml(typeText)}</div><p class="file-name">${safeName}</p><p class="file-desc">该文件格式不支持浏览器内嵌预览，可以点击下方“下载”查看原文件。</p></div></div></body></html>`;
};

export const openAttachmentPreview = (state, file, title = '附件预览') => {
  const downloadUrl = (file && (file.fileUrl || file.url)) || '';
  state.visible = true;
  state.loading = false;
  state.title = title;
  state.html = buildAttachmentPreviewHtml(file);
  state.downloadUrl = downloadUrl;
  state.fallbackName = (file && (file.fileName || file.name || file.agreementName)) || '附件';
  state.downloadLabel = '下载';
};

export const closeNoticePreview = state => {
  state.visible = false;
  state.loading = false;
  state.title = '审批表预览';
  state.html = '';
  state.downloadUrl = '';
  state.fallbackName = '';
  state.downloadLabel = '下载Word';
};
