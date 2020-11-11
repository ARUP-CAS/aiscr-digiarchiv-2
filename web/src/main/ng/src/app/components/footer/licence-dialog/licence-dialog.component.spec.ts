import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LicenceDialogComponent } from './licence-dialog.component';

describe('LicenceDialogComponent', () => {
  let component: LicenceDialogComponent;
  let fixture: ComponentFixture<LicenceDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LicenceDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LicenceDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
