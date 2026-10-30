import { Injectable, signal } from '@angular/core';

const SESSION_KEY = 'banking-demo-session';

/**
 * The single seam where Keycloak will be plugged in.
 *
 * There is no authentication on the backend today: no login endpoint exists and the APIs are not
 * protected. This service therefore holds nothing but a local flag that lets routing and guards be
 * demonstrated. It issues no token and stores no credential, so nothing here can be mistaken for a
 * real session.
 *
 * Migration: implement these four methods with `keycloak-js` and secure the gateway. `getToken()` will
 * start returning a real bearer token, at which point the HTTP interceptor begins sending it. No other
 * file in the application needs to change.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authenticated = signal(this.readSession());

  readonly isAuthenticated = this.authenticated.asReadonly();

  /** Opens the demonstration. This is explicitly not an authentication. */
  enterDemo(): void {
    sessionStorage.setItem(SESSION_KEY, 'true');
    this.authenticated.set(true);
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_KEY);
    this.authenticated.set(false);
  }

  /**
   * Null until Keycloak is wired up. The interceptor sends no `Authorization` header while this is
   * null, rather than inventing a placeholder token.
   */
  getToken(): string | null {
    return null;
  }

  private readSession(): boolean {
    try {
      return sessionStorage.getItem(SESSION_KEY) === 'true';
    } catch {
      // sessionStorage can be unavailable (private mode, SSR-like contexts).
      return false;
    }
  }
}
