import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'

const TOKEN_KEY = 'secure-auth-token'
const USER_KEY = 'secure-auth-user'
const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/+$/, '')
const TIMEOUT_MS = 12000

function currentRoute() {
  const [path, query = ''] = window.location.hash.slice(1).split('?')

  return {
    path: path || '/login',
    query: new URLSearchParams(query),
  }
}

function readSession() {
  for (const storage of [sessionStorage, localStorage]) {
    const token = storage.getItem(TOKEN_KEY)
    const rawUser = storage.getItem(USER_KEY)

    if (token && rawUser) {
      try {
        return { token, user: JSON.parse(rawUser) }
      } catch {
        storage.removeItem(TOKEN_KEY)
        storage.removeItem(USER_KEY)
      }
    }
  }

  return null
}

function clearSession() {
  for (const storage of [sessionStorage, localStorage]) {
    storage.removeItem(TOKEN_KEY)
    storage.removeItem(USER_KEY)
  }
}

async function api(path, { method = 'GET', body, token, signal } = {}) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS)

  const abort = () => controller.abort()
  signal?.addEventListener('abort', abort, { once: true })

  try {
    const headers = { Accept: 'application/json' }

    if (body !== undefined) {
      headers['Content-Type'] = 'application/json'
    }

    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    })

    const contentType = response.headers.get('content-type') || ''

    const data =
        response.status === 204
            ? null
            : contentType.includes('json')
                ? await response.json().catch(() => null)
                : await response.text().catch(() => '')

    if (!response.ok) {
      const message =
          data && typeof data === 'object'
              ? data.message || data.error || data.detail
              : data

      const error = new Error(
          message || `Request failed (${response.status})`
      )

      error.status = response.status
      throw error
    }

    return data
  } finally {
    clearTimeout(timeout)
    signal?.removeEventListener('abort', abort)
  }
}

function errorText(error) {
  if (error.name === 'AbortError') {
    return 'The request timed out. Please try again.'
  }

  if (error instanceof TypeError) {
    return 'Could not reach the API. Check your backend and VITE_API_BASE_URL.'
  }

  return error.message || 'Something went wrong. Please try again.'
}

function normalizeRole(value = '') {
  return String(value).replace(/^ROLE_/, '').toUpperCase()
}

function getEmailVerified(profile) {
  return profile?.emailVerified === true
}

