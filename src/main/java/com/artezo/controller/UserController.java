package com.artezo.controller;

import com.artezo.dto.request.UserPatchDTO;
import com.artezo.dto.request.UserRegistrationDTO;
import com.artezo.dto.response.UserResponseDTO;
import com.artezo.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    // ─────────────────────────────────────────────────────────────
    // POST /api/users/register
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationDTO dto) {
        log.info("[Controller] POST /api/users/register - email={}", dto.getEmail());
        try {
            UserResponseDTO response = userService.registerUser(dto);
            log.info("[Controller] register - success, userId={}", response.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("[Controller] register - failed, reason={}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/users/{userId}
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/get-user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        log.info("[Controller] GET /api/users/{} - getUserById called", userId);
        try {
            UserResponseDTO response = userService.getUserById(userId);
            log.info("[Controller] getUserById - found userId={}", userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("[Controller] getUserById - not found, userId={}, reason={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/users?page=0&size=10&sort=createdAt,desc
    // ─────────────────────────────────────────────────────────────
    @GetMapping("/get-all-users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "userId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("[Controller] GET /api/users - getAllUsers called, page={}, size={}, sortBy={}, sortDir={}",
                page, size, sortBy, sortDir);
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sort);
            Page<UserResponseDTO> response = userService.getAllUsers(pageable);

            log.info("[Controller] getAllUsers - returning page={} of {}, totalElements={}",
                    response.getNumber(), response.getTotalPages(), response.getTotalElements());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("[Controller] getAllUsers - failed, reason={}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PATCH /api/users/{userId}
    // ─────────────────────────────────────────────────────────────
    @PatchMapping("/patch-user/{userId}")
    public ResponseEntity<?> patchUser(
            @PathVariable Long userId,
            @RequestBody UserPatchDTO dto) {
        log.info("[Controller] PATCH /api/users/{} - patchUser called", userId);
        try {
            UserResponseDTO response = userService.patchUser(userId, dto);
            log.info("[Controller] patchUser - success, userId={}", userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("[Controller] patchUser - failed, userId={}, reason={}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // DELETE /api/users/{userId}
    // ─────────────────────────────────────────────────────────────
    @DeleteMapping("/delete-user/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        log.info("[Controller] DELETE /api/users/{} - deleteUser called", userId);
        try {
            userService.deleteUser(userId);
            log.info("[Controller] deleteUser - success, userId={}", userId);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            log.error("[Controller] deleteUser - failed, userId={}, reason={}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}