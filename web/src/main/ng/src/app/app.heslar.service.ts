import { Condition } from 'src/app/shared/condition';
import { Injectable } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AppHeslarService {

  heslare: { [key: string]: any } = {};

  constructor(
    // private decimalPipe: DecimalPipe,
    private config: AppConfiguration,
    // private state: AppState,
    private http: HttpClient) { }

  hasHeslar(heslar: string): boolean {
    return this.heslare[heslar];
  }

  getHeslar(h: string, sort: string) {
    // const url = this.config['searchParams']['host'] + 'translations/select';
    const params = new HttpParams()
      .set('id', h);
    return this.http.get('api/search/getheslar', { params })
      .pipe(map((resp: any) => {
        this.heslare[h] = resp.response.docs;
        return this.heslare[h];
      }));
  }

  setConditionFromHeslar(heslar: string, dispField: string, value: string, condition: Condition) {
    if (this.heslare[heslar]) {
      condition.heslar = heslar;
      condition.heslarDisplay = dispField;
      condition.heslarField = value;
      if (!condition.value) {
        condition.value = this.heslare[heslar][0][value];
        condition.dispValue = this.heslare[heslar][0][dispField];
      }

    } else {
      this.getHeslar(heslar, dispField).subscribe(res => {
        condition.heslar = heslar;
        condition.heslarDisplay = dispField;
        condition.heslarField = value;
        if (!condition.value) {
          condition.value = res[0][value];
          condition.dispValue = res[0][dispField];
        }
      });
    }
  }
}
