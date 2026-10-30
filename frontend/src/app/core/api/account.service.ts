import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Account, AccountStatus, CreateAccountRequest } from '../models/banking.models';

/** Data access for account-service, through the API Gateway. */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/accounts`;

  list(customerReference?: string): Observable<Account[]> {
    let params = new HttpParams();
    if (customerReference?.trim()) {
      params = params.set('customerReference', customerReference.trim());
    }
    return this.http.get<Account[]>(this.baseUrl, { params });
  }

  get(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateAccountRequest): Observable<Account> {
    return this.http.post<Account>(this.baseUrl, request);
  }

  updateStatus(id: string, status: AccountStatus): Observable<Account> {
    return this.http.patch<Account>(`${this.baseUrl}/${id}/status`, { status });
  }
}
