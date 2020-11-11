import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { JednotkaDokumentuComponent } from './jednotka-dokumentu.component';

describe('JednotkaDokumentuComponent', () => {
  let component: JednotkaDokumentuComponent;
  let fixture: ComponentFixture<JednotkaDokumentuComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ JednotkaDokumentuComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(JednotkaDokumentuComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
