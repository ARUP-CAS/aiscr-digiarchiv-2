import { AppConfiguration } from 'src/app/app-configuration';
import { Condition } from 'src/app/shared/condition';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AppState } from 'src/app/app.state';
import { Component, OnInit, Inject, AfterViewInit } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';
import { AppWindowRef } from 'src/app/app.window-ref';

@Component({
  selector: 'app-searchbar',
  templateUrl: './searchbar.component.html',
  styleUrls: ['./searchbar.component.scss']
})
export class SearchbarComponent implements OnInit, AfterViewInit {

  isAdvancedCollapsed = true;
  conditions: Condition[] = [];
  formats = ['GML', 'WKT', 'GeoJSON'];
  exportUrl: string;

  constructor(
    private windowRef: AppWindowRef,
    private router: Router,
    private route: ActivatedRoute,
    public state: AppState,
    public config: AppConfiguration,
    private service: AppService,
    @Inject(DOCUMENT) private document: Document
  ) { }

  ngOnInit(): void {
    this.service.currentLang.subscribe(res => {
      const parts = this.router.url.split('?');
      const str = (parts.length > 1 ? parts[1] : '') + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export-mapa?' + str;
    });
    this.route.queryParams.subscribe(val => {
      const parts = this.router.url.split('?');
      const str = (parts.length > 1 ? parts[1] : '') + '&lang=' + this.state.currentLang;
      this.exportUrl = 'export-mapa?' + str;
    });
  }

  ngAfterViewInit() {
  }

  openExport() {
    const link = this.windowRef.nativeWindow.open(this.exportUrl + '&format=raw');
  }

  search() {
    const p: any = {};
    // if ()
    p.q = this.state.q ? (this.state.q !== '' ? this.state.q : null) : null;
    p.page = 0;
    p.mapId = null;
    this.state.mapResult = null;
    this.state.setFacetChanged();
    this.router.navigate(['/results'], { queryParams: p, queryParamsHandling: 'merge' });
  }

  changeShowWithoutThumbs() {
    this.state.hideWithoutThumbs = !this.state.hideWithoutThumbs;
    const val = this.state.hideWithoutThumbs ? true : null;
    this.router.navigate([], { queryParams: { hideWithoutThumbs: val }, queryParamsHandling: 'merge' });
  }

  toggleMapa() {
    // this.state.locationFilterEnabled = false;
    // this.state.locationFilterBounds = null;
    this.state.isMapaCollapsed = !this.state.isMapaCollapsed;
    this.state.switchingMap = !this.state.isMapaCollapsed;
    this.state.setMapResult(null, false);
    if (!this.state.isMapaCollapsed) {
      this.document.body.classList.add('app-view-map');
    } else {
      this.document.body.classList.remove('app-view-map');
    }
    const p: any = {};
    p.mapa = !this.state.isMapaCollapsed;
    p.loc_rpt = null;

    if (p.mapa) {
      if (this.router.isActive('/id', false)) {
        p.loc_rpt = null;
        p.vyber = null;
      } else {
        const lat = this.state.stats.lat;
        const lng = this.state.stats.lng;
        // if (lat.max === lat.min) {
        //   lat.min = lat.min - 0.05;
        //   lat.max = lat.max + 0.05;
        //   lng.min = lng.min - 0.05;
        //   lng.max = lng.max + 0.05;
        // }
        p.loc_rpt = lat.min + ',' + lng.min +
          ',' + lat.max + ',' + lng.max;
      }
    } else {

      p.loc_rpt = null;
      p.pian_id = null;
      p.mapa = null;
      p.mapId = null;
      if (!this.state.locationFilterEnabled) {
        p.loc_rpt = null;
        p.vyber = null;
      }
    }
    let url = '/map';
    if (this.router.isActive('/id', false)) {
      url = '/id/' + this.state.documentId;
      p.loc_rpt = null;
      p.vyber = null;
    }
    this.router.navigate([url], { queryParams: p, queryParamsHandling: 'merge' });
  }

  exportMapa(format: string) {

  }
}
