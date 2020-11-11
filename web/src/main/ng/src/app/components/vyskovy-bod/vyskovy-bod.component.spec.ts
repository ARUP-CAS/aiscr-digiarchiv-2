import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { VyskovyBodComponent } from './vyskovy-bod.component';

describe('VyskovyBodComponent', () => {
  let component: VyskovyBodComponent;
  let fixture: ComponentFixture<VyskovyBodComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ VyskovyBodComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(VyskovyBodComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
