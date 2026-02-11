import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EntityContainer } from './entity-container';

describe('EntityContainer', () => {
  let component: EntityContainer;
  let fixture: ComponentFixture<EntityContainer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EntityContainer]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EntityContainer);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
