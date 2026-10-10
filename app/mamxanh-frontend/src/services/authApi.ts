import { apiClient } from '../lib/apiClient';

// Runtime API contract: generated OpenAPI from the Backend; manual YAML is planned reference only during migration.

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

export type PasswordResetConfirmPayload = { token: string; newPassword: string; confirmPassword: string };

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

export async function requestPasswordReset(email: string): Promise<MessageResponse> {
  const { data } = await apiClient.post<MessageResponse>('/auth/password-resets', { email });
  return data;
}

export async function confirmPasswordReset(payload: PasswordResetConfirmPayload): Promise<void> {
  await apiClient.post('/auth/password-resets/confirm', payload);
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', payload);
  return data;
}

/** UC-03.5: exchanges the Google ID Token from Google Identity Services for a Mâm Xanh access token. */
export async function googleLogin(idToken: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/google', { idToken });
  return data;
}
