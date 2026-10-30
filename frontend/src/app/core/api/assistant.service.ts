import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ChatRequest, ChatResponse } from '../models/banking.models';

/**
 * Data access for assistant-service.
 *
 * It is not routed through the gateway, which defines no route for it, so it has its own base URL.
 * The frontend never performs a banking operation on the assistant's behalf: it only relays messages.
 */
@Injectable({ providedIn: 'root' })
export class AssistantService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.assistantBaseUrl}/api/assistant`;

  chat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  /** Names of the banking tools the assistant discovered over MCP. */
  tools(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/tools`);
  }

  reset(conversationId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/conversations/${conversationId}`);
  }
}
