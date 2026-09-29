package com.lokmit.foundation.contact.repository;

import com.lokmit.foundation.contact.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for website contact enquiries.
 *
 * <p>Extends {@link JpaSpecificationExecutor} so admin list queries (status
 * filter, search, pagination, sorting) compose from parameterized Criteria
 * predicates — no string-concatenated SQL anywhere.</p>
 */
@Repository
public interface ContactMessageRepository
        extends JpaRepository<ContactMessage, Long>, JpaSpecificationExecutor<ContactMessage> {
}
