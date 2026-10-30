import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { errorInterceptor } from '../../core/http/error.interceptor';
import { Account } from '../../core/models/banking.models';
import { TransferPage } from './transfer-page';

const ACCOUNTS: Account[] = [
  { id: 'a-1', customerReference: 'CUST-1', status: 'ACTIVE', currency: 'EUR', balance: 500 },
  { id: 'a-2', customerReference: 'CUST-2', status: 'ACTIVE', currency: 'EUR', balance: 100 },
];

describe('TransferPage', () => {
  let fixture: ComponentFixture<TransferPage>;
  let http: HttpTestingController;

  const text = () => fixture.nativeElement.textContent as string;

  const fillValidForm = () => {
    const form = (fixture.componentInstance as any).form;
    form.setValue({
      sourceAccountId: 'a-1',
      destinationAccountId: 'a-2',
      amount: '200',
      currency: 'EUR',
      country: 'MA',
    });
  };

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [TransferPage],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(TransferPage);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();

    http.expectOne((r) => r.url.endsWith('/api/accounts')).flush(ACCOUNTS);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('loads the accounts into both selects', () => {
    expect(text()).toContain('CUST-1');
    expect(text()).toContain('CUST-2');
  });

  it('rejects a transfer to the same account', () => {
    const form = (fixture.componentInstance as any).form;
    form.setValue({
      sourceAccountId: 'a-1',
      destinationAccountId: 'a-1',
      amount: '10',
      currency: 'EUR',
      country: 'MA',
    });

    expect(form.hasError('sameAccount')).toBe(true);
    expect(form.invalid).toBe(true);
  });

  it('sends nothing until the transfer is confirmed', () => {
    fillValidForm();
    fixture.detectChanges();

    (fixture.componentInstance as any).review();
    fixture.detectChanges();

    // The confirmation step is shown and no request has left yet.
    http.expectNone((r) => r.method === 'POST');
    expect(text()).toContain('Confirmer le virement');
  });

  it('declares success only after a 201 carrying the transfer', async () => {
    fillValidForm();
    (fixture.componentInstance as any).review();
    fixture.detectChanges();

    (fixture.componentInstance as any).execute();
    fixture.detectChanges();

    // Still in flight: nothing may announce success.
    expect(text()).not.toContain('Virement exécuté');

    http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/api/transfers')).flush({
      id: 't-1',
      sourceAccountId: 'a-1',
      destinationAccountId: 'a-2',
      amount: 200,
      currency: 'EUR',
      createdAt: '2026-01-15T10:30:00Z',
    });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('Virement exécuté');
    expect(text()).toContain('t-1');

    // Balances are re-read from the server rather than patched locally.
    http.expectOne((r) => r.url.endsWith('/api/accounts')).flush(ACCOUNTS);
  });

  it('shows the backend refusal and never a success', async () => {
    fillValidForm();
    (fixture.componentInstance as any).review();
    fixture.detectChanges();
    (fixture.componentInstance as any).execute();

    http.expectOne((r) => r.method === 'POST').flush(
      {
        status: 409,
        error: 'Conflict',
        code: 'INSUFFICIENT_FUNDS',
        message: 'insufficient funds on account a-1',
      },
      { status: 409, statusText: 'Conflict' },
    );
    http.expectOne((r) => r.url.endsWith('/api/accounts')).flush(ACCOUNTS);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('INSUFFICIENT_FUNDS');
    expect(text()).toContain('insufficient funds on account a-1');
    expect(text()).not.toContain('Virement exécuté');
  });
});
