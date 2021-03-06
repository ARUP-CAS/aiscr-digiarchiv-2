import { DatePipe } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-pian',
  templateUrl: './pian.component.html',
  styleUrls: ['./pian.component.scss']
})
export class PianComponent implements OnInit {

  @Input() result;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasRights: boolean;
  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    public state: AppState,
    public service: AppService
  ) {}

  ngOnInit(): void {
    this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
     `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {AMČR}, 
       title = {Záznam ${this.result.ident_cely}},
       url = {https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       publisher = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
  }

  toggleFav() {
    if (this.result.isFav) {
      this.service.removeFav(this.result.ident_cely).subscribe(res => {
        this.result.isFav = false;
      });
    } else {
      this.service.addFav(this.result.ident_cely).subscribe(res => {
        this.result.isFav = true;
      });
    }
  }
}
