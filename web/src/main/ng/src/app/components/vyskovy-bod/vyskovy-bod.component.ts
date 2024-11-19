import { Component, Input, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';

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

  constructor(public state: AppState) { }

  ngOnInit(): void {
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }


}
