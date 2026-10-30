package com.example.account.repository;

import com.example.account.domain.Account;
import com.example.account.domain.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    AccountRepository repository;

    @Test
    void persistsAccountWithGeneratedId() {
        Account saved = repository.saveAndFlush(Account.builder().customerReference("C1")
                .currency("EUR").balance(12.5).status(AccountStatus.ACTIVE).build());

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).get()
                .extracting(Account::getBalance).isEqualTo(12.5);
    }

    @Test
    void findsByCustomerReference() {
        repository.saveAndFlush(Account.builder().customerReference("C1").currency("EUR")
                .status(AccountStatus.ACTIVE).build());
        repository.saveAndFlush(Account.builder().customerReference("C2").currency("EUR")
                .status(AccountStatus.ACTIVE).build());

        assertThat(repository.findByCustomerReference("C1")).hasSize(1);
    }
}
