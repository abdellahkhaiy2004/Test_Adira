import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let auth: AuthService;

  const run = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, { url: '/accounts' } as RouterStateSnapshot),
    );

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    auth = TestBed.inject(AuthService);
  });

  it('redirects to /login when there is no session', () => {
    const result = run();

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toContain('/login');
  });

  it('keeps the attempted url so the user lands where they meant to', () => {
    const result = run() as UrlTree;

    expect(TestBed.inject(Router).serializeUrl(result)).toContain('redirectTo=%2Faccounts');
  });

  it('lets the navigation through once the session exists', () => {
    auth.enterDemo();

    expect(run()).toBe(true);
  });
});

describe('AuthService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
  });

  it('issues no token until Keycloak is wired up', () => {
    const auth = TestBed.inject(AuthService);
    auth.enterDemo();

    expect(auth.getToken()).toBeNull();
  });

  it('clears the session on logout', () => {
    const auth = TestBed.inject(AuthService);
    auth.enterDemo();
    expect(auth.isAuthenticated()).toBe(true);

    auth.logout();
    expect(auth.isAuthenticated()).toBe(false);
  });
});
