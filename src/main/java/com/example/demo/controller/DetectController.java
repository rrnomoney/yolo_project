package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.pojo.User;
import com.example.demo.pojo.DetectRecord;
import com.example.demo.repository.DetectRecordRepository;
import com.example.demo.repository.UserRepository;
import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/api/detect")
public class DetectController {

    @Value("${yolo.api.url:http://localhost:5000/detect}")
    private String yoloApiUrl;

    private final RestTemplate restTemplate;
    private final DetectRecordRepository detectRecordRepository;
    private final UserRepository userRepository;

    public DetectController(RestTemplate restTemplate, DetectRecordRepository detectRecordRepository, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.detectRecordRepository = detectRecordRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/toDetect")
    public String toDetect() {
        return "detect";
    }

    @PostMapping("/image")
    @ResponseBody
    public ResponseEntity<String> detectImage(@RequestParam("file") MultipartFile file, Principal principal) {
        try {
            // 构造多部分表单请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送请求到 YOLO 服务
            String result = restTemplate.postForObject(yoloApiUrl, requestEntity, String.class);

            // 保存检测记录
            if (principal != null) {
                String username = principal.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null) {
                    DetectRecord record = new DetectRecord(user, file.getOriginalFilename(), result, LocalDateTime.now());
                    detectRecordRepository.save(record);
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("图片检测转发失败: " + e.getMessage());
            return ResponseEntity.status(500).body("检测转发失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public String getDetectionHistory(org.springframework.ui.Model model, java.security.Principal principal) {
        if (principal != null) {
            String username = principal.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                java.util.List<DetectRecord> records = detectRecordRepository.findByUserOrderByDetectTimeDesc(user);
                model.addAttribute("records", records);
            }
        }
        return "history";
    }
}
