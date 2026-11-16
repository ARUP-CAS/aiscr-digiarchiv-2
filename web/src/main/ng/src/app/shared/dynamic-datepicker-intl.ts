import { Injectable, OnDestroy, inject } from '@angular/core';
import { DateAdapter } from '@angular/material/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';
import { TranslateService } from '@ngx-translate/core';
import { Subject, takeUntil } from 'rxjs';

@Injectable()
export class DynamicDatepickerIntl extends MatDatepickerIntl implements OnDestroy {
  private readonly translate = inject(TranslateService);
  private readonly dateAdapter = inject(DateAdapter);
  private readonly destroy$ = new Subject<void>();

  constructor() {
    super();
    this.updateLabels();

    this.translate.onLangChange
      .pipe(takeUntil(this.destroy$))
      .subscribe((event) => {
        this.dateAdapter.setLocale(event.lang);
        this.updateLabels();
      });
  }

  private updateLabels(): void {
    this.translate
      .get([
        'datepicker.next_24_years',
        'datepicker.prev_24_years',
        'datepicker.next_year',
        'datepicker.prev_year',
        'datepicker.next_month',
        'datepicker.prev_month',
      ])
      .subscribe((translations) => {
        this.nextMultiYearLabel = translations['datepicker.next_24_years'];
        this.prevMultiYearLabel = translations['datepicker.prev_24_years'];
        this.nextYearLabel = translations['datepicker.next_year'];
        this.prevYearLabel = translations['datepicker.prev_year'];
        this.nextMonthLabel = translations['datepicker.next_month'];
        this.prevMonthLabel = translations['datepicker.prev_month'];

        // Required to trigger UI update in Angular Material
        this.changes.next();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}