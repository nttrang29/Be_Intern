package com.example.financeapp.wallet.service;

import com.example.financeapp.category.entity.Category;
import com.example.financeapp.transaction.entity.Transaction;
import com.example.financeapp.transaction.entity.TransactionType;
import com.example.financeapp.transaction.repository.TransactionRepository;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.wallet.dto.response.WalletTransactionHistoryDTO;
import com.example.financeapp.wallet.dto.response.WalletTransferHistoryDTO;
import com.example.financeapp.wallet.entity.Wallet;
import com.example.financeapp.wallet.entity.WalletTransfer;
import com.example.financeapp.wallet.repository.CurrencyRepository;
import com.example.financeapp.wallet.repository.WalletMemberRepository;
import com.example.financeapp.wallet.repository.WalletMergeHistoryRepository;
import com.example.financeapp.wallet.repository.WalletRepository;
import com.example.financeapp.wallet.repository.WalletTransferRepository;
import com.example.financeapp.wallet.service.ExchangeRateService;
import com.example.financeapp.wallet.service.impl.WalletServiceImpl;
import com.example.financeapp.fund.repository.FundRepository;
import com.example.financeapp.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrencyRepository currencyRepository;
    @Mock private WalletMemberRepository walletMemberRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletMergeHistoryRepository walletMergeHistoryRepository;
    @Mock private WalletTransferRepository walletTransferRepository;
    @Mock private FundRepository fundRepository;
    @Mock private ExchangeRateService exchangeRateService;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void getWalletTransactions_returnsAllEntriesWhenAuthorized() {
        Long walletId = 1L;
        Long requesterId = 42L;

        Wallet wallet = new Wallet();
        wallet.setWalletId(walletId);
        wallet.setWalletName("Team Wallet");
        wallet.setCurrencyCode("USD");

        User creator = new User();
        creator.setUserId(7L);
        creator.setFullName("Alice");
        creator.setEmail("alice@example.com");

        TransactionType type = new TransactionType();
        type.setTypeId(1L);
        type.setTypeName("Chi tiêu");

        Category category = new Category();
        category.setCategoryId(3L);
        category.setCategoryName("Ăn uống");

        Transaction transaction = new Transaction();
        transaction.setTransactionId(99L);
        transaction.setWallet(wallet);
        transaction.setUser(creator);
        transaction.setTransactionType(type);
        transaction.setCategory(category);
        transaction.setAmount(BigDecimal.TEN);
        transaction.setOriginalAmount(new BigDecimal("230000"));
        transaction.setOriginalCurrency("VND");
        transaction.setExchangeRate(new BigDecimal("23000"));
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setNote("Dinner");

        when(walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, requesterId)).thenReturn(true);
        when(transactionRepository.findDetailedByWalletId(walletId)).thenReturn(List.of(transaction));

        List<WalletTransactionHistoryDTO> result = walletService.getWalletTransactions(requesterId, walletId);

        assertThat(result).hasSize(1);
        WalletTransactionHistoryDTO dto = result.get(0);
        assertThat(dto.getTransactionId()).isEqualTo(99L);
        assertThat(dto.getCreator().getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getCategory().getCategoryName()).isEqualTo("Ăn uống");
        assertThat(dto.getWallet().getWalletName()).isEqualTo("Team Wallet");
    }

    @Test
    void getWalletTransactions_throwsWhenUserLacksAccess() {
        when(walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> walletService.getWalletTransactions(2L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("quyền truy cập");
    }

    @Test
    void getWalletTransfers_returnsIncomingAndOutgoingEntries() {
        Long walletId = 5L;
        Long requesterId = 8L;
        when(walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, requesterId)).thenReturn(true);

        Wallet walletA = new Wallet();
        walletA.setWalletId(walletId);
        walletA.setWalletName("Primary");
        walletA.setCurrencyCode("USD");

        Wallet walletB = new Wallet();
        walletB.setWalletId(6L);
        walletB.setWalletName("Savings");
        walletB.setCurrencyCode("USD");

        User operator = new User();
        operator.setUserId(9L);
        operator.setFullName("Bob");
        operator.setEmail("bob@example.com");

        WalletTransfer outgoing = new WalletTransfer();
        outgoing.setTransferId(1L);
        outgoing.setFromWallet(walletA);
        outgoing.setToWallet(walletB);
        outgoing.setUser(operator);
        outgoing.setAmount(BigDecimal.ONE);
        outgoing.setCurrencyCode("USD");
        outgoing.setTransferDate(LocalDateTime.now());
        outgoing.setCreatedAt(LocalDateTime.now());
        outgoing.setUpdatedAt(LocalDateTime.now());
        outgoing.setStatus(WalletTransfer.TransferStatus.COMPLETED);

        WalletTransfer incoming = new WalletTransfer();
        incoming.setTransferId(2L);
        incoming.setFromWallet(walletB);
        incoming.setToWallet(walletA);
        incoming.setUser(operator);
        incoming.setAmount(new BigDecimal("2"));
        incoming.setCurrencyCode("USD");
        incoming.setTransferDate(LocalDateTime.now());
        incoming.setCreatedAt(LocalDateTime.now());
        incoming.setUpdatedAt(LocalDateTime.now());
        incoming.setStatus(WalletTransfer.TransferStatus.COMPLETED);

        when(walletTransferRepository.findByWalletId(walletId)).thenReturn(List.of(outgoing, incoming));

        List<WalletTransferHistoryDTO> dtos = walletService.getWalletTransfers(requesterId, walletId);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getDirection()).isEqualTo(WalletTransferHistoryDTO.Direction.OUTGOING);
        assertThat(dtos.get(1).getDirection()).isEqualTo(WalletTransferHistoryDTO.Direction.INCOMING);
    }
}
