package com.campusnexus.security;

import com.campusnexus.common.exception.BizException;
import com.campusnexus.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BizException(ErrorCode.UNAUTHORIZED);
    }

    public static Long userId() {
        return currentUser().id();
    }
}
