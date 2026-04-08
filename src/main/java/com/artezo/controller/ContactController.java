package com.artezo.controller;

import com.artezo.dto.request.ContactRequestDTO;
import com.artezo.dto.response.ContactResponseDTO;
import com.artezo.exceptions.DuplicateSubmissionException;
import com.artezo.service.ContactService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private static final Logger logger = LoggerFactory.getLogger(ContactController.class);

    @Autowired
    private ContactService contactService;

    @PostMapping("/submit-form")
    public ResponseEntity<String> submitForm(@Valid @RequestBody ContactRequestDTO contactRequestDTO) {
        logger.info("Received contact form submission: {}", contactRequestDTO);
        try {
            contactService.saveContact(contactRequestDTO);
            return ResponseEntity.ok("Thank you for your message!");
        } catch (DuplicateSubmissionException e) {
            logger.warn("Duplicate submission attempt from: {}", contactRequestDTO.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Already form submitted! We will inform you shortly.");
        }
    }

    @GetMapping("/get-all-contact-us")
    public ResponseEntity<Page<ContactResponseDTO>> getAllContacts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<ContactResponseDTO> contacts = contactService.getAllContacts(page, size, sortBy, sortDir);
        return ResponseEntity.ok(contacts);
    }

    @GetMapping("get-by-formId/{formId}")
    public ResponseEntity<ContactResponseDTO> getContactById(@PathVariable Long formId) {
        ContactResponseDTO contact = contactService.getContactById(formId);
        return ResponseEntity.ok(contact);
    }

    @DeleteMapping("delete-by-formId/{formId}")
    public ResponseEntity<String> deleteContact(@PathVariable Long formId) {
        contactService.deleteContact(formId);
        return ResponseEntity.ok("Contact deleted successfully!");
    }


}
