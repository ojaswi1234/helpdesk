package com.helpdesk.service;

import com.helpdesk.exception.AttachmentStorageException;
import com.helpdesk.exception.UnauthorizedAttachmentAccessException;
import com.helpdesk.model.Attachment;
import com.helpdesk.model.Role;
import com.helpdesk.model.Ticket;
import com.helpdesk.model.TicketComment;
import com.helpdesk.model.User;
import com.helpdesk.repository.AttachmentRepository;
import com.helpdesk.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentService.class);

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final Path storageLocation;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             UserRepository userRepository,
                             TicketService ticketService,
                             @Value("${helpdesk.attachments.storage-dir:./uploads/attachments}") String storageDir) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.storageLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new AttachmentStorageException("Could not initialize attachment storage directory: " + this.storageLocation, ex);
        }
    }

    @Transactional
    public Attachment uploadAttachment(Long ticketId, Long commentId, MultipartFile file, CustomUserPrincipal principal) {
        Ticket ticket = ticketService.getTicketEntityAndVerifyTenant(ticketId, principal);
        User uploader = userRepository.findById(principal.getUserId()).orElseThrow();

        if (file.isEmpty()) {
            throw new AttachmentStorageException("Cannot upload an empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        // Path Traversal Security Protection
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new AttachmentStorageException("Invalid filename containing path traversal characters: " + originalFilename);
        }

        String storedFilename = UUID.randomUUID() + "_" + originalFilename;

        try {
            Path targetPath = this.storageLocation.resolve(storedFilename).normalize();
            if (!targetPath.getParent().equals(this.storageLocation)) {
                throw new AttachmentStorageException("Cannot store file outside current storage directory");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new AttachmentStorageException("Failed to store file: " + originalFilename, ex);
        }

        Attachment attachment = new Attachment(
                ticket,
                null,
                uploader,
                originalFilename,
                storedFilename,
                file.getSize(),
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        );

        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public Resource loadAttachmentAsResource(Long attachmentId, CustomUserPrincipal principal) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new AttachmentStorageException("Attachment not found with ID: " + attachmentId));

        // Enforce ticket tenant isolation
        ticketService.verifyTenantAccess(attachment.getTicket(), principal);

        // Enforce internal note privacy
        TicketComment comment = attachment.getComment();
        if (comment != null && comment.isInternalNote() && principal.getRole() == Role.ROLE_CUSTOMER) {
            throw new UnauthorizedAttachmentAccessException("Customers cannot access attachments linked to internal staff notes.");
        }

        try {
            Path filePath = this.storageLocation.resolve(attachment.getStoredFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new AttachmentStorageException("File not found or unreadable on disk: " + attachment.getOriginalFilename());
            }
        } catch (MalformedURLException ex) {
            throw new AttachmentStorageException("Malformed URI for attachment: " + attachment.getStoredFilename(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsForTicket(Long ticketId, CustomUserPrincipal principal) {
        ticketService.getTicketEntityAndVerifyTenant(ticketId, principal);
        return attachmentRepository.findByTicketId(ticketId);
    }
}
