import { Pipe, PipeTransform } from '@angular/core';

/** Formats an amount with its currency in the French convention: `1234.5, 'EUR'` → `1 234,50 EUR`. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(amount: number | null | undefined, currency: string | null | undefined): string {
    if (amount === null || amount === undefined || Number.isNaN(amount)) {
      return '—';
    }
    const formatted = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);

    return currency ? `${formatted} ${currency}` : formatted;
  }
}
