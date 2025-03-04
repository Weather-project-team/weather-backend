package com.weather.weather.controller;

import com.weather.weather.entity.BookmarkRequestDTO;
import com.weather.weather.entity.User;
import com.weather.weather.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // 즐찾 추가
    @PostMapping
    public ResponseEntity<?> addBookmark(
            @AuthenticationPrincipal User user, // ✅ Spring Security 인증 객체에서 사용자 정보 가져오기
            @RequestBody BookmarkRequestDTO request) {

        bookmarkService.addBookmark(user.getId(), request.getLocation());
        return ResponseEntity.ok("즐겨찾기에 추가되었습니다.");
    }

    // 즐찾 삭제
    // DELETE 요청에는 @RequestParam 사용해야 함
    @DeleteMapping
    public ResponseEntity<?> removeBookmark(@RequestParam Long userId, @RequestParam String location) {
        try {
            bookmarkService.removeBookmark(userId, location);
            return ResponseEntity.ok("즐겨찾기에서 삭제되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 즐찾 조회
    // GET 요청에도 @RequestParam 사용해야 함
    @GetMapping
    public ResponseEntity<?> getBookmarks(@RequestParam Long userId) {
        try {
            List<String> bookmarks = bookmarkService.getUserBookmarks(userId);
            return ResponseEntity.ok(bookmarks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}