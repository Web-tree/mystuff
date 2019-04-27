import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule} from '@angular/common/http/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BrowserModule} from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatAutocompleteModule, MatChipsModule, MatIconModule, MatInputModule} from '@angular/material';
import {ConfigService} from '../_services/config.service';
import {StuffService} from '../_services/stuff.service';
import {UserService} from '../_services/user.service';
import { AddCategoryComponent } from '../add-category/add-category.component';
import { AddStuffComponent } from './add-stuff.component';
import {DebugElement} from '@angular/core';
import {Observable} from 'rxjs';
import {By} from '@angular/platform-browser';

describe('AddStuffComponent', () => {
  let component: AddStuffComponent;
  let fixture: ComponentFixture < AddStuffComponent > ;
  let stuffService: StuffService;

  beforeEach(async (() => {
    TestBed.configureTestingModule({
        imports: [BrowserModule, FormsModule, ReactiveFormsModule,
          MatAutocompleteModule, MatChipsModule, MatIconModule, MatInputModule,
          HttpClientTestingModule, RouterTestingModule, BrowserAnimationsModule
        ],
        declarations: [AddStuffComponent, AddCategoryComponent],
        providers: [StuffService, UserService, ConfigService]
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AddStuffComponent);
    component = fixture.componentInstance;
    stuffService = TestBed.get(StuffService);
    fixture.detectChanges();
  });

  describe('form component', () => {
    it('should be created', () => {
      expect(component).toBeTruthy();
    });
    it('should call stuffService on submit', () => {
      let observable = new Observable();
      spyOn(observable, 'subscribe');
      spyOn(stuffService, 'addStuff').and.returnValue(observable);
      component.name.setValue('stuff name');
      component.addStuff.controls['description'].setValue('stuff desc');
      fixture.detectChanges();
      component.save();

      expect(stuffService.addStuff).toHaveBeenCalledWith(component.addStuff.value, []);
      expect(observable.subscribe).toHaveBeenCalled();

    });
    it('the addStuff service should recieve correct input data', () => {
      let observable = new Observable();
      spyOn(observable, 'subscribe');
      spyOn(stuffService, 'addStuff').and.returnValue(observable);
      component.name.setValue('stuff name');
      component.addStuff.controls['description'].setValue('stuff desc');
      fixture.detectChanges();
      component.save();

      stuffService.addStuff(component.addStuff.value, [1, 2, 3]).subscribe(stuff => {
        expect(stuff.name).toBe('stuff name');
        expect(stuff.description).toBe('stuff desc');
      });
    });
  });

  describe('name field', () => {
    it('should exist', () => {
      expect(component.name).not.toBeNull(component.name);
    });
    it('should be invalid', () => {
      expect(component.name.valid).toBeFalsy();
    });
    it('should be required', () => {
      expect(component.name.errors['required']).toBeTruthy();
    });
  });

  describe('save button', () => {
    let submitButton: DebugElement;
    beforeEach(() => {
      submitButton = fixture.debugElement.query(By.css('button[type="submit"]'));
    });

    it('should exist', () => {
      expect(submitButton).not.toBeNull();
    });
    it('should have text inside', () => {
      expect(submitButton.nativeElement.textContent).toContain('Save');
    });
    it('should be bound to "save" method', () => {
      component.name.setValue('stuff name');
      fixture.detectChanges();
      spyOn(component, 'save');
      submitButton.nativeElement.click();
      expect(component.save).toHaveBeenCalled();
    });
  });
});
