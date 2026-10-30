import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AccountService } from '../../core/api/account.service';
import { TransactionService } from '../../core/api/transaction.service';
import { FR } from '../../core/i18n/fr';
import {
  Account,
  ApiError,
  TRANSFER_IN,
  TRANSFER_OUT,
  Transaction,
} from '../../core/models/banking.models';
import { PageHeader } from '../../shared/components/page-header';
import { StatePanel } from '../../shared/components/state-panel';
import { MoneyPipe } from '../../shared/pipes/money.pipe';

const PAGE_SIZE = 10;

/**
 * Transaction history.
 *
 * The backend returns the whole list and supports no paging parameter, so paging is done here and
 * labelled as client-side rather than pretending the API supports it.
 */
@Component({
  selector: 'app-transactions-page',
  imports: [RouterLink, DatePipe, MoneyPipe, PageHeader, StatePanel],
  templateUrl: './transactions-page.html',
  styles: `
    .toolbar {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      align-items: flex-end;
      margin-bottom: 0.5rem;
    }
    .toolbar .field {
      margin-bottom: 0;
      flex: 1 1 18rem;
    }
    .pagination {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      flex-wrap: wrap;
      margin-top: 1rem;
    }
    .leg {
      font-size: 0.8125rem;
      color: var(--text-muted);
    }
  `,
})
export class TransactionsPage implements OnInit {
  private readonly transactionService = inject(TransactionService);
  private readonly accountService = inject(AccountService);

  protected readonly text = FR;
  protected readonly loading = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly transactions = signal<Transaction[]>([]);
  protected readonly accounts = signal<Account[]>([]);
  protected readonly selectedAccountId = signal('');
  protected readonly page = signal(1);

  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.transactions().length / PAGE_SIZE)),
  );

  protected readonly pageItems = computed(() => {
    const start = (this.page() - 1) * PAGE_SIZE;
    return this.transactions().slice(start, start + PAGE_SIZE);
  });

  ngOnInit(): void {
    this.accountService.list().subscribe({
      next: (accounts) => this.accounts.set(accounts),
      // The filter dropdown is a convenience; losing it must not hide the history.
      error: () => this.accounts.set([]),
    });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.page.set(1);

    this.transactionService.list(this.selectedAccountId()).subscribe({
      next: (transactions) => {
        this.transactions.set(transactions);
        this.loading.set(false);
      },
      error: (apiError: ApiError) => {
        this.error.set(apiError);
        this.loading.set(false);
      },
    });
  }

  protected onAccountChange(accountId: string): void {
    this.selectedAccountId.set(accountId);
    this.load();
  }

  protected previous(): void {
    this.page.update((current) => Math.max(1, current - 1));
  }

  protected next(): void {
    this.page.update((current) => Math.min(this.totalPages(), current + 1));
  }

  /** Transfer legs are recognisable by their merchant category. */
  protected legLabel(transaction: Transaction): string | null {
    if (transaction.merchantCategory === TRANSFER_OUT) {
      return FR.transactions.transferOut;
    }
    if (transaction.merchantCategory === TRANSFER_IN) {
      return FR.transactions.transferIn;
    }
    return null;
  }

  protected accountLabel(accountId: string): string {
    return this.accounts().find((account) => account.id === accountId)?.customerReference ?? accountId;
  }
}
