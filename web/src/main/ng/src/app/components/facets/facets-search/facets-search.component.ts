import { Component, OnInit } from '@angular/core';
import { FormControl, FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs/internal/Subscription';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { AppService } from '../../../app.service';
import { AppState } from '../../../app.state';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';

@Component({
  imports: [
    TranslateModule, RouterModule,  FormsModule, MatInputModule,
    MatCardModule, MatIconModule, MatTooltipModule, MatFormFieldModule,
    MatButtonModule
],
  selector: 'app-facets-search',
  templateUrl: './facets-search.component.html',
  styleUrls: ['./facets-search.component.scss']
})
export class FacetsSearchComponent implements OnInit {

  val: string;
  searchControl = new FormControl();
  formCtrlSub: Subscription;

  model: string;
  modelChanged: Subject<string> = new Subject<string>();

  constructor(
    private service: AppService,
    public state: AppState
  ) { }

  ngOnInit(): void {

    this.modelChanged.pipe(
      debounceTime(500),
      distinctUntilChanged())
      .subscribe(val => {
        this.val = val;
        this.service.filterFacets(val);
      });
  }

  changed(e: any) {
    this.modelChanged.next(e.target.value);
}

  ngOnDestroy() {
    // this.formCtrlSub.unsubscribe();
  }

}
