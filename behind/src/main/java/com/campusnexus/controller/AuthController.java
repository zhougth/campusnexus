package com.campusnexus.controller;

import com.campusnexus.dto.LoginDTO;
import com.campusnexus.dto.RegisterDTO;
import com.campusnexus.dto.UpdatePasswordDTO;
import com.campusnexus.dto.UpdateProfileDTO;
import com.campusnexus.service.AuthService;
import com.campusnexus.common.result.Result;
import com.campusnexus.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;



    /**
     * 修改用户头像
     * @param file 头像文件
     * @return 新头像 URL
     */
    @PutMapping("/avatar")
    public Result<String> avatar(@RequestParam("file") MultipartFile file) {
        return Result.success(authService.updateAvatar(file));
    }

    /**
     * 注册
     * @param dto
     * @return
     */
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    /**
     * 登录
     * @param dto
     * @return
     */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 登出
     * @param authorization
     * @return
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            authService.logout(authorization.substring(BEARER_PREFIX.length()));
        }
        return Result.success();
    }

    /**
     * 获取当前用户信息
     * @return
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(authService.currentUser());
    }

    /**
     * 更新用户信息
     * @param dto
     * @return
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        authService.updateProfile(dto);
        return Result.success();
    }

    /**
     * 更新用户密码
     * @param dto
     * @return
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordDTO dto) {
        authService.updatePassword(dto);
        return Result.success();
    }
}