function App() {
  const [routeState, setRouteState] = useState(currentRoute)
  const { path: route, query } = routeState

  const [session, setSession] = useState(readSession)
  const [profile, setProfile] = useState(null)
  const [dashboard, setDashboard] = useState(null)
  const [users, setUsers] = useState([])
  const [busy, setBusy] = useState(false)
  const [otpSending, setOtpSending] = useState(false)
  const [notice, setNotice] = useState(null)

  const navigate = useCallback((path, params = {}) => {
    const queryString = new URLSearchParams(params).toString()

    window.location.hash =
        `${path}${queryString ? `?${queryString}` : ''}`
  }, [])

  useEffect(() => {
    const onHashChange = () => {
      setRouteState(currentRoute())
      setNotice(null)
    }

    window.addEventListener('hashchange', onHashChange)

    return () => {
      window.removeEventListener('hashchange', onHashChange)
    }
  }, [])

  // Load authoritative profile data from the backend.
  useEffect(() => {
    if (!session?.token) {
      setProfile(null)
      setDashboard(null)
      setUsers([])
      return
    }

    const controller = new AbortController()

    async function loadProfile() {
      setBusy(true)

      try {
        const data = await api('/auth/profile', {
          token: session.token,
          signal: controller.signal,
        })

        setProfile(data)
      } catch (error) {
        if (error.name === 'AbortError') return

        if (error.status === 401 || error.status === 403) {
          clearSession()
          setSession(null)
          setProfile(null)
          setDashboard(null)
          setUsers([])

          setNotice({
            type: 'error',
            text: 'Your session has expired. Please sign in again.',
          })

          navigate('/login')
        } else {
          setNotice({ type: 'error', text: errorText(error) })
        }
      } finally {
        if (!controller.signal.aborted) {
          setBusy(false)
        }
      }
    }

    loadProfile()

    return () => controller.abort()
  }, [session?.token, navigate])

  const role = normalizeRole(
      profile?.role || session?.user?.role || 'USER'
  )

  // Redirect signed-in users away from authentication pages.
  useEffect(() => {
    if (!session?.token || !profile) return

    if (
        ['/login', '/register', '/forgot-password'].includes(route)
    ) {
      navigate('/profile')
    }
  }, [session, profile, role, route, navigate])

  // Admin-only dashboard data.
  useEffect(() => {
    if (
        !session?.token ||
        !profile ||
        route !== '/admin/dashboard'
    ) {
      return
    }

    if (role !== 'ADMIN') {
      setNotice({
        type: 'error',
        text: 'Administrator access is required.',
      })

      navigate('/profile')
      return
    }

    const controller = new AbortController()

    async function loadAdminData() {
      setBusy(true)

      try {
        const [stats, list] = await Promise.all([
          api('/api/admin/dashboard', {
            token: session.token,
            signal: controller.signal,
          }),
          api('/api/admin/getUsers', {
            token: session.token,
            signal: controller.signal,
          }),
        ])

        setDashboard(stats)
        setUsers(Array.isArray(list) ? list : [])
      } catch (error) {
        if (error.name === 'AbortError') return

        if (error.status === 401 || error.status === 403) {
          setNotice({
            type: 'error',
            text: 'You do not have permission to view the admin dashboard.',
          })
        } else {
          setNotice({ type: 'error', text: errorText(error) })
        }
      } finally {
        if (!controller.signal.aborted) {
          setBusy(false)
        }
      }
    }

    loadAdminData()

    return () => controller.abort()
  }, [session?.token, profile, role, route, navigate])

  async function submit(event, action) {
    event.preventDefault()

    if (busy) return

    const form = event.currentTarget
    const values = Object.fromEntries(new FormData(form))

    setBusy(true)
    setNotice(null)

    try {
      if (action === 'register') {
        const result = await api('/auth/register', {
          method: 'POST',
          body: {
            fullName: String(values.fullName || '').trim(),
            email: String(values.email || '').trim().toLowerCase(),
            password: String(values.password || ''),
          },
        })

        setNotice({
          type: 'success',
          text:
              result?.message ||
              'Registration successful. Sign in, then request a 6-digit email verification OTP.',
        })

        form.reset()
      }

      if (action === 'login') {
        const result = await api('/auth/login', {
          method: 'POST',
          body: {
            email: String(values.email || '').trim().toLowerCase(),
            password: String(values.password || ''),
          },
        })

        if (!result?.token) {
          throw new Error(
              'The login response did not include a token. Check your backend LoginResponse.'
          )
        }

        // LoginResponse provided in your backend does not include role.
        // The authenticated /auth/profile endpoint supplies the real role.
        const user = {
          id: result.id ?? result.userId,
          fullName: result.fullName,
          email: result.email,
          role: result.role,
        }

        clearSession()

        const storage =
            values.remember === 'on' ? localStorage : sessionStorage

        storage.setItem(TOKEN_KEY, result.token)
        storage.setItem(USER_KEY, JSON.stringify(user))

        setProfile(null)
        setDashboard(null)
        setUsers([])
        setSession({ token: result.token, user })

        navigate('/profile')
      }

      if (action === 'forgot') {
        const result = await api('/auth/forgot-password', {
          method: 'POST',
          body: {
            email: String(values.email || '').trim().toLowerCase(),
          },
        })

        setNotice({
          type: 'success',
          text:
              result?.message ||
              'If the email is registered, a password reset link will be sent.',
        })

        form.reset()
      }

      if (action === 'reset') {
        const resetToken = String(
            values.token || query.get('token') || new URLSearchParams(window.location.search).get('token') || ''
        ).trim()
        const newPassword = String(values.newPassword || '')
        const confirmPassword = String(values.confirmPassword || '')

        if (!resetToken) {
          throw new Error(
              'The reset token is missing. Please request a new password-reset email and open its latest link.'
          )
        }

        if (newPassword !== confirmPassword) {
          setNotice({
            type: 'error',
            text: 'New password and confirm password do not match.',
          })
          return
        }

        const result = await api('/auth/reset-password', {
          method: 'POST',
          body: {
            token: resetToken,
            newPassword,
          },
        })

        setNotice({
          type: 'success',
          text: result?.message || 'Password reset successful. Please log in.',
        })

        navigate('/login')
      }
    } catch (error) {
      setNotice({ type: 'error', text: errorText(error) })
    } finally {
      setBusy(false)
    }
  }

  // Send a 6-digit verification OTP to the currently signed-in user.
  async function sendVerificationOtp() {
    if (!session?.token) {
      setNotice({ type: 'error', text: 'Please sign in first.' })
      navigate('/login')
      return
    }

    if (busy || otpSending) return

    setOtpSending(true)
    setNotice(null)

    try {
      const result = await api('/auth/verification-otp/send', {
        method: 'POST',
        token: session.token,
      })

      setNotice({
        type: 'success',
        text: result?.message || 'A verification OTP has been sent to your email.',
      })
      navigate('/verify-otp')
    } catch (error) {
      setNotice({ type: 'error', text: errorText(error) })
    } finally {
      setOtpSending(false)
    }
  }

  // Verify the submitted 6-digit OTP using the authenticated backend endpoint.
  async function verifyOtp(otp) {
    if (!session?.token) {
      setNotice({ type: 'error', text: 'Please sign in again to verify your email.' })
      navigate('/login')
      return
    }

    if (busy) return

    setBusy(true)
    setNotice(null)

    try {
      const result = await api('/auth/verification-otp/verify', {
        method: 'POST',
        token: session.token,
        body: { otp },
      })

      const updatedProfile = await api('/auth/profile', {
        token: session.token,
      })
      setProfile(updatedProfile)
      setNotice({
        type: 'success',
        text: result?.message || 'Email verified successfully.',
      })
      navigate('/profile')
    } catch (error) {
      setNotice({ type: 'error', text: errorText(error) })
    } finally {
      setBusy(false)
    }
  }

  function signOut() {
    clearSession()
    setSession(null)
    setProfile(null)
    setDashboard(null)
    setUsers([])

    navigate('/login')
    setNotice({
      type: 'success',
      text: 'You have signed out.',
    })
  }

  const content = useMemo(() => {
    if (route === '/verify-otp') {
      if (!session?.token) {
        return (
            <AuthShell
                eyebrow="EMAIL VERIFICATION"
                title="Sign in required"
                description="Please sign in to verify your email address."
            >
              <button className="primary-button" type="button" onClick={() => navigate('/login')}>
                Go to sign in
              </button>
              <Notice notice={notice} />
            </AuthShell>
        )
      }

      return (
          <VerifyOtpPage
              busy={busy}
              otpSending={otpSending}
              notice={notice}
              onVerify={verifyOtp}
              onResend={sendVerificationOtp}
              onBack={() => navigate('/profile')}
          />
      )
    }

    if (route === '/reset-password') {

      return (
          <AuthShell
              eyebrow="ACCOUNT RECOVERY"
              title="Choose a new password"
              description="Choose a new password. The reset token is read automatically from your email link."
          >
            <form onSubmit={(event) => submit(event, 'reset')}>

              <input
                  type="hidden"
                  name="token"
                  value={query.get('token') || ''}
                  readOnly
              />

              {!query.get('token') && (
                  <Notice
                      notice={{
                        type: 'error',
                        text: 'The reset token is missing from this link. Request a new reset email and open the latest link.',
                      }}
                  />
              )}

              <Field
                  label="New password"
                  name="newPassword"
                  type="password"
                  autoComplete="new-password"
                  minLength={8}
                  required
              />

              <Field
                  label="Confirm password"
                  name="confirmPassword"
                  type="password"
                  autoComplete="new-password"
                  minLength={8}
                  required
              />

              <Button busy={busy}>Reset password</Button>
            </form>

            <Notice notice={notice} />

            <p className="switch-link">
              <a href="#/login">Back to sign in</a>
            </p>
          </AuthShell>
      )
    }

    if (!session?.token) {
      if (route === '/register') {
        return (
            <AuthShell
                eyebrow="GET STARTED"
                title="Create your account"
                description="Set up your secure account in a few steps."
            >
              <form onSubmit={(event) => submit(event, 'register')}>
                <Field
                    label="Full name"
                    name="fullName"
                    autoComplete="name"
                    minLength={2}
                    placeholder="Your name"
                    required
                />

                <Field
                    label="Email address"
                    name="email"
                    type="email"
                    autoComplete="email"
                    placeholder="you@example.com"
                    required
                />

                <Field
                    label="Password"
                    name="password"
                    type="password"
                    autoComplete="new-password"
                    minLength={8}
                    required
                />

                <Button busy={busy}>Create account</Button>
              </form>

              <Notice notice={notice} />

              <p className="switch-link">
                Already registered? <a href="#/login">Sign in</a>
              </p>
            </AuthShell>
        )
      }

      if (route === '/forgot-password') {
        return (
            <AuthShell
                eyebrow="ACCOUNT RECOVERY"
                title="Reset your password"
                description="We’ll send a reset link if this email is registered."
            >
              <form onSubmit={(event) => submit(event, 'forgot')}>
                <Field
                    label="Email address"
                    name="email"
                    type="email"
                    autoComplete="email"
                    placeholder="you@example.com"
                    required
                />

                <Button busy={busy}>Send reset link</Button>
              </form>

              <Notice notice={notice} />

              <p className="switch-link">
                <a href="#/login">Back to sign in</a>
              </p>
            </AuthShell>
        )
      }

      return (
          <AuthShell
              eyebrow="SECURE ACCESS"
              title="Welcome back"
              description="Sign in to continue to your account."
          >
            <form onSubmit={(event) => submit(event, 'login')}>
              <Field
                  label="Email address"
                  name="email"
                  type="email"
                  autoComplete="username"
                  placeholder="you@example.com"
                  required
              />

              <Field
                  label="Password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  required
              />

              <label className="check-row">
                <input type="checkbox" name="remember" />
                <span>Keep me signed in on this device</span>
              </label>

              <Button busy={busy}>
                Sign in <span aria-hidden="true">→</span>
              </Button>
            </form>

            <div className="form-links">
              <a href="#/forgot-password">Forgot password?</a>
              <span>
              New here? <a href="#/register">Create an account</a>
            </span>
            </div>

            <Notice notice={notice} />
          </AuthShell>
      )
    }

    if (route === '/admin/dashboard' && role === 'ADMIN') {
      return (
          <AdminPage
              profile={profile || session.user}
              dashboard={dashboard}
              users={users}
              busy={busy}
              notice={notice}
              signOut={signOut}
              navigate={navigate}
          />
      )
    }

    if (route === '/profile' || route === '/admin/dashboard') {
      return (
          <ProfilePage
              profile={profile || session.user}
              busy={busy}
              otpSending={otpSending}
              notice={notice}
              signOut={signOut}
              onVerify={sendVerificationOtp}
              navigate={navigate}
          />
      )
    }

    return (
        <main className="loading-view">
          <span className="spinner" />
          Loading your account…
        </main>
    )
  }, [
    route,
    session,
    profile,
    role,
    dashboard,
    users,
    busy,
    notice,
    query,
    navigate,
    verifyOtp,
    sendVerificationOtp,
    otpSending,
  ])

  return (
      <div className="app-shell">
        <div className="ambient ambient-one" />
        <div className="ambient ambient-two" />

        {content}

        <footer className="site-footer">
          <span className="lock-mark">⌑</span>
          Your account is protected with secure authentication
        </footer>
      </div>
  )
}

