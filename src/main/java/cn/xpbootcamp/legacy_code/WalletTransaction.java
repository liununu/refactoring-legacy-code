package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;

import javax.transaction.InvalidTransactionException;

import static cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus.TO_BE_EXECUTED;
import static cn.xpbootcamp.legacy_code.utils.IdGenerator.generateTransactionId;
import static java.util.Optional.ofNullable;

public class WalletTransaction {

    private static final String ID_PREFIX = "t_";

    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long createdTimestamp;
    private Double amount;
    private WalletTransactionStatus status;

    private DistributedLock distributedLock;
    private WalletService walletService;

    public WalletTransaction(
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

    private String generateWalletTransactionId(String preAssignedId) {
        return ofNullable(preAssignedId)
                .filter(id -> id.startsWith(ID_PREFIX))
                .orElseGet(() -> ID_PREFIX + generateTransactionId());
    }

    public void setDistributedLock(DistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    public void setWalletService(WalletService walletService) {
        this.walletService = walletService;
    }

    public boolean execute() throws InvalidTransactionException {
        if (buyerId == null || (sellerId == null || amount < 0.0)) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }
        if (status == WalletTransactionStatus.EXECUTED) return true;
        boolean isLocked = false;
        try {
            isLocked = distributedLock.lock(id);

            // 锁定未成功，返回false
            if (!isLocked) {
                return false;
            }
            if (status == WalletTransactionStatus.EXECUTED) return true; // double check
            long executionInvokedTimestamp = System.currentTimeMillis();
            // 交易超过20天
            if (executionInvokedTimestamp - createdTimestamp > 1728000000) {
                this.status = WalletTransactionStatus.EXPIRED;
                return false;
            }
            String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
            if (walletTransactionId != null) {
                this.status = WalletTransactionStatus.EXECUTED;
                return true;
            } else {
                this.status = WalletTransactionStatus.FAILED;
                return false;
            }
        } finally {
            if (isLocked) {
                distributedLock.unlock(id);
            }
        }
    }

}