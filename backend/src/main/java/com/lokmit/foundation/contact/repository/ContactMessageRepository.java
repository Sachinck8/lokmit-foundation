package com.lokmit.foundation.contact.repository;

import com.lokmit.foundation.contact.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for website contact enquiries.
 */
@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
}
