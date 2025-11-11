package com.example.financeapp.service.impl;

import com.example.financeapp.dto.CreateWalletRequest;
import com.example.financeapp.dto.SharedWalletDTO;
import com.example.financeapp.dto.WalletMemberDTO;
import com.example.financeapp.entity.User;
import com.example.financeapp.entity.Wallet;
import com.example.financeapp.entity.WalletMember;
import com.example.financeapp.entity.WalletMember.WalletRole;
import com.example.financeapp.repository.CurrencyRepository;
import com.example.financeapp.repository.UserRepository;
import com.example.financeapp.repository.WalletMemberRepository;
import com.example.financeapp.repository.WalletRepository;
import com.example.financeapp.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private WalletMemberRepository walletMemberRepository;

    @Override
    @Transactional
    public Wallet createWallet(Long userId, CreateWalletRequest request) {
        // 1. Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // 2. Kiểm tra loại tiền có hợp lệ
        if (!currencyRepository.existsById(request.getCurrencyCode())) {
            throw new RuntimeException("Loại tiền tệ không hợp lệ: " + request.getCurrencyCode());
        }

        // 3. Kiểm tra tên ví trùng (trong phạm vi user)
        if (walletRepository.existsByWalletNameAndUser_UserId(request.getWalletName(), userId)) {
            throw new RuntimeException("Bạn đã có ví tên \"" + request.getWalletName() + "\"");
        }

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWalletName(request.getWalletName().trim());
        wallet.setCurrencyCode(request.getCurrencyCode().toUpperCase());
        wallet.setBalance(BigDecimal.valueOf(request.getInitialBalance()));
        wallet.setDescription(request.getDescription());
        wallet.setDefault(false);

        if (Boolean.TRUE.equals(request.getSetAsDefault())) {
            walletRepository.unsetDefaultWallet(userId, null); // bỏ mặc định tất cả
            wallet.setDefault(true);
        }

        Wallet savedWallet = walletRepository.save(wallet);

        // 5. Tạo WalletMember với role OWNER
        WalletMember ownerMember = new WalletMember(savedWallet, user, WalletRole.OWNER);
        walletMemberRepository.save(ownerMember);

        return savedWallet;
    }

    @Override
    @Transactional
    public void setDefaultWallet(Long userId, Long walletId) {
        walletRepository.findByWalletIdAndUser_UserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        walletRepository.unsetDefaultWallet(userId, walletId);
        walletRepository.setDefaultWallet(userId, walletId);
    }
    @Override
    public List<Wallet> getWalletsByUserId(Long userId) {
        return walletRepository.findByUser_UserId(userId);
    }

    @Override
    public Wallet getWalletDetails(Long userId, Long walletId) {
        // Kiểm tra user có quyền truy cập wallet không (owner hoặc member)
        if (!hasAccess(walletId, userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập ví này");
        }

        return walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ví"));
    }

    // ============ SHARED WALLET IMPLEMENTATION ============

    @Override
    public List<SharedWalletDTO> getAllAccessibleWallets(Long userId) {
        // Lấy tất cả wallet memberships của user
        List<WalletMember> memberships = walletMemberRepository.findByUser_UserId(userId);

        List<SharedWalletDTO> result = new ArrayList<>();

        for (WalletMember membership : memberships) {
            Wallet wallet = membership.getWallet();

            // Tìm owner của wallet
            WalletMember owner = walletMemberRepository
                    .findByWallet_WalletIdAndRole(wallet.getWalletId(), WalletRole.OWNER)
                    .orElse(null);

            // Đếm tổng số members
            long totalMembers = walletMemberRepository.countByWallet_WalletId(wallet.getWalletId());

            SharedWalletDTO dto = new SharedWalletDTO();
            dto.setWalletId(wallet.getWalletId());
            dto.setWalletName(wallet.getWalletName());
            dto.setCurrencyCode(wallet.getCurrencyCode());
            dto.setBalance(wallet.getBalance());
            dto.setDescription(wallet.getDescription());
            dto.setMyRole(membership.getRole().toString());
            dto.setTotalMembers((int) totalMembers);
            dto.setCreatedAt(wallet.getCreatedAt());
            dto.setUpdatedAt(wallet.getUpdatedAt());

            if (owner != null) {
                dto.setOwnerId(owner.getUser().getUserId());
                dto.setOwnerName(owner.getUser().getFullName());
            }

            result.add(dto);
        }

        return result;
    }

    @Override
    @Transactional
    public WalletMemberDTO shareWallet(Long walletId, Long ownerId, String memberEmail) {
        // 1. Kiểm tra wallet tồn tại
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new RuntimeException("Ví không tồn tại"));

        // 2. Kiểm tra người share có phải owner không
        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể chia sẻ ví");
        }

        // 3. Tìm user được chia sẻ qua email
        User memberUser = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email: " + memberEmail));

        // 4. Kiểm tra không thể share với chính mình
        if (memberUser.getUserId().equals(ownerId)) {
            throw new RuntimeException("Không thể chia sẻ ví với chính bạn");
        }

        // 5. Kiểm tra user đã là member chưa
        if (walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, memberUser.getUserId())) {
            throw new RuntimeException("Người dùng này đã là thành viên của ví");
        }

        // 6. Tạo WalletMember mới với role MEMBER
        WalletMember newMember = new WalletMember(wallet, memberUser, WalletRole.MEMBER);
        WalletMember savedMember = walletMemberRepository.save(newMember);

        // 7. Tạo DTO để trả về
        return convertToMemberDTO(savedMember);
    }

    @Override
    public List<WalletMemberDTO> getWalletMembers(Long walletId, Long requesterId) {
        // Kiểm tra requester có quyền truy cập wallet không
        if (!hasAccess(walletId, requesterId)) {
            throw new RuntimeException("Bạn không có quyền xem thành viên của ví này");
        }

        // Lấy danh sách members
        List<WalletMember> members = walletMemberRepository.findByWallet_WalletId(walletId);

        return members.stream()
                .map(this::convertToMemberDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeMember(Long walletId, Long ownerId, Long memberUserId) {
        // 1. Kiểm tra người xóa có phải owner không
        if (!isOwner(walletId, ownerId)) {
            throw new RuntimeException("Chỉ chủ sở hữu mới có thể xóa thành viên");
        }

        // 2. Không thể xóa chính mình (owner)
        if (ownerId.equals(memberUserId)) {
            throw new RuntimeException("Không thể xóa chủ sở hữu khỏi ví");
        }

        // 3. Kiểm tra member có trong wallet không
        WalletMember member = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, memberUserId)
                .orElseThrow(() -> new RuntimeException("Thành viên không tồn tại trong ví này"));

        // 4. Không thể xóa owner khác (nếu có nhiều owner trong tương lai)
        if (member.getRole() == WalletRole.OWNER) {
            throw new RuntimeException("Không thể xóa chủ sở hữu");
        }

        // 5. Xóa member
        walletMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void leaveWallet(Long walletId, Long userId) {
        // 1. Kiểm tra user có trong wallet không
        WalletMember member = walletMemberRepository
                .findByWallet_WalletIdAndUser_UserId(walletId, userId)
                .orElseThrow(() -> new RuntimeException("Bạn không phải thành viên của ví này"));

        // 2. Owner không thể rời khỏi ví của mình
        if (member.getRole() == WalletRole.OWNER) {
            throw new RuntimeException("Chủ sở hữu không thể rời khỏi ví. Vui lòng xóa ví hoặc chuyển quyền sở hữu.");
        }

        // 3. Xóa member
        walletMemberRepository.delete(member);
    }

    @Override
    public boolean hasAccess(Long walletId, Long userId) {
        return walletMemberRepository.existsByWallet_WalletIdAndUser_UserId(walletId, userId);
    }

    @Override
    public boolean isOwner(Long walletId, Long userId) {
        return walletMemberRepository.isOwner(walletId, userId);
    }

    // ============ HELPER METHODS ============

    private WalletMemberDTO convertToMemberDTO(WalletMember member) {
        User user = member.getUser();
        return new WalletMemberDTO(
                member.getMemberId(),
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getAvatar(),
                member.getRole().toString(),
                member.getJoinedAt()
        );
    }
}
