import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MuseionStatsDialog } from './museion-stats-dialog';

describe('MuseionStatsDialog', () => {
  let component: MuseionStatsDialog;
  let fixture: ComponentFixture<MuseionStatsDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MuseionStatsDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MuseionStatsDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
