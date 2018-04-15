package org.webtree.mystuff.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.webtree.mystuff.domain.Category;
import org.webtree.mystuff.domain.Stuff;
import org.webtree.mystuff.domain.User;
import org.webtree.mystuff.restModel.CreateStuff;
import org.webtree.mystuff.service.CategoryService;
import org.webtree.mystuff.service.StuffService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/rest/stuff")
@CrossOrigin
public class StuffController {
    private final StuffService stuffService;
    private final CategoryService categoryService;

    @Autowired
    public StuffController(StuffService stuffService, CategoryService categoryService) {
        this.categoryService = categoryService;
        this.stuffService = stuffService;
    }

    @GetMapping("/category/names")
    public List<Category> getCategoriesList(@RequestParam("startsFrom") String searchString) {
        return categoryService.getCategoriesBySearchString(searchString);
    }

    @PostMapping("/category")
    @Transactional
    public Category addOrReturnExistingCategory(@RequestParam String categoryName, Authentication authentication) {
        Category category = categoryService.findByName(categoryName);
        if (category != null) {
            return category;
        } else {
            return categoryService.save(Category.builder()
                .creator((User) authentication.getPrincipal())
                .name(categoryName)
                .build());
        }
    }

    @PostMapping
    public Stuff add(@RequestBody CreateStuff createStuff, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return stuffService.create(createStuff.getStuff(), user.getId(), createStuff.getCategories());
    }

    @GetMapping("/{id}")
    public Stuff get(@PathVariable long id) {
        return stuffService.getById(id);
    }

    @GetMapping("/list")
    public Iterable<Stuff> getList(Principal principal) {
        return stuffService.getUserStuff(principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id, Principal principal) {
        stuffService.delete(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/addExisting")
    @Transactional
    public ResponseEntity<?> addExisting(@RequestHeader long id, Authentication authentication) {
        Stuff stuff = stuffService.getById(id);
        stuff.getUsers().add((User) authentication.getPrincipal());
        stuffService.save(stuff);
        return ResponseEntity.ok().build();
    }
}
