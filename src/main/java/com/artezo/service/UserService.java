package com.artezo.service;

import com.artezo.dto.request.UserPatchDTO;
import com.artezo.dto.request.UserRegistrationDTO;
import com.artezo.dto.response.UserResponseDTO;
import com.artezo.dto.response.UserSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponseDTO registerUser(UserRegistrationDTO dto);

    UserResponseDTO getUserById(Long userId);

    Page<UserResponseDTO> getAllUsers(Pageable pageable);

    UserResponseDTO patchUser(Long userId, UserPatchDTO dto);

    void deleteUser(Long userId);

    UserResponseDTO registerGoogleUser(String name, String email);

    List<UserSummaryDto> getUserSummary();

}