-- Opaque short-lived capability for opening Razorpay checkout in the system browser.
CREATE TABLE payment_checkout_sessions (
  token_hash VARCHAR(64) PRIMARY KEY,
  payment_order_id UUID NOT NULL REFERENCES payment_orders(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_checkout_sessions_order ON payment_checkout_sessions(payment_order_id);
CREATE INDEX idx_checkout_sessions_expiry ON payment_checkout_sessions(expires_at);
