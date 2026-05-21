package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.DisputeDTO;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Dispute;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.DisputeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class DisputeServiceImplTest {

    @Mock
    private DisputeRepository disputeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    private AppUser suspendedUser;
    private AppUser activeUser;
    private Dispute pendingDispute;

    @BeforeEach
    void setUp() {
        suspendedUser = new AppUser();
        suspendedUser.setId(UUID.randomUUID());
        suspendedUser.setEmail("suspended@test.com");
        suspendedUser.setSuspended(true);

        activeUser = new AppUser();
        activeUser.setId(UUID.randomUUID());
        activeUser.setEmail("active@test.com");
        activeUser.setSuspended(false);

        pendingDispute = new Dispute();
        pendingDispute.setId(UUID.randomUUID());
        pendingDispute.setUser(suspendedUser);
        pendingDispute.setReason("I did nothing wrong");
        pendingDispute.setStatus("PENDING");
        pendingDispute.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testSubmitDisputeSuccess() {
        when(userRepository.findById(suspendedUser.getId())).thenReturn(Optional.of(suspendedUser));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(invocation -> {
            Dispute d = invocation.getArgument(0);
            d.setId(UUID.randomUUID());
            d.setCreatedAt(LocalDateTime.now());
            return d;
        });

        DisputeDTO result = disputeService.submitDispute(suspendedUser.getId(), "My reason");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("My reason", result.getReason());
        verify(disputeRepository, times(1)).save(any(Dispute.class));
    }

    @Test
    void testSubmitDisputeFailsForActiveUser() {
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            disputeService.submitDispute(activeUser.getId(), "I want to appeal");
        });

        assertEquals("Only suspended users can submit a dispute", thrown.getMessage());
        verify(disputeRepository, never()).save(any(Dispute.class));
    }

    @Test
    void testGetDisputesByUser() {
        when(userRepository.findById(suspendedUser.getId())).thenReturn(Optional.of(suspendedUser));
        when(disputeRepository.findByUser(suspendedUser)).thenReturn(Arrays.asList(pendingDispute));

        List<DisputeDTO> results = disputeService.getDisputesByUser(suspendedUser.getId());

        assertEquals(1, results.size());
        assertEquals(pendingDispute.getId(), results.get(0).getId());
    }

    @Test
    void testGetAllPendingDisputes() {
        when(disputeRepository.findByStatus("PENDING")).thenReturn(Arrays.asList(pendingDispute));

        List<DisputeDTO> results = disputeService.getAllPendingDisputes();

        assertEquals(1, results.size());
        assertEquals(pendingDispute.getId(), results.get(0).getId());
    }

    @Test
    void testResolveDisputeApprove() {
        when(disputeRepository.findById(pendingDispute.getId())).thenReturn(Optional.of(pendingDispute));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(pendingDispute);

        DisputeDTO result = disputeService.resolveDispute(pendingDispute.getId(), true, "Approved appeal");

        assertEquals("APPROVED", result.getStatus());
        assertEquals("Approved appeal", result.getAdminNotes());
        assertFalse(suspendedUser.isSuspended());
        assertFalse(suspendedUser.isFlagged());
        assertEquals(0, suspendedUser.getFraudActivityCount());

        verify(userRepository, times(1)).save(suspendedUser);
        verify(disputeRepository, times(1)).save(pendingDispute);
    }

    @Test
    void testResolveDisputeReject() {
        when(disputeRepository.findById(pendingDispute.getId())).thenReturn(Optional.of(pendingDispute));
        when(disputeRepository.save(any(Dispute.class))).thenReturn(pendingDispute);

        DisputeDTO result = disputeService.resolveDispute(pendingDispute.getId(), false, "Rejected appeal");

        assertEquals("REJECTED", result.getStatus());
        assertEquals("Rejected appeal", result.getAdminNotes());
        assertTrue(suspendedUser.isSuspended());

        verify(userRepository, never()).save(suspendedUser);
        verify(disputeRepository, times(1)).save(pendingDispute);
    }

    @Test
    void testResolveDisputeAlreadyResolved() {
        pendingDispute.setStatus("APPROVED");
        when(disputeRepository.findById(pendingDispute.getId())).thenReturn(Optional.of(pendingDispute));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            disputeService.resolveDispute(pendingDispute.getId(), true, "Notes");
        });

        assertEquals("Dispute is already resolved", thrown.getMessage());
    }
}
