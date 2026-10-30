import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AccountService } from '../../core/api/account.service';
import { FR } from '../../core/i18n/fr';
import { Account, AccountStatus, ApiError } from '../../core/models/banking.models';
import { NotificationService } from '../../core/notifications/notification.service';
import { PageHeader } from '../../shared/components/page-header';
import { StatePanel } from '../../shared/components/state-panel';
import { StatusBadge } from '../../shared/components/status-badge';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { currencyValidator } from '../../shared/validators/amount.validators';

@Component({
  selector: 'app-accounts-page',
  imports: [ReactiveFormsModule, RouterLink, MoneyPipe, PageHeader, StatePanel, StatusBadge],
  templateUrl: './accounts-page.html',
  styles: `
    .toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      align-items: flex-end;
      margin-bottom: 1.25rem;
    }
    .toolbar .field {
      margin-bottom: 0;
      flex: 1 1 16rem;
    }
    .create-form {
      margin-bottom: 1.5rem;
    }
    .create-form__grid {
      display: grid;
      gap: 0 1rem;
    }
    @media (min-width: 640px) {
      .create-form__grid {
        grid-template-columns: repeat(3, 1fr);
      }
    }
    .row-actions {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }
  `,
})
export class AccountsPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly notifications = inject(NotificationService);
  private readonly fb = inject(FormBuilder);

  protected readonly text = FR;
  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly accounts = signal<Account[]>([]);
  protected readonly filter = signal('');
  protected readonly showCreate = signal(false);
  protected readonly creating = signal(false);
  protected readonly pendingStatusId = signal<string | null>(null);

  protected readonly createForm = this.fb.nonNullable.group({
    customerReference: ['', [Validators.required]],
    currency: ['EUR', [Validators.required, currencyValidator]],
    balance: [null as number | null],
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.accountService.list(this.filter()).subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: (apiError: ApiError) => {
        this.error.set(apiError);
        this.loading.set(false);
      },
    });
  }

  protected applyFilter(value: string): void {
    this.filter.set(value);
    this.load();
  }

  protected clearFilter(): void {
    this.filter.set('');
    this.load();
  }

  protected toggleCreate(): void {
    this.showCreate.update((open) => !open);
  }

  protected create(): void {
    if (this.createForm.invalid || this.creating()) {
      this.createForm.markAllAsTouched();
      return;
    }
    this.creating.set(true);
    const { customerReference, currency, balance } = this.createForm.getRawValue();

    this.accountService
      .create({
        customerReference,
        currency,
        ...(balance === null ? {} : { balance: Number(balance) }),
      })
      .subscribe({
        next: () => {
          this.creating.set(false);
          this.showCreate.set(false);
          this.createForm.reset({ customerReference: '', currency: 'EUR', balance: null });
          this.notifications.success(FR.accounts.created);
          this.load();
        },
        error: (apiError: ApiError) => {
          this.creating.set(false);
          this.notifications.error(apiError.message);
        },
      });
  }

  protected toggleStatus(account: Account): void {
    const next: AccountStatus = account.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
    const question = next === 'BLOCKED' ? FR.accounts.confirmBlock : FR.accounts.confirmUnblock;
    if (!confirm(question)) {
      return;
    }

    this.pendingStatusId.set(account.id);
    this.accountService.updateStatus(account.id, next).subscribe({
      next: () => {
        this.pendingStatusId.set(null);
        this.notifications.success(next === 'BLOCKED' ? FR.accounts.blocked : FR.accounts.unblocked);
        this.load();
      },
      error: (apiError: ApiError) => {
        this.pendingStatusId.set(null);
        this.notifications.error(apiError.message);
      },
    });
  }

  protected invalid(control: string): boolean {
    const field = this.createForm.get(control);
    return !!field && field.invalid && field.touched;
  }
}
