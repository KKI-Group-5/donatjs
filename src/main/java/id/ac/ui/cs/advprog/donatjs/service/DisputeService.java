package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.DisputeDTO;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

public interface DisputeService {
    DisputeDTO submitDispute(@NonNull UUID userId, @NonNull String reason);
    List<DisputeDTO> getDisputesByUser(@NonNull UUID userId);
    List<DisputeDTO> getAllPendingDisputes();
    DisputeDTO resolveDispute(@NonNull UUID disputeId, boolean approve, String adminNotes);
}
