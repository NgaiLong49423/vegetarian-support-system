/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** REST API base URL including the version prefix, e.g. http://localhost:8080/api/v1 */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
