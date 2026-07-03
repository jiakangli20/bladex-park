const escapeHtml = value =>
  `${value || ''}`.replace(/[&<>"']/g, char => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[char]));

const isFullHtml = html => /<!doctype|<html[\s>]/i.test(html || '');

export const printHtml = (html, title = '打印') => {
  if (!html) return false;
  const iframe = document.createElement('iframe');
  iframe.setAttribute('aria-hidden', 'true');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';

  const content = isFullHtml(html)
    ? html
    : `<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(title)}</title></head><body>${html}</body></html>`;

  const removeFrame = () => {
    setTimeout(() => {
      if (iframe.parentNode) {
        iframe.parentNode.removeChild(iframe);
      }
    }, 500);
  };

  iframe.onload = () => {
    const frameWindow = iframe.contentWindow;
    if (!frameWindow) {
      removeFrame();
      return;
    }
    frameWindow.onafterprint = removeFrame;
    setTimeout(() => {
      frameWindow.focus();
      frameWindow.print();
      setTimeout(removeFrame, 3000);
    }, 50);
  };

  document.body.appendChild(iframe);
  const doc = iframe.contentDocument || iframe.contentWindow.document;
  doc.open();
  doc.write(content);
  doc.close();
  return true;
};
