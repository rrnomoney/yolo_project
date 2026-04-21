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
import org.springframework.ui.Model;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/api/detect")
public class DetectController {

    @Value("${yolo.api.url:http://localhost:5000/detect}")
    private String yoloApiUrl;
    @Value("${yolo.video.api.url:http://localhost:5000/detect_video}")
    private String yoloVideoApiUrl;

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

    @PostMapping("/video")
    @ResponseBody
    public ResponseEntity<String> detectVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confidence", required = false, defaultValue = "0.5") float confidence,
            Principal principal) {
        try {
            if (principal == null) {
                return ResponseEntity.status(401).body("请先登录");
            }

            User user = userRepository.findByUsername(principal.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.status(401).body("当前用户不存在");
            }

            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("上传失败：请选择视频文件");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("上传失败：文件名无效");
            }

            String lowerName = originalFilename.toLowerCase();
            if (!(lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") ||
                    lowerName.endsWith(".mov") || lowerName.endsWith(".mkv"))) {
                return ResponseEntity.badRequest().body("上传失败：仅支持 mp4、avi、mov、mkv 格式视频");
            }

            // 👉 视频限制 100MB
            long maxSize = 100 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return ResponseEntity.badRequest().body("上传失败：视频不能超过 100MB");
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

            HttpEntity<MultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            // 👉 调 Python 视频接口
            String result = restTemplate.postForObject(yoloVideoApiUrl, requestEntity, String.class);

            // 解析 Python 返回结果
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(result);

            String videoUrl = root.has("video_url") ? root.get("video_url").asText() : null;
            Integer frameCount = root.has("frame_count") ? root.get("frame_count").asInt() : 0;

            // 保存视频检测记录
            DetectRecord record = new DetectRecord();
            record.setUser(user);
            record.setImageName(originalFilename);
            record.setDetectResult(result);
            record.setDetectTime(LocalDateTime.now());
            record.setFileType("video");
            record.setVideoUrl(videoUrl);
            record.setFrameCount(frameCount);

            detectRecordRepository.save(record);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("视频检测转发失败: " + e.getMessage());
            return ResponseEntity.status(500).body("视频检测转发失败: " + e.getMessage());
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

    @GetMapping("/stats")
    public String getStats(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/toLogin";
        }

        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        if (user == null) {
            return "redirect:/toLogin";
        }

        List<DetectRecord> records = detectRecordRepository.findByUserOrderByDetectTimeDesc(user);

        int totalImages = records.size();
        int totalObjects = 0;
        Map<String, Integer> classCountMap = new LinkedHashMap<>();

        ObjectMapper objectMapper = new ObjectMapper();

        for (DetectRecord record : records) {
            try {
                String detectResult = record.getDetectResult();
                if (detectResult == null || detectResult.isBlank()) {
                    continue;
                }

                JsonNode root = objectMapper.readTree(detectResult);
                JsonNode predictions = root.get("predictions");

                if (predictions != null && predictions.isArray()) {
                    for (JsonNode prediction : predictions) {
                        totalObjects++;

                        String className = prediction.has("name")
                                ? prediction.get("name").asText()
                                : "unknown";

                        classCountMap.put(className, classCountMap.getOrDefault(className, 0) + 1);
                    }
                }
            } catch (Exception e) {
                System.err.println("解析检测记录失败: " + e.getMessage());
            }
        }

        int classTypeCount = classCountMap.size();

        model.addAttribute("totalImages", totalImages);
        model.addAttribute("totalObjects", totalObjects);
        model.addAttribute("classTypeCount", classTypeCount);
        model.addAttribute("classCountMap", classCountMap);

        return "stats";
    }
}
