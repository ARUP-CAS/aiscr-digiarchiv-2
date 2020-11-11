import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { BibtextDialogComponent } from './bibtext-dialog.component';

describe('BibtextDialogComponent', () => {
  let component: BibtextDialogComponent;
  let fixture: ComponentFixture<BibtextDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ BibtextDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BibtextDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
