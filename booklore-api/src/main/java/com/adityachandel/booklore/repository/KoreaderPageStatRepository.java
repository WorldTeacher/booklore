package com.adityachandel.booklore.repository;

import com.adityachandel.booklore.model.entity.KoreaderPageStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KoreaderPageStatRepository extends JpaRepository<KoreaderPageStatEntity, Long> {
    
    List<KoreaderPageStatEntity> findByKoreaderStatisticsId(Long koreaderStatisticsId);
    
    void deleteByKoreaderStatisticsId(Long koreaderStatisticsId);
}
