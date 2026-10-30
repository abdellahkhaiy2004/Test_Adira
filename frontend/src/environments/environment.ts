/**
 * Production configuration.
 *
 * The API Gateway is the single entry point for the banking APIs. The assistant is not behind the
 * gateway because no route exists for it there, so it keeps its own base URL.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  assistantBaseUrl: '/assistant-api',
};
