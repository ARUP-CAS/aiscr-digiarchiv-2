import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuseionPredmetyDialog } from './museion-predmety-dialog';

describe('MuseionPredmetyDialog', () => {
  let component: MuseionPredmetyDialog;
  let fixture: ComponentFixture<MuseionPredmetyDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuseionPredmetyDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuseionPredmetyDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
