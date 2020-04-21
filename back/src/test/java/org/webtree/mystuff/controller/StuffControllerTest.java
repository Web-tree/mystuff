package org.webtree.mystuff.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MvcResult;
import org.webtree.mystuff.model.domain.AuthDetails;
import org.webtree.mystuff.model.domain.Category;
import org.webtree.mystuff.model.domain.Stuff;
import org.webtree.mystuff.model.domain.User;
import org.webtree.mystuff.model.rest.CreateStuff;
import org.webtree.mystuff.security.WithMockCustomUser;
import org.webtree.mystuff.service.CategoryService;
import org.webtree.mystuff.service.StuffService;
import org.webtree.mystuff.service.UserService;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@WithMockCustomUser
public class StuffControllerTest extends AbstractControllerTest {
    private static final String NAME = "stuff name example";
    private static final String USER_1 = "user1";
    private static final String USER_2 = "user2";
    private static final String CATEGORY1 = "category1";
    private static final String CATEGORY2 = "category2";
    @Rule
    public ClearGraphDBRule clearGraphDBRule = new ClearGraphDBRule();

    @SpyBean
    public StuffService stuffService;
    @SpyBean
    public UserService userService;
    @SpyBean
    public CategoryService categoryService;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Captor
    private ArgumentCaptor<Set<Long>> categoriesCaptor;

