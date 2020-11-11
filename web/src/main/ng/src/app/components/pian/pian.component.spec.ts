import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { PianComponent } from './pian.component';

describe('PianComponent', () => {
  let component: PianComponent;
  let fixture: ComponentFixture<PianComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ PianComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(PianComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
