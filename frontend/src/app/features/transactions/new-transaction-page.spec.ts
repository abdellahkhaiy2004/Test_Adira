import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { errorInterceptor } from '../../core/http/error.interceptor';
import { Account } from '../../core/models/banking.models';
import { NewTransactionPage } from './new-transaction-page';

const ACCOUNTS: Account[] = [
  { id: 'a-1', customerReference: 'CUST-1', status: 'ACTIVE', currency: 'EUR', balance: 500 },
];

describe('NewTransactionPage', () => {
  let fixture: ComponentFixture<NewTransactionPage>;
  let http: HttpTestingController;

  const text = () => fixture.nativeElement.textContent as string;
  const form = () => (fixture.componentInstance as any).form;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [NewTransactionPage],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(NewTransactionPage);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne((r) => r.url.endsWith('/api/accounts')).flush(ACCOUNTS);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('sends nothing while the form is invalid', () => {
    (fixture.componentInstance as any).submit();

    http.expectNone((r) => r.method === 'POST');
  });

  it('rejects an amount with more than two decimals before any request', () => {
    form().setValue({
      accountId: 'a-1',
      amount: '10.005',
      currency: 'EUR',
      merchantCategory: 'GROCERY',
      country: 'MA',
    });
    (fixture.componentInstance as any).submit();

    expect(form().controls.amount.errors).toEqual({ amountDecimals: true });
    http.expectNone((r) => r.method === 'POST');
  });

  it('pre-fills the currency from the chosen account', () => {
    form().controls.accountId.setValue('a-1');

    expect(form().controls.currency.value).toBe('EUR');
  });

  it('announces success only with the created transaction', async () => {
    form().setValue({
      accountId: 'a-1',
      amount: '250.00',
      currency: 'EUR',
      merchantCategory: 'GROCERY',
      country: 'MA',
    });
    (fixture.componentInstance as any).submit();
    fixture.detectChanges();

    expect(text()).not.toContain('Transaction enregistrée');

    const request = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/api/transactions'));
    expect(request.request.body.amount).toBe(250);
    request.flush({
      id: 't-9',
      accountId: 'a-1',
      amount: 250,
      currency: 'EUR',
      merchantCategory: 'GROCERY',
      country: 'MA',
      createdAt: '2026-01-15T10:30:00Z',
    });
    http.expectOne((r) => r.url.endsWith('/api/accounts')).flush(ACCOUNTS);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('Transaction enregistrée');
    expect(text()).toContain('t-9');
  });

  it('shows the backend ceiling refusal and keeps the form values', async () => {
    form().setValue({
      accountId: 'a-1',
      amount: '5000',
      currency: 'EUR',
      merchantCategory: 'GROCERY',
      country: 'MA',
    });
    (fixture.componentInstance as any).submit();

    http.expectOne((r) => r.method === 'POST').flush(
      {
        status: 400,
        error: 'Bad Request',
        code: 'AMOUNT_EXCEEDS_MAX',
        message: 'amount exceeds the maximum allowed (1000)',
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('amount exceeds the maximum allowed (1000)');
    expect(text()).not.toContain('Transaction enregistrée');
    // The user can correct and retry without retyping everything.
    expect(form().controls.amount.value).toBe('5000');
  });
});
