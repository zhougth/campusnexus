package com.campusnexus.runner;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusnexus.entity.User;
import com.campusnexus.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * 管理员账号初始化启动器。
 *
 * <p>应用启动完成后，根据配置项检查是否需要创建首个管理员账号。
 * 如果未配置初始化用户名/密码，或系统中已存在管理员，则不会执行初始化。</p>
 */
public class AdminBootstrapRunner implements ApplicationRunner {

    /**
     * 用户数据访问对象，用于查询管理员数量并插入管理员账号。
     * 该字段由 Lombok 的 @RequiredArgsConstructor 通过构造器注入。
     */
    private final UserMapper userMapper;

    /**
     * 密码编码器，用于对初始化管理员密码进行加密存储。
     * 该字段由 Lombok 的 @RequiredArgsConstructor 通过构造器注入。
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 初始化管理员用户名。
     * 对应配置项 app.admin.init-username，未配置时为空。
     */
    @Value("${app.admin.init-username:}")
    private String initUsername;

    /**
     * 初始化管理员明文密码。
     * 对应配置项 app.admin.init-password，未配置时为空。
     */
    @Value("${app.admin.init-password:}")
    private String initPassword;

    /**
     * Spring Boot 启动完成后执行的管理员初始化方法。
     *
     * <p>执行流程：</p>
     * <ol>
     *     <li>检查初始化用户名和密码是否都已配置；任一为空则直接返回。</li>
     *     <li>查询系统中 role=1 的管理员数量；如果已存在管理员则直接返回。</li>
     *     <li>创建管理员用户，加密密码并设置角色、状态和 Token 版本。</li>
     *     <li>将管理员用户写入数据库，并记录初始化日志。</li>
     * </ol>
     *
     * @param args Spring Boot 应用启动参数，当前初始化逻辑中不使用
     */
    @Override
    public void run(ApplicationArguments args) {
        // 未配置初始化用户名或密码时，不执行自动初始化。
        if (!StringUtils.hasText(initUsername) || !StringUtils.hasText(initPassword)) {
            return;
        }

        // 查询是否已经存在管理员账号，存在时不重复创建。
        Long adminCount = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getRole, 1));
        if (adminCount != null && adminCount > 0) {
            return;
        }

        // 构建初始化管理员账号，并对明文密码进行加密。
        User admin = new User();
        admin.setUsername(initUsername);
        admin.setPasswordHash(passwordEncoder.encode(initPassword));
        admin.setRole(1);          // 1 表示管理员角色
        admin.setStatus(1);        // 1 表示账号启用状态
        admin.setTokenVersion(0);  // 初始化 Token 版本

        // 保存管理员账号并输出初始化日志。
        userMapper.insert(admin);
        log.info("Initialized administrator account: {}", initUsername);
    }
}
