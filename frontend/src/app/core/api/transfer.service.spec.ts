import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { errorInterceptor } from '../http/error.interceptor';
import { ApiError } from '../models/banking.models';
import { TransactionService } from './transaction.service';
import { TransferService } from './transfer.service';

describe('Transaction and transfer services', () => {
  let transactions: TransactionService;
  let transfers: TransferService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    transactions = TestBed.inject(TransactionService);
    transfers = TestBed.inject(TransferService);
    http = TestBed.inject(HttpTestingController);
  });

  it('filters the history by account', () => {
    transactions.list('acc-1').subscribe();

    const request = http.expectOne((r) => r.params.get('accountId') === 'acc-1');
    expect(request.request.method).toBe('GET');
    request.flush([]);
    http.verify();
  });

  it('sends the full transaction payload', () => {
    transactions
      .create({
        accountId: 'acc-1',
        amount: 250,
        currency: 'EUR',
        merchantCategory: 'GROCERY',
        country: 'MA',
      })
      .subscribe();

    const request = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/api/transactions'));
    expect(request.request.body.amount).toBe(250);
    expect(request.request.body.merchantCategory).toBe('GROCERY');
    request.flush({});
    http.verify();
  });

  it('sends the full transfer payload', () => {
    transfers
      .transfer({
        sourceAccountId: 'a',
        destinationAccountId: 'b',
        amount: 100,
        currency: 'EUR',
        country: 'MA',
      })
      .subscribe();

    const request = http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/api/transfers'));
    expect(request.request.body).toEqual({
      sourceAccountId: 'a',
      destinationAccountId: 'b',
      amount: 100,
      currency: 'EUR',
      country: 'MA',
    });
    request.flush({});
    http.verify();
  });

  it('keeps a business refusal intact so the user sees the real reason', async () => {
    const failure = new Promise<ApiError>((resolve) => {
      transfers
        .transfer({
          sourceAccountId: 'a',
          destinationAccountId: 'b',
          amount: 5000,
          currency: 'EUR',
          country: 'MA',
        })
        .subscribe({ error: resolve });
    });

    http.expectOne((r) => r.method === 'POST').flush(
      {
        status: 400,
        error: 'Bad Request',
        code: 'AMOUNT_EXCEEDS_MAX',
        message: 'amount exceeds the maximum allowed (1000)',
      },
      { status: 400, statusText: 'Bad Request' },
    );

    const error = await failure;
    expect(error.code).toBe('AMOUNT_EXCEEDS_MAX');
    expect(error.message).toContain('1000');
  });
});
