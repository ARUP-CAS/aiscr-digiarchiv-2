import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { MatDialog } from '@angular/material/dialog';
import { FileViewerComponent } from 'src/app/components/file-viewer/file-viewer.component';
import { AppService } from 'src/app/app.service';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-samostatny-nalez',
  templateUrl: './samostatny-nalez.component.html',
  styleUrls: ['./samostatny-nalez.component.scss']
})
export class SamostatnyNalezComponent implements OnInit {

  @Input() result;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;
  @Input() mapDetail: boolean;
  hasRights: boolean;
  hasDetail: boolean;
  imgSrc: string;
  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    public service: AppService
  ) { }

  ngOnInit(): void {
    this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    if (this.result.soubor_filepath?.length > 0) {
      this.imgSrc = this.config.server + '/api/img?id=' + this.result.soubor_filepath[0];
    }
    const now = this.datePipe.transform(new Date(), 'yyyy-MM-dd');
    this.bibTex = 
     `@misc{
       https://digiarchiv.aiscr.cz/id/${this.result.ident_cely},
       author = “AMČR”, 
       title = “Záznam ${this.result.ident_cely}”,
       howpublished = “\\url{https://digiarchiv.aiscr.cz/id/${this.result.ident_cely}}”,
       note = “Archeologická mapa České republiky [cit. ${now}]”
     }`;
  }

  ngOnChanges(c) {
    if (c.result) {
      this.hasDetail = false;
      this.detailExpanded = false;
    }
    if (this.mapDetail) {
      this.getFullId();
    }
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
      // this.result.akce = res.response.docs[0].akce;
      // this.result.lokalita = res.response.docs[0].lokalita;
      this.hasDetail = true;
    });
  }

  toggleDetail() {
    if (!this.hasDetail && !this.inDocument) {
      this.service.getId(this.result.ident_cely).subscribe((res: any) => {
        this.getFullId();
      });
    }
    this.detailExpanded = !this.detailExpanded;
  }

  print() {
    if (this.inDocument) {
      this.service.print();
    } else {
      this.state.printing = true;
      this.router.navigate(['/id', this.result.ident_cely]);
    }
  }

  viewFiles() {
    const canView = this.state.hasRights(this.result.pristupnost, this.result.organizace);
    // const canView = true;
    if (canView) {
      this.state.dialogRef = this.dialog.open(FileViewerComponent, {
        panelClass: 'app-file-viewer',
        width: '755px',
        data: this.result
      });
    } else {
      const msg = this.service.getTranslation('insuficient rights');
      alert(msg);
    }
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
