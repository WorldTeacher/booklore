package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.KoreaderStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KoreaderStatisticsRepository extends JpaRepository<KoreaderStatisticsEntity, Long> {
    
    Optional<KoreaderStatisticsEntity> findByUserIdAndKoreaderBookMd5(Long userId, String koreaderBookMd5);
    
    List<KoreaderStatisticsEntity> findByUserId(Long userId);
    
    List<KoreaderStatisticsEntity> findByUserIdAndBookId(Long userId, Long bookId);
}