function Field({ label, name, type = 'text', ...props }) {
  return (
      <label className="field">
        <span>{label}</span>
        <input name={name} type={type} {...props} />
      </label>
  )
}

function Button({ children, busy }) {
  return (
      <button className="primary-button" type="submit" disabled={busy}>
        {busy ? (
            <>
              <span className="spinner small" /> Please wait…
            </>
        ) : (
            children
        )}
      </button>
  )
}

function Notice({ notice }) {
  if (!notice) return null

  return (
      <div className={`notice ${notice.type}`} role="status">
      <span className="notice-icon">
        {notice.type === 'error' ? '!' : '✓'}
      </span>
        {notice.text}
      </div>
  )
}

function AuthShell({ eyebrow, title, description, children }) {
  return (
      <main className="auth-layout">
        <aside className="brand-panel">
          <a className="brand" href="#/login">
            <span className="brand-symbol">N</span> NORTHSTAR
          </a>

          <div className="brand-copy">
            <div className="eyebrow">SECURITY THAT FEELS SIMPLE</div>
            <h2>
              Your digital life,
              <br />
              <em>in safe hands.</em>
            </h2>
            <p>
              A thoughtful, secure space for your account and the people
              who matter to it.
            </p>
          </div>

          <div className="brand-meta">
          <span>
            <i /> End-to-end protected
          </span>
            <span>01 / 03</span>
          </div>

          <div className="brand-orbit orbit-a" />
          <div className="brand-orbit orbit-b" />
        </aside>

        <section className="auth-panel">
          <div className="auth-card">
            <div className="mobile-brand">
              <span className="brand-symbol">N</span> NORTHSTAR
            </div>

            <div className="eyebrow">{eyebrow}</div>
            <h1>{title}</h1>
            <p className="auth-description">{description}</p>

            {children}
          </div>
        </section>
      </main>
  )
}

