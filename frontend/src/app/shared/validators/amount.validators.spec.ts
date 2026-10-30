import { FormControl, FormGroup } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import { amountValidator, currencyValidator, differentAccountsValidator } from './amount.validators';

describe('amountValidator', () => {
  const check = (value: unknown) => amountValidator(new FormControl(value));

  it('accepts a positive amount with at most two decimals', () => {
    expect(check('250.00')).toBeNull();
    expect(check('0.01')).toBeNull();
    expect(check(1000)).toBeNull();
  });

  it('leaves the empty case to the required validator', () => {
    expect(check('')).toBeNull();
    expect(check(null)).toBeNull();
  });

  it('rejects zero and negative amounts', () => {
    expect(check('0')).toEqual({ amountPositive: true });
    expect(check('-5')).toEqual({ amountPositive: true });
  });

  it('rejects a non-numeric amount', () => {
    expect(check('abc')).toEqual({ amountNumber: true });
  });

  it('rejects more than two decimals, as the backend does', () => {
    expect(check('1.005')).toEqual({ amountDecimals: true });
  });

  it('accepts trailing zeros beyond two decimals', () => {
    expect(check('1.5000')).toBeNull();
  });

  it('accepts a comma as the decimal separator', () => {
    expect(check('12,50')).toBeNull();
  });
});

describe('currencyValidator', () => {
  const check = (value: unknown) => currencyValidator(new FormControl(value));

  it('accepts three uppercase letters', () => {
    expect(check('EUR')).toBeNull();
  });

  it('rejects anything else', () => {
    expect(check('eur')).toEqual({ currencyFormat: true });
    expect(check('EURO')).toEqual({ currencyFormat: true });
    expect(check('E1R')).toEqual({ currencyFormat: true });
  });
});

describe('differentAccountsValidator', () => {
  const group = (source: string, destination: string) =>
    new FormGroup(
      { source: new FormControl(source), destination: new FormControl(destination) },
      { validators: differentAccountsValidator('source', 'destination') },
    );

  it('rejects a transfer to the same account', () => {
    expect(group('a', 'a').errors).toEqual({ sameAccount: true });
  });

  it('accepts two different accounts', () => {
    expect(group('a', 'b').errors).toBeNull();
  });

  it('stays silent while the form is incomplete', () => {
    expect(group('', '').errors).toBeNull();
  });
});
