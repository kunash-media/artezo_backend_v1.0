package com.artezo.service;


import com.artezo.dto.request.ContactRequestDTO;
import com.artezo.dto.response.ContactResponseDTO;

import java.util.List;

public interface ContactService {
    ContactResponseDTO saveContact(ContactRequestDTO contactRequestDTO);
    List<ContactResponseDTO> getAllContacts();

    ContactResponseDTO getContactById(Long formId);
    void deleteContact(Long formId);
}
