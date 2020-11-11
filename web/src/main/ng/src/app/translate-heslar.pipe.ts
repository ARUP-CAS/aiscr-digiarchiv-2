import { AppService } from 'src/app/app.service';
import { Pipe, PipeTransform, Injectable, EventEmitter, OnDestroy, ChangeDetectorRef } from '@angular/core';
import {TranslateService, LangChangeEvent} from '@ngx-translate/core';

@Pipe({
  name: 'translateHeslar',
    pure: false // required to update the value when the promise is resolved
})
export class TranslateHeslar implements PipeTransform, OnDestroy {


  value: string = '';
  lastKey: string;
  lastParams: any[];
  onLangChange;

  constructor(
    private translate: TranslateService, 
    public service: AppService, 
    private _ref: ChangeDetectorRef) {

  }


  updateValue(key: string, field: string): void {
    this.value = this.service.getHeslarTranslation(key, field);
//    this.translate.get(key, interpolateParams).subscribe((res: string) => {
//      this.value = res ? res : key;
      this.lastKey = key;
      this._ref.markForCheck();
//    });
  }

  transform(val: string, field: string): any {
    this.updateValue(val, field);

    // if there is a subscription to onLangChange, clean it
    this._dispose();

    // subscribe to onLangChange event, in case the language changes
    if (!this.onLangChange) {
      this.onLangChange = this.translate.onLangChange.subscribe((event: LangChangeEvent) => {
        if (this.lastKey) {
          this.lastKey = null; // we want to make sure it doesn't return the same value until it's been updated
          this.updateValue(val, field);
        }
      });
    }


    return this.value;

  }
  
  

  /**
   * Clean any existing subscription to change events
   * @private
   */
  _dispose(): void {

    if (typeof this.onLangChange !== 'undefined') {
      this.onLangChange.unsubscribe();
      this.onLangChange = undefined;
    }
  }

  ngOnDestroy(): void {
    this._dispose();
  }

}
