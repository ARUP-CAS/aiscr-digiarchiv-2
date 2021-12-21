import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ExportMapaComponent } from './export-mapa.component';

describe('ExportMapaComponent', () => {
  let component: ExportMapaComponent;
  let fixture: ComponentFixture<ExportMapaComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ExportMapaComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ExportMapaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
