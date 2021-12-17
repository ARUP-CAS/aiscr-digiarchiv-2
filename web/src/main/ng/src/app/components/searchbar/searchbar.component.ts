import { AppConfiguration } from 'src/app/app-configuration';
import { Condition } from 'src/app/shared/condition';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AppState } from 'src/app/app.state';
import { Component, OnInit, Inject, AfterViewInit } from '@angular/core';
import { AppHeslarService } from 'src/app/app.heslar.service';
import { DOCUMENT } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';

@Component({
  selector: 'app-searchbar',
  templateUrl: './searchbar.component.html',
  styleUrls: ['./searchbar.component.scss']
})
export class SearchbarComponent implements OnInit, AfterViewInit {

  isAdvancedCollapsed = true;
  conditions: Condition[] = [];

  constructor(
    private dialog: MatDialog,
    private router: Router,
    public state: AppState,
    public config: AppConfiguration,
    public heslarService: AppHeslarService,
    @Inject(DOCUMENT) private document: Document
  ) { }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
  }

  search() {
    const p: any = {};
    p.q = this.state.q ? (this.state.q !== '' ? this.state.q : null) : null;
    p.page = 0;
    this.router.navigate(['/results'], { queryParams: p, queryParamsHandling: 'merge' });
  }

  changeShowWithoutThumbs() {
    this.state.hideWithoutThumbs = !this.state.hideWithoutThumbs;
    const val = this.state.hideWithoutThumbs ? true : null;
    this.router.navigate([], { queryParams: { hideWithoutThumbs: val }, queryParamsHandling: 'merge' });
  }

  toggleMapa() {
    this.state.isMapaCollapsed = !this.state.isMapaCollapsed;
    this.state.setMapResult(null, false);
    if (!this.state.isMapaCollapsed) {
      this.document.body.classList.add('app-view-map');
    } else {
      this.document.body.classList.remove('app-view-map');
    }
    const p: any = {};
    p.mapa = !this.state.isMapaCollapsed;
    if (!p.mapa) {
      // p.loc_rpt = null;
      p.pian_id = null;
      p.mapa = null;
      if (!this.state.locationFilterEnabled) {
        p.loc_rpt = null;
      }
    }
    let url = '/results';
    if (this.router.isActive('/id', false)) {
      url = '/id/' + this.state.documentId;
      p.loc_rpt = null;
    }
    this.router.navigate([url], { queryParams: p, queryParamsHandling: 'merge' });
  }

  exportMapa() {
    
  }
}
