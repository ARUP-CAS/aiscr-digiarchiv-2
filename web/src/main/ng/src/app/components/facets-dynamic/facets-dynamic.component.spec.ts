import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FacetsDynamicComponent } from './facets-dynamic.component';

describe('FacetsDynamicComponent', () => {
  let component: FacetsDynamicComponent;
  let fixture: ComponentFixture<FacetsDynamicComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FacetsDynamicComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FacetsDynamicComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
