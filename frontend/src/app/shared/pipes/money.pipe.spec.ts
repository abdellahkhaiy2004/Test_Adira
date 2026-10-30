import { describe, expect, it } from 'vitest';

import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  /** fr-FR uses a narrow no-break space as the thousands separator. */
  const normalise = (value: string) => value.replace(/ | /g, ' ');

  it('formats an amount with two decimals and its currency', () => {
    expect(normalise(pipe.transform(1234.5, 'EUR'))).toBe('1 234,50 EUR');
  });

  it('always shows two decimals', () => {
    expect(normalise(pipe.transform(10, 'MAD'))).toBe('10,00 MAD');
  });

  it('formats zero rather than hiding it', () => {
    expect(normalise(pipe.transform(0, 'EUR'))).toBe('0,00 EUR');
  });

  it('shows a dash when there is no amount', () => {
    expect(pipe.transform(null, 'EUR')).toBe('—');
    expect(pipe.transform(undefined, 'EUR')).toBe('—');
  });

  it('omits the currency when none is given', () => {
    expect(normalise(pipe.transform(5, null))).toBe('5,00');
  });
});
