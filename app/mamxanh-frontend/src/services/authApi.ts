import { apiClient } from '../lib/apiClient';

// Contract: docs/api/openapi.yaml (tag Authentication).

export type RegisterPayload = {
  displayName: string;
  email: string;
  password: string;
  confirmPassword: string;
};

export type RegistrationResponse = {
  accountStatus: 'ACTIVE';
  emailVerified: false;
  message: string;
};

export type MessageResponse = { message: string };

export type LoginPayload = { email: string; password: string };

export type AccountSummary = {
  id: number;
  displayName: string;
  email: string;
  avatarUrl?: string | null;
  role: 'CUSTOMER' | 'EXPERT' | 'ADMIN';
  accountStatus: 'ACTIVE';
  emailVerified: true;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  expiresInSeconds: number;
  account: AccountSummary;
};

export async function register(payload: RegisterPayload): Promise<RegistrationResponse> {
  const { data } = await apiClient.post<RegistrationResponse>('/auth/register', payload);
  return data;
}

export async function verifyEmail(token: string): Promise<void> {
  await apiClient.post('/auth/email-verifications', { token });
}

export async function resendVerificationEmail(email: string): Promise<MessageResponse> {
  const { data } = await apiClient.post<MessageResponse>('/auth/email-verifications/resend', { email });
  return data;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', payload);
  return data;
}
