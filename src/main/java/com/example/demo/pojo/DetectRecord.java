package com.example.demo.pojo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "detect_record")
public class DetectRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(nullable = false)
    private String imageName;

    @Column(columnDefinition = "TEXT")
    private String detectResult;

    private LocalDateTime detectTime;
    @Column(nullable = false)
    private String fileType = "image";

    @Column(columnDefinition = "TEXT")
    private String videoUrl;

    private Integer frameCount;
    public DetectRecord() {}

    public DetectRecord(User user, String imageName, String detectResult, LocalDateTime detectTime) {
        this.user = user;
        this.imageName = imageName;
        this.detectResult = detectResult;
        this.detectTime = detectTime;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public String getDetectResult() { return detectResult; }
    public void setDetectResult(String detectResult) { this.detectResult = detectResult; }

    public LocalDateTime getDetectTime() { return detectTime; }
    public void setDetectTime(LocalDateTime detectTime) { this.detectTime = detectTime; }
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public Integer getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(Integer frameCount) {
        this.frameCount = frameCount;
    }
}

