import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';

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
    public state: AppState
  ) {}

  ngOnInit(): void { 
  }

  pian(id: string) {
    if (this.pians) {
      return this.pians.filter(p => p.ident_cely === id);
    } else {
      return [];
    }
  }
}
