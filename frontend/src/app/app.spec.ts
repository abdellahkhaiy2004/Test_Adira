import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { App } from './app';
import { AuthService } from './core/auth/auth.service';
import { NotificationService } from './core/notifications/notification.service';

describe('App shell', () => {
  let fixture: ComponentFixture<App>;
  let auth: AuthService;

  const text = () => fixture.nativeElement.textContent as string;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });

    fixture = TestBed.createComponent(App);
    auth = TestBed.inject(AuthService);
  });

  it('creates the shell', () => {
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('hides the navigation until the session exists', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('nav')).toBeNull();
  });

  it('shows the banking navigation once the session exists', async () => {
    auth.enterDemo();
    fixture.detectChanges();
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('nav')).not.toBeNull();
    expect(text()).toContain('Comptes');
    expect(text()).toContain('Transactions');
    expect(text()).toContain('Virement');
    expect(text()).toContain('Assistant');
  });

  it('renders notifications and lets them be dismissed', async () => {
    const notifications = TestBed.inject(NotificationService);
    notifications.error('Solde insuffisant');
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('Solde insuffisant');

    notifications.dismiss(notifications.notifications()[0].id);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).not.toContain('Solde insuffisant');
  });
});
