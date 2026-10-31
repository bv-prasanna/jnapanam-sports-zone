import * as WebBrowser from 'expo-web-browser';
import { API_URL, api } from '@/lib/api';

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

async function processOrder(order: PaymentOrder): Promise<PaymentOrder> {
  if (order.provider === 'MOCK') {
    return api<PaymentOrder>('/api/v1/payments/verify', {
      method: 'POST',
      body: JSON.stringify({
        paymentOrderId: order.id,
        providerPaymentId: `pay_mobile_${Date.now()}`,
        signature: 'mock-signature',
      }),
    });
  }
  if (order.provider !== 'RAZORPAY') {
    throw new Error('Unsupported payment gateway. Please contact support.');
  }

  const session = await api<{ path: string }>(`/api/v1/payments/${order.id}/checkout-session`, {
    method: 'POST',
  });
  if (!session.path.startsWith('/api/v1/public/payments/checkout/')) {
    throw new Error('Invalid secure checkout session');
  }
  await WebBrowser.openBrowserAsync(`${API_URL}${session.path}`, {
    showTitle: true,
  });

  // The Razorpay callback is verified on the server. Never trust the browser-close
  // event as evidence of payment; webhooks can also settle the transaction later.
  const receipt = await api<PaymentOrder>(`/api/v1/payments/${order.id}/receipt`);
  if (receipt.status !== 'PAID') {
    throw new Error('Payment not confirmed yet. Refresh payment status before trying again.');
  }
  return receipt;
}

export async function completePayment(purpose: PaymentPurpose, referenceId: string) {
  const order = await api<PaymentOrder>('/api/v1/payments/orders', {
    method: 'POST',
    headers: { 'Idempotency-Key': idempotencyKey(purpose) },
    body: JSON.stringify({ purpose, referenceId, couponCode: null, loyaltyPoints: 0 }),
  });
  return processOrder(order);
}
