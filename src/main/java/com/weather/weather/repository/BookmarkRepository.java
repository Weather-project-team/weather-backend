package com.weather.weather.repository;

import com.weather.weather.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // ✅ 특정 사용자의 즐겨찾기 도시 목록 조회
    @Query("SELECT b.location FROM Bookmark b WHERE b.userId = :userId")
    List<String> findBookmarksByUserId(@Param("userId") Long userId);

    // ✅ 특정 사용자가 특정 도시를 즐겨찾기했는지 확인
    Optional<Bookmark> findByUserIdAndLocation(Long userId, String location);

    // ✅ 특정 사용자의 특정 도시 즐겨찾기 삭제
    void deleteByUserIdAndLocation(Long userId, String location);
}