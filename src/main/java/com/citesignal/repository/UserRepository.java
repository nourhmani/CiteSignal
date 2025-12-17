package com.citesignal.repository;

import com.citesignal.model.Departement;
import com.citesignal.model.RoleName;
import com.citesignal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByResetToken(String token);
    List<User> findByRole(RoleName role);
    List<User> findByRoleAndDepartement(RoleName role, Departement departement);
}