    @Test
    public void whenAddStuff_shouldReturnNewId() throws Exception {
        Stuff stuff = Stuff.Builder.create().withName(NAME).build();
        mockMvc.perform(
            post("/rest/stuff")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CreateStuff.Builder.create().withStuff(stuff).build()))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name").value(NAME));
    }

    @Test
    public void whenGetStuff_shouldReturnItFromService() throws Exception {
        stuffService.save(Stuff.Builder.create().withId(1L).withName(NAME).build());
        mockMvc.perform(get("/rest/stuff/1").contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value(NAME));
    }

    @Test
    public void whenGetStuffList_shouldReturnListOfStuffs() throws Exception {
        stuffService.save(buildNewStuff(NAME, USER_1));
        stuffService.save(buildNewStuff(NAME + 2, USER_1));

        mockMvc.perform(get("/rest/stuff/list").contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[1].id").isNumber());
    }

    @Test
    public void whenGetStuffList_shouldReturnOnlyUsersStuff() throws Exception {
        stuffService.save(buildNewStuff(NAME, USER_1));
        stuffService.save(buildNewStuff(NAME + 2, USER_1 + 2));

        mockMvc.perform(get("/rest/stuff/list").contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id").isNumber())
            .andExpect(jsonPath("$[0].users", hasSize(1)))
            .andExpect(jsonPath("$[0].users[0].username").value(USER_1));
    }

    @Test
    @WithAnonymousUser
    public void whenAnonymousUser_shouldResponseForbidden() throws Exception {
        mockMvc.perform(get("/rest/stuff/list").contentType(APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/rest/stuff").contentType(APPLICATION_JSON))
            .andExpect(status().isUnauthorized());

        Stuff stuff = buildNewStuff(NAME, USER_1);
        mockMvc.perform(get("/rest/stuff/1", objectMapper.writeValueAsString(stuff)).contentType(APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/rest/stuff/" + stuff.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void whenDeleteStuff_shouldNotReturnItForUser() throws Exception {
        Stuff stuff = stuffService.save(buildNewStuff(NAME, USER_1));

        mockMvc.perform(delete("/rest/stuff/" + stuff.getId()))
            .andExpect(status().isOk());
        mockMvc.perform(get("/rest/stuff/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithAnonymousUser
    public void whenAddExistingStuff_shouldReturnForBothUsers() throws Exception {
        Stuff stuff = stuffService.save(buildNewStuff(NAME, USER_1));
        AuthDetails authDetails = new AuthDetails();
        authDetails.setUsername(USER_2);
        authDetails.setPassword("pass");
        User user2 = userService.add(User.Builder.create().withUsername(USER_2).withPassword("pass").build());
        MvcResult mvcResult = mockMvc.perform(
            post("/rest/token/new")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authDetails))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andReturn();
        String user2token = mvcResult.getResponse().getContentAsString();

        mockMvc.perform(post("/rest/stuff/addExisting")
            .header("Authorization", "Bearer " + user2token)
            .header("id", stuff.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.errors").doesNotExist());

        Stuff updatedStuff = stuffService.getById(stuff.getId());
        assertThat(updatedStuff.getUsers().size()).isEqualTo(2);
        assertThat(updatedStuff.getUsers()).containsExactlyInAnyOrder(user2, userService.loadUserByUsername(USER_1));
    }


    @Test
    public void whenAddStuffCategory_shouldSaveCorrect() throws Exception {
        CreateStuff createStuff = CreateStuff.Builder.create()
            .withStuff(Stuff.Builder.create().withName(NAME).build())
            .withCategories(buildNewStaffCategories(CATEGORY1, CATEGORY2).stream().map(Category::getId).collect(Collectors.toSet()))
            .build();
        MvcResult mvcResult = mockMvc.perform(
            post("/rest/stuff")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createStuff))
        )
            .andExpect(status().isOk())
            .andReturn();

        String postResponse = mvcResult.getResponse().getContentAsString();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Stuff savedStaff = objectMapper.readValue(postResponse, Stuff.class);

        Stuff stuffFromService = stuffService.getById(savedStaff.getId());
        assertThat(stuffFromService.getCategories()).hasSize(2);
        Set<String> expectedCategories = Sets.newHashSet(CATEGORY1, CATEGORY2);
        stuffFromService.getCategories().forEach(category -> assertThat(expectedCategories).contains(category.getName()));
    }

    @Test
    public void whenReadStuff_shouldReturnCorrectFromService() throws Exception {
        Stuff stuff = stuffService.save(buildNewStuffWithStaffCategory(NAME, USER_1, buildNewStaffCategories(CATEGORY1)));
        mockMvc.perform(get("/rest/stuff/" + stuff.getId()).contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories").isArray())
            .andExpect(jsonPath("$.categories").isNotEmpty())
            .andExpect(jsonPath("$.categories[0].name").value(CATEGORY1))
            .andExpect(jsonPath("$.categories[1]").doesNotExist());
    }

    @Test
    public void whenRequestCategoriesBySearchString_shouldReturnCorrect() throws Exception {
        String query = "cat";
        User user = addUser();
        Category sc1 = categoryService.save(Category.Builder.create().withName("ater").withCreator(user).build());
        Category sc2 = categoryService.save(Category.Builder.create().withName("catfg").withCreator(user).build());
        Category sc3 = categoryService.save(Category.Builder.create().withName("cat").withCreator(user).build());
        Category sc4 = categoryService.save(Category.Builder.create().withName("catui").withCreator(user).build());
        Category sc5 = categoryService.save(Category.Builder.create().withName("catgf").withCreator(user).build());

        mockMvc.perform(get("/rest/stuff/category/names?startsFrom=" + query).contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.[0].name").value(sc2.getName()))
            .andExpect(jsonPath("$.[1].name").value(sc3.getName()))
            .andExpect(jsonPath("$.[2].name").value(sc4.getName()))
            .andExpect(jsonPath("$.[3].name").value(sc5.getName()));

    }

    @Test
    public void whenAddStuffWithCategories_shouldCallCreateOnService() throws Exception {
        Stuff stuff = Stuff.Builder.create().withName(NAME).build();
        Set<Long> categories = Sets.newHashSet(1L, 2L, 3L);
        mockMvc.perform(post("/rest/stuff").contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(CreateStuff.Builder.create().withStuff(stuff).withCategories(categories).build()))
        );
        verify(stuffService).create(refEq(stuff, "id", "categories"), anyLong(), categoriesCaptor.capture());
        assertThat(categoriesCaptor.getValue()).isEqualTo(categories);
    }

    @Test
    public void whenAddExistingCategory_shouldReturnIt_andDonNotTryToCreate() throws Exception {
        Category category = categoryService.save(Category.Builder.create().withName(CATEGORY1).build());
        reset(categoryService);
        mockMvc.perform(post("/rest/stuff/category")
            .contentType(APPLICATION_JSON)
            .param("categoryName", CATEGORY1)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(category.getId()))
            .andExpect(jsonPath("$.name").value(CATEGORY1));
        verify(categoryService, never()).save(any());
    }

    @Test // bug #32
    public void whenGetStuff_shouldNotReturnUserPassword() throws Exception {
        Stuff stuff = stuffService.save(buildNewStuffWithStaffCategory(NAME, USER_1, buildNewStaffCategories(CATEGORY1)));
        mockMvc.perform(get("/rest/stuff/" + stuff.getId()).contentType(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.creator").isNotEmpty())
            .andExpect(jsonPath("$.creator.username").value(USER_1))
            .andExpect(jsonPath("$.creator.password").doesNotExist());
    }

    private Stuff buildNewStuff(String name, String username) {
        return Stuff.Builder.create().withUsers(buildNewUsers(username)).withName(name).build();
    }

    private Set<Category> buildNewStaffCategories(String... categoryNames) {
        User user = addUser();
        Set<Category> categories = new HashSet<>();
        for (String categoryName : categoryNames) {
            Category category = categoryService.save(Category.Builder.create().withName(categoryName).withCreator(user).build());
            categories.add(category);
        }
        return categories;

    }

    private User addUser() {
        return userService.add(User.Builder.create().withUsername(USER_1).build());
    }

    private Set<User> buildNewUsers(String username) {
        return Sets.newHashSet(userService.add(User.Builder.create().withUsername(username).withPassword("qweasd").build()));
    }


    private Stuff buildNewStuffWithStaffCategory(String name, String username, Set<Category> categories) {
        Set<User> users = buildNewUsers(username);
        return Stuff.Builder.create().withCreator(users.iterator().next()).withUsers(users).withName(name)
            .withCategories(categories).build();
    }


}