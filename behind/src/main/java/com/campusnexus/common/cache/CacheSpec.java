package com.campusnexus.common.cache;

import java.time.Duration;

public enum CacheSpec {
    CATEGORY_ENABLED("campusnexus:cache:category:enabled", Duration.ofMinutes(5)),
    DASHBOARD("campusnexus:cache:dashboard", Duration.ofMinutes(5)),
    STATS_HOME("stats:home", Duration.ofMinutes(5));

    private final String key;
    private final Duration ttl;

    CacheSpec(String key, Duration ttl) {
        this.key = key;
        this.ttl = ttl;
    }

    public String key() { return key; }
    public Duration ttl() { return ttl; }
}
