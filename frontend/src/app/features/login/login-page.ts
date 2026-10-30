import { Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { FR } from '../../core/i18n/fr';

/**
 * Authentication entry point.
 *
 * The backend has no authentication endpoint and none of its APIs is protected, so this page says so
 * instead of imitating a Keycloak login. There is no credential field and no token is fabricated.
 */
@Component({
  selector: 'app-login-page',
  template: `
    <div class="card">
      <h1>{{ text.login.title }}</h1>

      <div class="notice" role="note">
        <p class="notice__title">{{ text.login.notImplementedTitle }}</p>
        <p>{{ text.login.notImplementedBody }}</p>
        <p class="text-sm muted">{{ text.login.notImplementedDetail }}</p>
      </div>

      <button type="button" class="btn" (click)="enter()">{{ text.login.enter }}</button>
    </div>
  `,
  styles: `
    .card {
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
    }
    h1 {
      margin: 0;
      font-size: 1.5rem;
    }
    .notice {
      border: 1px solid #ffe69c;
      background: #fff3cd;
      color: #664d03;
      border-radius: var(--radius);
      padding: 1rem;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }
    .notice__title {
      font-weight: 700;
      margin: 0;
    }
    .notice p {
      margin: 0;
    }
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly text = FR;

  protected enter(): void {
    this.auth.enterDemo();
    const redirectTo = this.route.snapshot.queryParamMap.get('redirectTo') ?? '/dashboard';
    void this.router.navigateByUrl(redirectTo);
  }
}
