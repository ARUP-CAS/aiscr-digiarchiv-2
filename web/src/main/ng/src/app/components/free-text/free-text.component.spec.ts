import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { FreeTextComponent } from './free-text.component';

describe('FreeTextComponent', () => {
  let component: FreeTextComponent;
  let fixture: ComponentFixture<FreeTextComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ FreeTextComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FreeTextComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
