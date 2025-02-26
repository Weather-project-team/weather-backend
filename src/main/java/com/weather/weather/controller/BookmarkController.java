package com.weather.weather.controller;

import com.weather.weather.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // ✅ 요청마다 userId를 프론트에서 보내도록 설정
    @PostMapping
    public ResponseEntity<?> addBookmark(@RequestParam Long userId, @RequestParam String location) {
        try {
            bookmarkService.addBookmark(userId, location);
            return ResponseEntity.ok("즐겨찾기에 추가되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ 요청마다 userId
    @DeleteMapping
    public ResponseEntity<?> removeBookmark(@RequestParam Long userId, @RequestParam String location) {
        bookmarkService.removeBookmark(userId, location);
        return ResponseEntity.ok("즐겨찾기에서 삭제되었습니다.");
    }

    // ✅ 요청마다 userId
    @GetMapping
    public ResponseEntity<List<String>> getBookmarks(@RequestParam Long userId) {
        return ResponseEntity.ok(bookmarkService.getUserBookmarks(userId));
    }
}