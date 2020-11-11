import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ExterniZdrojComponent } from './externi-zdroj.component';

describe('ExterniZdrojComponent', () => {
  let component: ExterniZdrojComponent;
  let fixture: ComponentFixture<ExterniZdrojComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ExterniZdrojComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExterniZdrojComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
