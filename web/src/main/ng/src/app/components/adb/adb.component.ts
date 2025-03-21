import { DatePipe } from '@angular/common';
import { Component, OnInit, Input } from '@angular/core';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-adb',
  templateUrl: './adb.component.html',
  styleUrls: ['./adb.component.scss']
})
export class AdbComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;
  @Input() onlyHead = false;

  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    public service: AppService,
    public state: AppState
  ) { }

  ngOnInit(): void {
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex =
      `@misc{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = {Archeologický informační systém České republiky},
       title = {Záznam ${this.result.ident_cely}},
       howpublished = url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}},
       note = {Archeologická mapa České republiky [cit. ${now}]}
     }`;
    if (this.result?.ident_cely) { 
    } else {
      const pianid = this.result.id ? this.result.id : this.result;

      this.service.getIdAsChild([pianid], "adb").subscribe((res: any) => {
        this.result = res.response.docs[0]; 
      });
    }
  }

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
  }

}
