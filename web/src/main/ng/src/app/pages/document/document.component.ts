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

  children_start = 0;
  children_rows = 20;

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
    this.state.hasError = false;
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });
    this.state.printing = this.state.printing || this.router.isActive('print', false);
    this.route.queryParams.subscribe(val => {
      this.search(this.route.snapshot.params.id);
      this.state.documentId = this.route.snapshot.params.id;
    });
  }

  ngAfterViewInit() {
    if (this.state.printing || this.router.isActive('print', false)) {
      // this.state.printing = false;
      this.tryPrint();
    }
  }

  tryPrint() {
    setTimeout(() => {
      if (this.state.loading || this.state.imagesLoading) {
        this.state.imagesLoading =  this.state.imagesLoaded < this.state.numImages;
        this.tryPrint();
      } else {
        this.state.loading = true;
        this.service.print();
      }
    }, 1000);
  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') 
    + ' | ' + this.service.getTranslation('title.record') 
    + ' - ' + (this.result ? this.result.ident_cely : '') );
  }

  search(id: string) {
    this.loading = true;
    this.state.imagesLoaded = 0;
    this.state.hasError = false;
    this.service.getId(id, true).subscribe((resp: SolrResponse) => {
      if (resp.error) {
        this.state.loading = false;
        this.loading = false;
        this.state.hasError = true;
        this.service.showErrorDialog('dialog.alert.error', 'dialog.alert.search_error');
        return;
      }
      this.state.setSearchResponse(resp);
      if (resp.response.numFound > 0) {
        this.result = resp.response.docs[0];
        if (this.result.autor) {
          this.result.autorFormatted = this.result.autor.join(' – ');
        }

        this.state.setMapResult(this.result, false);
        this.setTitle();
      }
      this.link = this.config.serverUrl + 'id/' + id;
      this.loading = false;
    });
  }

  isResultEmpty(obj) {
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
