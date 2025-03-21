import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppConfiguration } from 'src/app/app-configuration';
import { DatePipe } from '@angular/common';

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

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    private service: AppService,
    public state: AppState,
    public config: AppConfiguration
  ) {}

  ngOnInit(): void {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }
}
