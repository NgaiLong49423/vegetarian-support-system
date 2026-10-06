import type { Page } from '@playwright/test';

/** Test-only server-session fixture; production UI has no fake account or role switch. */
export async function seedDemoSession(page: Page, role: 'CUSTOMER' | 'EXPERT' | 'ADMIN' = 'CUSTOMER') {
  await page.addInitScript((selectedRole) => {
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: 'header.payload.signature',
      expiresAt: Date.now() + 3_600_000,
      account: {
        id: 901,
        displayName: `Demo ${selectedRole}`,
        email: `demo-${selectedRole.toLowerCase()}@mamxanh.local`,
        role: selectedRole,
        accountStatus: 'ACTIVE',
        emailVerified: true,
      },
    }));
  }, role);
}
