package com.artezo.service;


import com.artezo.dto.request.ContactRequestDTO;
import com.artezo.dto.response.ContactResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ContactService {
    ContactResponseDTO saveContact(ContactRequestDTO contactRequestDTO);
//    List<ContactResponseDTO> getAllContacts();

    public Page<ContactResponseDTO> getAllContacts(int page, int size, String sortBy, String sortDir);

    ContactResponseDTO getContactById(Long formId);

    void deleteContact(Long formId);
}
