import { Injectable, signal } from '@angular/core';

export type NotificationKind = 'success' | 'error';

export interface Notification {
  id: number;
  kind: NotificationKind;
  message: string;
}

/** Transient messages shown by the shell. The only state shared between features. */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly items = signal<Notification[]>([]);
  private nextId = 1;

  readonly notifications = this.items.asReadonly();

  success(message: string): void {
    this.push('success', message);
  }

  error(message: string): void {
    this.push('error', message);
  }

  dismiss(id: number): void {
    this.items.update((current) => current.filter((item) => item.id !== id));
  }

  private push(kind: NotificationKind, message: string): void {
    const id = this.nextId++;
    this.items.update((current) => [...current, { id, kind, message }]);
    // Errors stay until dismissed; successes fade on their own.
    if (kind === 'success') {
      setTimeout(() => this.dismiss(id), 5000);
    }
  }
}
