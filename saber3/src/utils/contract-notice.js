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
  objectUrl: '',
  previewType: 'html',
  documentBlob: null,
  pdfBlob: null,
  pdfFileName: '',
  previewError: '',
});

const releasePreviewObjectUrl = state => {
  if (state && state.objectUrl && typeof URL !== 'undefined') {
    URL.revokeObjectURL(state.objectUrl);
  }
  if (state) {
    state.objectUrl = '';
    state.pdfBlob = null;
    state.pdfFileName = '';
  }
};

const decodeDownloadFilename = value => {
  const normalized = String(value || '')
    .trim()
    .replace(/^['"]|['"]$/g, '');
  if (!normalized) return '';
  try {
    return decodeURIComponent(normalized);
  } catch {
    return normalized;
  }
};

const dispositionFilename = disposition => {
  if (!disposition) return '';
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match && utf8Match[1]) {
    return decodeDownloadFilename(utf8Match[1]);
  }
  const filenameMatch = disposition.match(/filename="?([^";]+)"?/i);
  return filenameMatch && filenameMatch[1] ? decodeDownloadFilename(filenameMatch[1]) : '';
};

export const resolveDownloadFilename = (disposition, fallbackName) => {
  const preferredName = decodeDownloadFilename(fallbackName);
  const responseName = dispositionFilename(disposition);
  if (!preferredName) return responseName || '下载文件';
  if (/\.[a-z0-9]{1,10}$/i.test(preferredName)) return preferredName;
  const extension = responseName.match(/(\.[a-z0-9]{1,10})$/i);
  return extension ? `${preferredName}${extension[1]}` : preferredName;
};

export const resolvePdfDownloadFilename = fileName => {
  const decodedName = decodeDownloadFilename(fileName);
  const baseName = decodedName.replace(/\.[a-z0-9]{1,10}$/i, '') || '审批文件';
  return `${baseName}.pdf`;
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
  releasePreviewObjectUrl(state);
  state.loading = true;
  state.visible = true;
  state.title = title || '审批表预览';
  state.html = '';
  state.downloadUrl = downloadUrl || '';
  state.fallbackName = fallbackName || '审批文件';
  state.downloadLabel = '下载Word';
  state.previewType = 'html';
  state.documentBlob = null;
  state.pdfBlob = null;
  state.pdfFileName = '';
  state.previewError = '';
  return getNoticePreview(params)
    .then(res => {
      const data = res.data.data || {};
      state.title = data.noticeName || title || '审批表预览';
      if (data.fileName) {
        state.fallbackName = data.fileName;
      }
      if (!data.pdfUrl) {
        state.html = data.html || '';
        return null;
      }
      return downloadBlob(data.pdfUrl).then(pdfResponse => {
        const rawPdfBlob =
          pdfResponse.data instanceof Blob
            ? pdfResponse.data
            : new Blob([pdfResponse.data], { type: 'application/pdf' });
        state.pdfFileName = resolvePdfDownloadFilename(data.fileName || state.fallbackName);
        state.pdfBlob =
          typeof File === 'undefined'
            ? rawPdfBlob
            : new File([rawPdfBlob], state.pdfFileName, { type: 'application/pdf' });
        state.objectUrl = URL.createObjectURL(state.pdfBlob);
        const placeholder = data.pdfPlaceholder || '__BLADEX_OFFICE_PREVIEW_PDF__';
        state.html = String(data.html || '')
          .split(placeholder)
          .join(`${state.objectUrl}#toolbar=0&navpanes=0`);
      });
    })
    .catch(error => {
      releasePreviewObjectUrl(state);
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
  const explicitType = String((file && (file.fileType || file.extension)) || '')
    .replace(/^\./, '')
    .toLowerCase();
  if (/^[a-z0-9]+$/.test(explicitType)) return explicitType;
  const candidates = [file && (file.fileName || file.name), file && (file.fileUrl || file.url)];
  for (const candidate of candidates) {
    const match = String(candidate || '').match(/\.([a-z0-9]+)(?:\?|#|$)/i);
    if (match) return match[1].toLowerCase();
  }
  return '';
};

export const buildAttachmentPreviewHtml = file => {
  const url = String((file && (file.fileUrl || file.url)) || '');
  const fileName = String((file && (file.fileName || file.name || file.agreementName)) || '附件');
  const ext = fileExtension(file);
  const safeUrl = escapeHtml(url);
  const safeName = escapeHtml(fileName);
  const sourceText = escapeHtml(
    file && (file.sourceName || file.remark || file.materialName || '')
  );
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
  return `<!doctype html><html><head><meta charset="utf-8">${baseStyle}</head><body><div class="preview-wrap"><div class="file-panel"><div class="file-icon">${escapeHtml(
    typeText
  )}</div><p class="file-name">${safeName}</p><p class="file-desc">该文件格式不支持浏览器内嵌预览，可以点击下方“下载”查看原文件。</p></div></div></body></html>`;
};

export const openAttachmentPreview = (state, file, title = '附件预览') => {
  releasePreviewObjectUrl(state);
  const downloadUrl = (file && (file.fileUrl || file.url)) || '';
  const ext = fileExtension(file);
  const inlinePreviewExtensions = ['pdf', 'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
  state.visible = true;
  state.loading = ext === 'docx' || inlinePreviewExtensions.includes(ext);
  state.title = title;
  state.html = '';
  state.downloadUrl = downloadUrl;
  state.fallbackName = (file && (file.fileName || file.name || file.agreementName)) || '附件';
  state.downloadLabel = '下载';
  state.previewType = ext === 'docx' ? 'docx' : 'html';
  state.documentBlob = null;
  state.pdfBlob = null;
  state.pdfFileName = '';
  state.previewError = '';
  if (ext !== 'docx' && !inlinePreviewExtensions.includes(ext)) {
    state.html = buildAttachmentPreviewHtml(file);
    return Promise.resolve();
  }
  return downloadBlob(normalizeNoticeDownloadUrl(downloadUrl))
    .then(res => {
      const contentType =
        (res.headers && res.headers['content-type']) ||
        (ext === 'docx'
          ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
          : ext === 'pdf'
          ? 'application/pdf'
          : `image/${ext === 'jpg' ? 'jpeg' : ext}`);
      const blob =
        res.data instanceof Blob ? res.data : new Blob([res.data], { type: contentType });
      if (ext === 'docx') {
        state.documentBlob = blob;
        return;
      }
      state.objectUrl = URL.createObjectURL(blob);
      state.html = buildAttachmentPreviewHtml({
        ...file,
        fileUrl: state.objectUrl,
        fileType: ext,
      });
    })
    .catch(() => {
      state.previewError =
        ext === 'docx'
          ? 'Word 文件读取失败，可以下载原文件后查看。'
          : '文件读取失败，可以下载原文件后查看。';
    })
    .finally(() => {
      state.loading = false;
    });
};

export const closeNoticePreview = state => {
  releasePreviewObjectUrl(state);
  state.visible = false;
  state.loading = false;
  state.title = '审批表预览';
  state.html = '';
  state.downloadUrl = '';
  state.fallbackName = '';
  state.downloadLabel = '下载Word';
  state.previewType = 'html';
  state.documentBlob = null;
  state.pdfBlob = null;
  state.pdfFileName = '';
  state.previewError = '';
};
