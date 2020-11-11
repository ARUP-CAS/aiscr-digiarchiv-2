import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-vyskovy-bod',
  templateUrl: './vyskovy-bod.component.html',
  styleUrls: ['./vyskovy-bod.component.scss']
})
export class VyskovyBodComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;

  constructor() { }

  ngOnInit(): void {
  }

}
