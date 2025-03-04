package com.weather.weather.service;

import com.weather.weather.entity.Bookmark;
import com.weather.weather.repository.BookmarkRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;

    // ✅ 1️⃣ 사용자의 즐겨찾기 목록 가져오기
    public List<String> getUserBookmarks(Long userId) {
        return bookmarkRepository.findByUserId(userId)
                .stream()
                .map(Bookmark::getLocation)
                .toList();
    }

    // ✅ 2️⃣ 즐겨찾기 추가
    @Transactional
    public void addBookmark(Long userId, String location) {
        // 이미 즐겨찾기한 도시인지 확인
        if (bookmarkRepository.findByUserIdAndLocation(userId, location).isPresent()) {
            throw new RuntimeException("이미 즐겨찾기에 추가된 도시입니다.");
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(userId);
        bookmark.setLocation(location);
        bookmarkRepository.save(bookmark);
    }

    // ✅ 3️⃣ 즐겨찾기 삭제
    @Transactional
    public void removeBookmark(Long userId, String location) {
        // ✅ 존재하는 즐겨찾기인지 확인
        if (!bookmarkRepository.findByUserIdAndLocation(userId, location).isPresent()) {
            throw new RuntimeException("해당 도시가 즐겨찾기에 없습니다.");
        }
        bookmarkRepository.deleteByUserIdAndLocation(userId, location);
    }

}