import {Component, OnInit, ViewChild} from '@angular/core';
import {MatAutocompleteSelectedEvent, MatChipInputEvent, MatInput, MatAutocomplete} from "@angular/material";
import {Category} from "../_models/Category";
import {FormBuilder, FormControl} from "@angular/forms";
import {COMMA, ENTER} from "@angular/cdk/keycodes";
import {StuffService} from "../_services/stuff.service";

@Component({
  selector: 'add-category',
  templateUrl: './add-category.component.html',
  styleUrls: ['./add-category.component.css']
})
export class AddCategoryComponent implements OnInit {
  @ViewChild('matInput') matInput: MatInput;
  @ViewChild('auto') matAutocomplete: MatAutocomplete;

  categories: Map<string, Category> = new Map<string, Category>();
  autocompleteCategories: Set<Category>;
  separatorKeysCodes = [ENTER, COMMA];
  control = new FormControl();

  constructor(private fb: FormBuilder,
              private stuffService: StuffService) {
  }

  ngOnInit() {
    this.control.valueChanges
      .debounceTime(500).distinctUntilChanged()
      .subscribe(value => {
        if (value.length > 1) {
          this.stuffService.getCategories(value).subscribe(categories => {
            this.autocompleteCategories = categories;
          });
        }
      });
  }

  addCategoryFromAutocomplete(event: MatAutocompleteSelectedEvent) {
    console.log("addCategoryFromAutocomplete");
    const category: Category = event.option.value;
    this.addCategory(category);
    this.matInput['nativeElement'].value = '';
  }

  persistCategory(categoryName: string) {
    this.stuffService.addCategory(new Category(null, categoryName)).subscribe(category => {
      this.addCategory(category);
    });
  }

  remove(category: Category): void {
    console.log("remove");
    this.categories.delete(category.name);
  }

  add(event: MatChipInputEvent): void {
    console.log("add");
    let name = event.value.trim();
    if (!name) {
      return;
    }
    let category = this.findExistingCategory(name);
    if (!category) {
      this.persistCategory(name)
    } else {
      this.addCategory(category);
    }

    this.resetForm(event.input);
  }

  getCategories(): Category[] {
    return Array.from(this.categories.values());
  }

  private resetForm(input: HTMLInputElement) {
    if (input) {
      input.value = '';
      this.autocompleteCategories = null;
    }
  }

  private findExistingCategory(name: string): Category {
    let findCategory = (categories, name) => {
      for (let i in categories) {
        let category: Category = categories[i];
        if (category.name == name) {
          return category;
        }
      }
      return null;
    };
    return findCategory(this.autocompleteCategories, name) || findCategory(this.categories, name);
  }

  private addCategory(category: Category) {
    if (category.id == null) {
      throw new Error("You shouldn't add category without id");
    }
    console.log("new category", category);
    console.log("old categories", this.categories);
    this.categories.set(category.name, category);
  }
}
