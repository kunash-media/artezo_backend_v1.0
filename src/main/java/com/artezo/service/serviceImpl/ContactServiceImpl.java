package com.artezo.service.serviceImpl;

import com.artezo.dto.request.ContactRequestDTO;
import com.artezo.dto.response.ContactResponseDTO;
import com.artezo.entity.ContactEntity;
import com.artezo.repository.ContactRepository;
import com.artezo.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContactServiceImpl implements ContactService {
    @Autowired
    private ContactRepository contactRepository;

    @Override
    public ContactResponseDTO saveContact(ContactRequestDTO contactRequestDTO) {
        ContactEntity ContactEntity = new ContactEntity();
        ContactEntity.setName(contactRequestDTO.getName());
        ContactEntity.setEmail(contactRequestDTO.getEmail());
        ContactEntity.setPhone(contactRequestDTO.getPhone());
        ContactEntity.setMessage(contactRequestDTO.getMessage());
        ContactEntity savedContact = contactRepository.save(ContactEntity);
        return convertToResponseDTO(savedContact);
    }


    @Override
    public List<ContactResponseDTO> getAllContacts() {
        return contactRepository.findAll().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
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
