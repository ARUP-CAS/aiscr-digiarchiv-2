import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { MapaShimComponent } from './mapa-shim.component';

describe('MapaShimComponent', () => {
  let component: MapaShimComponent;
  let fixture: ComponentFixture<MapaShimComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ MapaShimComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MapaShimComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
