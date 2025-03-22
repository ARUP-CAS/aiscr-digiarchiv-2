import { MediaMatcher } from '@angular/cdk/layout';
import { ChangeDetectorRef, Component, Inject, NgZone, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Title } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { SolrDocument } from 'src/app/shared/solr-document';


@Component({
  selector: 'app-map-view-container',
  templateUrl: './map-view-container.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewContainerComponent {

  isBrowser: boolean;

  constructor(
    @Inject(PLATFORM_ID) platformId: any
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  
}
