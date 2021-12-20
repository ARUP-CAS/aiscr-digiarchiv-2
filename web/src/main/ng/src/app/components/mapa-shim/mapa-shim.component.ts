import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-mapa',
  templateUrl: './mapa-shim.component.html',
  styleUrls: ['./mapa-shim.component.scss']
})
export class MapaShimComponent implements OnInit {
  @Input() isResults = true;
  @Input() showResults = false;

  constructor() { }

  ngOnInit(): void {
  }

}
