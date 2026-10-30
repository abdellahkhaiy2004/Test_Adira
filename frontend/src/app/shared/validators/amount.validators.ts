import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Client-side mirrors of the backend's input rules. They only spare the user a round trip; the bank
 * remains the authority and its refusals are displayed verbatim.
 *
 * The configured amount ceiling is deliberately absent: it lives in the Config Server and only the
 * backend knows its value.
 */

/** Strictly positive, numeric, at most two decimals. */
export const amountValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const raw = control.value;
  if (raw === null || raw === undefined || raw === '') {
    return null; // `required` reports an empty field.
  }

  const value = typeof raw === 'number' ? raw : Number(String(raw).replace(',', '.'));
  if (Number.isNaN(value)) {
    return { amountNumber: true };
  }
  if (value <= 0) {
    return { amountPositive: true };
  }

  const decimals = String(raw).replace(',', '.').split('.')[1];
  if (decimals && decimals.replace(/0+$/, '').length > 2) {
    return { amountDecimals: true };
  }
  return null;
};

/** Three uppercase letters, as the backend requires. */
export const currencyValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) {
    return null;
  }
  return /^[A-Z]{3}$/.test(String(value)) ? null : { currencyFormat: true };
};

/** Form-level check that a transfer does not target its own source. */
export function differentAccountsValidator(
  sourceKey: string,
  destinationKey: string,
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const source = group.get(sourceKey)?.value;
    const destination = group.get(destinationKey)?.value;
    if (source && destination && source === destination) {
      return { sameAccount: true };
    }
    return null;
  };
}
