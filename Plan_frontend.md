# Plan_frontend.md — Angular frontend for the banking platform

Companion to `architecture.md` and `PLAN.md`. Written before the implementation and followed by it.
Interface language: **French**. Code, identifiers and comments: English.

---

## 1. Frontend objectives

A single-page Angular application letting an authorised user:

- consult bank accounts and their balances;
- consult transaction history;
- record a transaction;
- transfer money between two accounts;
- talk to the conversational banking assistant.

The frontend never reaches a database and never applies a banking rule as if it were authoritative. It
calls the backend HTTP APIs, shows what they return, and reports their refusals verbatim. Client-side
validation exists only to spare the user a pointless round trip.

## 2. User roles and interaction boundaries

The examination defines no authorisation model, and the backend currently has **no security layer**
(no Keycloak, no tokens, every endpoint is open). The frontend therefore recognises a single role,
*bank user*, and does not pretend otherwise.

| Boundary | Rule |
|---|---|
| Business rules | Enforced by the backend only. The UI mirrors them, never replaces them. |
| Money movement | Only through `POST /api/transactions` and `POST /api/transfers`. |
| Assistant actions | Only through `POST /api/assistant/chat`. The UI never calls a banking endpoint on the assistant's behalf. |
| Success | Displayed only after a 2xx response carrying the created resource. |

## 3. UI/UX principles

- Sober, professional, high-contrast; no decorative animation.
- One primary action per screen, visually dominant.
- Money always rendered with its currency and two decimals, right-aligned, via a shared pipe.
- Every asynchronous view has four explicit states: **loading**, **empty**, **error**, **loaded**.
- Destructive or financial actions require an explicit confirmation step.
- Errors show the backend's own `message` and `code`, never a generic "something went wrong".
- Accessibility: labelled inputs, `aria-live` for notifications, visible focus, keyboard-reachable
  controls, colour never the sole carrier of meaning.
- Internationalisation-ready: all visible strings sit in one `fr` dictionary
  (`core/i18n/fr.ts`), so a second locale is a new file, not a rewrite.

## 4. Application navigation

```
/login                      Authentication entry point
/                           → redirects to /dashboard
/dashboard                  Overview: accounts, balances, recent transactions, quick actions
/accounts                   Accounts list
/accounts/:id               Account details
/transactions               Transaction history (filter by account)
/transactions/new           New transaction form
/transfers                  Transfer form + confirmation
/assistant                  Conversational assistant
**                          404
```

All routes except `/login` are behind `authGuard`.

## 5. Page-by-page specifications

### A. Authentication (`/login`)
The backend exposes **no authentication endpoint**. Keycloak is named in the examination context but is
not implemented server-side, so the page must not imitate a real login.

- The screen states plainly that authentication is not yet wired to Keycloak and that the backend is
  currently unprotected.
- A single "Entrer dans la démonstration" button sets a local session flag so routing and guards can be
  demonstrated.
- No password field, no credential storage, no fake token.
- `AuthService` is written as the single seam where `keycloak-js` will later plug in: `login()`,
  `logout()`, `isAuthenticated()`, `getToken()`. `getToken()` returns `null` today, and the HTTP
  interceptor therefore attaches no `Authorization` header.

### B. Dashboard (`/dashboard`)
- Account cards: reference, status badge (ACTIVE / BLOCKED), currency, balance.
- Totals per currency (computed client-side from the accounts returned; a display aid, not a bank figure).
- The five most recent transactions, newest first.
- Quick actions to `/transactions/new`, `/transfers`, `/assistant`.
- Backend unreachable → error panel with a retry button. No placeholder figures are ever shown.

### C. Accounts (`/accounts`, `/accounts/:id`)
- Table: reference, status, currency, balance, link to details.
- Filter by `customerReference` (server-side, the parameter the API supports).
- Details view: the account's fields plus its transaction history.
- Create-account form (reference, currency, optional opening balance).
- Block / reactivate via `PATCH /api/accounts/{id}/status`, behind a confirmation.

