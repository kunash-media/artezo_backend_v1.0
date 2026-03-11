package com.artezo.service;

import com.artezo.dto.request.UserPatchDTO;
import com.artezo.dto.request.UserRegistrationDTO;
import com.artezo.dto.response.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDTO registerUser(UserRegistrationDTO dto);

    UserResponseDTO getUserById(Long userId);

    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    UserResponseDTO patchUser(Long userId, UserPatchDTO dto);

    void deleteUser(Long userId);
}