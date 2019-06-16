import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";
import { By } from '@angular/platform-browser';

import {StuffListComponent} from './stuff-list.component';
import { Stuff } from '../_models';
import { StuffService } from '../_services/stuff.service';
import { UserService } from '../_services';
import { ConfigService } from '../_services/config.service';

describe('StuffListComponent', () => {
  let component: StuffListComponent;
  let fixture: ComponentFixture<StuffListComponent>;
  let testStuffArray: Stuff[] = [
    {id: 1, name: 'testUser1', description: 'TestDescription1', categories: []},
    {id: 2, name: 'testUser2', description: 'TestDescription2', categories: []}
  ];

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [StuffListComponent],
      imports: [RouterTestingModule, HttpClientTestingModule],
      providers: [StuffService, UserService, ConfigService ]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StuffListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show a list of stuff', () => {
    expect(testStuffArray).toBeTruthy();
    expect(fixture.debugElement.query(By.css('ul'))).toBeDefined();
  });
  it('should show message and hide list if the list is empty', () => {
    testStuffArray = [];
    fixture.detectChanges();
    expect(fixture.debugElement.query(By.css('.alert-info'))).toBeDefined();
    expect(fixture.debugElement.query(By.css('ul'))).toBeNull();
  });
});
