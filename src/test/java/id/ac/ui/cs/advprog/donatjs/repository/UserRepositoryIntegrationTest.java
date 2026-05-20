package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@SuppressWarnings("null")
public class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private AppUser validUser;

    @BeforeEach
    void setUp() {
        validUser = new AppUser();
        validUser.setEmail("test@example.com");
        validUser.setPassword("password");
        validUser.setName("Test User");
        validUser.setBio("A bio");
        validUser.setDateOfBirth(LocalDate.of(1990, 1, 1));
        // Default fraud counts
        validUser.setRejectedDonationCount(0);
        validUser.setRejectedCampaignCount(0);
        validUser.setSuspended(false);
        validUser.setFlagged(false);
        validUser.setAdmin(false);
        validUser.setFraudActivityCount(0);
    }

    @Test
    void whenSave_thenPersistSuccessfully() {
        AppUser savedUser = userRepository.save(validUser);
        entityManager.flush();

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");

        AppUser found = entityManager.find(AppUser.class, savedUser.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("Test User");
    }

    @Test
    void whenDuplicateEmail_thenThrowDataIntegrityViolationException() {
        AppUser user1 = new AppUser();
        user1.setEmail("duplicate@example.com");
        user1.setPassword("pass");
        userRepository.save(user1);
        entityManager.flush();

        AppUser user2 = new AppUser();
        user2.setEmail("duplicate@example.com");
        user2.setPassword("pass2");

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(user2);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenFindByEmail_thenReturnUser() {
        userRepository.save(validUser);
        entityManager.flush();

        Optional<AppUser> found = userRepository.findByEmail("test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    void whenFindByEmailNotExists_thenReturnEmpty() {
        Optional<AppUser> found = userRepository.findByEmail("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void whenFindByFlaggedTrue_thenReturnFlaggedUsers() {
        userRepository.save(validUser); // Not flagged

        AppUser flaggedUser = new AppUser();
        flaggedUser.setEmail("flagged@example.com");
        flaggedUser.setFlagged(true);
        userRepository.save(flaggedUser);
        
        entityManager.flush();

        List<AppUser> flaggedUsers = userRepository.findByFlaggedTrue();
        assertThat(flaggedUsers).hasSize(1);
        assertThat(flaggedUsers.get(0).getEmail()).isEqualTo("flagged@example.com");
    }

    @Test
    void whenFindBySuspendedTrue_thenReturnSuspendedUsers() {
        userRepository.save(validUser); // Not suspended

        AppUser suspendedUser = new AppUser();
        suspendedUser.setEmail("suspended@example.com");
        suspendedUser.setSuspended(true);
        userRepository.save(suspendedUser);
        
        entityManager.flush();

        List<AppUser> suspendedUsers = userRepository.findBySuspendedTrue();
        assertThat(suspendedUsers).hasSize(1);
        assertThat(suspendedUsers.get(0).getEmail()).isEqualTo("suspended@example.com");
    }

    @Test
    void whenUpdateUser_thenPersistChanges() {
        AppUser savedUser = userRepository.save(validUser);
        entityManager.flush();

        savedUser.setBio("Updated bio");
        savedUser.setFraudActivityCount(5);
        userRepository.save(savedUser);
        entityManager.flush();

        AppUser found = entityManager.find(AppUser.class, savedUser.getId());
        assertThat(found.getBio()).isEqualTo("Updated bio");
        assertThat(found.getFraudActivityCount()).isEqualTo(5);
    }

    @Test
    void whenDeleteUser_thenRemoveFromDatabase() {
        AppUser savedUser = userRepository.save(validUser);
        entityManager.flush();
        UUID id = savedUser.getId();

        userRepository.delete(savedUser);
        entityManager.flush();

        AppUser found = entityManager.find(AppUser.class, id);
        assertThat(found).isNull();
    }
}
