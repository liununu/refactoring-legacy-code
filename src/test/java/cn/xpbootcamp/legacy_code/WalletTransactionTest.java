package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static cn.xpbootcamp.legacy_code.enums.STATUS.TO_BE_EXECUTED;
import static org.assertj.core.api.Assertions.assertThat;

class WalletTransactionTest {

    @Test
    void should_return_wallet_transaction_when_create_success()
            throws NoSuchFieldException, IllegalAccessException {
        String preAssignedId = UUID.randomUUID().toString();
        Long buyerId = 123L;
        Long sellerId = 234L;
        Long productId = 8989L;
        String orderId = UUID.randomUUID().toString();

        WalletTransaction walletTransaction =
                new WalletTransaction(preAssignedId, buyerId, sellerId, productId, orderId);

        assertThat(walletTransaction).isNotNull();
        assertThat(getPrivateField(walletTransaction, "id", String.class)).startsWith("t_");
        assertThat(getPrivateField(walletTransaction, "buyerId", Long.class)).isEqualTo(buyerId);
        assertThat(getPrivateField(walletTransaction, "sellerId", Long.class)).isEqualTo(sellerId);
        assertThat(getPrivateField(walletTransaction, "productId", Long.class)).isEqualTo(productId);
        assertThat(getPrivateField(walletTransaction, "orderId", String.class)).isEqualTo(orderId);
        assertThat(getPrivateField(walletTransaction, "createdTimestamp", Long.class)).isNotNull();
        assertThat(getPrivateField(walletTransaction, "amount", Double.class)).isNull();
        assertThat(getPrivateField(walletTransaction, "status", STATUS.class))
                .isEqualTo(TO_BE_EXECUTED);
        assertThat(getPrivateField(walletTransaction, "walletTransactionId", String.class)).isNull();
    }

    private <T> T getPrivateField(
            WalletTransaction walletTransaction, String fieldName, Class<T> type)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = WalletTransaction.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(walletTransaction));
    }
}
