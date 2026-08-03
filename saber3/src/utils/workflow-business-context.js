import { getCustomerDetail } from '@/api/business/customer';
import { getDetail as getContractDetail } from '@/api/contract/contract';

function mergePresent(base = {}, override = {}) {
  return Object.entries(override || {}).reduce(
    (result, [key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        result[key] = value;
      }
      return result;
    },
    { ...(base || {}) }
  );
}

export async function enrichContractWorkflowContext(source = {}) {
  let context = { ...(source || {}) };

  if (context.contractId) {
    try {
      const response = await getContractDetail(context.contractId);
      context = mergePresent((response.data && response.data.data) || {}, context);
    } catch (error) {
      // Keep the entry-page data available when the detail endpoint is temporarily unavailable.
    }
  }

  let customer = {};
  if (context.customerId) {
    try {
      const response = await getCustomerDetail(context.customerId);
      customer = (response.data && response.data.data) || {};
    } catch (error) {
      customer = {};
    }
  }

  const customerName =
    context.customerName || customer.enterpriseName || context.contractName || '';
  const contactName =
    customer.contactName || customer.approvalContactName || context.contactName || '';
  const contactPhone = customer.contactPhone || context.contactPhone || '';

  return {
    ...context,
    customerId: context.customerId || customer.customerId,
    customerName,
    enterpriseName: customerName,
    tenantName: customerName,
    lesseeName: customerName,
    contactName,
    contactPhone,
    customerPhone: contactPhone,
    applicantContactPhone: contactPhone,
    creditCode: customer.creditCode || context.creditCode || '',
    registeredAddress:
      customer.registeredAddress || customer.address || context.registeredAddress || '',
    contactEmail: customer.contactEmail || context.contactEmail || '',
  };
}
