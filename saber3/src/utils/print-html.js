const escapeHtml = value =>
  `${value || ''}`.replace(
    /[&<>"']/g,
    char =>
      ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;',
      }[char])
  );

const isFullHtml = html => /<!doctype|<html[\s>]/i.test(html || '');

const extractPdfDataUrl = html => {
  const matched = `${html || ''}`.match(
    /data=["'](data:application\/pdf;base64,[A-Za-z0-9+/=]+)["']/i
  );
  return matched ? matched[1] : '';
};

const printPdf = dataUrl => {
  const commaIndex = dataUrl.indexOf(',');
  if (commaIndex < 0) return false;
  const binary = window.atob(dataUrl.slice(commaIndex + 1));
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index += 1) {
    bytes[index] = binary.charCodeAt(index);
  }
  const objectUrl = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
  const iframe = document.createElement('iframe');
  iframe.setAttribute('aria-hidden', 'true');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';

  const cleanup = () => {
    setTimeout(() => {
      URL.revokeObjectURL(objectUrl);
      if (iframe.parentNode) iframe.parentNode.removeChild(iframe);
    }, 1000);
  };
  iframe.onload = () => {
    try {
      const frameWindow = iframe.contentWindow;
      if (!frameWindow) {
        cleanup();
        return;
      }
      frameWindow.onafterprint = cleanup;
      setTimeout(() => {
        frameWindow.focus();
        frameWindow.print();
      }, 200);
    } catch (error) {
      cleanup();
    }
  };
  iframe.src = objectUrl;
  document.body.appendChild(iframe);
  return true;
};

export const printHtml = (html, title = '打印') => {
  if (!html) return false;
  const pdfDataUrl = extractPdfDataUrl(html);
  if (pdfDataUrl) return printPdf(pdfDataUrl);
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
    : `<!doctype html><html><head><meta charset="utf-8"><title>${escapeHtml(
        title
      )}</title></head><body>${html}</body></html>`;

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
