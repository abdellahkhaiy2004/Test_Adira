package com.example.account.mcp;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Banking tools published over MCP. Every tool delegates to {@link AccountService}, the same layer REST and
 * GraphQL use, so an assistant can never bypass a business rule.
 *
 * <p>Balance movements (debit / credit) are deliberately NOT exposed here: moving money must go through
 * transaction-service, which enforces the transaction rules (ceiling, currency, history).
 */
@Component
@RequiredArgsConstructor
public class AccountTools {

    private final AccountService accountService;

    public record AccountInfo(String id, String customerReference, String status, String currency,
                              double balance) {

        static AccountInfo from(Account a) {
            return new AccountInfo(a.getId().toString(), a.getCustomerReference(), a.getStatus().name(),
                    a.getCurrency(), a.getBalance());
        }
    }

    @Tool(name = "list_accounts",
            description = "List bank accounts. Optionally filter by customer reference.")
    public List<AccountInfo> listAccounts(
            @ToolParam(description = "Customer reference to filter on; omit to list every account",
                    required = false) String customerReference) {
        return accountService.listAccounts(customerReference).stream().map(AccountInfo::from).toList();
    }

    @Tool(name = "get_account",
            description = "Get one bank account by its id, including its current balance and status.")
    public AccountInfo getAccount(
            @ToolParam(description = "Account id (UUID)") String accountId) {
        return AccountInfo.from(accountService.getAccount(UUID.fromString(accountId)));
    }

    @Tool(name = "create_account",
            description = "Open a new bank account for a customer. The account starts ACTIVE.")
    public AccountInfo createAccount(
            @ToolParam(description = "Customer reference, e.g. CUST-1") String customerReference,
            @ToolParam(description = "ISO currency code in uppercase, e.g. EUR") String currency,
            @ToolParam(description = "Opening balance, 0 or more", required = false) Double initialBalance) {
        return AccountInfo.from(
                accountService.createAccount(customerReference, currency, initialBalance, AccountStatus.ACTIVE));
    }

    @Tool(name = "set_account_status",
            description = "Block or reactivate a bank account. A BLOCKED account cannot be debited or credited.")
    public AccountInfo setAccountStatus(
            @ToolParam(description = "Account id (UUID)") String accountId,
            @ToolParam(description = "New status: ACTIVE or BLOCKED") String status) {
        return AccountInfo.from(accountService.updateStatus(UUID.fromString(accountId),
                AccountStatus.valueOf(status.trim().toUpperCase())));
    }
}