### D. Transactions (`/transactions`)
Columns, exactly the fields the API returns and no others: id, accountId, amount, currency,
merchantCategory, country, createdAt.

- Filter by account (`?accountId=`), the only filter the backend supports.
- Pagination is **client-side**, and labelled as such: the API returns the full list with no paging
  parameters. No page/size parameter is invented.
- Transfer legs are recognisable by `merchantCategory` = `TRANSFER_OUT` / `TRANSFER_IN`.

### E. New transaction (`/transactions/new`)
Fields: account (select, from the accounts API), amount, currency, merchant category, country.

Client-side validation: all required; amount strictly positive, numeric, at most 2 decimals; currency
3 uppercase letters; currency pre-filled from the chosen account and flagged when the user diverges.

The amount ceiling (`bank.transaction.max-amount`) is **not** duplicated in the frontend — it is
configuration owned by the Config Server, and only the backend decides. A 400 `AMOUNT_EXCEEDS_MAX` is
displayed as returned.

Submission is disabled while in flight, so a double click cannot create two transactions.

### F. Transfer (`/transfers`)
Fields: source account, destination account, amount, currency, country.

- Source ≠ destination, checked client-side at form level.
- Currency defaults from the source account; a destination in another currency is flagged before
  submitting, while the backend remains the authority (422 `CURRENCY_MISMATCH`).
- Explicit confirmation step restating amount, currency and both accounts.
- The result panel appears **only** after a 201 carrying the transfer id. Any failure shows the
  backend code and message, and the balances are re-read rather than guessed.

### G. Assistant (`/assistant`)
- Conversation thread, user and assistant messages visually distinct.
- `conversationId` kept in component state and returned to the backend on each turn.
- "L'assistant réfléchit…" indicator while the request is in flight; the input is disabled meanwhile.
- The tools the assistant actually discovered are listed from `GET /api/assistant/tools`; when the list
  is empty the UI says the assistant can converse but perform no banking operation.
- A 503 `ASSISTANT_UNAVAILABLE` (no API key, model unreachable) is shown as an explicit failure of the
  turn. The UI never invents a reply.

## 6. User interaction flows

Every financial operation follows the same eight steps:

| Step | Transaction | Transfer |
|---|---|---|
| 1. Initial | Empty form, accounts loaded | Empty form, accounts loaded |
| 2. Action | User fills and submits | User fills and submits |
| 3. Validation | Client checks; errors shown inline, no request sent | Client checks incl. source ≠ destination |
| 4. Request | `POST /api/transactions` | Confirmation step, then `POST /api/transfers` |
| 5. Loading | Button disabled, spinner | Button disabled, spinner |
| 6. Success | 201 → success panel with the created id, form reset | 201 → result panel with the transfer id |
| 7. Failure | Error panel with backend `code` + `message`; form keeps its values | Same; no partial success is ever implied |
| 8. Feedback | Link to the account, balances re-read from the API | Both balances re-read from the API |

## 7. Angular architecture

Angular 21, standalone components, signals for local state, typed reactive forms.

