package com.tableit.userservice.controller;

import com.tableit.userservice.TestcontainersConfiguration;
import com.tableit.userservice.repository.CityRepository;
import com.tableit.userservice.security.TestJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// End-to-end through the real filter chain, controllers, service and database. Every test acts as a fresh user.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProfileApiTest {

    @DynamicPropertySource
    static void jwtKey(DynamicPropertyRegistry registry) {
        TestJwt.registerAccessPublicKey(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CityRepository cityRepository;

    private long bratislavaId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        bratislavaId = cityRepository.findAll().stream()
                .filter(city -> city.getName().equals("Bratislava"))
                .findFirst()
                .orElseThrow()
                .getId();
        userId = UUID.randomUUID();
    }

    // --- authentication ------------------------------------------------------------------------

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(json(post("/api/users/me/profile"), profileJson("Ada"))).andExpect(status().isUnauthorized());
        mockMvc.perform(json(put("/api/users/me/interests"), interestsJson("hiking", "books", "coffee")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/interests")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/cities")).andExpect(status().isUnauthorized());
    }

    // --- profile -------------------------------------------------------------------------------

    @Test
    void createsAndReadsOwnProfile() throws Exception {
        createProfile(userId, profileJson("Ada"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/users/me"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.city.name").value("Bratislava"))
                .andExpect(jsonPath("$.interests", hasSize(0)));

        mockMvc.perform(as(userId, get("/api/users/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.dateOfBirth").value("1995-05-17"))
                .andExpect(jsonPath("$.city.countryCode").value("SK"));
    }

    @Test
    void takesUserIdFromTokenNotFromRequestBody() throws Exception {
        UUID otherUser = UUID.randomUUID();
        String bodyClaimingOtherUser = """
                {"userId": "%s", "firstName": "Ada", "dateOfBirth": "1995-05-17", "cityId": %d}
                """.formatted(otherUser, bratislavaId);

        createProfile(userId, bodyClaimingOtherUser)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()));

        mockMvc.perform(as(otherUser, get("/api/users/me"))).andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateProfileWithConflict() throws Exception {
        createProfile(userId, profileJson("Ada")).andExpect(status().isCreated());

        createProfile(userId, profileJson("Ada"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updatesOwnProfile() throws Exception {
        createProfile(userId, profileJson("Ada")).andExpect(status().isCreated());
        String update = """
                {"firstName": "Grace", "lastName": "  ", "dateOfBirth": "1990-12-09", "cityId": %d,
                 "avatarUrl": "https://example.com/grace.png", "bio": "  Loves chess  "}
                """.formatted(bratislavaId);

        mockMvc.perform(json(as(userId, put("/api/users/me/profile")), update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Grace"))
                .andExpect(jsonPath("$.lastName").value(nullValue()))
                .andExpect(jsonPath("$.bio").value("Loves chess"));

        mockMvc.perform(as(userId, get("/api/users/me")))
                .andExpect(jsonPath("$.firstName").value("Grace"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-12-09"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/grace.png"));
    }

    @Test
    void rejectsInvalidProfileInput() throws Exception {
        createProfile(userId, profileJson("   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("firstName")));

        String underage = """
                {"firstName": "Ada", "dateOfBirth": "%s", "cityId": %d}
                """.formatted(LocalDate.now().minusYears(17), bratislavaId);
        createProfile(userId, underage)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("18")));
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        createProfile(userId, "{\"firstName\": \"Ada\",")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void returnsNotFoundForMissingProfile() throws Exception {
        mockMvc.perform(as(userId, get("/api/users/me"))).andExpect(status().isNotFound());
        mockMvc.perform(json(as(userId, put("/api/users/me/profile")), profileJson("Ada")))
                .andExpect(status().isNotFound());
        mockMvc.perform(json(as(userId, put("/api/users/me/interests")), interestsJson("hiking", "books", "coffee")))
                .andExpect(status().isNotFound());
    }

    // --- interests -----------------------------------------------------------------------------

    @Test
    void acceptsThreeValidInterests() throws Exception {
        createProfile(userId, profileJson("Ada")).andExpect(status().isCreated());

        replaceInterests(interestsJson("hiking", "books", "coffee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interests[*].code", containsInAnyOrder("hiking", "books", "coffee")));
    }

    @Test
    void replacesTheCompleteInterestSet() throws Exception {
        createProfile(userId, profileJson("Ada")).andExpect(status().isCreated());
        replaceInterests(interestsJson("hiking", "books", "coffee")).andExpect(status().isOk());

        replaceInterests(interestsJson("music", "art", "travel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interests[*].code", containsInAnyOrder("music", "art", "travel")));

        mockMvc.perform(as(userId, get("/api/users/me")))
                .andExpect(jsonPath("$.interests[*].code", containsInAnyOrder("music", "art", "travel")));
    }

    @Test
    void rejectsTwoInterests() throws Exception {
        createProfile(userId, profileJson("Ada")).andExpect(status().isCreated());

        replaceInterests(interestsJson("hiking", "books"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("codes")));
    }

    @Test
    void rejectsAndListsUnknownInterestCodes() throws Exception {
        createProfile(userId, profileJson("Ada")).andExpect(status().isCreated());

        replaceInterests(interestsJson("hiking", "skydiving", "chess"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown interest codes: chess, skydiving"));
    }

    // --- reference data ------------------------------------------------------------------------

    @Test
    void listsInterests() throws Exception {
        mockMvc.perform(as(userId, get("/api/users/interests")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(12))))
                .andExpect(jsonPath("$[?(@.code == 'hiking')].name", hasItem("Hiking")))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].code").isString())
                .andExpect(jsonPath("$[0].name").isString());
    }

    @Test
    void listsCities() throws Exception {
        mockMvc.perform(as(userId, get("/api/users/cities")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Bratislava')].countryCode", hasItem("SK")))
                .andExpect(jsonPath("$[?(@.name == 'Bratislava')].id", hasItem((int) bratislavaId)));
    }

    // --- helpers -------------------------------------------------------------------------------

    private ResultActions createProfile(UUID user, String body) throws Exception {
        return mockMvc.perform(json(as(user, post("/api/users/me/profile")), body));
    }

    private ResultActions replaceInterests(String body) throws Exception {
        return mockMvc.perform(json(as(userId, put("/api/users/me/interests")), body));
    }

    private static MockHttpServletRequestBuilder as(UUID user, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwt.accessToken().subject(user.toString()).sign());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String profileJson(String firstName) {
        return """
                {"firstName": "%s", "lastName": "Lovelace", "dateOfBirth": "1995-05-17", "cityId": %d}
                """.formatted(firstName, bratislavaId);
    }

    private static String interestsJson(String... codes) {
        return "{\"codes\": [" + String.join(", ", java.util.Arrays.stream(codes).map(c -> "\"" + c + "\"").toList()) + "]}";
    }
}
