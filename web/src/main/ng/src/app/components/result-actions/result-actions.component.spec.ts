import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultActionsComponent } from './result-actions.component';

describe('ResultActionsComponent', () => {
  let component: ResultActionsComponent;
  let fixture: ComponentFixture<ResultActionsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ResultActionsComponent]
    });
    fixture = TestBed.createComponent(ResultActionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
