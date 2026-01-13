package com.megastar.vulntrack;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megastar.vulntrack.model.Issue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.PathMatcher;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
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
    @Autowired
    private PathMatcher pathMatcher;

    @Test
    public void createIssue_shouldReturnCreatedIssue() throws Exception {
        Issue issue = new Issue(null, "Test Issue", "Test Description",2,1L);
        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value(issue.getTitle()));
    }

    @Test
    public void getAllIssues_shouldReturnAllIssues() throws Exception {
        mockMvc.perform(get("/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


    @Test
    public void getIssueId_shouldReturnIssueId() throws Exception {
        Issue issue = new Issue(null, "Test2 Issue", "Description",3,2L);

        String response = mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
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
        Issue issue = new Issue(null, null, "Description", 4, 2L);

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void createIssue_withoutSeverity_shouldReturn400() throws Exception {
        Issue issue = new Issue(null, "Not null", "Description", null, 2L);

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].defaultMessage")
                .value("Severity is required"));
    }

    @Test
    public void createIssue_withoutMultipleFields_shouldReturnAllFieldsError() throws Exception {
        Issue issue = new Issue(null, null, "Not null", null, 4L);
        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("title", "severity")));
    }

    @Test
    public void createIssue_withDuplicateTitles_shouldReturnConflict409() throws Exception {
        Issue issue = new Issue(null, "Same Title", "Description", 2, 2L);

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
                .andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issue)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Issue with this title already exists"));
    }

    @Test
    public void CreateIssue_withInjectedId_shouldReject() throws Exception {
        Issue issue = new Issue(999L, "Injected ID", "Test", 3, 4L);

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
                .content(objectMapper.writeValueAsString(issue)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
