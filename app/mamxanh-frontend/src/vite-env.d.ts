/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** REST API base URL including the version prefix, e.g. http://localhost:8080/api/v1 */
  readonly VITE_API_BASE_URL?: string;
  /** Public Google OAuth client ID for Google Identity Services (UC-03.5); empty hides Google sign-in. */
  readonly VITE_GOOGLE_CLIENT_ID?: string;
  /** Enabled only by the Playwright coverage build; never enable this for normal development or deployment. */
  readonly VITE_COVERAGE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
