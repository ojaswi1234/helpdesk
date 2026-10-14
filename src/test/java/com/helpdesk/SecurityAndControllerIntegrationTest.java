package com.helpdesk;

import com.helpdesk.model.User;
import com.helpdesk.repository.UserRepository;
import com.helpdesk.service.CustomUserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAndControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Unauthenticated request to /dashboard should redirect to /login")
    void testUnauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Customer user accessing admin organization settings receives 403 Forbidden")
    @WithMockUser(username = "acme_customer", roles = {"CUSTOMER"})
    void testCustomerAccessToAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/admin/organization"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Organization admin accessing admin organization settings receives 200 OK")
    void testOrgAdminAccessToAdmin_ReturnsOk() throws Exception {
        User admin = userRepository.findByUsername("acme_admin").orElseThrow();
        CustomUserPrincipal principal = new CustomUserPrincipal(admin);

        mockMvc.perform(get("/admin/organization").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/organization"));
    }

    @Test
    @DisplayName("Customer accessing tickets list receives 200 OK")
    void testCustomerAccessToTickets_ReturnsOk() throws Exception {
        User customer = userRepository.findByUsername("acme_customer").orElseThrow();
        CustomUserPrincipal principal = new CustomUserPrincipal(customer);

        mockMvc.perform(get("/tickets").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"));
    }

    @Test
    @DisplayName("REST API GET /api/v1/tickets returns 200 OK with paginated JSON")
    void testRestApiTickets_ReturnsOk() throws Exception {
        User agent = userRepository.findByUsername("acme_agent").orElseThrow();
        CustomUserPrincipal principal = new CustomUserPrincipal(agent);

        mockMvc.perform(get("/api/v1/tickets").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
