import { Component, OnInit, AfterViewInit, Inject, PLATFORM_ID, forwardRef, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';

import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { SolrResponse } from '../../shared/solr-response';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CitationComponent } from "../../components/citation/citation.component";
import { EntityContainer } from "../../entities/entity-container/entity-container";
import { DecimalPipe, isPlatformBrowser } from '@angular/common';
import { MapViewComponent } from "../map-view/map-view.component";

@Component({
  imports: [
    TranslateModule, RouterModule, 
    MatCardModule, MatIconModule, MatSidenavModule,
    MatProgressBarModule, MatTooltipModule,
    MatButtonModule, DecimalPipe,
    CitationComponent,
    EntityContainer,
    forwardRef(() => MapViewComponent)
  ],
  selector: 'app-document',
  templateUrl: './document.component.html',
  styleUrls: ['./document.component.scss']
})
export class DocumentComponent implements OnInit, AfterViewInit {
  opened = true;
  loading = signal<boolean>(false);
  result = signal<any>(null);
  link: string;
  now = new Date();

  children_start = 0;
  children_rows = 20;
  isBrowser: boolean = false;

  constructor(
    @Inject(PLATFORM_ID) platformId: any,
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.state.bodyClass = 'app-page-results';
  }

  ngOnInit(): void {
    this.state.hasError = false;
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });
    this.state.printing.set(this.state.printing() || this.router.isActive('print', false));
    this.route.queryParams.subscribe(val => {
      this.search(this.route.snapshot.params['id']);
      this.state.documentId.set(this.route.snapshot.params['id']);
    });
  }

  ngAfterViewInit() {
    if (this.state.printing() || this.router.isActive('print', false)) {
      this.tryPrint();
    }
  }

  tryPrint() {
    setTimeout(() => {
      if (this.state.loading() || this.state.imagesLoading) {
        this.state.imagesLoading = this.state.imagesLoaded < this.state.numImages;
        this.tryPrint();
      } else {
        this.state.loading.set(true);
        this.service.print();
      }
    }, 2000);
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc')
      + ' | ' + this.service.getTranslation('title.record')
      + ' - ' + (this.result() ? this.result().ident_cely : ''));
  }

  search(id: string) {
    this.loading.set(true);
    this.state.imagesLoaded = 0;
    this.state.hasError = false;
    this.result.set(null);
    this.service.getId(id, true).subscribe((resp: SolrResponse) => {
      this.state.loading.set(false);
      this.loading.set(false);
      if (resp.error) {
        this.state.hasError = true;
        this.service.showErrorDialog('dialog.alert.error', 'dialog.alert.search_error');
        return;
      }
      this.state.setSearchResponse(resp);
      if (resp.response.numFound > 0) {
        const doc: any = resp.response.docs[0];
        this.state.entity = doc.entity;
        if (doc.autor) {
          doc.autorFormatted = doc.autor.join(' – ');
        }

        this.state.setMapResult(doc, false);
        if (doc.entity === 'lokalita' && doc.lokalita_igsn) {
          this.link = 'https://doi.org/' + doc.lokalita_igsn;
        } else if (doc.samostatny_nalez_igsn) {
          this.link = 'https://doi.org/' + doc.samostatny_nalez_igsn;
        } else if (doc.dokument_doi) {
          this.link = 'https://doi.org/' + doc.dokument_doi;
        } else {
          this.link = this.config.serverUrl + 'id/' + id;
        }

        this.result.set(doc);
        this.setTitle();
      }
    });
  }

  isResultEmpty(obj: any) {
    return (!obj || (Object.keys(obj).length === 0));
  }

  isActiveFacet() {
    if (this.state.breadcrumbs?.length === 0) {
      return false;
    } else {
      return true;
    }
  }

}
