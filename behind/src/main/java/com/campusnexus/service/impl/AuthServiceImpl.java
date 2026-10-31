package com.campusnexus.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusnexus.dto.LoginDTO;
import com.campusnexus.dto.RegisterDTO;
import com.campusnexus.dto.UpdatePasswordDTO;
import com.campusnexus.dto.UpdateProfileDTO;
import com.campusnexus.common.exception.BizException;
import com.campusnexus.common.exception.ErrorCode;
import com.campusnexus.security.JwtPayload;
import com.campusnexus.security.JwtService;
import com.campusnexus.security.SecurityUtils;
import com.campusnexus.security.TokenBlacklistService;
import com.campusnexus.entity.User;
import com.campusnexus.mapper.UserMapper;
import com.campusnexus.service.AuthService;
import com.campusnexus.storage.FileCleanupService;
import com.campusnexus.storage.FileStorageService;
import com.campusnexus.vo.UserVO;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024 * 1024;
    private static final Set<String> AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final FileStorageService fileStorageService;
    private final FileCleanupService fileCleanupService;

    @Override
    @Transactional
    public UserVO register(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        String email = normalizeEmail(dto.getEmail());
        if (email != null && userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, email)) > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(email);
        user.setRole(0);
        user.setStatus(1);
        user.setTokenVersion(0);
        userMapper.insert(user);

        User saved = userMapper.selectById(user.getId());
        return toVO(saved == null ? user : saved);
    }

    @Override
    public String login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername().trim()));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR);
        }
        return jwtService.generateToken(user);
    }

    @Override
    public UserVO currentUser() {
        User user = userMapper.selectById(SecurityUtils.userId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    @Transactional
    public void updateProfile(UpdateProfileDTO dto) {
        User user = requireCurrentUser();

        if (StringUtils.hasText(dto.getUsername())) {
            String username = dto.getUsername().trim();
            if (!username.equals(user.getUsername())
                    && userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0) {
                throw new BizException(ErrorCode.USERNAME_EXISTS);
            }
            user.setUsername(username);
        }

        if (dto.getEmail() != null) {
            String email = normalizeEmail(dto.getEmail());
            if (email != null && !email.equals(user.getEmail())
                    && userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getEmail, email)) > 0) {
                throw new BizException(ErrorCode.EMAIL_EXISTS);
            }
            user.setEmail(email);
        }

        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updatePassword(UpdatePasswordDTO dto) {
        User user = requireCurrentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PASSWORD_ERROR, "旧密码错误");
        }
        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        userMapper.updateById(user);
    }

    @Override
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            JwtPayload payload = jwtService.parse(token);
            tokenBlacklistService.blacklist(payload.jti(), jwtService.remainingSeconds(payload));
        } catch (JwtException | IllegalArgumentException ignored) {
            // The request is already authenticated; an invalid token simply has nothing to revoke.
        }
    }

    @Override
    public String updateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_FAILED, "头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        String extension = avatarExtension(file.getOriginalFilename());

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BizException(ErrorCode.OSS_UPLOAD_FAILED, "读取头像文件失败");
        }

        User user = requireCurrentUser();
        String objectKey = "avatar/" + user.getId() + "/" + UUID.randomUUID() + "." + extension;
        String url = fileStorageService.store(bytes, objectKey);

        User patch = new User();
        patch.setId(user.getId());
        patch.setAvatar(url);
        userMapper.updateById(patch);

        if (StringUtils.hasText(user.getAvatar()) && !user.getAvatar().equals(url)) {
            fileCleanupService.deleteLater(user.getAvatar());
        }
        return url;
    }

    private String avatarExtension(String filename) {
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!AVATAR_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.FILE_TYPE_UNSUPPORTED);
        }
        return extension;
    }

    private User requireCurrentUser() {
        User user = userMapper.selectById(SecurityUtils.userId());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim() : null;
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }
}
