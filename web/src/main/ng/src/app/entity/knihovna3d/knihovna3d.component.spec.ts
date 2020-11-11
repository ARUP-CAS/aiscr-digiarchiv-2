import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { Knihovna3dComponent } from './knihovna3d.component';

describe('Knihovna3dComponent', () => {
  let component: Knihovna3dComponent;
  let fixture: ComponentFixture<Knihovna3dComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ Knihovna3dComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(Knihovna3dComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
