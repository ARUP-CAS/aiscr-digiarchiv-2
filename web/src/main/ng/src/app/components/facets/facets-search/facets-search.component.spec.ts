import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FacetsSearchComponent } from './facets-search.component';

describe('FacetsSearchComponent', () => {
  let component: FacetsSearchComponent;
  let fixture: ComponentFixture<FacetsSearchComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FacetsSearchComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FacetsSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
