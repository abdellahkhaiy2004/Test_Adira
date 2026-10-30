/**
 * Development configuration: the services started with `mvnw spring-boot:run` on localhost.
 *
 * 8080 is the API Gateway (accounts, transactions, transfers).
 * 8084 is assistant-service, which the gateway does not route.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  assistantBaseUrl: 'http://localhost:8084',
};
