import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService } from '../auth/auth.service';

/**
 * Attaches the bearer token when there is one.
 *
 * `getToken()` returns null until Keycloak is wired up, so today no header is added. Sending a
 * placeholder would make an unprotected backend look authenticated, which it is not.
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthService).getToken();
  if (!token) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
