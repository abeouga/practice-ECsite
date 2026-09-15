package com.example.backend;

import com.example.backend.entity.AppUser;
import com.example.backend.entity.CartItem;
import com.example.backend.entity.Product;
import com.example.backend.repository.CartItemRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.PurchaseRepository;
import com.example.backend.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EcShopApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired PurchaseRepository purchaseRepository;

    private Long coffeeId;
    private Long teaId;
    private Long soldOutId;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        purchaseRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new AppUser("alice@example.com", passwordEncoder.encode("password"), "Alice"));
        userRepository.save(new AppUser("bob@example.com", passwordEncoder.encode("password"), "Bob"));
        coffeeId = productRepository.save(new Product("Coffee", 680L, "食品", 5, null, "Coffee description")).getProductId();
        teaId = productRepository.save(new Product("Tea", 520L, "食品", 4, null, "Tea description")).getProductId();
        soldOutId = productRepository.save(new Product("Lamp", 2800L, "生活用品", 0, null, "Lamp description")).getProductId();
    }

    @Test
    void publicProductsAndFailedLoginAreAvailable() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(false)))
                .andExpect(jsonPath("$.user").doesNotExist());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        Csrf csrf = csrf();
        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("メールアドレスまたはパスワードが正しくありません。")));
    }

    @Test
    void userCartPurchaseHistoryAndDoubleSubmitAreSeparated() throws Exception {
        Csrf csrf = csrf();

        mockMvc.perform(post("/api/cart/items")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + coffeeId + ",\"quantity\":1}"))
                .andExpect(status().isUnauthorized());

        MockHttpSession aliceSession = login("alice@example.com", csrf);
        mockMvc.perform(get("/api/products").param("genre", "食品"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/products/" + coffeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Coffee")));

        mockMvc.perform(post("/api/cart/items")
                        .session(aliceSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + soldOutId + ",\"quantity\":1}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/cart/items")
                        .session(aliceSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + coffeeId + ",\"quantity\":2}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/cart/items")
                        .session(aliceSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + teaId + ",\"quantity\":1}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalAmount", is(1880)));
        long cartCountBeforeConfirm = cartItemRepository.count();
        mockMvc.perform(get("/api/cart").session(aliceSession)).andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertEquals(cartCountBeforeConfirm, cartItemRepository.count());

        mockMvc.perform(post("/api/purchases")
                        .session(aliceSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].price").isNumber());
        mockMvc.perform(get("/api/cart").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.totalAmount", is(0)));
        mockMvc.perform(get("/api/purchases").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(post("/api/purchases")
                        .session(aliceSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isConflict());

        MockHttpSession bobSession = login("bob@example.com", csrf);
        mockMvc.perform(get("/api/cart").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(get("/api/purchases").session(bobSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/auth/logout")
                        .session(aliceSession)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated", is(false)));
    }

    @Test
    void csrfAndQuantityValidationAreEnforced() throws Exception {
        Csrf csrf = csrf();
        MockHttpSession session = login("alice@example.com", csrf);
        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + coffeeId + ",\"quantity\":0}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/cart/items")
                        .session(session)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + coffeeId + ",\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void failedPurchaseKeepsCartAndCreatesNoHistory() throws Exception {
        AppUser alice = userRepository.findByEmailIgnoreCase("alice@example.com").orElseThrow();
        Product overflowProduct = productRepository.save(
                new Product("Too Expensive", Long.MAX_VALUE, "検証", 1, null, "overflow test"));
        cartItemRepository.saveAndFlush(new CartItem(alice, overflowProduct, 2));

        Csrf csrf = csrf();
        MockHttpSession session = login("alice@example.com", csrf);
        mockMvc.perform(post("/api/purchases")
                        .session(session)
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(1, cartItemRepository.count());
        org.junit.jupiter.api.Assertions.assertEquals(0, purchaseRepository.count());
    }

    private MockHttpSession login(String email, Csrf csrf) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Csrf csrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String token = body.replaceAll(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
        return new Csrf(token, result.getResponse().getCookie("XSRF-TOKEN"));
    }

    private record Csrf(String token, Cookie cookie) {
    }
}
