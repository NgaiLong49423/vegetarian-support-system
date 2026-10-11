import { useState } from 'react';
import { GoogleLogin, GoogleOAuthProvider } from '@react-oauth/google';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim() ?? '';

type Props = {
  /** Google ID Token returned by Google Identity Services; the Backend verifies it (UC-03.5). */
  onCredential: (idToken: string) => void;
  /** Google closed or failed the sign-in without returning a credential. */
  onFailure: () => void;
};

/**
 * "Continue with Google" button. The Google script is loaded only on the pages that render this component,
 * and only when VITE_GOOGLE_CLIENT_ID is configured; email sign-in stays available in every other case.
 */
export function GoogleSignIn({ onCredential, onFailure }: Props) {
  const [scriptFailed, setScriptFailed] = useState(false);
  if (!GOOGLE_CLIENT_ID) {
    return <p className="mt-6 rounded-xl border border-brand-100 bg-brand-50 p-3 text-center text-xs text-ink-muted">Đăng nhập Google chưa được cấu hình cho môi trường này.</p>;
  }
  if (scriptFailed) {
    return <p role="status" className="mt-6 rounded-xl border border-brand-100 bg-brand-50 p-3 text-center text-xs text-ink-muted">Không tải được đăng nhập Google. Bạn vẫn có thể đăng nhập bằng email.</p>;
  }
  return <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID} locale="vi" onScriptLoadError={() => setScriptFailed(true)}>
    <div className="mt-6 flex min-h-11 justify-center">
      <GoogleLogin
        text="continue_with"
        shape="rectangular"
        width="320"
        onSuccess={response => response.credential ? onCredential(response.credential) : onFailure()}
        onError={onFailure}
      />
    </div>
  </GoogleOAuthProvider>;
}
