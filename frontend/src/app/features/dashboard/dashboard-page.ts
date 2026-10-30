import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
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

const RECENT_COUNT = 5;

/** Overview of the accounts and the latest transactions. Every figure comes from the API. */
@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink, DatePipe, MoneyPipe, PageHeader, StatePanel, StatusBadge],
  templateUrl: './dashboard-page.html',
  styles: `
    .totals {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .total {
      flex: 1 1 12rem;
    }
    .total__label {
      color: var(--text-muted);
      font-size: 0.875rem;
    }
    .total__value {
      font-size: 1.5rem;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
    }
    .account__top {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 0.75rem;
      margin-bottom: 0.75rem;
    }
    .account__reference {
      font-weight: 700;
    }
    .account__balance {
      font-size: 1.375rem;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
    }
    .account__id {
      margin-top: 0.35rem;
    }
    .section__head {
      display: flex;
      justify-content: space-between;
      align-items: baseline;
      gap: 1rem;
      margin-bottom: 0.75rem;
    }
    .section__head h2 {
      margin: 0;
    }
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
  `,
})
export class DashboardPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly transactionService = inject(TransactionService);

  protected readonly text = FR;
  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly accounts = signal<Account[]>([]);
  protected readonly recent = signal<Transaction[]>([]);

  /** Display aid computed from the accounts shown; not a figure supplied by the bank. */
  protected readonly totalsByCurrency = computed(() => {
    const totals = new Map<string, number>();
    for (const account of this.accounts()) {
      totals.set(account.currency, (totals.get(account.currency) ?? 0) + account.balance);
    }
    return [...totals.entries()].map(([currency, total]) => ({ currency, total }));
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      accounts: this.accountService.list(),
      transactions: this.transactionService.list(),
    }).subscribe({
      next: ({ accounts, transactions }) => {
        this.accounts.set(accounts);
        this.recent.set(transactions.slice(0, RECENT_COUNT));
        this.loading.set(false);
      },
      error: (apiError: ApiError) => {
        this.error.set(apiError);
        this.loading.set(false);
      },
    });
  }
}
