import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';

import { errorInterceptor } from '../../core/http/error.interceptor';
import { AssistantPage } from './assistant-page';

describe('AssistantPage', () => {
  let fixture: ComponentFixture<AssistantPage>;
  let http: HttpTestingController;

  const text = () => fixture.nativeElement.textContent as string;

  const send = (message: string) => {
    (fixture.componentInstance as any).form.controls.message.setValue(message);
    (fixture.componentInstance as any).send();
    fixture.detectChanges();
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AssistantPage],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    fixture = TestBed.createComponent(AssistantPage);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  it('lists the banking tools discovered over MCP', async () => {
    http.expectOne((r) => r.url.endsWith('/api/assistant/tools')).flush(['get_account', 'transfer_money']);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('get_account');
    expect(text()).toContain('transfer_money');
  });

  it('says the assistant can perform no operation when no tool is available', async () => {
    http.expectOne((r) => r.url.endsWith('/api/assistant/tools')).flush([]);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('Aucun outil bancaire');
  });

  it('sends the message and shows the reply', async () => {
    http.expectOne((r) => r.url.endsWith('/api/assistant/tools')).flush([]);
    send('Quels sont mes comptes ?');

    const request = http.expectOne((r) => r.url.endsWith('/api/assistant/chat'));
    expect(request.request.body.message).toBe('Quels sont mes comptes ?');
    // No conversation id on the first turn: the backend creates one.
    expect(request.request.body.conversationId).toBeUndefined();

    request.flush({ conversationId: 'conv-1', reply: 'Vous avez deux comptes.' });
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('Quels sont mes comptes ?');
    expect(text()).toContain('Vous avez deux comptes.');
  });

  it('keeps the conversation id for the following turns', async () => {
    http.expectOne((r) => r.url.endsWith('/api/assistant/tools')).flush([]);

    send('premier');
    http.expectOne((r) => r.url.endsWith('/api/assistant/chat')).flush({
      conversationId: 'conv-1',
      reply: 'ok',
    });
    fixture.detectChanges();
    await fixture.whenStable();

    send('second');
    const second = http.expectOne((r) => r.url.endsWith('/api/assistant/chat'));
    expect(second.request.body.conversationId).toBe('conv-1');
    second.flush({ conversationId: 'conv-1', reply: 'ok' });
  });

  it('reports a failed turn instead of inventing an answer', async () => {
    http.expectOne((r) => r.url.endsWith('/api/assistant/tools')).flush([]);
    send('Combien ai-je ?');

    http.expectOne((r) => r.url.endsWith('/api/assistant/chat')).flush(
      {
        status: 503,
        error: 'Service Unavailable',
        code: 'ASSISTANT_UNAVAILABLE',
        message: 'The assistant could not complete the request',
      },
      { status: 503, statusText: 'Service Unavailable' },
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(text()).toContain('n’a pas pu être traité');
    expect(text()).toContain('The assistant could not complete the request');
  });
});
