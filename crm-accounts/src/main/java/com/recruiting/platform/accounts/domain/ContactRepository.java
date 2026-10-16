package com.recruiting.platform.accounts.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    List<Contact> findByAccountIdAndEndedOnIsNull(UUID accountId);

    List<Contact> findByPersonId(UUID personId);

    Optional<Contact> findByAccountIdAndPersonIdAndEndedOnIsNull(UUID accountId, UUID personId);
}
