import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FlotComponent } from './flot.component';

describe('FlotComponent', () => {
  let component: FlotComponent;
  let fixture: ComponentFixture<FlotComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FlotComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FlotComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
