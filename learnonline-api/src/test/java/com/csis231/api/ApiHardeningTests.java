package com.csis231.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the defects fixed during the architecture review. Each test failed against the code
 * before the refactoring, so every intentional behaviour change is documented here.
 */
@SpringBootTest(classes = {ApiApplication.class, TestMailConfig.class})
@AutoConfigureMockMvc
class ApiHardeningTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void invalidResetCodeIsUnauthorizedRatherThanServerError() throws Exception {
        String name = register();
        send(post("/api/auth/password/forgot"), null, Map.of("email", email(name))).andExpect(status().isOk());

        send(post("/api/auth/password/reset"), null,
                Map.of("email", email(name), "code", wrongCode(latestOtp(name, "PASSWORD_RESET")),
                        "newPassword", "brand-new-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or code"));
    }

    @Test
    void passwordFlowDoesNotRevealWhetherAnAccountExists() throws Exception {
        String name = register();

        String known = message(send(post("/api/auth/password/forgot"), null, Map.of("email", email(name))));
        String unknown = message(send(post("/api/auth/password/forgot"), null, Map.of("email", "ghost@example.com")));
        assertThat(known).isEqualTo(unknown);

        send(post("/api/auth/password/reset"), null,
                Map.of("email", "ghost@example.com", "code", "123456", "newPassword", "brand-new-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or code"));
    }

    @Test
    void repeatedWrongOtpCodesLockVerificationUntilANewCodeIsRequested() throws Exception {
        String name = register();
        login(name).andExpect(status().isAccepted());
        String code = latestOtp(name, "LOGIN_2FA");

        for (int attempt = 0; attempt < 5; attempt++) {
            send(post("/api/auth/otp/verify"), null, Map.of("username", name, "code", wrongCode(code)))
                    .andExpect(status().isUnauthorized());
        }
        send(post("/api/auth/otp/verify"), null, Map.of("username", name, "code", code))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Too many invalid OTP attempts. Please wait and request a new OTP."));

        send(post("/api/auth/otp/request"), null, Map.of("username", name)).andExpect(status().isOk());
        send(post("/api/auth/otp/verify"), null, Map.of("username", name, "code", latestOtp(name, "LOGIN_2FA")))
                .andExpect(status().isOk());
    }

    @Test
    void disabledAccountsCannotLoginOrUseExistingTokens() throws Exception {
        String name = register();
        String token = token(name);
        jdbc.update("update users set is_active = false where username = ?", name);

        login(name).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/csis-users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void tokensOfDeletedUsersAreRejectedCleanly() throws Exception {
        String name = register();
        String token = token(name);
        jdbc.update("delete from otp_codes where user_id = (select id from users where username = ?)", name);
        jdbc.update("delete from users where username = ?", name);

        mvc.perform(get("/api/csis-users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/courses").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void clientErrorsAreNotReportedAsServerErrors() throws Exception {
        String token = token(register());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(get("/api/courses/not-a-number").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nestedQuestionValidationFailuresAreBadRequests() throws Exception {
        String instructor = registerAs("INSTRUCTOR");
        String token = token(instructor);
        long course = id(send(post("/api/courses"), token, Map.of("title", "Validated", "published", true))
                .andExpect(status().isCreated()));
        long quiz = id(send(post("/api/quizzes"), token, Map.of("courseId", course, "name", "Quiz"))
                .andExpect(status().isCreated()));

        send(post("/api/quizzes/" + quiz + "/questions"), token, List.of(Map.of(
                "questionText", "", "answers", List.of(Map.of("answerText", "A", "correct", true)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void answeringTheSameQuestionTwiceCannotInflateTheScore() throws Exception {
        String instructor = registerAs("INSTRUCTOR");
        String instructorToken = token(instructor);
        String studentToken = token(register());
        long course = id(send(post("/api/courses"), instructorToken, Map.of("title", "Fair", "published", true))
                .andExpect(status().isCreated()));
        long quiz = id(send(post("/api/quizzes"), instructorToken, Map.of("courseId", course, "name", "Quiz"))
                .andExpect(status().isCreated()));
        send(post("/api/quizzes/" + quiz + "/questions"), instructorToken, List.of(Map.of(
                "questionText", "Q", "answers", List.of(Map.of("answerText", "A", "correct", true)))))
                .andExpect(status().isCreated());
        send(post("/api/enrollments/enroll"), studentToken, Map.of("courseId", course)).andExpect(status().isOk());

        JsonNode question = read(send(get("/api/quizzes/" + quiz), instructorToken, null)).at("/questions/0");
        Map<String, Object> answer = Map.of("questionId", question.get("id").asLong(),
                "answerId", question.at("/options/0/id").asLong());

        send(post("/api/quizzes/" + quiz + "/submit"), studentToken, Map.of("answers", List.of(answer, answer)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void creatingACategoryCannotOverwriteAnExistingOne() throws Exception {
        String token = token(register());
        String original = "Original" + SEQUENCE.incrementAndGet();
        long id = id(send(post("/api/categories"), token, Map.of("name", original)).andExpect(status().isOk()));

        Map<String, Object> smuggled = new HashMap<>();
        smuggled.put("id", id);
        smuggled.put("name", "Smuggled" + SEQUENCE.incrementAndGet());
        send(post("/api/categories"), token, smuggled).andExpect(status().isOk());

        send(get("/api/categories/" + id), token, null).andExpect(jsonPath("$.name").value(original));
    }

    @Test
    void duplicateCategoryNamesAreConflictsEvenWithSurroundingSpaces() throws Exception {
        String token = token(register());
        String name = "Spaced" + SEQUENCE.incrementAndGet();
        send(post("/api/categories"), token, Map.of("name", name)).andExpect(status().isOk());

        send(post("/api/categories"), token, Map.of("name", "  " + name + "  "))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Duplicate category"));
    }

    @Test
    void deletingDataThatIsStillReferencedIsAConflict() throws Exception {
        String instructor = registerAs("INSTRUCTOR");
        String token = token(instructor);
        long category = id(send(post("/api/categories"), token,
                Map.of("name", "Referenced" + SEQUENCE.incrementAndGet())).andExpect(status().isOk()));
        send(post("/api/courses"), token, Map.of("title", "Uses category", "categoryId", category))
                .andExpect(status().isCreated());

        send(delete("/api/categories/" + category), token, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void registrationStoresThePhoneNumber() throws Exception {
        String name = "phone" + SEQUENCE.incrementAndGet();
        Map<String, Object> body = new HashMap<>(registration(name));
        body.put("phone", "+961 1 234 567");
        send(post("/api/auth/register"), null, body).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select phone from users where username = ?", String.class, name))
                .isEqualTo("+961 1 234 567");
    }

    private ResultActions login(String name) throws Exception {
        return send(post("/api/auth/login"), null, Map.of("username", name, "password", "password1"));
    }

    private String register() throws Exception {
        String name = "hardening" + SEQUENCE.incrementAndGet();
        send(post("/api/auth/register"), null, registration(name)).andExpect(status().isOk());
        return name;
    }

    private String registerAs(String role) throws Exception {
        String name = register();
        jdbc.update("update users set role = ? where username = ?", role, name);
        return name;
    }

    private String token(String name) throws Exception {
        login(name).andExpect(status().isAccepted());
        return read(send(post("/api/auth/otp/verify"), null,
                Map.of("username", name, "code", latestOtp(name, "LOGIN_2FA")))
                .andExpect(status().isOk())).get("token").asText();
    }

    private static Map<String, Object> registration(String name) {
        return Map.of("username", name, "email", email(name), "password", "password1");
    }

    private static String email(String name) {
        return name + "@example.com";
    }

    private String latestOtp(String username, String purpose) {
        return jdbc.queryForObject("""
                select o.code from otp_codes o join users u on u.id = o.user_id
                where u.username = ? and o.purpose = ? order by o.id desc limit 1""",
                String.class, username, purpose);
    }

    private static String wrongCode(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String token, Object body) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        }
        return mvc.perform(request);
    }

    private JsonNode read(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsByteArray());
    }

    private long id(ResultActions result) throws Exception {
        return read(result).get("id").asLong();
    }

    private String message(ResultActions result) throws Exception {
        return read(result).get("message").asText();
    }
}
