import request from '@/axios';
import { baseUrl } from '@/config/env';

export const getNoticePreview = params => {
  return request({
    url: '/blade-contract/print/preview',
    method: 'get',
    params,
  });
};

export const noticePrintUrl = (noticeType, { paymentId, contractId } = {}) => {
  const routeMap = {
    'payment-notice': paymentId ? `${baseUrl}/blade-contract/print/payment-notice/${paymentId}` : '',
    'invoice-apply': paymentId ? `${baseUrl}/blade-contract/print/invoice-apply/${paymentId}` : '',
    'reminder-notice': paymentId ? `${baseUrl}/blade-contract/print/reminder-notice/${paymentId}` : '',
    'contract-approval': contractId ? `${baseUrl}/blade-contract/print/contract-approval/${contractId}` : '',
    'contract-fixed': contractId ? `${baseUrl}/blade-contract/print/contract-text/fixed/${contractId}` : '',
    'contract-floating': contractId ? `${baseUrl}/blade-contract/print/contract-text/floating/${contractId}` : '',
    'project-approval': paymentId ? `${baseUrl}/blade-contract/print/project-approval/${paymentId}` : '',
    'overdue-notice': paymentId ? `${baseUrl}/blade-contract/print/overdue-notice/${paymentId}` : '',
    'legal-letter': paymentId ? `${baseUrl}/blade-contract/print/legal-letter/${paymentId}` : '',
    'move-out-notice': paymentId
      ? `${baseUrl}/blade-contract/print/move-out-notice/payment/${paymentId}`
      : contractId
        ? `${baseUrl}/blade-contract/print/move-out-notice/${contractId}`
        : '',
    'termination-approval': contractId ? `${baseUrl}/blade-contract/print/termination-approval/${contractId}` : '',
    'termination-agreement': contractId ? `${baseUrl}/blade-contract/print/termination-agreement/${contractId}` : '',
    'room-review': contractId ? `${baseUrl}/blade-contract/print/room-review/${contractId}` : '',
  };
  return routeMap[noticeType] || '';
};
