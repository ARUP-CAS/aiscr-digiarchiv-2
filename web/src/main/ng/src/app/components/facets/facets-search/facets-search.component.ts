import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Subscription } from 'rxjs/internal/Subscription';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';

@Component({
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

  changed(e) {
    this.modelChanged.next(e.target.value);
}

  ngOnDestroy() {
    // this.formCtrlSub.unsubscribe();
  }

}
