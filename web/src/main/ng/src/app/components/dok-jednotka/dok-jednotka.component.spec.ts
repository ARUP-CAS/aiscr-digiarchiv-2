import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { DokJednotkaComponent } from './dok-jednotka.component';

describe('DokJednotkaComponent', () => {
  let component: DokJednotkaComponent;
  let fixture: ComponentFixture<DokJednotkaComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ DokJednotkaComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DokJednotkaComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
