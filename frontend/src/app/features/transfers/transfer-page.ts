import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AccountService } from '../../core/api/account.service';
import { TransferService } from '../../core/api/transfer.service';
import { FR } from '../../core/i18n/fr';
import { Account, ApiError, Transfer } from '../../core/models/banking.models';
import { PageHeader } from '../../shared/components/page-header';
import { StatePanel } from '../../shared/components/state-panel';
import { MoneyPipe } from '../../shared/pipes/money.pipe';
import {
  amountValidator,
  currencyValidator,
  differentAccountsValidator,
} from '../../shared/validators/amount.validators';

/**
 * Account-to-account transfer.
 *
 * Two steps on purpose: the user reviews amount, currency and both accounts before anything is sent.
 * The success panel appears only after a 201 carrying the transfer id; any failure shows the backend's
 * own code and message, and the balances are re-read from the API rather than guessed.
 */
@Component({
  selector: 'app-transfer-page',
  imports: [ReactiveFormsModule, RouterLink, MoneyPipe, PageHeader, StatePanel],
  templateUrl: './transfer-page.html',
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
    .confirm {
      border: 1px solid var(--border);
      border-radius: var(--radius);
      background: #eef4f9;
      padding: 1rem 1.25rem;
      margin-bottom: 1rem;
    }
    .confirm__amount {
      font-size: 1.5rem;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
    }
    .confirm__line {
      margin: 0.25rem 0;
    }
    .result__id {
      word-break: break-all;
    }
  `,
})
export class TransferPage implements OnInit {
  private readonly accountService = inject(AccountService);
  private readonly transferService = inject(TransferService);
  private readonly fb = inject(FormBuilder);

  protected readonly text = FR;
  protected readonly loadingAccounts = signal(false);
  protected readonly loadError = signal<ApiError | null>(null);
  protected readonly accounts = signal<Account[]>([]);
  protected readonly confirming = signal(false);
  protected readonly submitting = signal(false);
  protected readonly submitError = signal<ApiError | null>(null);
  protected readonly completed = signal<Transfer | null>(null);

  protected readonly form = this.fb.nonNullable.group(
    {
      sourceAccountId: ['', [Validators.required]],
      destinationAccountId: ['', [Validators.required]],
      amount: ['', [Validators.required, amountValidator]],
      currency: ['', [Validators.required, currencyValidator]],
      country: ['', [Validators.required]],
    },
    { validators: differentAccountsValidator('sourceAccountId', 'destinationAccountId') },
  );

  private readonly sourceId = signal('');
  private readonly destinationId = signal('');

  protected readonly sourceAccount = computed(() =>
    this.accounts().find((account) => account.id === this.sourceId()),
  );
  protected readonly destinationAccount = computed(() =>
    this.accounts().find((account) => account.id === this.destinationId()),
  );

  /** Both accounts must share a currency; flagged early, still decided by the backend. */
  protected readonly currencyMismatch = computed(() => {
    const source = this.sourceAccount();
    const destination = this.destinationAccount();
    return !!source && !!destination && source.currency !== destination.currency;
  });

  protected readonly sameAccount = computed(() => this.form.hasError('sameAccount'));

  ngOnInit(): void {
    this.loadAccounts();

    this.form.controls.sourceAccountId.valueChanges.subscribe((id) => {
      this.sourceId.set(id);
      const account = this.accounts().find((item) => item.id === id);
      if (account) {
        this.form.controls.currency.setValue(account.currency);
      }
    });
    this.form.controls.destinationAccountId.valueChanges.subscribe((id) => this.destinationId.set(id));
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

  /** Step one: validate and show the confirmation. Nothing is sent yet. */
  protected review(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitError.set(null);
    this.confirming.set(true);
  }

  protected cancelConfirmation(): void {
    this.confirming.set(false);
  }

  /** Step two: the only place a transfer is actually requested. */
  protected execute(): void {
    if (this.submitting()) {
      return;
    }
    this.submitting.set(true);
    this.submitError.set(null);

    const value = this.form.getRawValue();
    this.transferService
      .transfer({
        sourceAccountId: value.sourceAccountId,
        destinationAccountId: value.destinationAccountId,
        amount: Number(String(value.amount).replace(',', '.')),
        currency: value.currency.toUpperCase(),
        country: value.country.trim(),
      })
      .subscribe({
        next: (transfer) => {
          this.submitting.set(false);
          this.confirming.set(false);
          this.completed.set(transfer);
          this.form.reset({
            sourceAccountId: '',
            destinationAccountId: '',
            amount: '',
            currency: '',
            country: '',
          });
          this.loadAccounts();
        },
        error: (apiError: ApiError) => {
          this.submitting.set(false);
          this.confirming.set(false);
          this.submitError.set(apiError);
          // Balances may or may not have moved; ask the server instead of assuming.
          this.loadAccounts();
        },
      });
  }

  protected again(): void {
    this.completed.set(null);
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
