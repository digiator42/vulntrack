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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

  final String userId = "X-USER-ID";

  @Test
  public void request_withoutUserHeader_shouldReturn401() throws Exception {

    mockMvc.perform(post("/issues"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void request_withInvalidUserHeader_shouldReturn401() throws Exception {

    mockMvc.perform(get("/issues")
        .header("X-USER-ID", "abc"))
        .andExpect(status().isUnauthorized());
  }

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
        .header("X-USER-ID", 2)
        .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("Valid title"));
  }

  @Test
  public void getAllIssues_shouldReturnAllIssues() throws Exception {
    mockMvc.perform(get("/issues")
        .header("X-USER-ID", 1))
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
        .header("X-USER-ID", 1)
        .content(payload))
        .andDo(print())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(get("/issues/" + id)
        .header("X-USER-ID", 1))
        .andExpect(status().isOk())
        .andDo(print())
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
        .header("X-USER-ID", 1)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
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
        .header("X-USER-ID", 2)
        .content(payload))
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
        .header("X-USER-ID", 2)
        .content(payload))
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
        .header("X-USER-ID", 2)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(post("/issues")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-USER-ID", 2)
        .content(payload))
        .andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message")
            .value("Issue with this title already exists"));
  }

  @Test
  public void createIssue_withInjectedId_shouldReject() throws Exception {

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
        .header("X-USER-ID", 2)
        .content(payload))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getIssue_withDifferentOwner_shouldReturn403() throws Exception {

    String payload = """
        {
          "title": "Private issue",
          "description": "secret",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-USER-ID", 2)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long issueId = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(get("/issues/" + issueId)
        .header("X-USER-ID", "3"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void createIssue_withInjectedOwnerId_shouldReject() throws Exception {
    // injecting the id after X-USER-ID

    String payload = """
        {
          "title": "Injected Owner ID",
          "description": "secret",
          "severity": 3,
          "ownerId": 999
        }
        """;

    mockMvc.perform(post("/issues")
        .contentType(MediaType.APPLICATION_JSON)
        .header("X-USER-ID", 1)
        .content(payload))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getIssue_shouldReturnOnlyUserIssues() throws Exception {
    String payload = """
        {
          "title": "Owner 1",
          "description": "secret",
          "severity": 3
        }
        """;

    String payload2 = """
        {
          "title": "Owner 1/2",
          "description": "secret",
          "severity": 3
        }
        """;

    mockMvc.perform(post("/issues")
        .header("X-USER-ID", 3)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(post("/issues")
        .header("X-USER-ID", 3)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload2))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(get("/issues")
        .header("X-USER-ID", 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].title").value("Owner 1"));
  }

  @Test
  public void updateIssue_ownerCanUpdate() throws Exception {
    String payload = """
        {
          "title": "Create Me",
          "description": "secret description",
          "severity": 3
        }
        """;

    String updatedPayload = """
        {
          "title": "Update Me",
          "description": "secret description...",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header(userId, 4)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(put("/issues/" + id)
        .header(userId, 4)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatedPayload))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Update Me"));
  }

  @Test
  public void updateIssue_withDifferentOwner_shouldReturn403() throws Exception {
    String payload = """
        {
          "title": "Create Me again",
          "description": "secret description",
          "severity": 3
        }
        """;

    String updatedPayload = """
        {
          "title": "Update Me again",
          "description": "secret description...",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header(userId, 5)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(put("/issues/" + id)
        .header(userId, 6)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatedPayload))
        .andDo(print())
        .andExpect(status().isForbidden());

  }

  @Test
  public void deleteIssue_ownerCanDelete() throws Exception {

    String payload = """
        {
          "title": "owner can delete",
          "description": "description",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header("X-USER-ID", 1)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(patch("/issues/" + id)
        .header("X-USER-ID", 1))
        .andDo(print())
        .andExpect(status().isNoContent());
  }

  @Test
  public void deleteIssue_withDifferentOwner_shouldReturn403() throws Exception {
    String payload = """
        {
          "title": "different owner can not delete",
          "description": "description",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header("X-USER-ID", 1)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(patch("/issues/" + id)
        .header("X-USER-ID", 2))
        .andDo(print())
        .andExpect(status().isForbidden());
  }
}
