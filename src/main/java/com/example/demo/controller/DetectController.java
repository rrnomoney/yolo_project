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
    public ResponseEntity<String> detectImage(@RequestParam("file") MultipartFile file,@RequestParam(value = "confidence", required = false, defaultValue = "0.5") float confidence, Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("请先登录");
            }

            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body("当前用户不存在");
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("上传失败：请选择图片文件");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("上传失败：文件名无效");
            }

            String lowerName = originalFilename.toLowerCase();
            if (!(lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png"))) {
                return ResponseEntity.badRequest().body("上传失败：仅支持 jpg、jpeg、png 格式图片");
            }

            // ✅ 改成 20MB
            long maxSize = 20 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body("上传失败：图片不能超过 20MB");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFilename;
                }
            };
            body.add("file", resource);
            body.add("confidence", String.valueOf(confidence));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String result = restTemplate.postForObject(yoloApiUrl, requestEntity, String.class);

            DetectRecord record = new DetectRecord(user, originalFilename, result, LocalDateTime.now());
            detectRecordRepository.save(record);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("图片检测转发失败: " + e.getMessage());
            return ResponseEntity.status(500).body("检测转发失败: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public String getDetectionHistory(org.springframework.ui.Model model, Principal principal) {
        java.util.List<DetectRecord> records = new java.util.ArrayList<>();

        if (principal != null) {
            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user != null) {
                records = detectRecordRepository.findByUserOrderByDetectTimeDesc(user);
            }
        }

        model.addAttribute("records", records);
        return "history";
    }

    @PostMapping("/history/delete/{id}")
    public String deleteHistory(@PathVariable Long id, Principal principal) {
        if (principal == null) {
            return "redirect:/toLogin";
        }

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/toLogin";
        }

        DetectRecord record = detectRecordRepository.findById(id).orElse(null);
        if (record != null && record.getUser() != null && record.getUser().getId().equals(user.getId())) {
            detectRecordRepository.delete(record);
        }

        return "redirect:/api/detect/history";
    }
    //先拿当前登录用户
    //再查这条记录
    //只有这条记录属于当前用户，才允许删除
}
