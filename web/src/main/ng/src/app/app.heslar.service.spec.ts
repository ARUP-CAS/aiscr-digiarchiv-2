import { TestBed } from '@angular/core/testing';

import { App.HeslarService } from './app.heslar.service';

describe('App.HeslarService', () => {
  let service: App.HeslarService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(App.HeslarService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
