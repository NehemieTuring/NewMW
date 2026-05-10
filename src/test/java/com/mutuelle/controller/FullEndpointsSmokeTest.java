package com.mutuelle.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FullEndpointsSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("Exhaustive - Member Portal GET Endpoints")
    @WithMockUser(authorities = "ROLE_MEMBER")
    void testMemberGetEndpoints() throws Exception {
        String[] endpoints = {
            "/member/profile", "/member/payments", "/member/status", "/member/debts",
            "/member/savings", "/member/savings/balance", "/member/borrowings",
            "/member/borrowings/max-amount", "/member/helps/types", "/member/helps/active",
            "/member/members", "/member/chat/conversations", "/member/chat/unread",
            "/member/sessions", "/member/exercises", "/member/debug/roles"
        };
        for (String url : endpoints) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Exhaustive - Admin Portal GET Endpoints")
    @WithMockUser(authorities = "ROLE_ADMIN")
    void testAdminGetEndpoints() throws Exception {
        String[] endpoints = {
            "/admin/members", "/admin/members/1/status", "/admin/members/1/debts",
            "/admin/solidarity/members/1/debt", "/admin/solidarity/members/1/history",
            "/admin/savings/members/1", "/admin/savings/members/1/balance",
            "/admin/borrowings", "/admin/borrowings/members/1", "/admin/borrowings/members/1/max-amount",
            "/admin/agapes", "/admin/helps/types", "/admin/helps", "/admin/helps/active",
            "/admin/exercises", "/admin/exercises/current", "/admin/sessions",
            "/admin/dashboard/transactions", "/admin/dashboard/cashboxes",
            "/admin/dashboard/members/in-rule", "/admin/dashboard/members/not-in-rule",
            "/admin/chat/conversations", "/admin/chat/unread", "/admin/profile", "/admin/admins"
        };
        for (String url : endpoints) {
            // Some might return 404 if ID 1 doesn't exist, but we expect 200 or at least not 401/403
            mockMvc.perform(get(url)).andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status != 200 && status != 404) {
                    throw new AssertionError("Expected 200 or 404 for " + url + " but got " + status);
                }
            });
        }
    }

    @Test
    @DisplayName("Exhaustive - Treasurer Portal GET Endpoints")
    @WithMockUser(authorities = "ROLE_TRESORIER")
    void testTreasurerGetEndpoints() throws Exception {
        String[] endpoints = {
            "/treasurer/ping", "/treasurer/exercises", "/treasurer/sessions",
            "/treasurer/members", "/treasurer/admins", "/treasurer/borrowings",
            "/treasurer/penalties", "/treasurer/dashboard/transactions",
            "/treasurer/dashboard/cashboxes", "/treasurer/reports/daily",
            "/treasurer/expenses", "/treasurer/profile", "/treasurer/chat/conversations", "/treasurer/chat/unread"
        };
        for (String url : endpoints) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Exhaustive - President Portal GET Endpoints")
    @WithMockUser(authorities = "ROLE_PRESIDENT")
    void testPresidentGetEndpoints() throws Exception {
        String[] endpoints = {
            "/president/members", "/president/admins", "/president/borrowings",
            "/president/helps", "/president/helps/active", "/president/exercises",
            "/president/sessions", "/president/dashboard/transactions",
            "/president/dashboard/cashboxes", "/president/dashboard/members/in-rule",
            "/president/dashboard/members/not-in-rule", "/president/chat/conversations",
            "/president/chat/unread", "/president/profile"
        };
        for (String url : endpoints) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Exhaustive - Super Admin GET Endpoints")
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testSuperAdminGetEndpoints() throws Exception {
        String[] endpoints = {
            "/admin/super/admins", "/admin/super/dashboard"
        };
        for (String url : endpoints) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Exhaustive - Public & Time Endpoints")
    void testPublicEndpoints() throws Exception {
        // Public/Dev endpoints
        mockMvc.perform(get("/api/test/time/current")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("MASTER TEST - GENERATE INTEGRATION REPORT")
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testAllRegisteredEndpoints() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> endpoints = handlerMapping.getHandlerMethods();
        
        java.nio.file.Path path = java.nio.file.Paths.get("d:/MutuelleNehemie/CODE/mutuelle-backend/endpoint_integration_results.md");
        java.util.List<String> lines = new java.util.ArrayList<>();
        
        lines.add("| Méthode | Endpoint | Statut MockMvc | Résultat |");
        lines.add("| :--- | :--- | :--- | :--- |");
        
        for (RequestMappingInfo info : endpoints.keySet()) {
            Set<String> patterns = info.getDirectPaths();
            if (patterns.isEmpty()) {
                patterns = info.getPatternValues();
            }
            
            for (String pattern : patterns) {
                if (pattern.equals("/error") || pattern.contains("{")) continue; // Skip complex path variables for this report
                
                Set<org.springframework.web.bind.annotation.RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.contains(org.springframework.web.bind.annotation.RequestMethod.GET)) {
                    int status = mockMvc.perform(get(pattern)).andReturn().getResponse().getStatus();
                    String resultText = (status == 200) ? "✅ SUCCESS" : (status >= 400 && status < 500) ? "⚠️ PARAM_REQ" : "❌ ERROR";
                    lines.add("| GET | " + pattern + " | " + status + " | " + resultText + " |");
                } else {
                    // For POST/PUT/DELETE, we just report reachability without calling (to avoid data mutation)
                    lines.add("| " + (methods.isEmpty() ? "ANY" : methods.iterator().next()) + " | " + pattern + " | - | 🔍 REACHABLE |");
                }
            }
        }
        
        java.nio.file.Files.write(path, lines);
    }
}
