package com.artezo.service.serviceImpl;

import com.artezo.dto.request.ContactRequestDTO;
import com.artezo.dto.response.ContactResponseDTO;
import com.artezo.entity.ContactEntity;
import com.artezo.exceptions.DuplicateSubmissionException;
import com.artezo.repository.ContactRepository;
import com.artezo.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {
    @Autowired
    private ContactRepository contactRepository;

    @Override
    public ContactResponseDTO saveContact(ContactRequestDTO contactRequestDTO) {
        // Check for duplicate submission within last 24 hours
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        Optional<ContactEntity> existingContact = contactRepository.findByEmailAndCreatedAtAfter(contactRequestDTO.getEmail(), twentyFourHoursAgo);

        if (existingContact.isPresent()) {
            throw new DuplicateSubmissionException("You have already submitted a form recently. We will get back to you shortly!");
        }

        ContactEntity contactEntity = new ContactEntity();
        contactEntity.setName(contactRequestDTO.getName());
        contactEntity.setEmail(contactRequestDTO.getEmail());
        contactEntity.setPhone(contactRequestDTO.getPhone());
        contactEntity.setMessage(contactRequestDTO.getMessage());
        ContactEntity savedContact = contactRepository.save(contactEntity);
        return convertToResponseDTO(savedContact);
    }


    @Override
    public Page<ContactResponseDTO> getAllContacts(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<ContactEntity> contactPage = contactRepository.findAll(pageable);

        return contactPage.map(this::convertToResponseDTO);
    }


    // Helper methods for mapping
    private ContactEntity mapToEntity(ContactRequestDTO dto) {
        ContactEntity ContactEntity = new ContactEntity();
        ContactEntity.setName(dto.getName());
        ContactEntity.setEmail(dto.getEmail());
        ContactEntity.setPhone(dto.getPhone());
        ContactEntity.setMessage(dto.getMessage());
        return ContactEntity;
    }

    @Override
    public ContactResponseDTO getContactById(Long formId) {
        ContactEntity ContactEntity = contactRepository.findById(formId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ContactEntity not found with id: " + formId));
        return convertToResponseDTO(ContactEntity);
    }

    @Override
    public void deleteContact(Long formId) {
        if (!contactRepository.existsById(formId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ContactEntity not found with id: " + formId);
        }
        contactRepository.deleteById(formId);
    }

    private ContactResponseDTO convertToResponseDTO(ContactEntity ContactEntity) {
        ContactResponseDTO responseDTO = new ContactResponseDTO();
        responseDTO.setFormId(ContactEntity.getFormId());
        responseDTO.setName(ContactEntity.getName());
        responseDTO.setEmail(ContactEntity.getEmail());
        responseDTO.setPhone(ContactEntity.getPhone());
        responseDTO.setMessage(ContactEntity.getMessage());
        return responseDTO;
    }
}
