import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { TvarComponent } from './tvar.component';

describe('TvarComponent', () => {
  let component: TvarComponent;
  let fixture: ComponentFixture<TvarComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ TvarComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TvarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
