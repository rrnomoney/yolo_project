package com.example.demo.repository;

import com.example.demo.pojo.DetectRecord;
import com.example.demo.pojo.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetectRecordRepository extends JpaRepository<DetectRecord, Long> {
    List<DetectRecord> findByUserOrderByDetectTimeDesc(User user);
}

