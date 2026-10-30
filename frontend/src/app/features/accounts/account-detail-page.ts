import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AccountService } from '../../core/api/account.service';
import { TransactionService } from '../../core/api/transaction.service';
import { FR } from '../../core/i18n/fr';
import { Account, ApiError, Transaction } from '../../core/models/banking.models';
import { PageHeader } from '../../shared/components/page-header';
import { StatePanel } from '../../shared/components/state-panel';
import { StatusBadge } from '../../shared/components/status-badge';
import { MoneyPipe } from '../../shared/pipes/money.pipe';

/** One account and its history. `id` is bound from the route by withComponentInputBinding(). */
@Component({
  selector: 'app-account-detail-page',
  imports: [RouterLink, DatePipe, MoneyPipe, PageHeader, StatePanel, StatusBadge],
  templateUrl: './account-detail-page.html',
  styles: `
    .summary {
      display: grid;
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    @media (min-width: 640px) {
      .summary {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    .definition {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.5rem 0;
      border-bottom: 1px solid var(--border);
    }
    .definition:last-child {
      border-bottom: 0;
    }
    .definition__term {
      color: var(--text-muted);
    }
    .definition__value {
      font-weight: 600;
      text-align: right;
      word-break: break-all;
    }
    .balance {
      font-size: 2rem;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
    }
  `,
})
export class AccountDetailPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly transactionService = inject(TransactionService);

  /** Route parameter. */
  readonly id = input.required<string>();

  protected readonly text = FR;
  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly account = signal<Account | null>(null);
  protected readonly transactions = signal<Transaction[]>([]);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      account: this.accountService.get(this.id()),
      transactions: this.transactionService.list(this.id()),
    }).subscribe({
      next: ({ account, transactions }) => {
        this.account.set(account);
        this.transactions.set(transactions);
        this.loading.set(false);
      },
      error: (apiError: ApiError) => {
        this.error.set(apiError);
        this.loading.set(false);
      },
    });
  }
}