- **core/** — things instantiated once: HTTP services, interceptors, guards, i18n, configuration.
- **shared/** — presentational, reusable, business-free: badges, panels, spinner, money pipe.
- **features/** — one folder per screen, each a standalone routed component.
- Components never call `HttpClient` directly; they depend on a service in `core/api/`.

Angular capabilities only: Router, HttpClient, Reactive Forms, guards, interceptors. No UI framework,
no state-management library, no HTTP wrapper.

## 8. Folder structure

```
frontend/src/app/
├── core/
│   ├── api/            account.service.ts, transaction.service.ts,
│   │                   transfer.service.ts, assistant.service.ts
│   ├── auth/           auth.service.ts, auth.guard.ts
│   ├── http/           error.interceptor.ts, auth.interceptor.ts
│   ├── i18n/           fr.ts
│   ├── models/         account.model.ts, transaction.model.ts,
│   │                   transfer.model.ts, assistant.model.ts, api-error.model.ts
│   └── notifications/  notification.service.ts
├── shared/
│   ├── components/     status-badge, state-panel, confirm-dialog, page-header
│   └── pipes/          money.pipe.ts
├── features/
│   ├── login/ dashboard/ accounts/ transactions/ transfers/ assistant/ not-found/
├── app.config.ts
├── app.routes.ts
└── app.ts / app.html / app.scss
environments/environment.ts, environment.development.ts
```

## 9. Component responsibilities

| Component | Responsibility |
|---|---|
| `App` | Shell: navigation, notification host, router outlet |
| `LoginPage` | Explains the authentication status, enters the demonstration |
| `DashboardPage` | Aggregates accounts + recent transactions, quick actions |
| `AccountsPage` | Lists, filters and creates accounts; status changes |
| `AccountDetailPage` | One account and its history |
| `TransactionsPage` | History, account filter, client-side pagination |
| `NewTransactionPage` | Transaction form, validation, submission, result |
| `TransferPage` | Transfer form, confirmation, result |
| `AssistantPage` | Chat thread, tool list, loading and error states |
| `StatusBadge` | Renders ACTIVE / BLOCKED |
| `StatePanel` | One component for loading / empty / error |
| `MoneyPipe` | `1234.5, "EUR"` → `1 234,50 EUR` |

## 10. API integration contracts

Base URL: the **API Gateway**, `http://localhost:8080`, in `environment.apiBaseUrl`.
The assistant is not routed through the gateway (no route exists for it), so it has its own
`environment.assistantBaseUrl` = `http://localhost:8084`.

All verified against the implemented backend:

| Feature | Method | Path | Request | Response |
|---|---|---|---|---|
| List accounts | GET | `/api/accounts?customerReference=` | — | `Account[]` |
| Get account | GET | `/api/accounts/{id}` | — | `Account` |
| Create account | POST | `/api/accounts` | `{customerReference, currency, balance?, status?}` | `Account` (201) |
| Change status | PATCH | `/api/accounts/{id}/status` | `{status}` | `Account` |
| History | GET | `/api/transactions?accountId=` | — | `Transaction[]` |
| Get transaction | GET | `/api/transactions/{id}` | — | `Transaction` |
| Create transaction | POST | `/api/transactions` | `{accountId, amount, currency, merchantCategory, country}` | `Transaction` (201) |
| Transfer | POST | `/api/transfers` | `{sourceAccountId, destinationAccountId, amount, currency, country}` | `Transfer` (201) |
| Get transfer | GET | `/api/transfers/{id}` | — | `Transfer` |
| Chat | POST | `/api/assistant/chat` | `{message, conversationId?}` | `{conversationId, reply}` |
| Tools | GET | `/api/assistant/tools` | — | `string[]` |

`Account`: `{id, customerReference, status, currency, balance}` — `balance` is a number (double).
`Transaction`: `{id, accountId, amount, currency, merchantCategory, country, createdAt}`.
`Transfer`: `{id, sourceAccountId, destinationAccountId, amount, currency, createdAt}`.

Error body, identical across services: `{status, error, code, message}`.

| code | HTTP | Meaning shown to the user |
|---|---|---|
| `INVALID_REQUEST` | 400 | Invalid input |
| `AMOUNT_EXCEEDS_MAX` | 400 | Above the configured ceiling |
| `ACCOUNT_NOT_FOUND` / `NOT_FOUND` | 404 | Unknown account / resource |
| `ACCOUNT_NOT_ACTIVE` | 409 | Blocked account |
| `INSUFFICIENT_FUNDS` | 409 | Not enough funds |
| `REJECTED_BY_ACCOUNT_SERVICE` | 409 | Refused on execution |
| `CURRENCY_MISMATCH` | 422 | Currency differs from the account's |
| `ACCOUNT_SERVICE_UNAVAILABLE` | 503 | Account service down; **nothing was applied** |
| `ASSISTANT_UNAVAILABLE` | 503 | Assistant turn failed |

**Deliberately not used:** GraphQL and MCP. Both exist on the backend and expose the same operations,
but a single transport keeps the client simple; REST through the gateway is the documented entry point.

**Provisional / not available:** there is no authentication endpoint, no paging parameter, no account
deletion, and no endpoint returning the configured amount ceiling. None of these is called.

## 11. Authentication integration

Target: Keycloak, with the gateway validating a bearer token. **Not implemented on either side.**

What the frontend does today:
- `AuthService` holds the session state behind the exact API a Keycloak adapter would satisfy.
- `authGuard` redirects unauthenticated users to `/login`.
- `authInterceptor` attaches `Authorization: Bearer …` **only** when `getToken()` returns a token; it
  returns `null` today, so no header is sent and no fake token exists.
- `errorInterceptor` maps 401 → clear session and redirect to `/login`, and 403 → explicit message.

Migration is one file: implement `AuthService` with `keycloak-js` and secure the gateway. Nothing else
changes.

## 12. State and data management

No state library. Each feature component owns its data in signals and asks its service on init.
`NotificationService` is the only shared state (a signal holding transient messages). Account lists are
re-read after any operation that can change a balance, rather than patched locally — the backend is the
only source of truth for money.

## 13. Error handling

`errorInterceptor` normalises every failure into `ApiError {status, code, message}`:
- backend error body → used as-is;
- network failure / status 0 → "Service indisponible. Vérifiez que la passerelle est démarrée.";
- 5xx without a body → generic message plus the status.

Components render errors through `StatePanel` (page-level) or `NotificationService` (action-level).
A failed operation never clears a form.

## 14. Security

- Only the demonstration session flag is stored, in `sessionStorage`; it is not a credential.
- No account number, balance or token is written to `localStorage`.
- Guards protect navigation only; the document states that this is not authorisation, which the backend
  does not yet perform.
- `Authorization` is attached only when a real token exists.
- Angular's default escaping is relied upon; no `innerHTML` with backend or assistant content.

## 15. Responsive design

Mobile-first, CSS Grid and Flexbox, breakpoints at 640 px and 1024 px. Tables collapse to stacked
definition rows on small screens. Navigation becomes a hamburger below 640 px. Touch targets ≥ 44 px.

## 16. Accessibility

Semantic landmarks; every input has a `<label for>`; invalid fields carry `aria-invalid` and
`aria-describedby` pointing at the message; notifications are `role="status"` / `role="alert"`;
the chat thread is an `aria-live="polite"` log; focus moves to the result panel after submission;
contrast ratio ≥ 4.5:1.

## 17. Testing strategy

Jasmine + Karma (the CLI default), headless Chrome.

| Area | Test |
|---|---|
| Services | `HttpTestingController`: URL, method, body, and error mapping |
| Forms | Required, positive amount, 2 decimals, source ≠ destination |
| Components | Loading / empty / error / loaded rendering |
| Guard | Redirects when unauthenticated, passes when authenticated |
| Transaction | Submits once, shows the backend refusal, no false success |
| Transfer | Confirmation required; success only after a 201 |
| Assistant | Sends the message, keeps `conversationId`, renders a failed turn |
| Pipe | Money formatting |
| Build | `npm run build` must succeed |

## 18. Implementation order

1. Scaffold, environments, models, i18n dictionary.
2. Core: interceptors, auth, API services.
3. Shared components and the money pipe.
4. Shell and routing.
5. Dashboard, accounts.
6. Transactions, new transaction.
7. Transfer.
8. Assistant.
9. Responsive and accessibility pass.
10. Tests, then build verification.

## 19. Acceptance criteria

- `npm run build` succeeds.
- `npm test` passes headless.
- Every route renders, and private routes redirect when the session is absent.
- Every API call goes through a `core/api` service, never from a component.
- No invented endpoint or field; only the contracts in section 10 are called.
- Backend refusals are displayed with their real code and message.
- No success is ever shown without a 2xx carrying the created resource.
- The authentication screen states honestly that Keycloak is not wired up.
- The interface is in French and usable at 360 px wide.
