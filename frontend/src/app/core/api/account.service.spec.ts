import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { errorInterceptor } from '../http/error.interceptor';
import { ApiError } from '../models/banking.models';
import { AccountService } from './account.service';

describe('AccountService', () => {
  let service: AccountService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(AccountService);
    http = TestBed.inject(HttpTestingController);
  });

  it('requests the accounts list without a filter', () => {
    service.list().subscribe();

    const request = http.expectOne((r) => r.url.endsWith('/api/accounts'));
    expect(request.request.method).toBe('GET');
    expect(request.request.params.has('customerReference')).toBe(false);
    request.flush([]);
    http.verify();
  });

  it('passes the customer reference as a query parameter', () => {
    service.list('  CUST-1  ').subscribe();

    const request = http.expectOne((r) => r.params.get('customerReference') === 'CUST-1');
    request.flush([]);
    http.verify();
  });

  it('posts the account payload on create', () => {
    service.create({ customerReference: 'CUST-1', currency: 'EUR', balance: 50 }).subscribe();

    const request = http.expectOne((r) => r.method === 'POST');
    expect(request.request.body).toEqual({
      customerReference: 'CUST-1',
      currency: 'EUR',
      balance: 50,
    });
    request.flush({});
    http.verify();
  });

  it('patches only the status', () => {
    service.updateStatus('abc', 'BLOCKED').subscribe();

    const request = http.expectOne((r) => r.method === 'PATCH');
    expect(request.request.url).toContain('/api/accounts/abc/status');
    expect(request.request.body).toEqual({ status: 'BLOCKED' });
    request.flush({});
    http.verify();
  });

  it('surfaces the backend code and message unchanged', async () => {
    const failure = new Promise<ApiError>((resolve) => {
      service.get('missing').subscribe({ error: resolve });
    });

    http.expectOne((r) => r.url.endsWith('/api/accounts/missing')).flush(
      { status: 404, error: 'Not Found', code: 'ACCOUNT_NOT_FOUND', message: 'Account not found: missing' },
      { status: 404, statusText: 'Not Found' },
    );

    const error = await failure;
    expect(error.code).toBe('ACCOUNT_NOT_FOUND');
    expect(error.message).toBe('Account not found: missing');
  });

  it('reports an unreachable gateway as a network error', async () => {
    const failure = new Promise<ApiError>((resolve) => {
      service.list().subscribe({ error: resolve });
    });

    http.expectOne((r) => r.url.endsWith('/api/accounts')).error(new ProgressEvent('error'));

    const error = await failure;
    expect(error.code).toBe('NETWORK_ERROR');
    expect(error.status).toBe(0);
  });
});
