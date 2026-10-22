package com.campusnexus.security;

public record LoginUser(
        Long id,
        String username,
        Integer role,
        Integer tokenVersion
) {
    public boolean isAdmin() {
        return role != null && role == 1;
    }
}
