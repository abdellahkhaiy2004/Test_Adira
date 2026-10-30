import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AccountService } from '../../core/api/account.service';
import { TransactionService } from '../../core/api/transaction.service';
import { FR } from '../../core/i18n/fr';
import { Account, ApiError, Transaction } from '../../core/models/banking.models';
import { PageHeader } from '../../shared/components/page-header';
import { StatePanel } from '../../shared/components/state-panel';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import { amountValidator, currencyValidator } from '../../shared/validators/amount.validators';

/**
 * Records a debit on an account.
 *
 * Client-side validation mirrors the backend's input rules only. The amount ceiling is not duplicated
 * here: it is configuration owned by the Config Server, so the server decides and its refusal is shown
 * as returned. Nothing is called a success before a 201 carrying the created transaction.
 */
@Component({
  selector: 'app-new-transaction-page',
  imports: [ReactiveFormsModule, RouterLink, MoneyPipe, PageHeader, StatePanel],
  templateUrl: './new-transaction-page.html',
  styles: `
    .form-card {
      max-width: 40rem;
    }
    .grid-2 {
      display: grid;
      gap: 0 1rem;
    }
    @media (min-width: 640px) {
      .grid-2 {
        grid-template-columns: repeat(2, 1fr);
      }
    }
    .row-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-top: 0.5rem;
    }
    .result {
      margin-bottom: 1.5rem;
    }
    .result__id {
      word-break: break-all;
    }
  `,
})
export class NewTransactionPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly transactionService = inject(TransactionService);
  private readonly fb = inject(FormBuilder);

  protected readonly text = FR;
  protected readonly loadingAccounts = signal(false);
  protected readonly loadError = signal<ApiError | null>(null);
  protected readonly accounts = signal<Account[]>([]);
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<ApiError | null>(null);
  protected readonly created = signal<Transaction | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    accountId: ['', [Validators.required]],
    amount: ['', [Validators.required, amountValidator]],
    currency: ['', [Validators.required, currencyValidator]],
    merchantCategory: ['', [Validators.required]],
    country: ['', [Validators.required]],
  });

  private readonly selectedAccountId = signal('');
  private readonly typedCurrency = signal('');

  /** Warns when the typed currency differs from the account's; the backend remains the authority. */
  protected readonly currencyMismatch = computed(() => {
    const account = this.accounts().find((item) => item.id === this.selectedAccountId());
    const currency = this.typedCurrency();
    return !!account && !!currency && currency.length === 3 && account.currency !== currency;
  });

  ngOnInit(): void {
    this.loadAccounts();

    this.form.controls.accountId.valueChanges.subscribe((accountId) => {
      this.selectedAccountId.set(accountId);
      const account = this.accounts().find((item) => item.id === accountId);
      if (account) {
        // Pre-fill with the account's own currency: the common case, still editable.
        this.form.controls.currency.setValue(account.currency);
      }
    });

    this.form.controls.currency.valueChanges.subscribe((currency) =>
      this.typedCurrency.set((currency ?? '').toUpperCase()),
    );
  }

  protected loadAccounts(): void {
    this.loadingAccounts.set(true);
    this.loadError.set(null);
    this.accountService.list().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loadingAccounts.set(false);
      },
      error: (apiError: ApiError) => {
        this.loadError.set(apiError);
        this.loadingAccounts.set(false);
      },
    });
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);
    this.created.set(null);

    const value = this.form.getRawValue();
    this.transactionService
      .create({
        accountId: value.accountId,
        amount: Number(String(value.amount).replace(',', '.')),
        currency: value.currency.toUpperCase(),
        merchantCategory: value.merchantCategory.trim(),
        country: value.country.trim(),
      })
      .subscribe({
        next: (transaction) => {
          this.submitting.set(false);
          this.created.set(transaction);
          this.form.reset({ accountId: '', amount: '', currency: '', merchantCategory: '', country: '' });
          // Balances changed server-side: re-read rather than patch locally.
          this.loadAccounts();
        },
        error: (apiError: ApiError) => {
          this.submitting.set(false);
          // The form keeps its values so the user can correct and retry.
          this.submitError.set(apiError);
        },
      });
  }

  protected again(): void {
    this.created.set(null);
  }

  protected invalid(control: keyof typeof this.form.controls): boolean {
    const field = this.form.controls[control];
    return field.invalid && field.touched;
  }

  protected errorFor(control: keyof typeof this.form.controls): string {
    const errors = this.form.controls[control].errors ?? {};
    if (errors['required']) return FR.validation.required;
    if (errors['amountNumber']) return FR.validation.amountNumber;
    if (errors['amountPositive']) return FR.validation.amountPositive;
    if (errors['amountDecimals']) return FR.validation.amountDecimals;
    if (errors['currencyFormat']) return FR.validation.currencyFormat;
    return FR.validation.required;
  }
}
