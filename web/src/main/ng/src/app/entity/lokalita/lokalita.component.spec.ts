import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { LokalitaComponent } from './lokalita.component';

describe('LokalitaComponent', () => {
  let component: LokalitaComponent;
  let fixture: ComponentFixture<LokalitaComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ LokalitaComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LokalitaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
