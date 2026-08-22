import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Museion } from './museion';

describe('Museion', () => {
  let component: Museion;
  let fixture: ComponentFixture<Museion>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Museion]
    })
    .compileComponents();

    fixture = TestBed.createComponent(Museion);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
