import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NapovedaComponent } from './napoveda.component';

describe('NapovedaComponent', () => {
  let component: NapovedaComponent;
  let fixture: ComponentFixture<NapovedaComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NapovedaComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NapovedaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
