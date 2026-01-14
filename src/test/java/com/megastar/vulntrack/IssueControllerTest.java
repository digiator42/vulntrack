package com.megastar.vulntrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class IssueControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void createIssue_shouldReturnCreatedIssue() throws Exception {

        String payload = """
        {
          "title": "Valid title",
          "description": "Desc",
          "severity": 3
        }
        """;

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Valid title"));
    }

    @Test
    public void getAllIssues_shouldReturnAllIssues() throws Exception {
        mockMvc.perform(get("/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void getIssueId_shouldReturnIssueId() throws Exception {

        String payload = """
        {
          "title": "Test Issue ID",
          "description": "Desc",
          "severity": 3
        }
        """;

        String response = mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/issues/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

    }

    @Test
    public void createIssue_withoutTitle_shouldReturn400() throws Exception {

        String payload = """
        {
          "title": null,
          "description": "Desc",
          "severity": 4
        }
        """;

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createIssue_withoutSeverity_shouldReturn400() throws Exception {

        String payload = """
        {
          "title": "Not null",
          "description": "Desc",
          "severity": null
        }
        """;

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].defaultMessage")
                .value("Severity is required"));
    }

    @Test
    public void createIssue_withoutMultipleFields_shouldReturnAllFieldsError() throws Exception {
        String payload = """
        {
          "title": null,
          "description": "Desc",
          "severity": null
        }
        """;

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("title", "severity")));
    }

    @Test
    public void createIssue_withDuplicateTitles_shouldReturnConflict409() throws Exception {

        String payload = """
        {
          "title": "Duplicate Title",
          "description": "Desc",
          "severity": 3
        }
        """;


        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Issue with this title already exists"));
    }

    @Test
    public void CreateIssue_withInjectedId_shouldReject() throws Exception {

        String payload = """
        {
              "id": 999,
              "title": "Injected ID",
              "description": "Test",
              "severity": 3,
              "ownerId": 2
        }
        """;
        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
