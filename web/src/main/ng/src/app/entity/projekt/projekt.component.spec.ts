import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjektComponent } from './projekt.component';

describe('ProjektComponent', () => {
  let component: ProjektComponent;
  let fixture: ComponentFixture<ProjektComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjektComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjektComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
