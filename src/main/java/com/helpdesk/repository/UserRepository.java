package com.helpdesk.repository;

import com.helpdesk.model.Role;
import com.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByOrganizationIdAndRole(Long organizationId, Role role);
    List<User> findByOrganizationId(Long organizationId);
}
