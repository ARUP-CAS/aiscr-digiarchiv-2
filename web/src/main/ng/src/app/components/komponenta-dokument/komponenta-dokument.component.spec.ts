import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { KomponentaDokumentComponent } from './komponenta-dokument.component';

describe('KomponentaDokumentComponent', () => {
  let component: KomponentaDokumentComponent;
  let fixture: ComponentFixture<KomponentaDokumentComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ KomponentaDokumentComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(KomponentaDokumentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
