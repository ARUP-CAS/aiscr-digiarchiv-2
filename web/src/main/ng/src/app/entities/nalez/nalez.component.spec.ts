import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { NalezComponent } from './nalez.component';

describe('NalezComponent', () => {
  let component: NalezComponent;
  let fixture: ComponentFixture<NalezComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ NalezComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NalezComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
