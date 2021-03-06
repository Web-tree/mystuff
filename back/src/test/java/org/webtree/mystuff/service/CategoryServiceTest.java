package org.webtree.mystuff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.neo4j.ogm.exception.CypherException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.webtree.mystuff.AbstractSpringTest;
import org.webtree.mystuff.boot.App;
import org.webtree.mystuff.model.domain.Category;
import org.webtree.mystuff.model.domain.User;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@Ignore("#30 neo4j")
public class CategoryServiceTest extends AbstractSpringTest {
    private static final String USERNAME = "testUser";
    private static final String CATEGORY1 = "cat1";
    private static final String CATEGORY2 = "cat2";
    private static final String SEARCH_STRING = "cat";
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    @Rule
    public AbstractSpringTest.ClearGraphDBRule clearGraphDBRule = new AbstractSpringTest.ClearGraphDBRule();
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private UserService userService;

    @Test
    public void whenAddCategory_shouldCreateId() {
        Category category = createCategory(CATEGORY1);
        Category addedCategory = categoryService.save(category);
        assertThat(addedCategory.getId()).isNotNull();
        assertThat(addedCategory.getId()).isGreaterThan(-1);
    }

    @Test
    public void testAddAndGetCategory() {
        Category category = categoryService.save(createCategory(CATEGORY1));
        assertThat(categoryService.getById(category.getId())).isEqualTo(category);
    }

    @Test
    public void testGetCategoriesWithSearchString() {
        Category category = createAndSaveCategory(CATEGORY1);
        List<Category> categoryList = categoryService.getCategoriesBySearchString(SEARCH_STRING);
        assertThat(categoryList).isNotNull();
        assertThat(categoryList.get(0).getName()).isEqualTo(category.getName());
    }

    @Test
    public void whenSaveCategoryWhichAlreadyExist_shouldThrowException() {
        categoryService.save(createCategory(CATEGORY1));
        User newUser = userService.add(User.Builder.create().withUsername("newUsername").build());

        exception.expect(CypherException.class);
        exception.expectMessage("already exists with label");
        categoryService.save(Category.Builder.create().withName(CATEGORY1).withCreator(newUser).build());
    }

    //Test for @IndexCreator Unique Constraint
    @Test
    public void shouldThrowExceptionWhenTryToSaveAlreadyExistedCategory() {
        assertThat(categoryService.testSave(createCategory(CATEGORY1))).isNotNull();
        exception.expect(CypherException.class);
        exception.expectMessage(containsString(String.format("already exists with label Category and property \"name\"=[%s]", CATEGORY1)));
        categoryService.testSave(createCategory(CATEGORY1));
    }

    private Category createAndSaveCategory(String categoryName) {
        Category category = createCategory(categoryName);
        categoryService.save(category);
        return category;
    }

    private User addUser() {
        return userService.add(User.Builder.create().withUsername(USERNAME).build());
    }

    private Category createCategory(String name) {
        return Category.Builder.create().withCreator(addUser()).withName(name).build();
    }
}
