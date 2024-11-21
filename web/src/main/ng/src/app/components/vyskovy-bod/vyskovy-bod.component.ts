import { DatePipe } from '@angular/common';
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

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    public state: AppState) { }

  ngOnInit(): void {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {AMČR},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }


}
