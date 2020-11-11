import {
  Directive, ElementRef, EventEmitter, HostListener, Input, NgZone, OnChanges, OnDestroy, OnInit, Output,
  SimpleChange
} from '@angular/core';


@Directive({
  selector: '[leaflet]'
})
export class LeafletDirective
  implements OnChanges, OnDestroy, OnInit {

  readonly DEFAULT_ZOOM = 1;
  readonly DEFAULT_CENTER;
  readonly DEFAULT_FPZ_OPTIONS = {};

  resizeTimer: any;


  @Input('leafletFitBoundsOptions') fitBoundsOptions = this.DEFAULT_FPZ_OPTIONS;
  @Input('leafletPanOptions') panOptions = this.DEFAULT_FPZ_OPTIONS;
  @Input('leafletZoomOptions') zoomOptions = this.DEFAULT_FPZ_OPTIONS;
  @Input('leafletZoomPanOptions') zoomPanOptions = this.DEFAULT_FPZ_OPTIONS;


  // Default configuration
  @Input('leafletOptions') options = {};

  // Configure callback function for the map
  @Output('leafletMapReady') mapReady = new EventEmitter<any>();

  // Zoom level for the map
  @Input('leafletZoom') zoom: number;
  @Output('leafletZoomChange') zoomChange = new EventEmitter<number>();

  // Center of the map
  @Input('leafletCenter') center: any;
  @Output('leafletCenterChange') centerChange = new EventEmitter<any>();

  // Set fit bounds for map
  @Input('leafletFitBounds') fitBounds: any;

  // Set the max bounds for the map
  @Input('leafletMaxBounds') maxBounds: any;

  // Set the min zoom for the map
  @Input('leafletMinZoom') minZoom: number;

  // Set the max zoom for the map
  @Input('leafletMaxZoom') maxZoom: number;


  // Mouse Map Events
  @Output('leafletClick') onClick = new EventEmitter<any>();
  @Output('leafletDoubleClick') onDoubleClick = new EventEmitter<any>();
  @Output('leafletMouseDown') onMouseDown = new EventEmitter<any>();
  @Output('leafletMouseUp') onMouseUp = new EventEmitter<any>();
  @Output('leafletMouseMove') onMouseMove = new EventEmitter<any>();
  @Output('leafletMouseOver') onMouseOver = new EventEmitter<any>();
  @Output('leafletMouseOut') onMouseOut = new EventEmitter<any>();

  // Map Move Events
  @Output('leafletMapMove') onMapMove = new EventEmitter<any>();
  @Output('leafletMapMoveStart') onMapMoveStart = new EventEmitter<any>();
  @Output('leafletMapMoveEnd') onMapMoveEnd = new EventEmitter<any>();

  // Map Zoom Events
  @Output('leafletMapZoom') onMapZoom = new EventEmitter<any>();
  @Output('leafletMapZoomStart') onMapZoomStart = new EventEmitter<any>();
  @Output('leafletMapZoomEnd') onMapZoomEnd = new EventEmitter<any>();

  private mapEventHandlers: any = {};

  constructor(private element: ElementRef, private zone: NgZone) {
    // Nothing here
  }

  ngOnInit() {
  }

  ngOnChanges(changes: { [key: string]: SimpleChange }) {

  }

  ngOnDestroy() {
  }

  public getMap() {
  }


  @HostListener('window:resize', [])
  onResize() {
    this.delayResize();
  }

  private addMapEventListeners() {

  }

  /**
   * Resize the map to fit it's parent container
   */
  private doResize() {

  }

  /**
   * Manage a delayed resize of the component
   */
  private delayResize() {
    if (null != this.resizeTimer) {
      clearTimeout(this.resizeTimer);
    }
    this.resizeTimer = setTimeout(this.doResize.bind(this), 200);
  }


  /**
   * Set the view (center/zoom) all at once
   * @param center The new center
   * @param zoom The new zoom level
   */
  private setView(center: any, zoom: number) {
  }

  /**
   * Set the map zoom level
   * @param zoom the new zoom level for the map
   */
  private setZoom(zoom: number) {

  }

  /**
   * Set the center of the map
   * @param center the center point
   */
  private setCenter(center: any) {

  }

  /**
   * Fit the map to the bounds
   * @param any the boundary to set
   */
  private setFitBounds(any: any) {

  }

  /**
   * Set the map's max bounds
   * @param any the boundary to set
   */
  private setMaxBounds(any: any) {
  }

  /**
   * Set the map's min zoom
   * @param number the new min zoom
   */
  private setMinZoom(zoom: number) {

  }

  /**
   * Set the map's min zoom
   * @param number the new min zoom
   */
  private setMaxZoom(zoom: number) {

  }

}