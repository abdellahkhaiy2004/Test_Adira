package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository repository;

    @InjectMocks
    AccountService service;

    @Test
    void createDefaultsToActiveWithZeroBalance() {
        when(repository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        Account a = service.createAccount("  CUST-1 ", "EUR", null, null);

        assertThat(a.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(a.getBalance()).isZero();
        assertThat(a.getCustomerReference()).isEqualTo("CUST-1");
    }

    @Test
    void createRejectsBlankCustomerReference() {
        assertThatThrownBy(() -> service.createAccount(" ", "EUR", 10.0, null))
                .isInstanceOf(InvalidAccountException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsBadCurrency() {
        assertThatThrownBy(() -> service.createAccount("C1", "euro", 10.0, null))
                .isInstanceOf(InvalidAccountException.class);
        assertThatThrownBy(() -> service.createAccount("C1", null, 10.0, null))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void createRejectsNegativeOrNonFiniteBalance() {
        assertThatThrownBy(() -> service.createAccount("C1", "EUR", -1.0, null))
                .isInstanceOf(InvalidAccountException.class);
        assertThatThrownBy(() -> service.createAccount("C1", "EUR", Double.NaN, null))
                .isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void getAccountThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(id)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void updateStatusBlocksAccount() {
        UUID id = UUID.randomUUID();
        Account existing = Account.builder().id(id).status(AccountStatus.ACTIVE).currency("EUR").build();
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Account result = service.updateStatus(id, AccountStatus.BLOCKED);

        assertThat(result.getStatus()).isEqualTo(AccountStatus.BLOCKED);
    }

    private Account stubLocked(UUID id, AccountStatus status, double balance) {
        Account a = Account.builder().id(id).status(status).currency("EUR").balance(balance).build();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void debitReducesBalance() {
        UUID id = UUID.randomUUID();
        Account a = stubLocked(id, AccountStatus.ACTIVE, 100.10);
        when(repository.save(a)).thenReturn(a);

        assertThat(service.debit(id, new BigDecimal("0.10")).getBalance()).isEqualTo(100.0);
    }

    @Test
    void debitRejectsInsufficientFunds() {
        UUID id = UUID.randomUUID();
        stubLocked(id, AccountStatus.ACTIVE, 50);

        assertThatThrownBy(() -> service.debit(id, new BigDecimal("50.01")))
                .isInstanceOf(InsufficientFundsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void debitAllowsExactBalance() {
        UUID id = UUID.randomUUID();
        Account a = stubLocked(id, AccountStatus.ACTIVE, 50);
        when(repository.save(a)).thenReturn(a);

        assertThat(service.debit(id, new BigDecimal("50")).getBalance()).isZero();
    }

    @Test
    void balanceOperationsRejectBlockedAccount() {
        UUID id = UUID.randomUUID();
        stubLocked(id, AccountStatus.BLOCKED, 50);

        assertThatThrownBy(() -> service.debit(id, BigDecimal.ONE)).isInstanceOf(AccountNotActiveException.class);
        assertThatThrownBy(() -> service.credit(id, BigDecimal.ONE)).isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void balanceOperationsRejectNonPositiveAmount() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> service.credit(id, BigDecimal.ZERO)).isInstanceOf(InvalidAccountException.class);
        assertThatThrownBy(() -> service.debit(id, new BigDecimal("-1"))).isInstanceOf(InvalidAccountException.class);
        assertThatThrownBy(() -> service.debit(id, null)).isInstanceOf(InvalidAccountException.class);
    }

    @Test
    void creditIncreasesBalance() {
        UUID id = UUID.randomUUID();
        Account a = stubLocked(id, AccountStatus.ACTIVE, 10);
        when(repository.save(a)).thenReturn(a);

        assertThat(service.credit(id, new BigDecimal("5.5")).getBalance()).isEqualTo(15.5);
    }

    @Test
    void creditUnknownAccountThrows() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.credit(id, BigDecimal.ONE)).isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void updateStatusRejectsNull() {
        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(), null))
                .isInstanceOf(InvalidAccountException.class);
    }
}