function VerifyPage({ token, onVerify, busy, notice, onLogin }) {
  return (
      <AuthShell
          eyebrow="EMAIL CONFIRMATION"
          title="Verify your email"
          description="Confirm your address to finish setting up your account."
      >
        <div className="verify-box">
          <span className="verify-icon">✉</span>
          <p>
            {token
                ? 'Your verification link is ready.'
                : 'A verification token is missing from this link.'}
          </p>
        </div>

        {token && (
            <button
                className="primary-button"
                type="button"
                disabled={busy}
                onClick={onVerify}
            >
              {busy ? 'Verifying…' : 'Verify email'}
            </button>
        )}

        <Notice notice={notice} />

        <p className="switch-link">
          Already verified?{' '}
          <button className="text-button" type="button" onClick={onLogin}>
            Sign in
          </button>
        </p>
      </AuthShell>
  )
}

function VerifyOtpPage({ busy, otpSending, notice, onVerify, onResend, onBack }) {
  const [otp, setOtp] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    if (!/^\d{6}$/.test(otp)) return
    onVerify(otp)
  }

  return (
      <AuthShell
          eyebrow="EMAIL VERIFICATION"
          title="Check your inbox"
          description="Enter the 6-digit verification code sent to your email address."
      >
        <form onSubmit={handleSubmit}>
          <Field
              label="6-digit OTP"
              name="otp"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="Enter 6-digit code"
              maxLength={6}
              pattern="[0-9]{6}"
              value={otp}
              onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
              required
          />
          <Button busy={busy}>Verify email</Button>
        </form>

        <Notice notice={notice} />

        <button
            className="text-button otp-resend"
            type="button"
            onClick={onResend}
            disabled={busy || otpSending}
        >
          {otpSending ? 'Sending OTP…' : 'Resend OTP'}
        </button>

        <p className="switch-link">
          <button className="text-button" type="button" onClick={onBack}>
            Back to profile
          </button>
        </p>
      </AuthShell>
  )
}

