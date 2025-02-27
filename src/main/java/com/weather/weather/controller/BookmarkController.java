package com.weather.weather.controller;

import com.weather.weather.entity.BookmarkRequestDTO;
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

    // 즐찾 추가
    @PostMapping
    public ResponseEntity<?> addBookmark(@RequestBody BookmarkRequestDTO request) {
        try {
            bookmarkService.addBookmark(request.getUserId(), request.getLocation());
            return ResponseEntity.ok("즐겨찾기에 추가되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 즐찾 삭제
    @DeleteMapping
    public ResponseEntity<?> removeBookmark(@RequestBody BookmarkRequestDTO request) {
        bookmarkService.removeBookmark(request.getUserId(), request.getLocation());
        return ResponseEntity.ok("즐겨찾기에서 삭제되었습니다.");
    }

    // 즐찾 조회
    @PostMapping("/list")
    public ResponseEntity<List<String>> getBookmarks(@RequestBody BookmarkRequestDTO request) {
        return ResponseEntity.ok(bookmarkService.getUserBookmarks(request.getUserId()));
    }
}
