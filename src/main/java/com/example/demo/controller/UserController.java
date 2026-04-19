package com.example.demo.controller;

import com.example.demo.pojo.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/toLogin")
    public String toLogin() {
        return "login";
    }

    @GetMapping("/toRegister")
    public String toRegister() {
        return "register";
    }

    @PostMapping("/api/user/register")
    @ResponseBody
    public Map<String, Object> register(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        if (userRepository.findByUsername(username).isPresent()) {
            response.put("success", false);
            response.put("message", "用户名已存在");
            return response;
        }

        User newUser = new User(username, passwordEncoder.encode(password));
        userRepository.save(newUser);
        response.put("success", true);
        return response;
    }
}
