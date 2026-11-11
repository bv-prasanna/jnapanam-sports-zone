package com.jnapanam.sportszone.loyalty.model;
import com.jnapanam.sportszone.identity.model.User;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoyaltyRefundAccountingTest {
 @Test void refundedEarnedPointsBecomeDebtIfAlreadySpent() {
    User user=mock(User.class);when(user.getId()).thenReturn(UUID.randomUUID());
    LoyaltyWallet wallet=new LoyaltyWallet(user);
    wallet.earn(10);
    wallet.redeem(9);
    wallet.reverseEarned(10);
    assertEquals(0,wallet.getPointsBalance());
    assertEquals(9,wallet.getEarnedPointsReversalDebt());
    wallet.earn(5);
    assertEquals(4,wallet.getEarnedPointsReversalDebt());
    assertEquals(0,wallet.getPointsBalance());
    wallet.earn(6);
    assertEquals(0,wallet.getEarnedPointsReversalDebt());
    assertEquals(2,wallet.getPointsBalance());
 }
}
