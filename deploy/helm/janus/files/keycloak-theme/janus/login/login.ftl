<#assign usernameValue = (auth.attemptedUsername)!((login.username)!'')>
<#assign hasFieldError = message?has_content && message.type == 'error'>
<!doctype html>
<html lang="${(locale.currentLanguageTag)!'es'}">
<head>
  <meta charset="utf-8">
  <meta name="robots" content="noindex, nofollow">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${msg("janusLoginPageTitle")}</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/login.css">
</head>
<body class="janus-login">
  <main class="login-shell">
    <section class="login-card" aria-labelledby="login-title">

      <h1 id="brand">Nivel 36</h1>
      <p class="subtitle">${msg("janusLoginSubtitle")}</p>

      <#if hasFieldError>
        <div class="login-alert" role="alert">${kcSanitize(message.summary)?no_esc}</div>
      </#if>

      <form id="kc-form-login" class="login-form" action="${url.loginAction}" method="post">
        <#if usernameEditDisabled?? && usernameEditDisabled>
          <input type="hidden" name="username" value="${usernameValue}">
        <#else>
          <label class="field">
            <span>${msg("janusEmailLabel")}</span>
            <input
              id="username"
              name="username"
              type="text"
              value="${usernameValue}"
              autocomplete="username"
              autofocus
              aria-invalid="${hasFieldError?string('true','false')}"
            >
          </label>
        </#if>

        <label class="field">
          <span>${msg("janusPasswordLabel")}</span>
          <span class="password-control">
            <input
              id="password"
              name="password"
              type="password"
              autocomplete="current-password"
              aria-invalid="${hasFieldError?string('true','false')}"
            >
            <button
              class="password-toggle"
              type="button"
              aria-label="${msg("janusShowPassword")}"
              data-show-label="${msg("janusShowPassword")}"
              data-hide-label="${msg("janusHidePassword")}"
              data-password-toggle
            >
              <svg viewBox="0 0 24 24" focusable="false">
                <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"/>
                <circle cx="12" cy="12" r="3"/>
              </svg>
            </button>
          </span>
        </label>

        <#if selectedCredential?has_content>
          <input type="hidden" name="credentialId" value="${selectedCredential}">
        </#if>

        <button class="submit-button" type="submit">${msg("janusSubmit")}</button>
      </form>

      <nav class="login-links" aria-label="${msg("janusAccessHelp")}">
        <span class="rule" aria-hidden="true"></span>
        <a href="mailto:soporte@nivel36.es">
          <svg viewBox="0 0 24 24" focusable="false">
            <path d="M4 13a8 8 0 0 1 16 0v4a3 3 0 0 1-3 3h-1v-7h4M4 17a3 3 0 0 0 3 3h1v-7H4v4Z"/>
          </svg>
          ${msg("janusSupport")}
        </a>
        <span class="divider" aria-hidden="true"></span>
        <#if realm.resetPasswordAllowed?? && realm.resetPasswordAllowed>
          <a href="${url.loginResetCredentialsUrl}">
            <svg viewBox="0 0 24 24" focusable="false">
              <rect x="5" y="11" width="14" height="10" rx="2"/>
              <path d="M8 11V8a4 4 0 0 1 8 0v3"/>
              <path d="M12 15v2"/>
            </svg>
            ${msg("janusRecoverAccess")}
          </a>
        <#else>
          <span class="disabled-link">
            <svg viewBox="0 0 24 24" focusable="false">
              <rect x="5" y="11" width="14" height="10" rx="2"/>
              <path d="M8 11V8a4 4 0 0 1 8 0v3"/>
              <path d="M12 15v2"/>
            </svg>
            ${msg("janusRecoverAccess")}
          </span>
        </#if>
        <span class="rule" aria-hidden="true"></span>
      </nav>
    </section>

    <section class="security-panel" aria-label="${msg("janusSecurityFeatures")}">
      <article>
        <svg viewBox="0 0 24 24" focusable="false">
          <path d="M12 3 4 6v6c0 5 3.4 8.5 8 10 4.6-1.5 8-5 8-10V6l-8-3Z"/>
          <path d="m9 12 2 2 4-5"/>
        </svg>
        <div>
          <h2>${msg("janusSecureSsoTitle")}</h2>
          <p>${msg("janusSecureSsoDescription")}</p>
        </div>
      </article>
      <article>
        <svg viewBox="0 0 24 24" focusable="false">
          <rect x="5" y="10" width="14" height="11" rx="2"/>
          <path d="M8 10V7a4 4 0 0 1 8 0v3"/>
        </svg>
        <div>
          <h2>${msg("janusEncryptedSessionTitle")}</h2>
          <p>${msg("janusEncryptedSessionDescription")}</p>
        </div>
      </article>
      <article>
        <svg viewBox="0 0 24 24" focusable="false">
          <circle cx="9" cy="8" r="3"/>
          <path d="M3 20a6 6 0 0 1 12 0"/>
          <circle cx="17" cy="10" r="2.5"/>
          <path d="M15 16.2A5 5 0 0 1 21 20"/>
        </svg>
        <div>
          <h2>${msg("janusManagedAccessTitle")}</h2>
          <p>${msg("janusManagedAccessDescription")}</p>
        </div>
      </article>
    </section>
  </main>

  <script>
    const toggle = document.querySelector('[data-password-toggle]');
    const password = document.querySelector('#password');
    toggle?.addEventListener('click', () => {
      const visible = password.type === 'text';
      password.type = visible ? 'password' : 'text';
      toggle.setAttribute('aria-label', visible ? toggle.dataset.showLabel : toggle.dataset.hideLabel);
    });
  </script>
</body>
</html>
