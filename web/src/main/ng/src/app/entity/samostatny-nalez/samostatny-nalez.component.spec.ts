import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SamostatnyNalezComponent } from './samostatny-nalez.component';

describe('SamostatnyNalezComponent', () => {
  let component: SamostatnyNalezComponent;
  let fixture: ComponentFixture<SamostatnyNalezComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SamostatnyNalezComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SamostatnyNalezComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
