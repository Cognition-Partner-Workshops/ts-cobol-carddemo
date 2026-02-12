package com.aws.carddemo.user;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.aws.carddemo.user.dto.UserCreateRequest;
import com.aws.carddemo.user.dto.UserUpdateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "ADMIN001", roles = {"ADMIN"})
    void fullCrudFlow() throws Exception {
        // Create
        var create = new UserCreateRequest("TEST0001", "Secr3tPwd!", "Test", "User", "U");
        String createResp = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("TEST0001"))
                .andReturn().getResponse().getContentAsString();

        JsonNode created = objectMapper.readTree(createResp);
        long id = created.get("id").asLong();

        // List (ensure at least 1)
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());

        // Get by id
        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("TEST0001"));

        // Update
        var update = new UserUpdateRequest(null, "Tester", null, "A");
        mockMvc.perform(put("/api/users/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Tester"))
                .andExpect(jsonPath("$.userType").value("A"));

        // Delete
        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isNoContent());

        // Get after delete -> 404
        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isNotFound());
    }
}
