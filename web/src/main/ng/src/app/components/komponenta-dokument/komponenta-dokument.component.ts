import { Component, Input, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-komponenta-dokument',
  templateUrl: './komponenta-dokument.component.html',
  styleUrls: ['./komponenta-dokument.component.scss']
})
export class KomponentaDokumentComponent implements OnInit {
  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;

  constructor(public state: AppState) { }

  ngOnInit(): void {
  }
}
