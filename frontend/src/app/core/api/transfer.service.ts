import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Transfer, TransferRequest } from '../models/banking.models';

/** Data access for the transfer endpoints of transaction-service. */
@Injectable({ providedIn: 'root' })
export class TransferService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/api/transfers`;

  transfer(request: TransferRequest): Observable<Transfer> {
    return this.http.post<Transfer>(this.baseUrl, request);
  }

  get(id: string): Observable<Transfer> {
    return this.http.get<Transfer>(`${this.baseUrl}/${id}`);
  }
}
