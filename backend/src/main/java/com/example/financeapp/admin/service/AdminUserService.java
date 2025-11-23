package com.example.financeapp.admin.service;

import com.example.financeapp.admin.dto.*;
import com.example.financeapp.admin.entity.AdminActionLog;
import com.example.financeapp.admin.repository.AdminActionLogRepository;
import com.example.financeapp.exception.ApiErrorCode;
import com.example.financeapp.exception.ApiException;
import com.example.financeapp.security.CustomUserDetails;
import com.example.financeapp.security.Role;
import com.example.financeapp.user.entity.User;
import com.example.financeapp.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final AdminActionLogRepository adminActionLogRepository;

    public AdminUserService(UserRepository userRepository, AdminActionLogRepository adminActionLogRepository) {
        this.userRepository = userRepository;
        this.adminActionLogRepository = adminActionLogRepository;
    }

    // Map User -> AdminUserResponse
    private AdminUserResponse toUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole() != null ? user.getRole() : Role.USER)
                .locked(user.isLocked())
                .googleAccount(user.isGoogleAccount())
                .firstLogin(user.isFirstLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    // Map AdminActionLog -> AdminActionLogResponse
    private AdminActionLogResponse toLogResponse(AdminActionLog log) {
        return AdminActionLogResponse.builder()
                .id(log.getId())
                .adminId(log.getAdminId())
                .adminEmail(log.getAdminEmail())
                .targetUserId(log.getTargetUserId())
                .action(log.getAction())
                .detail(log.getDetail())
                .createdAt(log.getCreatedAt())
                .build();
    }

    // Ghi log thao tác quản trị
    private void logAction(CustomUserDetails admin, User targetUser, String action, String detail) {
        AdminActionLog log = new AdminActionLog();
        log.setAdminId(admin.getId());
        log.setAdminEmail(admin.getUsername());
        log.setTargetUserId(targetUser.getUserId());
        log.setAction(action);
        log.setDetail(detail);
        adminActionLogRepository.save(log);
    }

    // 1) Danh sách user (bỏ qua user đã xóa mềm)
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .filter(u -> !u.isDeleted())
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    // 1b) Xem chi tiết 1 user
    public AdminUserDetailResponse getUserDetail(Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.USER_NOT_FOUND,
                        "Người dùng không tồn tại"
                ));

        if (target.isDeleted()) {
            throw new ApiException(
                    ApiErrorCode.USER_NOT_FOUND,
                    "Người dùng không tồn tại"
            );
        }

        return AdminUserDetailResponse.fromEntity(target);
    }

    // 2) Khóa user
    @Transactional
    public AdminUserResponse lockUser(Long userId, CustomUserDetails admin) {
        if (admin.getId().equals(userId)) {
            throw new ApiException(
                    ApiErrorCode.CANNOT_MODIFY_SELF,
                    "Bạn không thể khóa chính tài khoản của mình"
            );
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.USER_NOT_FOUND,
                        "Người dùng không tồn tại"
                ));

        if (target.isDeleted()) {
            throw new ApiException(
                    ApiErrorCode.USER_NOT_FOUND,
                    "Người dùng không tồn tại"
            );
        }

        if (target.isLocked()) {
            throw new ApiException(
                    ApiErrorCode.USER_ALREADY_LOCKED,
                    "Tài khoản này đã bị khóa trước đó"
            );
        }

        // Nếu target là ADMIN, cần đảm bảo không phải admin cuối cùng
        if (target.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(
                        ApiErrorCode.CANNOT_DOWNGRADE_LAST_ADMIN,
                        "Không thể khóa admin cuối cùng trong hệ thống"
                );
            }
        }

        target.setLocked(true);
        userRepository.save(target);

        logAction(admin, target, "LOCK_USER", "Admin khóa tài khoản userId=" + target.getUserId());

        return toUserResponse(target);
    }

    // 3) Mở khóa user
    @Transactional
    public AdminUserResponse unlockUser(Long userId, CustomUserDetails admin) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.USER_NOT_FOUND,
                        "Người dùng không tồn tại"
                ));

        if (target.isDeleted()) {
            throw new ApiException(
                    ApiErrorCode.USER_NOT_FOUND,
                    "Người dùng không tồn tại"
            );
        }

        if (!target.isLocked()) {
            throw new ApiException(
                    ApiErrorCode.USER_ALREADY_UNLOCKED,
                    "Tài khoản này đang ở trạng thái hoạt động"
            );
        }

        target.setLocked(false);
        userRepository.save(target);

        logAction(admin, target, "UNLOCK_USER", "Admin mở khóa tài khoản userId=" + target.getUserId());

        return toUserResponse(target);
    }

    // 4) Đổi role USER ↔ ADMIN
    @Transactional
    public AdminUserResponse changeRole(Long userId, ChangeRoleRequest request, CustomUserDetails admin) {
        if (admin.getId().equals(userId)) {
            throw new ApiException(
                    ApiErrorCode.CANNOT_MODIFY_SELF,
                    "Bạn không thể đổi role của chính mình"
            );
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.USER_NOT_FOUND,
                        "Người dùng không tồn tại"
                ));

        if (target.isDeleted()) {
            throw new ApiException(
                    ApiErrorCode.USER_NOT_FOUND,
                    "Người dùng không tồn tại"
            );
        }

        String roleStr = request.getRole().trim().toUpperCase();

        if (!roleStr.equals("USER") && !roleStr.equals("ADMIN")) {
            throw new ApiException(
                    ApiErrorCode.INVALID_ROLE_CHANGE,
                    "Chỉ được phép đổi role giữa USER và ADMIN"
            );
        }

        Role newRole = Role.valueOf(roleStr);
        Role oldRole = target.getRole() != null ? target.getRole() : Role.USER;

        if (oldRole == newRole) {
            return toUserResponse(target);
        }

        // Nếu hạ từ ADMIN xuống USER: check không phải admin cuối
        if (oldRole == Role.ADMIN && newRole == Role.USER) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(
                        ApiErrorCode.CANNOT_DOWNGRADE_LAST_ADMIN,
                        "Không thể hạ role admin cuối cùng xuống user"
                );
            }
        }

        target.setRole(newRole);
        userRepository.save(target);

        logAction(admin, target, "CHANGE_ROLE", "Admin đổi role từ " + oldRole + " sang " + newRole);

        return toUserResponse(target);
    }

    // 5) Lịch sử thao tác quản trị
    public List<AdminActionLogResponse> getAllAdminActionLogs() {
        return adminActionLogRepository.findAll()
                .stream()
                .map(this::toLogResponse)
                .collect(Collectors.toList());
    }

    // 6) Xoá user (SOFT DELETE)
    @Transactional
    public void deleteUser(Long userId, CustomUserDetails admin) {
        if (admin.getId().equals(userId)) {
            throw new ApiException(
                    ApiErrorCode.CANNOT_MODIFY_SELF,
                    "Bạn không thể xóa chính tài khoản của mình"
            );
        }

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(
                        ApiErrorCode.USER_NOT_FOUND,
                        "Người dùng không tồn tại"
                ));

        // Nếu target là ADMIN: không được xoá admin cuối cùng
        if (target.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException(
                        ApiErrorCode.CANNOT_DOWNGRADE_LAST_ADMIN,
                        "Không thể xóa admin cuối cùng trong hệ thống"
                );
            }
        }

        // XÓA MỀM
        target.setDeleted(true);
        target.setLocked(true);
        userRepository.save(target);

        logAction(admin, target, "DELETE_USER", "Admin xoá (soft delete) tài khoản userId=" + target.getUserId());
    }
}

