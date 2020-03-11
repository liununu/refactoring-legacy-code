package cn.xpbootcamp.legacy_code.service;

import cn.xpbootcamp.legacy_code.entity.User;
import cn.xpbootcamp.legacy_code.repository.UserRepository;

import java.util.UUID;

public class WalletServiceImpl implements WalletService {
    private UserRepository userRepository;

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String moveMoney(String id, long buyerId, long sellerId, double amount) {
        User buyer = userRepository.find(buyerId);
        if (buyer.getBalance() < amount) {
            return null;
        }
        User seller = userRepository.find(sellerId);
        seller.increase(amount);
        buyer.decrease(amount);
        return UUID.randomUUID().toString() + id;
    }
}
