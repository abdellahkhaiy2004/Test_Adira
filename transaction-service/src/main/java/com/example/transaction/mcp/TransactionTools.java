package com.example.transaction.mcp;

import com.example.transaction.domain.Transaction;
import com.example.transaction.domain.Transfer;
import com.example.transaction.service.TransactionService;
import com.example.transaction.service.TransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Banking tools published over MCP. Every tool delegates to the same service layer REST and GraphQL use,
 * so an assistant is subject to the identical business rules (amount ceiling, active account, currency
 * match, balance check) and can never move money outside them.
 */
@Component
@RequiredArgsConstructor
public class TransactionTools {

    private final TransactionService transactionService;
    private final TransferService transferService;

    public record TransactionInfo(String id, String accountId, String amount, String currency,
                                  String merchantCategory, String country, String createdAt) {

        static TransactionInfo from(Transaction t) {
            return new TransactionInfo(t.getId().toString(), t.getAccountId().toString(),
                    t.getAmount().toPlainString(), t.getCurrency(), t.getMerchantCategory(), t.getCountry(),
                    t.getCreatedAt().toString());
        }
    }

    public record TransferInfo(String id, String sourceAccountId, String destinationAccountId, String amount,
                               String currency, String createdAt) {

        static TransferInfo from(Transfer t) {
            return new TransferInfo(t.getId().toString(), t.getSourceAccountId().toString(),
                    t.getDestinationAccountId().toString(), t.getAmount().toPlainString(), t.getCurrency(),
                    t.getCreatedAt().toString());
        }
    }

    @Tool(name = "list_transactions",
            description = "List the transaction history, newest first. Filter by account when an id is given.")
    public List<TransactionInfo> listTransactions(
            @ToolParam(description = "Account id (UUID) to filter on; omit for every account",
                    required = false) String accountId) {
        UUID id = (accountId == null || accountId.isBlank()) ? null : UUID.fromString(accountId);
        return transactionService.getHistory(id).stream().map(TransactionInfo::from).toList();
    }

    @Tool(name = "get_transaction", description = "Get one transaction by its id.")
    public TransactionInfo getTransaction(
            @ToolParam(description = "Transaction id (UUID)") String transactionId) {
        return TransactionInfo.from(transactionService.getTransaction(UUID.fromString(transactionId)));
    }

    @Tool(name = "create_transaction",
            description = "Record a payment that debits an account. The account must exist, be ACTIVE, use the "
                    + "same currency, hold enough funds, and the amount must respect the configured ceiling.")
    public TransactionInfo createTransaction(
            @ToolParam(description = "Account id (UUID) to debit") String accountId,
            @ToolParam(description = "Amount, strictly positive, at most 2 decimals, e.g. 250.00") String amount,
            @ToolParam(description = "ISO currency code in uppercase, e.g. EUR") String currency,
            @ToolParam(description = "Merchant category, e.g. GROCERY") String merchantCategory,
            @ToolParam(description = "Country code, e.g. MA") String country) {
        return TransactionInfo.from(transactionService.createTransaction(
                UUID.fromString(accountId), new BigDecimal(amount), currency, merchantCategory, country));
    }

    @Tool(name = "transfer_money",
            description = "Transfer money from one account to another. Both accounts must exist, be ACTIVE and "
                    + "share the same currency; the source must hold enough funds.")
    public TransferInfo transferMoney(
            @ToolParam(description = "Source account id (UUID), debited") String sourceAccountId,
            @ToolParam(description = "Destination account id (UUID), credited") String destinationAccountId,
            @ToolParam(description = "Amount, strictly positive, at most 2 decimals") String amount,
            @ToolParam(description = "ISO currency code in uppercase, e.g. EUR") String currency,
            @ToolParam(description = "Country code, e.g. MA") String country) {
        return TransferInfo.from(transferService.transfer(UUID.fromString(sourceAccountId),
                UUID.fromString(destinationAccountId), new BigDecimal(amount), currency, country));
    }
}
