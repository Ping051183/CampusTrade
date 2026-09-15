package com.campustrade.config;

import com.campustrade.constant.RoleConstant;
import com.campustrade.constant.StatusConstant;
import com.campustrade.entity.User;
import com.campustrade.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员账户初始化器
 * 启动时自动确保管理员账户存在且密码正确
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String username = "admin";
        String password = "admin123";

        // 查询管理员用户
        User admin = userMapper.findByUsername(username);
        if (admin == null) {
            // 管理员不存在，创建
            admin = new User();
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setNickname("系统管理员");
            admin.setRole(RoleConstant.ADMIN);
            admin.setStatus(StatusConstant.USER_ENABLED);
            userMapper.insert(admin);
            log.info("✅ 管理员账户已创建 (admin/admin123)");
        } else {
            // 管理员已存在，检查密码是否正确
            if (!passwordEncoder.matches(password, admin.getPassword())) {
                // 密码不正确，更新为正确哈希
                admin.setPassword(passwordEncoder.encode(password));
                userMapper.updateById(admin);
                log.info("✅ 管理员密码已修复为正确哈希 (admin/admin123)");
            } else {
                log.info("✅ 管理员密码正确，无需修复");
            }
        }
    }
}
