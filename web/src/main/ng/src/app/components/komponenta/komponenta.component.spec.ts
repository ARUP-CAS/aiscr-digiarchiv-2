import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { KomponentaComponent } from './komponenta.component';

describe('KomponentaComponent', () => {
  let component: KomponentaComponent;
  let fixture: ComponentFixture<KomponentaComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ KomponentaComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(KomponentaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
