import { Component, input } from '@angular/core';

import { FR } from '../../core/i18n/fr';
import { AccountStatus } from '../../core/models/banking.models';

/** Account status. Colour is never the only signal: the label carries the meaning. */
@Component({
  selector: 'app-status-badge',
  template: `
    <span class="badge" [class.badge--blocked]="status() === 'BLOCKED'">
      <span class="badge__dot" aria-hidden="true"></span>
      {{ label() }}
    </span>
  `,
  styles: `
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      padding: 0.15rem 0.6rem;
      border-radius: 999px;
      font-size: 0.8125rem;
      font-weight: 600;
      color: #0f5132;
      background: #d1e7dd;
      border: 1px solid #a3cfbb;
      white-space: nowrap;
    }
    .badge--blocked {
      color: #842029;
      background: #f8d7da;
      border-color: #f1aeb5;
    }
    .badge__dot {
      width: 0.5rem;
      height: 0.5rem;
      border-radius: 50%;
      background: currentColor;
    }
  `,
})
export class StatusBadge {
  readonly status = input.required<AccountStatus>();

  label(): string {
    return FR.status[this.status()] ?? this.status();
  }
}
