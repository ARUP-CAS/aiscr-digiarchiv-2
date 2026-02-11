import { Component, forwardRef, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { MapViewComponent } from "./map-view.component";


@Component({
  imports: [forwardRef(() => MapViewComponent)],
  selector: 'app-map-view-container',
  templateUrl: './map-view-container.component.html',
  styleUrls: ['./map-view.component.scss']
})
export class MapViewContainerComponent {

  isBrowser: boolean = false; 

  constructor(
    @Inject(PLATFORM_ID) platformId: any
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  
}
