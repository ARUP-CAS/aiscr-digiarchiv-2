import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { InlineFilterComponent } from './inline-filter.component';

describe('InlineFilterComponent', () => {
  let component: InlineFilterComponent;
  let fixture: ComponentFixture<InlineFilterComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ InlineFilterComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(InlineFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
