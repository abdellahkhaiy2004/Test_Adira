import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AssistantService } from '../../core/api/assistant.service';
import { FR } from '../../core/i18n/fr';
import { ApiError } from '../../core/models/banking.models';
import { PageHeader } from '../../shared/components/page-header';

interface ChatEntry {
  id: number;
  author: 'user' | 'assistant';
  text: string;
  failed?: boolean;
}

/**
 * Chat with the banking assistant.
 *
 * The frontend performs no banking operation here: it relays the message to assistant-service, which
 * is the only component allowed to call the banking tools. A failed turn is shown as a failure; no
 * reply is ever invented.
 */
@Component({
  selector: 'app-assistant-page',
  imports: [ReactiveFormsModule, PageHeader],
  templateUrl: './assistant-page.html',
  styles: `
    .layout {
      display: grid;
      gap: 1.25rem;
    }
    @media (min-width: 1024px) {
      .layout {
        grid-template-columns: 1fr 18rem;
        align-items: start;
      }
    }
    .thread {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      min-height: 18rem;
      max-height: 28rem;
      overflow-y: auto;
      padding: 1rem;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius);
    }
    .message {
      max-width: 85%;
      padding: 0.65rem 0.9rem;
      border-radius: var(--radius);
      white-space: pre-wrap;
      overflow-wrap: anywhere;
    }
    .message--user {
      align-self: flex-end;
      background: var(--primary);
      color: #fff;
    }
    .message--assistant {
      align-self: flex-start;
      background: #eef2f6;
      border: 1px solid var(--border);
    }
    .message--failed {
      background: var(--danger-bg);
      border-color: var(--danger-border);
      color: var(--danger);
    }
    .message__author {
      display: block;
      font-size: 0.75rem;
      font-weight: 700;
      opacity: 0.8;
      margin-bottom: 0.2rem;
    }
    .composer {
      display: flex;
      gap: 0.75rem;
      margin-top: 1rem;
      align-items: flex-start;
    }
    .composer .field {
      flex: 1;
      margin-bottom: 0;
    }
    .tools__list {
      margin: 0;
      padding-left: 1.1rem;
    }
    .tools__list li {
      font-family: ui-monospace, Consolas, monospace;
      font-size: 0.8125rem;
    }
    .thinking {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      color: var(--text-muted);
      font-size: 0.9375rem;
    }
  `,
})
export class AssistantPage implements OnInit {
  private readonly assistantService = inject(AssistantService);
  private readonly fb = inject(FormBuilder);

  protected readonly text = FR;
  protected readonly entries = signal<ChatEntry[]>([]);
  protected readonly sending = signal(false);
  protected readonly tools = signal<string[]>([]);
  protected readonly toolsLoaded = signal(false);
  protected readonly toolsError = signal(false);

  private conversationId: string | null = null;
  private nextEntryId = 1;

  protected readonly form = this.fb.nonNullable.group({
    message: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.assistantService.tools().subscribe({
      next: (tools) => {
        this.tools.set(tools);
        this.toolsLoaded.set(true);
      },
      error: () => {
        this.toolsError.set(true);
        this.toolsLoaded.set(true);
      },
    });
  }

  protected send(): void {
    const message = this.form.controls.message.value.trim();
    if (!message || this.sending()) {
      return;
    }

    this.append('user', message);
    this.form.controls.message.setValue('');
    this.sending.set(true);

    this.assistantService
      .chat({ message, ...(this.conversationId ? { conversationId: this.conversationId } : {}) })
      .subscribe({
        next: (response) => {
          this.conversationId = response.conversationId;
          this.append('assistant', response.reply);
          this.sending.set(false);
        },
        error: (apiError: ApiError) => {
          // The turn failed. Say so plainly rather than showing an invented answer.
          this.append('assistant', `${FR.assistant.turnFailed} ${apiError.message}`, true);
          this.sending.set(false);
        },
      });
  }

  protected reset(): void {
    const previous = this.conversationId;
    this.conversationId = null;
    this.entries.set([]);
    if (previous) {
      this.assistantService.reset(previous).subscribe({ error: () => undefined });
    }
  }

  private append(author: 'user' | 'assistant', text: string, failed = false): void {
    this.entries.update((current) => [...current, { id: this.nextEntryId++, author, text, failed }]);
  }
}
