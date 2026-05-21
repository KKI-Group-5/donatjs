package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.DisputeDTO;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.Dispute;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.repository.DisputeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@Service
public class DisputeServiceImpl implements DisputeService {

    private static final String STATUS_PENDING = "PENDING";
    
    private final DisputeRepository disputeRepository;
    private final UserRepository userRepository;

    public DisputeServiceImpl(DisputeRepository disputeRepository, UserRepository userRepository) {
        this.disputeRepository = disputeRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public DisputeDTO submitDispute(@NonNull UUID userId, @NonNull String reason) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isSuspended()) {
            throw new IllegalStateException("Only suspended users can submit a dispute");
        }

        Dispute dispute = new Dispute();
        dispute.setUser(user);
        dispute.setReason(reason);
        dispute.setStatus(STATUS_PENDING);
        
        dispute = disputeRepository.save(dispute);
        return mapToDTO(dispute);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeDTO> getDisputesByUser(@NonNull UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
                
        return disputeRepository.findByUser(user).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisputeDTO> getAllPendingDisputes() {
        return disputeRepository.findByStatus(STATUS_PENDING).stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional
    public DisputeDTO resolveDispute(@NonNull UUID disputeId, boolean approve, String adminNotes) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException("Dispute not found"));

        if (!STATUS_PENDING.equals(dispute.getStatus())) {
            throw new IllegalStateException("Dispute is already resolved");
        }

        dispute.setStatus(approve ? "APPROVED" : "REJECTED");
        dispute.setAdminNotes(adminNotes);

        if (approve) {
            AppUser user = dispute.getUser();
            user.setSuspended(false);
            user.setSuspendedLegacy(false);
            user.setFlagged(false);
            user.setFlaggedForReview(false);
            user.setFraudActivityCount(0);
            userRepository.save(user);
        }

        dispute = disputeRepository.save(dispute);
        return mapToDTO(dispute);
    }

    private DisputeDTO mapToDTO(Dispute dispute) {
        DisputeDTO dto = new DisputeDTO();
        dto.setId(dispute.getId());
        dto.setUserId(dispute.getUser().getId());
        dto.setUserEmail(dispute.getUser().getEmail());
        dto.setReason(dispute.getReason());
        dto.setStatus(dispute.getStatus());
        dto.setAdminNotes(dispute.getAdminNotes());
        dto.setCreatedAt(dispute.getCreatedAt());
        return dto;
    }
}
