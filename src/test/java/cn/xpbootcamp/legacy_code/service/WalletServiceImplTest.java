package cn.xpbootcamp.legacy_code.service;

import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.repository.UserRepositoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private UserRepositoryImpl userRepository;

    @Test
    void should_return_transaction_id_when_buyer_has_enough_money() {
        // given
        String id = UUID.randomUUID().toString();
        long buyerId = 123L;
        long sellerId = 234L;
        double amount = 89.5;

        User buyer = new User();
        buyer.setBalance(amount);
        doReturn(buyer).when(userRepository).find(buyerId);
        doReturn(new User()).when(userRepository).find(sellerId);

        WalletServiceImpl walletService = new WalletServiceImpl();
        walletService.setUserRepository(userRepository);

        // when
        String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);

        // then
        assertThat(walletTransactionId).endsWith(id);
    }

    @Test
    void should_return_null_when_buyer_has_not_enough_money() {
        // given
        String id = UUID.randomUUID().toString();
        long buyerId = 123L;
        long sellerId = 234L;
        double amount = 89.5;

        User buyer = new User();
        buyer.setBalance(amount - 1);
        doReturn(buyer).when(userRepository).find(buyerId);

        WalletServiceImpl walletService = new WalletServiceImpl();
        walletService.setUserRepository(userRepository);

        // when
        String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);

        // then
        assertThat(walletTransactionId).isNull();
    }
}
