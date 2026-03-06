package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<AppUser, UUID> {
    // Spring magically implements this method just based on its name!
    Optional<AppUser> findByEmail(String email);
}