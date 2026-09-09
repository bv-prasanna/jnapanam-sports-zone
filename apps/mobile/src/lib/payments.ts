import { api } from '@/lib/api';

export type PaymentPurpose = 'BOOKING' | 'MEMBERSHIP' | 'TRAINING' | 'TOURNAMENT';

type PaymentOrder = {
  id: string;
  provider: string;
  status: string;
  amountPaise: number;
};

function idempotencyKey(purpose: PaymentPurpose) {
  return `mobile-${purpose.toLowerCase()}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export async function completePayment(purpose: PaymentPurpose, referenceId: string) {
  const order = await api<PaymentOrder>('/api/v1/payments/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey(purpose) },
    body: JSON.stringify({ purpose, referenceId, couponCode: null, loyaltyPoints: 0 }),
  });
  if (order.provider !== 'MOCK') {
    throw new Error('Online payment requires the production payment application build.');
  }
  await api('/api/v1/payments/verify', {
    method: 'POST',
    body: JSON.stringify({
      paymentOrderId: order.id,
      providerPaymentId: `pay_mobile_${Date.now()}`,
      signature: 'mock-signature',
    }),
  });
  return order;
}
