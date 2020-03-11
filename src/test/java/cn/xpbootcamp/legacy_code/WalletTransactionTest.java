package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.transaction.InvalidTransactionException;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.stream.Stream;

import static cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletTransactionTest {

    @Mock
    private DistributedLock distributedLock;
    @Mock
    private WalletService walletService;

    private static Stream<String> nullAndEmptyStrings() {
        return Stream.of("", null);
    }

    @Test
    void should_return_wallet_transaction_when_create_success()
            throws NoSuchFieldException, IllegalAccessException {
        // given
        String preAssignedId = UUID.randomUUID().toString();
        Long buyerId = 123L;
        Long sellerId = 234L;
        Double amount = 34.5;

        // when
        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

        // then
        assertThat(walletTransaction).isNotNull();
        assertThat(getPrivateField(walletTransaction, "id", String.class)).startsWith("t_").hasSize(38);
        assertThat(getPrivateField(walletTransaction, "buyerId", Long.class)).isEqualTo(buyerId);
        assertThat(getPrivateField(walletTransaction, "sellerId", Long.class)).isEqualTo(sellerId);
        assertThat(getPrivateField(walletTransaction, "createdTimestamp", Long.class)).isNotNull();
        assertThat(getPrivateField(walletTransaction, "amount", Double.class)).isEqualTo(amount);
        assertThat(getPrivateField(walletTransaction, "status", WalletTransactionStatus.class))
                .isEqualTo(TO_BE_EXECUTED);
    }

    @ParameterizedTest
    @MethodSource("nullAndEmptyStrings")
    void should_return_wallet_transaction_with_id_when_create_given_without_pre_assigned_id(
            String preAssignedId) throws NoSuchFieldException, IllegalAccessException {
        // given
        Long buyerId = 123L;
        Long sellerId = 234L;
        Double amount = 34.5;

        // when
        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);

        // then
        assertThat(getPrivateField(walletTransaction, "id", String.class)).startsWith("t_").hasSize(38);
    }

    @Test
    void should_return_true_when_execute_transaction_success() throws InvalidTransactionException {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        long buyerId = 123L;
        long sellerId = 234L;
        double amount = 34.5;

        given(distributedLock.lock(preAssignedId)).willReturn(true);
        String walletTransactionId = UUID.randomUUID().toString();
        given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount))
                .willReturn(walletTransactionId);

        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
        walletTransaction.setDistributedLock(distributedLock);
        walletTransaction.setWalletService(walletService);

        // when
        boolean executeResult = walletTransaction.execute();

        // then
        assertThat(executeResult).isTrue();
    }

    @Test
    void should_throw_invalid_transaction_exception_when_execute_with_null_buyer_id() {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        Long sellerId = 234L;
        Double amount = 34.5;
        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, null, sellerId, amount);

        // when then
        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(walletTransaction::execute);
    }

    @Test
    void should_throw_invalid_transaction_exception_when_execute_with_null_seller_id() {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        Long buyerId = 123L;
        Double amount = 34.5;
        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, null, amount);

        // when then
        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(walletTransaction::execute);
    }

    @Test
    void should_throw_invalid_transaction_exception_when_execute_with_amount_less_than_0() {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        Long buyerId = 123L;
        Long sellerId = 234L;
        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, -0.01);

        // when then
        assertThatExceptionOfType(InvalidTransactionException.class)
                .isThrownBy(walletTransaction::execute);
    }

    @Test
    void should_return_true_when_transaction_has_been_executed() throws InvalidTransactionException {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        long buyerId = 123L;
        long sellerId = 234L;
        double amount = 34.5;

        given(distributedLock.lock(preAssignedId)).willReturn(true);
        String walletTransactionId = UUID.randomUUID().toString();
        given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount))
                .willReturn(walletTransactionId);

        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
        walletTransaction.setDistributedLock(distributedLock);
        walletTransaction.setWalletService(walletService);
        walletTransaction.execute();

        // when
        boolean executeResult = walletTransaction.execute();

        // then
        assertThat(executeResult).isTrue();
        verify(distributedLock).lock(preAssignedId);
        verify(walletService).moveMoney(preAssignedId, buyerId, sellerId, amount);
        verify(distributedLock).unlock(preAssignedId);
    }

    @Test
    void should_return_false_when_transaction_distributed_not_lock_on()
            throws InvalidTransactionException {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        Long buyerId = 123L;
        Long sellerId = 234L;
        Double amount = 34.5;

        given(distributedLock.lock(preAssignedId)).willReturn(false);

        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
        walletTransaction.setDistributedLock(distributedLock);

        // when
        boolean executeResult = walletTransaction.execute();

        // then
        assertThat(executeResult).isFalse();
    }

    @Test
    void should_return_false_when_transaction_created_time_over_20_days()
            throws InvalidTransactionException, NoSuchFieldException, IllegalAccessException {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        Long buyerId = 123L;
        Long sellerId = 234L;
        Double amount = 34.5;

        given(distributedLock.lock(preAssignedId)).willReturn(true);

        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
        walletTransaction.setDistributedLock(distributedLock);
        setCreatedTimeStampOver20DaysAgo(walletTransaction);

        // when
        boolean executeResult = walletTransaction.execute();

        // then
        assertThat(executeResult).isFalse();
        assertThat(getPrivateField(walletTransaction, "status", WalletTransactionStatus.class))
                .isEqualTo(EXPIRED);
    }

    @Test
    void should_return_false_when_execute_transaction_with_wallet_service_move_money_failed()
            throws InvalidTransactionException, NoSuchFieldException, IllegalAccessException {
        // given
        String preAssignedId = "t_" + UUID.randomUUID().toString();
        long buyerId = 123L;
        long sellerId = 234L;
        double amount = 34.5;

        given(distributedLock.lock(preAssignedId)).willReturn(true);
        given(walletService.moveMoney(preAssignedId, buyerId, sellerId, amount)).willReturn(null);

        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
        walletTransaction.setDistributedLock(distributedLock);
        walletTransaction.setWalletService(walletService);

        // when
        boolean executeResult = walletTransaction.execute();

        // then
        assertThat(executeResult).isFalse();
        assertThat(getPrivateField(walletTransaction, "status", WalletTransactionStatus.class))
                .isEqualTo(FAILED);
    }

    private void setCreatedTimeStampOver20DaysAgo(WalletTransaction walletTransaction)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = WalletTransaction.class.getDeclaredField("createdTimestamp");
        field.setAccessible(true);
        field.set(walletTransaction, 0L);
    }

    private <T> T getPrivateField(
            WalletTransaction walletTransaction, String fieldName, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = WalletTransaction.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(walletTransaction));
    }
}