function Topbar({ profile, signOut, admin = false }) {
  return (
      <header className="topbar">
        <a
            className="brand dark-brand"
            href={admin ? '#/admin/dashboard' : '#/profile'}
        >
          <span className="brand-symbol">N</span> NORTHSTAR
        </a>

        <div className="topbar-right">
        <span className="secure-chip">
          <i /> Secure session
        </span>

          <div className="avatar">
            {(profile?.fullName || 'U').slice(0, 1).toUpperCase()}
          </div>

          <button className="quiet-button" type="button" onClick={signOut}>
            Sign out <span aria-hidden="true">↗</span>
          </button>
        </div>
      </header>
  )
}

function ProfilePage({ profile, busy, otpSending, notice, signOut, onVerify, navigate }) {
  const verified = getEmailVerified(profile)
  const isAdmin =
      String(profile?.role || '')
          .replace(/^ROLE_/, '')
          .toUpperCase() === 'ADMIN'

  return (
      <main className="workspace">
        <Topbar profile={profile} signOut={signOut} />

        <div className="page-heading">
          <div>
            <div className="eyebrow">YOUR ACCOUNT</div>
            <h1>
              Good to see you, {profile?.fullName?.split(' ')[0] || 'there'}.
            </h1>
            <p>Here’s the current status of your account.</p>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
            {isAdmin && (
                <button
                    type="button"
                    className="primary-button"
                    onClick={() => navigate('/admin/dashboard')}
                    aria-label="Open admin dashboard"
                    style={{ width: 'auto', padding: '12px 18px' }}
                >
                  ▦ Admin Dashboard
                </button>
            )}
            <span className="user-tag">
              <span className="status-dot" /> {profile?.role || 'USER'}
            </span>
          </div>
        </div>

        <Notice notice={notice} />

        {busy && (
            <p className="loading-line">
              <span className="spinner small" /> Updating account information…
            </p>
        )}

        <section className="summary-grid">
          <article className="summary-card welcome-card">
            <div className="card-kicker">
              ACCOUNT OVERVIEW<span>↗</span>
            </div>

            <div className="welcome-avatar">
              {(profile?.fullName || 'U').slice(0, 1).toUpperCase()}
            </div>

            <h2>{profile?.fullName || 'Your account'}</h2>
            <p>{profile?.email || 'Email unavailable'}</p>

            <div className="divider" />

            <span className={`status-pill ${verified ? 'positive' : 'pending'}`}>
            <i /> {verified ? 'Email verified' : 'Email verification pending'}
          </span>

            {!verified && (
                <div className="verification-action">
                  <p>
                    Verify your email to confirm ownership of this address.
                    Request a 6-digit verification code and enter it to confirm this email address.
                  </p>

                  <button
                      type="button"
                      className="primary-button"
                      onClick={onVerify}
                      disabled={busy || otpSending}
                  >
                    {busy || otpSending ? 'Sending...' : 'Verify email'}
                  </button>
                </div>
            )}
          </article>

          <article className="summary-card security-card">
            <div className="card-kicker">
              SECURITY STATUS <span>✳</span>
            </div>

            <h2>
              Your account is
              <br />
              looking good.
            </h2>

            <p>
              Authentication is active for this session. Keep your password
              private and sign out on shared devices.
            </p>

            <div className="security-meter">
              <span />
              <span />
              <span />
              <span />
            </div>

            <small>
              SECURE SESSION <b>ACTIVE</b>
            </small>
          </article>

          <article className="summary-card details-card">
            <div className="card-kicker">
              PROFILE DETAILS <span>•••</span>
            </div>

            <Detail label="Full name" value={profile?.fullName} />
            <Detail label="Email address" value={profile?.email} />
            <Detail label="Account role" value={profile?.role} />
            <Detail label="Email verified" value={verified ? 'Yes' : 'Not yet'} last />
          </article>
        </section>

        <section className="note-bar">
          <span className="note-icon">✦</span>
          <div>
            <b>A little reminder</b>
            <p>
              We’ll never ask you to share your password over email or chat.
            </p>
          </div>
          <span className="note-arrow">↗</span>
          {isAdmin && (
              <button
                  type="button"
                  className="primary-button"
                  onClick={() => navigate('/admin/dashboard')}
                  style={{ width: 'auto', padding: '12px 18px' }}
              >
                ▦ Admin Dashboard
              </button>
          )}
        </section>

      </main>
  )
}

