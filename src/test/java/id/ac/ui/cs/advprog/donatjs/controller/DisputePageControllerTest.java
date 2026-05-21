package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.service.CurrentUserService;
import id.ac.ui.cs.advprog.donatjs.service.DisputeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class DisputePageControllerTest {

    private DisputePageController disputePageController;

    @Mock
    private DisputeService disputeService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private Model model;

    @BeforeEach
    void setUp() {
        disputePageController = new DisputePageController(disputeService, currentUserService);
    }

    @Test
    void testDisputeFormSuspended() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setSuspended(true);

        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(disputeService.getDisputesByUser(user.getId())).thenReturn(Collections.emptyList());

        String view = disputePageController.disputeForm(model);

        assertEquals("dispute", view);
        verify(model).addAttribute(eq("myDisputes"), anyList());
    }

    @Test
    void testDisputeFormNotSuspended() {
        AppUser user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setSuspended(false);

        when(currentUserService.requireCurrentUser()).thenReturn(user);

        String view = disputePageController.disputeForm(model);

        assertEquals("redirect:/profile", view);
    }

    @Test
    void testAdminDisputes() {
        when(disputeService.getAllPendingDisputes()).thenReturn(Collections.emptyList());

        String view = disputePageController.adminDisputes(model);

        assertEquals("admin-disputes", view);
        verify(model).addAttribute(eq("pendingDisputes"), anyList());
    }

    private <T> java.util.List<T> anyList() {
        return any();
    }
}
