package com.example.demo;

import com.example.demo.pojo.User;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    /**
     * 自动初始化数据库：如果用户表为空，则创建一个默认的 admin 账号
     */
    @Bean
    public CommandLineRunner dataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User("admin", passwordEncoder.encode("123456"));
                userRepository.save(admin);
                System.out.println("数据库已初始化：已创建默认账号 admin / 123456");
            }
        };
    }

}
