import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { FR } from '../i18n/fr';
import { ApiError } from '../models/banking.models';

/**
 * Turns every failure into a single {@link ApiError} shape, so components never have to know whether a
 * request failed in the network, in the gateway or in a business rule.
 *
 * The backend's own `code` and `message` are kept as they are: a refusal must reach the user unchanged
 * rather than be replaced by a generic sentence.
 */
export const errorInterceptor: HttpInterceptorFn = (request, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);

  return next(request).pipe(
    catchError((response: HttpErrorResponse) => {
      const apiError = toApiError(response);

      if (response.status === 401) {
        auth.logout();
        void router.navigate(['/login']);
      }
      return throwError(() => apiError);
    }),
  );
};

function toApiError(response: HttpErrorResponse): ApiError {
  // Status 0 means the request never reached a server: gateway down, CORS, offline.
  if (response.status === 0) {
    return { status: 0, code: 'NETWORK_ERROR', message: FR.errors.network };
  }

  const body = response.error;
  if (body && typeof body === 'object' && typeof body.message === 'string') {
    return {
      status: response.status,
      code: typeof body.code === 'string' ? body.code : 'ERROR',
      message: body.message,
    };
  }

  switch (response.status) {
    case 401:
      return { status: 401, code: 'UNAUTHORIZED', message: FR.errors.unauthorized };
    case 403:
      return { status: 403, code: 'FORBIDDEN', message: FR.errors.forbidden };
    case 404:
      return { status: 404, code: 'NOT_FOUND', message: FR.errors.notFound };
    default:
      return {
        status: response.status,
        code: 'ERROR',
        message: `${FR.errors.unknown} (HTTP ${response.status})`,
      };
  }
}