function Detail({ label, value, last }) {
  return (
      <div className={`detail-row ${last ? 'last' : ''}`}>
        <span>{label}</span>
        <b>{value || '—'}</b>
      </div>
  )
}

function AdminPage({ profile, dashboard, users, busy, notice, signOut, navigate }) {
  return (
      <main className="workspace">
        <Topbar profile={profile} signOut={signOut} admin />

        <div className="page-heading">
          <div>
            <div className="eyebrow">ADMINISTRATION</div>
            <h1>Admin dashboard</h1>
            <p>Account activity and user directory.</p>
          </div>

          <span className="user-tag admin-tag">
          <span className="status-dot" /> ADMIN ACCESS
        </span>
        </div>

        <Notice notice={notice} />

        {busy && (
            <p className="loading-line">
              <span className="spinner small" /> Loading dashboard…
            </p>
        )}

        <section className="stats-grid">
          {[
            ['TOTAL USERS', dashboard?.totalUsers],
            ['VERIFIED', dashboard?.unverifiedUsers],
            ['AWAITING VERIFICATION', dashboard?.unverifiedUsers],
            ['TOTAL SIGN INS', dashboard?.totalLogins],
          ].map(([label, value], index) => (
              <article className="stat-card" key={label}>
                <span>{label}</span>
                <strong>{value ?? '-'}</strong>
                <small>
                  {index === 0
                      ? 'Registered accounts'
                      : index === 3
                          ? `${dashboard?.usersWithLogin ?? 0} users have signed in`
                          : 'Current account count'}
                </small>
              </article>
          ))}
        </section>

        <section className="directory-card">
          <div className="directory-heading">
            <div>
              <div className="eyebrow">DIRECTORY</div>
              <h2>Registered users</h2>
            </div>

            <span>
            {users.length} {users.length === 1 ? 'ACCOUNT' : 'ACCOUNTS'}
          </span>
          </div>

          {users.length ? (
              <div className="table-wrap">
                <table>
                  <thead>
                  <tr>
                    <th>USER</th>
                    <th>EMAIL</th>
                    <th>ROLE</th>
                    <th>VERIFICATION</th>
                    <th>SIGN INS</th>
                  </tr>
                  </thead>

                  <tbody>
                  {users.map((user, index) => {
                    const verified = getEmailVerified(user)

                    return (
                        <tr key={user.id ?? user.email ?? index}>
                          <td>
                        <span className="table-avatar">
                          {(user.fullName || 'U').slice(0, 1).toUpperCase()}
                        </span>
                            {user.fullName || '—'}
                          </td>

                          <td>{user.email || '—'}</td>

                          <td>
                        <span className="role-pill">
                          {normalizeRole(user.role || 'USER')}
                        </span>
                          </td>

                          <td>
                        <span
                            className={`status-pill compact ${
                                verified ? 'positive' : 'pending'
                            }`}
                        >
                          <i />
                          {verified ? 'Verified' : 'Pending'}
                        </span>
                          </td>

                          <td>{user.loginCount ?? '—'}</td>
                        </tr>
                    )
                  })}
                  </tbody>
                </table>
              </div>
          ) : (
              <div className="empty-state">
                {busy ? 'Loading user directory…' : 'No users to display.'}
              </div>
          )}
        </section>
      </main>
  )
}

export default App
