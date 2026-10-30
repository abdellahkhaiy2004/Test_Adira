import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CreateTransactionRequest, Transaction } from '../models/banking.models';

/** Data access for transaction-service, through the API Gateway. */
@Injectable({ providedIn: 'root' })
export class TransactionService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/transactions`;

  /** History, newest first. Without an account id the backend returns every transaction. */
  list(accountId?: string): Observable<Transaction[]> {
    let params = new HttpParams();
    if (accountId?.trim()) {
      params = params.set('accountId', accountId.trim());
    }
    return this.http.get<Transaction[]>(this.baseUrl, { params });
  }

  get(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateTransactionRequest): Observable<Transaction> {
    return this.http.post<Transaction>(this.baseUrl, request);
  }
}
