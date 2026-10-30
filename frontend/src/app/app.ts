import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth/auth.service';
import { FR } from './core/i18n/fr';
import { NotificationService } from './core/notifications/notification.service';

/** Application shell: navigation, notification host, router outlet. */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  protected readonly auth = inject(AuthService);
  protected readonly notifications = inject(NotificationService);

  protected readonly text = FR;
  protected readonly menuOpen = signal(false);

  protected readonly links = [
    { path: '/dashboard', label: FR.app.nav.dashboard },
    { path: '/accounts', label: FR.app.nav.accounts },
    { path: '/transactions', label: FR.app.nav.transactions },
    { path: '/transfers', label: FR.app.nav.transfers },
    { path: '/assistant', label: FR.app.nav.assistant },
  ];

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected logout(): void {
    this.auth.logout();
    this.closeMenu();
    void this.router.navigate(['/login']);
  }
}
