package cn.xpbootcamp.legacy_code.utils;

public class DistributedLock {

    public boolean lock(String transactionId) {
        return RedisDistributedLock.getSingletonInstance().lock(transactionId);
    }

    public void unlock(String transactionId) {
        RedisDistributedLock.getSingletonInstance().unlock(transactionId);
    }
}
