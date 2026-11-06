package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account, UC-STU-deactivate-student extension 6d: a token issued before an account was deactivated
 * is refused on the next request, for every role, while an enabled account's requests are unaffected.
 *
 * <p>These flip {@code enabled} straight on the user row, so they pin the shared per-request check without
 * depending on the deactivation endpoint. Each test is transactional, so the flag is restored on rollback.
 */
@DisplayName("Integration tests for the per-request account-state check")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    String studentJohnToken;

    String instructorBillToken;

    Integer studentJohnId;


    @BeforeEach
    void setUp() throws Exception {
        this.studentJohnToken = login("j.smith@abc.edu");
        this.instructorBillToken = login("b.gates@abc.edu");
        this.studentJohnId = this.userRepository.findByEmail("j.smith@abc.edu").orElseThrow().getId();
    }

    private String login(String email) throws Exception {
        MvcResult mvcResult = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(email, "123456"))).andReturn();
        JSONObject json = new JSONObject(mvcResult.getResponse().getContentAsString());
        return "Bearer " + json.getJSONObject("data").getString("token");
    }

    private void setEnabled(String email, boolean enabled) {
        PeerEvaluationUser user = this.userRepository.findByEmail(email).orElseThrow();
        user.setEnabled(enabled);
        this.userRepository.saveAndFlush(user);
    }

    @Test
    void testEnabledAccountIsUnaffected() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.studentJohnId).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true));
    }

    @Test
    void testTokenIssuedBeforeDeactivationIsRefusedOnTheNextRequest() throws Exception {
        setEnabled("j.smith@abc.edu", false);

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.studentJohnId).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testEveryRequestAfterTheFirstRefusalIsAlsoRefused() throws Exception {
        setEnabled("j.smith@abc.edu", false);

        for (int i = 0; i < 2; i++) {
            this.mockMvc.perform(get(this.baseUrl + "/students/" + this.studentJohnId).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void testTokenIsAcceptedAgainOnceTheAccountIsReenabled() throws Exception {
        setEnabled("j.smith@abc.edu", false);
        setEnabled("j.smith@abc.edu", true);

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.studentJohnId).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isOk());
    }

    @Test
    void testDeactivatedInstructorTokenIsAlsoRefused() throws Exception {
        setEnabled("b.gates@abc.edu", false);

        this.mockMvc.perform(post(this.baseUrl + "/students/search").contentType("application/json").content("{}")
                        .header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void testLoginOfADeactivatedAccountIsRefused() throws Exception {
        setEnabled("j.smith@abc.edu", false);

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false));
    }

}
