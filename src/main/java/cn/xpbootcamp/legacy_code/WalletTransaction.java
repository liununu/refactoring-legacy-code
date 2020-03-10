package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.WalletTransactionStatus;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.utils.DistributedLock;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
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
        if (preAssignedId != null && !preAssignedId.isEmpty()) {
            this.id = preAssignedId;
        } else {
            this.id = IdGenerator.generateTransactionId();
        }
        if (!this.id.startsWith("t_")) {
            this.id = "t_" + preAssignedId;
        }
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.amount = amount;
        this.status = WalletTransactionStatus.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
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