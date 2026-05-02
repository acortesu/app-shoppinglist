import { useEffect, useState } from 'react';

export function AuthGate({ onLogin, googleClientId }) {
  const [localError, setLocalError] = useState('');
  const [googleReady, setGoogleReady] = useState(false);
  const googleBtnId = 'google-signin-button';

  useEffect(() => {
    if (!googleClientId) {
      return;
    }

    const initGoogle = () => {
      if (!window.google?.accounts?.id) {
        return;
      }

      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: (response) => {
          if (!response?.credential) {
            setLocalError('Google no devolvió credencial. Intenta nuevamente.');
            return;
          }
          onLogin(response.credential);
        },
        auto_select: false,
        cancel_on_tap_outside: true,
        ux_mode: 'popup',
        itp_support: true
      });

      const btnContainer = document.getElementById(googleBtnId);
      if (btnContainer) {
        btnContainer.innerHTML = '';
        window.google.accounts.id.renderButton(btnContainer, {
          type: 'standard',
          theme: 'filled_black',
          size: 'large',
          text: 'continue_with',
          shape: 'pill',
          width: 320
        });
        setGoogleReady(true);
      }

      window.google.accounts.id.prompt((notification) => {
        if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
          console.debug('Google One Tap not active:', {
            notDisplayed: notification.isNotDisplayed?.(),
            skipped: notification.isSkippedMoment?.(),
            reason: notification.getNotDisplayedReason?.() || notification.getSkippedReason?.()
          });
        }
      });
    };

    if (window.google?.accounts?.id) {
      initGoogle();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = initGoogle;
    script.onerror = () => setLocalError('No se pudo cargar Google Sign-In.');
    document.head.appendChild(script);
  }, [googleClientId, onLogin]);

  return (
    <div className="auth-gate">
      <div className="brand-stripe-auth" />

      <div className="auth-card">
        <h1>AppCompras</h1>
        <p>Planifica tus compras con recetas.</p>

        {googleClientId ? (
          <div className="google-zone">
            <div id={googleBtnId} />
            {!googleReady && <p className="muted tiny">Cargando Google Sign-In...</p>}
          </div>
        ) : (
          <p className="auth-error">
            Falta <code>VITE_GOOGLE_CLIENT_ID</code> en <code>frontend/.env</code>.
          </p>
        )}
        {localError && <p className="auth-error">{localError}</p>}
      </div>
    </div>
  );
}
