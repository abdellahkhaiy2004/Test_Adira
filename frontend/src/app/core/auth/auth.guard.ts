import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Keeps unauthenticated users out of the private routes.
 *
 * This is navigation control, not authorisation: it protects nothing on the server. Real enforcement
 * belongs to the backend, which does not perform it yet.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }
  return router.createUrlTree(['/login'], { queryParams: { redirectTo: state.url } });
};
