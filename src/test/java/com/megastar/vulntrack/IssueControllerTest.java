package com.megastar.vulntrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megastar.vulntrack.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
  @Autowired
  private JwtService jwtService;

  private final String authorization = "Authorization";

  private String adminToken;

  @BeforeEach
  void setUp() {
    adminToken = "Bearer " + jwtService.generateToken(1L, "USER");
  }

  public Long createInitialIssue(int userId, String payload) throws Exception {

    if (payload == null) {

      String title = "Random Title " + userId;
      payload = """
          {
            "title": "{title}",
            "description": "description",
            "severity": 3
          }
          """.replaceFirst("\\{title\\}", title);
    }

    System.out.println("------------->>> " + payload);

    String response = mockMvc.perform(post("/issues")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    return id;
  }

  @Test
  public void request_withoutUserHeader_shouldReturn401() throws Exception {

    mockMvc.perform(post("/issues"))
        .andDo(print())
        .andExpect(status().isUnauthorized());
  }

  @Test
  void request_withInvalidUserHeader_shouldReturn401() throws Exception {

    mockMvc.perform(get("/issues")
        .header(authorization, "abc"))
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
        .header(authorization, adminToken)
        .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("Valid title"));
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
        .header(authorization, adminToken)
        .content(payload))
        .andDo(print())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(get("/issues/" + id)
        .header(authorization, adminToken))
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
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(post("/issues")
        .contentType(MediaType.APPLICATION_JSON)
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long issueId = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(get("/issues/" + issueId)
        .header(authorization, "3"))
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
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk());

    String userToken = jwtService.generateToken(1L, payload2);

    mockMvc.perform(post("/issues")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload2))
        .andDo(print())
        .andExpect(status().isOk());

    mockMvc.perform(get("/issues")
        .header(authorization, adminToken))
        .andExpect(status().isOk())
        // .andExpect(jsonPath("$.length()").value(2))
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
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(put("/issues/" + id)
        .header(authorization, adminToken)
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
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    String otherUserToken = jwtService.generateToken(999L, "USER");

    mockMvc.perform(put("/issues/" + id)
        .header(authorization, otherUserToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatedPayload))
        .andDo(print())
        .andExpect(status().isForbidden());

  }

  @Test
  public void deleteIssue_onlyAdminCanDelete() throws Exception {

    String payload = """
        {
          "title": "owner can not delete",
          "description": "description",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(delete("/issues/" + id)
        .header(authorization, adminToken))
        .andDo(print())
        .andExpect(status().isForbidden());
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
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(delete("/issues/" + id)
        .header(authorization, adminToken))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  public void patchIssue_ownerCanPatch() throws Exception {

    String payload = """
        {
          "title": "owner can patch",
          "description": "description",
          "severity": 3
        }
        """;

    String updatedPayload = """
        {
          "title": "Updated Title"
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(patch("/issues/" + id)
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(updatedPayload))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated Title"));

  }

  @Test
  public void patchIssue_withDifferentOwner_shouldReturn403() throws Exception {

    String payload = """
        {
          "title": "owner can patch403",
          "description": "description",
          "severity": 3
        }
        """;

    String injectedPayload = """
        {
          "title": "Hacked"
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    String otherUserToken = jwtService.generateToken(999L, "USER");

    mockMvc.perform(patch("/issues/" + id)
        .header(authorization, otherUserToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(injectedPayload))
        .andDo(print())
        .andExpect(status().isForbidden());
  }

  @Test
  public void patchIssue_nonExisting_shouldReturn404() throws Exception {

    String payload = """
        {
          "title": "Does not matter"
        }
        """;

    mockMvc.perform(patch("/issues/99")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  void patchIssue_emptyBody_shouldReturn400() throws Exception {

    String payload = """
        {
          "title": "owner can patch400",
          "description": "description",
          "severity": 3
        }
        """;

    String response = mockMvc.perform(post("/issues")
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andDo(print())
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Long id = objectMapper.readTree(response).get("id").asLong();

    mockMvc.perform(patch("/issues/" + id)
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchIssue_attemptToChangeOwner_shouldReturn400() throws Exception {

    Long id = createInitialIssue(7, null);

    String payload = """
        {
          "ownerId": 999
        }
        """;

    mockMvc.perform(patch("/issues/" + id)
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  void patchIssue_invalidField_shouldReturn400() throws Exception {
    String payload = """
        {
          "title": ""
        }
        """;

    Long id = createInitialIssue(1, null);

    mockMvc.perform(patch("/issues/" + id)
        .header(authorization, adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void sortIssues_withSizeAbuse_shouldReturn400() throws Exception {
    // /issues?page=1&size=20&sort=severity,asc

    mockMvc.perform(get("/issues?page=0&size=500&sort=severity,asc")
        .header(authorization, adminToken))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void sortIssues_withSortFieldAbuse_shouldReturn400() throws Exception {
    // /issues?page=1&size=20&sort=severity,asc

    mockMvc.perform(get("/issues?page=0&size=50&sort=ownerId,asc")
        .header(authorization, adminToken))
        .andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void sortIssues_withDefaultSort_shouldReturnTenIssues() throws Exception {

    String userToken = jwtService.generateToken(10L, "USER");
    mockMvc.perform(get("/issues")
        .header(authorization, userToken))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(10));
  }

  @Test
  void sortIssues_withInvalidDirection_shouldReturn400() throws Exception {
    mockMvc.perform(get("/issues?page=0&size=10&sort=severity,sideways")
        .header(authorization, adminToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sortIssues_withInvalidDirectionAndNoSize_shouldReturn400() throws Exception {
    mockMvc.perform(get("/issues?sort=severity,sideways")
        .header(authorization, adminToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sortIssues_withTooManySortParts_shouldReturn400() throws Exception {
    mockMvc.perform(get("/issues?sort=severity,asc,extra")
        .header(authorization, adminToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sortIssues_withNegativePage_shouldReturn400() throws Exception {
    mockMvc.perform(get("/issues?page=-1&size=10")
        .header(authorization, adminToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sortIssues_withZeroSize_shouldReturn400() throws Exception {
    mockMvc.perform(get("/issues?page=0&size=0")
        .header(authorization, adminToken))
        .andExpect(status().isBadRequest());
  }

  @Test
  void request_withTokenMissingRole_shouldReturn401() throws Exception {
    String badToken = jwtService.generateToken(1L, null);

    mockMvc.perform(get("/issues")
        .header(authorization, "Bearer " + badToken))
        .andExpect(status().isUnauthorized());
  }

}
