import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AdbComponent } from './adb.component';

describe('AdbComponent', () => {
  let component: AdbComponent;
  let fixture: ComponentFixture<AdbComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AdbComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AdbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
