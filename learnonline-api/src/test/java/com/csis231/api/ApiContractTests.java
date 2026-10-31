package com.csis231.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Black-box contract of the HTTP API: routes, status codes, JSON shapes and error payloads,
 * exercised through the real security filter chain. Written against the pre-refactoring code,
 * so it only depends on HTTP and on the database schema, never on internal classes.
 */
@SpringBootTest(classes = {ApiApplication.class, TestMailConfig.class})
@AutoConfigureMockMvc
class ApiContractTests {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;
    @Value("${jwt.secret}")
    private String jwtSecret;

    private record Account(long id, String username, String email, String password, String token) {
    }

    // ----- authentication -----------------------------------------------------------------

    @Test
    void registerThenLoginReturnsOtpChallengeWithoutToken() throws Exception {
        String name = unique("alice");
        send(post("/api/auth/register"), null, registration(name)).andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registered"));

        send(post("/api/auth/login"), null, Map.of("username", name, "password", "password1"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.otpRequired").value(true))
                .andExpect(jsonPath("$.purpose").value("LOGIN_2FA"))
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void registerRejectsDuplicatesAndInvalidPayloads() throws Exception {
        String name = unique("dup");
        send(post("/api/auth/register"), null, registration(name)).andExpect(status().isOk());

        send(post("/api/auth/register"), null, registration(name))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/register"));

        send(post("/api/auth/register"), null, Map.of("username", "x", "email", "bad", "password", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void selfRegistrationCannotChooseARole() throws Exception {
        String name = unique("role");
        Map<String, Object> body = new java.util.HashMap<>(registration(name));
        body.put("role", "ADMIN");
        send(post("/api/auth/register"), null, body).andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select role from users where username = ?", String.class, name))
                .isEqualTo("STUDENT");
    }

    @Test
    void loginWithBadCredentialsIsUnauthorized() throws Exception {
        Account user = account("STUDENT");
        send(post("/api/auth/login"), null, Map.of("username", user.username(), "password", "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        send(post("/api/auth/login"), null, Map.of("username", "nobody-here", "password", "wrong-password"))
                .andExpect(status().isUnauthorized());
        send(post("/api/auth/login"), null, Map.of("username", "", "password", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void otpVerifyIssuesTokenOnceAndRejectsWrongCodes() throws Exception {
        String name = unique("otp");
        send(post("/api/auth/register"), null, registration(name)).andExpect(status().isOk());
        send(post("/api/auth/login"), null, Map.of("username", name, "password", "password1"))
                .andExpect(status().isAccepted());
        String code = latestOtp(name, "LOGIN_2FA");

        send(post("/api/auth/otp/verify"), null, Map.of("username", name, "code", wrongCode(code)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or code"));

        send(post("/api/auth/otp/verify"), null, Map.of("username", name + "@example.com", "code", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.email").value(name + "@example.com"))
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.lastName").value("Last"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        send(post("/api/auth/otp/verify"), null, Map.of("username", name, "code", code))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void otpVerifyForUnknownUserIsUnauthorizedAndCodeLengthIsValidated() throws Exception {
        send(post("/api/auth/otp/verify"), null, Map.of("username", "ghost", "code", "123456"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or code"));
        send(post("/api/auth/otp/verify"), null, Map.of("username", "ghost", "code", "12"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otpResendReplacesThePreviousCodeAndHidesUnknownUsers() throws Exception {
        String name = unique("resend");
        send(post("/api/auth/register"), null, registration(name)).andExpect(status().isOk());
        send(post("/api/auth/login"), null, Map.of("username", name, "password", "password1"))
                .andExpect(status().isAccepted());
        String first = latestOtp(name, "LOGIN_2FA");

        send(post("/api/auth/otp/request"), null, Map.of("username", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP resent"));
        assertThat(jdbc.queryForObject("""
                select count(*) from otp_codes o join users u on u.id = o.user_id
                where u.username = ? and o.consumed_at is null""", Integer.class, name)).isEqualTo(1);
        assertThat(latestOtp(name, "LOGIN_2FA")).hasSize(first.length());

        send(post("/api/auth/otp/request"), null, Map.of("username", "ghost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP resent"));
    }

    @Test
    void passwordResetChangesThePassword() throws Exception {
        Account user = account("STUDENT");
        send(post("/api/auth/password/forgot"), null, Map.of("email", user.email())).andExpect(status().isOk());
        send(post("/api/auth/password/forgot"), null, Map.of("email", "nobody@example.com"))
                .andExpect(status().isOk());

        String code = latestOtp(user.username(), "PASSWORD_RESET");
        send(post("/api/auth/password/reset"), null,
                Map.of("email", user.email(), "code", code, "newPassword", "brand-new-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated"));

        send(post("/api/auth/login"), null, Map.of("username", user.username(), "password", "brand-new-password"))
                .andExpect(status().isAccepted());
        send(post("/api/auth/login"), null, Map.of("username", user.username(), "password", user.password()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoutesRejectMissingInvalidAndExpiredTokens() throws Exception {
        mvc.perform(get("/api/csis-users/me")).andExpect(status().isForbidden());
        mvc.perform(get("/api/csis-users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());

        Account user = account("STUDENT");
        String expired = Jwts.builder()
                .setSubject(user.username())
                .setIssuedAt(new Date(System.currentTimeMillis() - 60_000))
                .setExpiration(new Date(System.currentTimeMillis() - 30_000))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
        mvc.perform(get("/api/csis-users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/courses").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isOk());
    }

    @Test
    void meReturnsProfileWithComputedFullName() throws Exception {
        Account user = account("STUDENT");
        send(get("/api/csis-users/me"), user, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.id()))
                .andExpect(jsonPath("$.username").value(user.username()))
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.lastName").value("Last"))
                .andExpect(jsonPath("$.fullName").value("First Last"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    // ----- user administration --------------------------------------------------------------

    @Test
    void adminManagesUsers() throws Exception {
        Account admin = account("ADMIN");
        Account student = account("STUDENT");
        String name = unique("managed");

        send(get("/api/csis-users"), student, null).andExpect(status().isForbidden());

        send(get("/api/csis-users?page=0&size=2"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").isBoolean());
        send(get("/api/csis-users?size=0"), admin, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        ResultActions created = send(post("/api/csis-users"), admin, Map.of(
                "username", name, "email", name + "@example.com", "password", "password1",
                "firstName", "  Fn  ", "phone", "  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.firstName").value("Fn"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.twoFactorEnabled").value(false))
                .andExpect(jsonPath("$.password").doesNotExist());
        long id = id(created);

        send(post("/api/csis-users"), admin, Map.of(
                "username", name, "email", "other-" + name + "@example.com", "password", "password1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username already in use"));

        send(get("/api/csis-users/" + id), admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        send(put("/api/csis-users/" + id), admin, Map.of("lastName", "Updated", "phone", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Updated"))
                .andExpect(jsonPath("$.firstName").value("Fn"))
                .andExpect(jsonPath("$.phone").value("123"));
        send(put("/api/csis-users/" + id), admin, Map.of("email", student.email()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already in use"));

        send(delete("/api/csis-users/" + id), admin, null).andExpect(status().isNoContent());
        send(get("/api/csis-users/" + id), admin, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("User not found: " + id));
        send(put("/api/csis-users/" + id), admin, Map.of("lastName", "x")).andExpect(status().isNotFound());
        send(delete("/api/csis-users/" + id), admin, null).andExpect(status().isNotFound());
    }

    // ----- categories -----------------------------------------------------------------------

    @Test
    void categoriesCrud() throws Exception {
        Account user = account("STUDENT");
        String name = unique("Cat");

        mvc.perform(get("/api/categories")).andExpect(status().isForbidden());

        ResultActions created = send(post("/api/categories"), user, Map.of("name", "  " + name + "  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
        long id = id(created);

        send(get("/api/categories?size=1000"), user, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem(name)))
                .andExpect(jsonPath("$.totalElements").isNumber());
        send(get("/api/categories?size=0"), user, null).andExpect(status().isBadRequest());
        send(get("/api/categories/" + id), user, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));

        send(post("/api/categories"), user, Map.of("name", name))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Duplicate category"));
        send(post("/api/categories"), user, Map.of("name", "   ")).andExpect(status().isBadRequest());

        send(put("/api/categories/" + id), user, Map.of("name", name + "x")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name + "x"));

        send(delete("/api/categories/" + id), user, null).andExpect(status().isOk());
        send(get("/api/categories/" + id), user, null).andExpect(status().isNotFound());
        send(delete("/api/categories/" + id), user, null).andExpect(status().isNotFound());
    }

    // ----- courses --------------------------------------------------------------------------

    @Test
    void courseCatalogIsPublicAndOnlyShowsPublishedCourses() throws Exception {
        Account instructor = account("INSTRUCTOR");
        long category = category(instructor);
        String marker = unique("Needle");
        long published = course(instructor, marker + " published", category, true);
        course(instructor, marker + " draft", category, false);

        mvc.perform(get("/api/courses").param("search", marker.toLowerCase()).param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(published))
                .andExpect(jsonPath("$.content[0].title").value(marker + " published"))
                .andExpect(jsonPath("$.content[0].instructorUserId").value(instructor.id()))
                .andExpect(jsonPath("$.content[0].instructorName").value(instructor.username()))
                .andExpect(jsonPath("$.content[0].categoryId").value(category))
                .andExpect(jsonPath("$.content[0].published").value(true))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists());

        mvc.perform(get("/api/courses").param("categoryId", String.valueOf(category)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        mvc.perform(get("/api/courses").param("page", "-1").param("size", "-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(1));
    }

    @Test
    void courseManagementEnforcesRolesAndOwnership() throws Exception {
        Account owner = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account admin = account("ADMIN");
        Account student = account("STUDENT");

        send(post("/api/courses"), student, Map.of("title", "Nope"))
                .andExpect(status().isForbidden());
        send(post("/api/courses"), owner, Map.of("title", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        send(post("/api/courses"), owner, Map.of("title", "Bad category", "categoryId", 999_999))
                .andExpect(status().isNotFound());

        long id = course(owner, "Original", null, false);

        send(put("/api/courses/" + id), other, Map.of("title", "Hijacked"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("You cannot modify courses you do not own"));

        String before = field(send(get("/api/courses/" + id), admin, null), "updatedAt");
        Thread.sleep(5);
        String after = field(send(put("/api/courses/" + id), owner,
                Map.of("title", "Renamed", "published", true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.published").value(true)), "updatedAt");
        assertThat(Instant.parse(after)).isAfter(Instant.parse(before));

        send(put("/api/courses/999999"), owner, Map.of("title", "Missing")).andExpect(status().isNotFound());
        send(delete("/api/courses/" + id), other, null).andExpect(status().isForbidden());
        send(delete("/api/courses/" + id), admin, null).andExpect(status().isNoContent());
        mvc.perform(get("/api/courses/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void courseDetailDependsOnViewer() throws Exception {
        Account owner = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account student = account("STUDENT");
        long course = course(owner, "Detailed", null, true);
        material(owner, course);
        quizWithOneQuestion(owner, course);

        mvc.perform(get("/api/courses/" + course))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.materials", hasSize(0)))
                .andExpect(jsonPath("$.quizzes", hasSize(1)))
                .andExpect(jsonPath("$.quizzes[0].questionCount").value(1));

        send(get("/api/courses/" + course), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorName").value(owner.username()))
                .andExpect(jsonPath("$.materials", hasSize(1)));

        send(get("/api/courses/" + course), student, null).andExpect(status().isForbidden());
        send(get("/api/courses/" + course), other, null).andExpect(status().isForbidden());

        enroll(student, course).andExpect(status().isOk());
        send(get("/api/courses/" + course), student, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.materials", hasSize(1)));
    }

    // ----- enrollments ----------------------------------------------------------------------

    @Test
    void enrollmentRules() throws Exception {
        Account instructor = account("INSTRUCTOR");
        Account otherInstructor = account("INSTRUCTOR");
        Account admin = account("ADMIN");
        Account student = account("STUDENT");
        Account peer = account("STUDENT");
        long course = course(instructor, "Enrollable", null, true);
        long draft = course(instructor, "Draft", null, false);

        enroll(student, course)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.studentUserId").value(student.id()))
                .andExpect(jsonPath("$.studentUsername").value(student.username()))
                .andExpect(jsonPath("$.studentEmail").value(student.email()))
                .andExpect(jsonPath("$.courseId").value(course))
                .andExpect(jsonPath("$.courseTitle").value("Enrollable"))
                .andExpect(jsonPath("$.status").value("ENROLLED"))
                .andExpect(jsonPath("$.enrolledAt").exists());

        enroll(student, course).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Already enrolled in this course"));
        enroll(student, draft).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Cannot enroll in an unpublished course"));
        enroll(student, 999_999).andExpect(status().isNotFound());
        send(post("/api/enrollments/enroll"), student, Map.of()).andExpect(status().isBadRequest());

        send(post("/api/enrollments/enroll"), student, Map.of("studentUserId", peer.id(), "courseId", course))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot enroll another user"));
        send(post("/api/enrollments/enroll"), admin, Map.of("studentUserId", peer.id(), "courseId", course))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentUserId").value(peer.id()));

        send(get("/api/students/" + student.id() + "/enrollments"), student, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseTitle").value("Enrollable"));
        send(get("/api/students/" + student.id() + "/enrollments"), peer, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view enrollments for another student"));
        send(get("/api/students/" + student.id() + "/enrollments"), otherInstructor, null).andExpect(status().isOk());

        send(get("/api/courses/" + course + "/enrollments"), instructor, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        send(get("/api/courses/" + course + "/enrollments"), admin, null).andExpect(status().isOk());
        send(get("/api/courses/" + course + "/enrollments"), otherInstructor, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only view enrollments for your courses"));
        send(get("/api/courses/" + course + "/enrollments"), student, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only instructors or admins can view course enrollments"));
        mvc.perform(get("/api/courses/" + course + "/enrollments")).andExpect(status().isForbidden());
    }

    // ----- course materials -----------------------------------------------------------------

    @Test
    void materialRules() throws Exception {
        Account owner = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account student = account("STUDENT");
        long course = course(owner, "With materials", null, true);

        send(post("/api/courses/" + course + "/materials"), student, Map.of("title", "Nope"))
                .andExpect(status().isForbidden());
        send(post("/api/courses/" + course + "/materials"), other, Map.of("title", "Nope"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only modify materials for your own courses"));
        send(post("/api/courses/" + course + "/materials"), owner, Map.of("title", ""))
                .andExpect(status().isBadRequest());

        long material = id(send(post("/api/courses/" + course + "/materials"), owner, Map.of(
                "title", "Slides", "materialType", "PDF", "url", "http://x/y.pdf", "metadata", "{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(course))
                .andExpect(jsonPath("$.title").value("Slides"))
                .andExpect(jsonPath("$.materialType").value("PDF"))
                .andExpect(jsonPath("$.url").value("http://x/y.pdf"))
                .andExpect(jsonPath("$.metadata").value("{}"))
                .andExpect(jsonPath("$.createdAt").exists()));

        mvc.perform(get("/api/courses/" + course + "/materials"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Authentication required"));
        send(get("/api/courses/" + course + "/materials"), student, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Enroll in the course to view materials"));
        send(get("/api/courses/" + course + "/materials"), other, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view materials of courses you do not own"));
        enroll(student, course).andExpect(status().isOk());
        send(get("/api/courses/" + course + "/materials"), student, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        send(delete("/api/materials/" + material), student, null).andExpect(status().isForbidden());
        send(delete("/api/materials/" + material), owner, null).andExpect(status().isNoContent());
        send(delete("/api/materials/" + material), owner, null).andExpect(status().isNotFound());
    }

    // ----- quizzes --------------------------------------------------------------------------

    @Test
    void quizAuthoringRules() throws Exception {
        Account owner = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account student = account("STUDENT");
        long course = course(owner, "Quizzed", null, true);

        send(post("/api/quizzes"), student, Map.of("courseId", course, "name", "Quiz"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only instructors or admins can manage quizzes"));
        send(post("/api/quizzes"), other, Map.of("courseId", course, "name", "Quiz"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only manage quizzes for your own courses"));
        send(post("/api/quizzes"), owner, Map.of("courseId", 999_999, "name", "Quiz"))
                .andExpect(status().isNotFound());

        long quiz = id(send(post("/api/quizzes"), owner,
                Map.of("courseId", course, "name", "Quiz 1", "description", "Basics"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(course))
                .andExpect(jsonPath("$.name").value("Quiz 1"))
                .andExpect(jsonPath("$.description").value("Basics"))
                .andExpect(jsonPath("$.questionCount").value(0))
                .andExpect(jsonPath("$.createdAt").exists()));

        send(post("/api/quizzes/" + quiz + "/questions"), owner, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Questions are required"));
        send(post("/api/quizzes/" + quiz + "/questions"), owner, List.of(Map.of(
                "questionText", "Q", "answers", List.of(Map.of("answerText", "A", "correct", false)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Each question must have at least one correct answer"));
        send(post("/api/quizzes/" + quiz + "/questions"), other, questionPayload())
                .andExpect(status().isForbidden());
        send(post("/api/quizzes/" + quiz + "/questions"), owner, questionPayload())
                .andExpect(status().isCreated());

        send(get("/api/quizzes/" + quiz), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(quiz))
                .andExpect(jsonPath("$.courseId").value(course))
                .andExpect(jsonPath("$.name").value("Quiz 1"))
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].questionText").value("What is 2+2?"))
                .andExpect(jsonPath("$.questions[0].options", hasSize(2)))
                .andExpect(jsonPath("$.questions[0].options[0].id").isNumber())
                .andExpect(jsonPath("$.questions[0].options[0].answerText").isNotEmpty())
                .andExpect(jsonPath("$.questions[0].options[*].correct").isEmpty());

        send(get("/api/quizzes/" + quiz), student, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Enroll in the course to access this quiz"));
        send(get("/api/quizzes/999999"), owner, null).andExpect(status().isNotFound());

        send(delete("/api/quizzes/" + quiz), other, null).andExpect(status().isForbidden());
        send(delete("/api/quizzes/" + quiz), owner, null).andExpect(status().isNoContent());
        send(get("/api/quizzes/" + quiz), owner, null).andExpect(status().isNotFound());
    }

    @Test
    void quizSubmissionGradesAndStoresResults() throws Exception {
        Account owner = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account student = account("STUDENT");
        Account peer = account("STUDENT");
        long course = course(owner, "Graded", null, true);
        long quiz = quizWithOneQuestion(owner, course);
        JsonNode question = read(send(get("/api/quizzes/" + quiz), owner, null)).at("/questions/0");
        long questionId = question.get("id").asLong();
        long correct = optionId(question, "4");
        long wrong = optionId(question, "3");

        Map<String, Object> rightAnswer = Map.of("answers",
                List.of(Map.of("questionId", questionId, "answerId", correct)));

        send(post("/api/quizzes/" + quiz + "/submit"), peer, rightAnswer)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Enroll in the course before taking the quiz"));
        send(post("/api/quizzes/" + quiz + "/submit"), other, rightAnswer).andExpect(status().isForbidden());
        enroll(student, course).andExpect(status().isOk());

        send(get("/api/quizzes/" + quiz + "/my-result"), student, null)
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        send(post("/api/quizzes/" + quiz + "/submit"), student, Map.of("answers", List.of()))
                .andExpect(status().isBadRequest());
        send(post("/api/quizzes/" + quiz + "/submit"), student, Map.of("answers",
                List.of(Map.of("questionId", 999_999, "answerId", correct))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Answer references invalid question: 999999"));
        send(post("/api/quizzes/" + quiz + "/submit"), student, Map.of("answers",
                List.of(Map.of("questionId", questionId, "answerId", 999_999))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid answer option for question: " + questionId));

        send(post("/api/quizzes/" + quiz + "/submit"), student, Map.of("answers",
                List.of(Map.of("questionId", questionId, "answerId", wrong))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.percentage").value(0.0));

        send(post("/api/quizzes/" + quiz + "/submit"), student, rightAnswer)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(quiz))
                .andExpect(jsonPath("$.studentUserId").value(student.id()))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(1))
                .andExpect(jsonPath("$.percentage").value(100.0))
                .andExpect(jsonPath("$.completedAt").exists());

        send(post("/api/quizzes/" + quiz + "/submit"), owner, rightAnswer).andExpect(status().isOk());

        send(get("/api/quizzes/" + quiz + "/my-result"), student, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(quiz))
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.totalQuestions").value(1));

        send(get("/api/quizzes/" + quiz + "/results"), owner, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].studentUserId").isNumber());
        send(get("/api/quizzes/" + quiz + "/results"), student, null).andExpect(status().isForbidden());

        send(delete("/api/quizzes/" + quiz), owner, null).andExpect(status().isNoContent());
    }

    // ----- dashboards and statistics --------------------------------------------------------

    @Test
    void studentDashboard() throws Exception {
        Account instructor = account("INSTRUCTOR");
        Account student = account("STUDENT");
        long course = course(instructor, "Dashboarded", null, true);
        long quiz = quizWithOneQuestion(instructor, course);
        enroll(student, course).andExpect(status().isOk());
        JsonNode question = read(send(get("/api/quizzes/" + quiz), instructor, null)).at("/questions/0");
        send(post("/api/quizzes/" + quiz + "/submit"), student, Map.of("answers", List.of(Map.of(
                "questionId", question.get("id").asLong(), "answerId", optionId(question, "4")))))
                .andExpect(status().isOk());

        send(get("/api/student/dashboard"), student, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentUserId").value(student.id()))
                .andExpect(jsonPath("$.enrolledCourseCount").value(1))
                .andExpect(jsonPath("$.enrolledCourses[0].id").value(course))
                .andExpect(jsonPath("$.enrolledCourses[0].instructorName").value(instructor.username()))
                .andExpect(jsonPath("$.recentQuizResults", hasSize(1)))
                .andExpect(jsonPath("$.recentQuizResults[0].score").value(1))
                .andExpect(jsonPath("$.upcomingQuizzes", hasSize(1)))
                .andExpect(jsonPath("$.upcomingQuizzes[0].questionCount").value(1));

        send(get("/api/student/dashboard"), instructor, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only students can access the student dashboard"));
        mvc.perform(get("/api/student/dashboard")).andExpect(status().isForbidden());
    }

    @Test
    void instructorDashboardAndCourseList() throws Exception {
        Account instructor = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account admin = account("ADMIN");
        Account student = account("STUDENT");
        long course = course(instructor, "Taught", null, true);
        quizWithOneQuestion(instructor, course);
        enroll(student, course).andExpect(status().isOk());

        send(get("/api/instructor/dashboard"), instructor, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instructorUserId").value(instructor.id()))
                .andExpect(jsonPath("$.courseCount").value(1))
                .andExpect(jsonPath("$.totalEnrollments").value(1))
                .andExpect(jsonPath("$.courses[0].title").value("Taught"))
                .andExpect(jsonPath("$.courseStats[0].courseId").value(course))
                .andExpect(jsonPath("$.courseStats[0].courseTitle").value("Taught"))
                .andExpect(jsonPath("$.courseStats[0].enrollmentCount").value(1))
                .andExpect(jsonPath("$.courseStats[0].quizCount").value(1));
        send(get("/api/instructor/dashboard"), student, null).andExpect(status().isForbidden());

        send(get("/api/instructors/" + instructor.id() + "/courses"), instructor, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].instructorName").value(instructor.username()));
        send(get("/api/instructors/" + instructor.id() + "/courses"), admin, null).andExpect(status().isOk());
        send(get("/api/instructors/" + instructor.id() + "/courses"), other, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot view courses for this instructor"));
    }

    @Test
    void quizAverageStatistics() throws Exception {
        Account instructor = account("INSTRUCTOR");
        Account other = account("INSTRUCTOR");
        Account student = account("STUDENT");
        long course = course(instructor, "Measured", null, true);
        long attempted = quizWithOneQuestion(instructor, course);
        quizWithOneQuestion(instructor, course);
        enroll(student, course).andExpect(status().isOk());
        JsonNode question = read(send(get("/api/quizzes/" + attempted), instructor, null)).at("/questions/0");
        send(post("/api/quizzes/" + attempted + "/submit"), student, Map.of("answers", List.of(Map.of(
                "questionId", question.get("id").asLong(), "answerId", optionId(question, "4")))))
                .andExpect(status().isOk());
        send(post("/api/quizzes/" + attempted + "/submit"), student, Map.of("answers", List.of(Map.of(
                "questionId", question.get("id").asLong(), "answerId", optionId(question, "3")))))
                .andExpect(status().isOk());

        send(get("/api/statistics/courses/" + course + "/quiz-averages"), instructor, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].label").value("Quiz"))
                .andExpect(jsonPath("$[*].value", hasItem(50.0)))
                .andExpect(jsonPath("$[*].value", hasItem(0.0)));
        send(get("/api/statistics/courses/" + course + "/quiz-averages"), other, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not authorized to view course statistics"));
        send(get("/api/statistics/courses/" + course + "/quiz-averages"), student, null)
                .andExpect(status().isForbidden());
        send(get("/api/statistics/courses/999999/quiz-averages"), instructor, null)
                .andExpect(status().isNotFound());
    }

    @Test
    void errorPayloadsNeverExposeInternals() throws Exception {
        Account student = account("STUDENT");
        send(get("/api/courses/999999"), student, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", startsWith("Course not found")))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))))
                .andExpect(jsonPath("$.path").value("/api/courses/999999"));
        send(get("/api/csis-users"), student, null)
                .andExpect(status().isForbidden())
                .andExpect(content().string(everyItemIsNotAStackTrace()));
    }

    private static org.hamcrest.Matcher<String> everyItemIsNotAStackTrace() {
        return not(containsString("at com.csis231"));
    }

    // ----- helpers --------------------------------------------------------------------------

    private static String unique(String prefix) {
        return prefix + SEQUENCE.incrementAndGet() + "x" + Long.toString(System.nanoTime() % 100_000, 36);
    }

    private static Map<String, Object> registration(String username) {
        return Map.of("username", username, "email", username + "@example.com", "password", "password1",
                "firstName", "First", "lastName", "Last");
    }

    private static List<Map<String, Object>> questionPayload() {
        return List.of(Map.of("questionText", "What is 2+2?", "answers", List.of(
                Map.of("answerText", "3", "correct", false),
                Map.of("answerText", "4", "correct", true))));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, Account as, Object body) throws Exception {
        if (as != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + as.token());
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

    private String field(ResultActions result, String name) throws Exception {
        return read(result).get(name).asText();
    }

    private static long optionId(JsonNode question, String answerText) {
        for (JsonNode option : question.get("options")) {
            if (answerText.equals(option.get("answerText").asText())) {
                return option.get("id").asLong();
            }
        }
        throw new AssertionError("No option " + answerText);
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

    /** Registers a user, assigns the role directly in the database and logs in through the OTP flow. */
    private Account account(String role) throws Exception {
        String name = unique(role.toLowerCase());
        send(post("/api/auth/register"), null, registration(name)).andExpect(status().isOk());
        jdbc.update("update users set role = ? where username = ?", role, name);
        send(post("/api/auth/login"), null, Map.of("username", name, "password", "password1"))
                .andExpect(status().isAccepted());
        JsonNode auth = read(send(post("/api/auth/otp/verify"), null,
                Map.of("username", name, "code", latestOtp(name, "LOGIN_2FA"))).andExpect(status().isOk()));
        return new Account(auth.get("id").asLong(), name, name + "@example.com", "password1",
                auth.get("token").asText());
    }

    private long category(Account as) throws Exception {
        return id(send(post("/api/categories"), as, Map.of("name", unique("Category"))).andExpect(status().isOk()));
    }

    private long course(Account instructor, String title, Long categoryId, boolean published) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>(Map.of("title", title, "published", published));
        if (categoryId != null) {
            body.put("categoryId", categoryId);
        }
        return id(send(post("/api/courses"), instructor, body).andExpect(status().isCreated()));
    }

    private long material(Account instructor, long course) throws Exception {
        return id(send(post("/api/courses/" + course + "/materials"), instructor, Map.of("title", "Notes"))
                .andExpect(status().isCreated()));
    }

    private long quizWithOneQuestion(Account instructor, long course) throws Exception {
        long quiz = id(send(post("/api/quizzes"), instructor, Map.of("courseId", course, "name", "Quiz"))
                .andExpect(status().isCreated()));
        send(post("/api/quizzes/" + quiz + "/questions"), instructor, questionPayload())
                .andExpect(status().isCreated());
        return quiz;
    }

    private ResultActions enroll(Account student, long course) throws Exception {
        return send(post("/api/enrollments/enroll"), student, Map.of("courseId", course));
    }
}
