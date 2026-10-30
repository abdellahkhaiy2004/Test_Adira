import { Component, input, output } from '@angular/core';

import { FR } from '../../core/i18n/fr';
import { ApiError } from '../../core/models/banking.models';

/**
 * The three non-loaded states of every asynchronous view, in one place: loading, error, empty.
 * Renders nothing when there is nothing to say, so a view can keep it permanently in its template.
 */
@Component({
  selector: 'app-state-panel',
  template: `
    @if (loading()) {
      <div class="panel" role="status" aria-live="polite">
        <span class="spinner" aria-hidden="true"></span>
        <p>{{ text.loading }}</p>
      </div>
    } @else if (error(); as apiError) {
      <div class="panel panel--error" role="alert">
        <p class="panel__title">{{ text.errorTitle }}</p>
        <p class="panel__message">{{ apiError.message }}</p>
        @if (apiError.code && apiError.code !== 'ERROR') {
          <p class="panel__code">Code : {{ apiError.code }}</p>
        }
        @if (retryable()) {
          <button type="button" class="btn" (click)="retry.emit()">{{ text.retry }}</button>
        }
      </div>
    } @else if (empty()) {
      <div class="panel panel--empty">
        <p>{{ emptyMessage() }}</p>
      </div>
    }
  `,
  styles: `
    .panel {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
      padding: 2rem 1rem;
      text-align: center;
      border: 1px dashed var(--border);
      border-radius: 8px;
      color: var(--text-muted);
      background: var(--surface);
    }
    .panel--error {
      border-style: solid;
      border-color: #f1aeb5;
      background: #fdf2f3;
      color: #842029;
    }
    .panel__title { font-weight: 600; margin: 0; }
    .panel__message { margin: 0; }
    .panel__code { margin: 0; font-size: 0.8125rem; opacity: 0.8; }
    .spinner {
      width: 1.5rem;
      height: 1.5rem;
      border: 2px solid var(--border);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) {
      .spinner { animation: none; }
    }
  `,
})
export class StatePanel {
  readonly loading = input(false);
  readonly error = input<ApiError | null>(null);
  readonly empty = input(false);
  // Explicitly string: FR is `as const`, so the default alone would narrow this to a literal type.
  readonly emptyMessage = input<string>(FR.common.none);
  readonly retryable = input(true);

  readonly retry = output<void>();

  protected readonly text = {
    loading: FR.common.loading,
    retry: FR.common.retry,
    errorTitle: FR.errors.title,
  };
}
