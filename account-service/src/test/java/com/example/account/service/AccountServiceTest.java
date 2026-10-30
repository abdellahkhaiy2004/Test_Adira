package com.example.account.service;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import com.example.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void updateStatusRejectsNull() {
        assertThatThrownBy(() -> service.updateStatus(UUID.randomUUID(), null))
                .isInstanceOf(InvalidAccountException.class);
    }
}
