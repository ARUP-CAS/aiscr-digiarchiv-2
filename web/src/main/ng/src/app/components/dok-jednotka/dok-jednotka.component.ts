import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
  selector: 'app-dok-jednotka',
  templateUrl: './dok-jednotka.component.html',
  styleUrls: ['./dok-jednotka.component.scss']
})
export class DokJednotkaComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() pians: any[];
  @Input() adbs: any[];
  @Input() inDocument = false;

  constructor(
    private service: AppService,
    public state: AppState,
    public config: AppConfiguration
  ) {}

  ngOnInit(): void {
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }
}
