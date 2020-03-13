package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus;
import cn.xpbootcamp.legacy_code.exception.InvalidTransactionException;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;

import static cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus.*;
import static cn.xpbootcamp.legacy_code.utils.IdGenerator.generateId;
import static java.util.Optional.ofNullable;

public class WalletTransaction {

    private static final String ID_PREFIX = "t_";
    private static final int MAX_PERIOD_OF_EXPIRATION = 20 * 24 * 60 * 60 * 1000;

    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long createdTimestamp;
    private Double amount;
    private WalletTransactionStatus status;

    private DistributedLock distributedLock;
    private WalletService walletService;

    private WalletTransaction(
            String preAssignedId,
            Long buyerId,
            Long sellerId,
            Double amount) {
        this.id = generateWalletTransactionId(preAssignedId);
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
    }

    public static WalletTransaction generateWalletTransaction(
            String preAssignedId,
            Long buyerId,
            Long sellerId,
            Double amount) {
        if (buyerId == null || sellerId == null || amount < 0.0) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        return new WalletTransaction(preAssignedId, buyerId, sellerId, amount);
    }

    private String generateWalletTransactionId(String preAssignedId) {
        return ofNullable(preAssignedId)
                .filter(id -> id.startsWith(ID_PREFIX))
                .orElseGet(() -> ID_PREFIX + generateId());
    }

    public void setDistributedLock(DistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    public void setWalletService(WalletService walletService) {
        this.walletService = walletService;
    }

    public boolean execute() {
        if (isTransactionExpired()) {
            this.status = EXPIRED;
            return false;
        }
        if (status == EXECUTED) {
            return true;
        }
        boolean isLocked = false;
        try {
            isLocked = distributedLock.lock(id);

            if (!isLocked) {
                return false;
            }
            if (status == EXECUTED) {
                return true;
            }

            String walletTransactionReceiptId = walletService.moveMoney(id, buyerId, sellerId, amount);
            if (isMoveMoneySuccess(walletTransactionReceiptId)) {
                this.status = EXECUTED;
                return true;
            } else {
                this.status = FAILED;
                return false;
            }
        } finally {
            if (isLocked) {
                distributedLock.unlock(id);
            }
        }
    }

    private boolean isMoveMoneySuccess(String walletTransactionReceiptId) {
        return walletTransactionReceiptId != null;
    }

    private boolean isTransactionExpired() {
        return System.currentTimeMillis() - createdTimestamp > MAX_PERIOD_OF_EXPIRATION;
    }

}