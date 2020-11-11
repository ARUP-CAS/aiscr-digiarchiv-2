import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { KontaktDialogComponent } from './kontakt-dialog.component';

describe('KontaktDialogComponent', () => {
  let component: KontaktDialogComponent;
  let fixture: ComponentFixture<KontaktDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ KontaktDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(KontaktDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
