import { Platform } from 'react-native';
import { getEventLocationHeaders } from './event-location';

export type TokenResponse = {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
};

export type User = {
  id: string;
  fullName: string;
  email: string;
  mobile?: string;
  status: string;
  roles: string[];
  requiresUpfrontPayment: boolean;
};

const defaultHost = Platform.OS === 'android' ? '10.0.2.2' : 'localhost';
export const API_URL = (process.env.EXPO_PUBLIC_API_URL || `http://${defaultHost}:8080`).replace(/\/$/, '');

let accessToken: string | null = null;
export function setAccessToken(token: string | null) { accessToken = token; }

export class ApiError extends Error {
  constructor(message: string, public status: number) { super(message); }
}

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method || 'GET').toUpperCase();
  const geoHeaders = ['POST','PUT','PATCH','DELETE'].includes(method)
    ? await getEventLocationHeaders() : {};
  const publicAuth = path.startsWith('/api/v1/auth/');
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      Accept: 'application/json',
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(!publicAuth && accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...geoHeaders,
      ...options.headers,
    },
  });

  if (!response.ok) {
    let message = 'Something went wrong. Please try again.';
    try {
      const body = await response.json();
      message = body.message || body.detail || message;
    } catch {}
    throw new ApiError(message, response.status);
  }
  if (response.status === 204) return undefined as T;
  const body = await response.text();
  if (!body.trim()) return undefined as T;
  return JSON.parse(body) as T;
}
