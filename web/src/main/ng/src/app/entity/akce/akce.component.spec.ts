import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AkceComponent } from './akce.component';

describe('AkceComponent', () => {
  let component: AkceComponent;
  let fixture: ComponentFixture<AkceComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AkceComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AkceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
