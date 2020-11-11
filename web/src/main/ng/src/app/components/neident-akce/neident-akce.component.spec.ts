import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NeidentAkceComponent } from './neident-akce.component';

describe('NeidentAkceComponent', () => {
  let component: NeidentAkceComponent;
  let fixture: ComponentFixture<NeidentAkceComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NeidentAkceComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NeidentAkceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
