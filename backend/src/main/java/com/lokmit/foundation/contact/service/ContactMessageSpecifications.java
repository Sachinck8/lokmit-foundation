package com.lokmit.foundation.contact.service;

import com.lokmit.foundation.contact.entity.ContactMessage;
import org.springframework.data.jpa.domain.Specification;

/**
 * Parameterized query builders for the admin enquiry list.
 *
 * <p>Uses the JPA Criteria API via Spring Data {@link Specification}s — values
 * are bound as query parameters, never concatenated into SQL, so there is no
 * injection surface. Predicates compose so status filter, search and ordering
 * run entirely database-side over the existing
 * {@code idx_contact_messages_status_created} index.</p>
 */
final class ContactMessageSpecifications {

    private ContactMessageSpecifications() {
        throw new AssertionError("Utility class must not be instantiated.");
    }

    static Specification<ContactMessage> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<ContactMessage> senderOrSubjectContains(String term) {
        String like = "%" + term.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("senderName")), like),
                cb.like(cb.lower(root.get("senderEmail")), like),
                cb.like(cb.lower(root.get("subject")), like));
    }
}
