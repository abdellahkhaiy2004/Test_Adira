import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

/** Every route but /login is behind the guard. Pages are lazily loaded. */
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/login-page').then((m) => m.LoginPage),
    title: 'Authentification — Banque',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard-page').then((m) => m.DashboardPage),
    title: 'Tableau de bord — Banque',
  },
  {
    path: 'accounts',
    canActivate: [authGuard],
    loadComponent: () => import('./features/accounts/accounts-page').then((m) => m.AccountsPage),
    title: 'Comptes — Banque',
  },
  {
    path: 'accounts/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/accounts/account-detail-page').then((m) => m.AccountDetailPage),
    title: 'Détail du compte — Banque',
  },
  {
    path: 'transactions',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transactions/transactions-page').then((m) => m.TransactionsPage),
    title: 'Transactions — Banque',
  },
  {
    path: 'transactions/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transactions/new-transaction-page').then((m) => m.NewTransactionPage),
    title: 'Nouvelle transaction — Banque',
  },
  {
    path: 'transfers',
    canActivate: [authGuard],
    loadComponent: () => import('./features/transfers/transfer-page').then((m) => m.TransferPage),
    title: 'Virement — Banque',
  },
  {
    path: 'assistant',
    canActivate: [authGuard],
    loadComponent: () => import('./features/assistant/assistant-page').then((m) => m.AssistantPage),
    title: 'Assistant — Banque',
  },
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/not-found-page').then((m) => m.NotFoundPage),
    title: 'Page introuvable — Banque',
  },
];
