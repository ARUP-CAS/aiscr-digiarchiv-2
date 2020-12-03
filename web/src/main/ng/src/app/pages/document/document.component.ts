import { AppConfiguration } from 'src/app/app-configuration';
import { AppState } from 'src/app/app.state';
import { Component, OnInit, AfterViewInit, Input } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';
import { ActivatedRoute, Router, Params } from '@angular/router';
import { SolrResponse } from 'src/app/shared/solr-response';

@Component({
  selector: 'app-document',
  templateUrl: './document.component.html',
  styleUrls: ['./document.component.scss']
})
export class DocumentComponent implements OnInit, AfterViewInit {
  opened = true;
  loading = false;
  result: any;
  link: string;
  now = new Date();

  constructor(
    private titleService: Title,
    private route: ActivatedRoute,
    private router: Router,
    private config: AppConfiguration,
    public state: AppState,
    private service: AppService
    ) {
    this.state.bodyClass = 'app-page-results';
  }

  ngOnInit(): void {
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });
    this.route.queryParams.subscribe(val => {
      this.search(this.route.snapshot.params.id);
      this.state.documentId = this.route.snapshot.params.id;
    });
  }

  ngAfterViewInit() {
    if (this.state.printing || this.router.isActive('print', false)) {
      this.state.printing = false;
      setTimeout(() => {
        this.service.print();
      }, 2000);
    }
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('logo_desc') + ' | ' + this.service.getTranslation('citation.dokument'));
  }

  search(id: string) {
    this.loading = true;
    this.service.getId(id).subscribe((resp: SolrResponse) => {
      this.state.setSearchResponse(resp);
      if (resp.response.numFound > 0) {
        this.result = resp.response.docs[0];
        this.state.setMapResult(this.result, false);
      }
      this.link = this.config.serverUrl + '/id/' + id;
      this.loading = false;
    });
  }

  isResultEmpty (obj) {
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
