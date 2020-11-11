import { Component, OnInit, Input } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { AppService } from 'src/app/app.service';
import { MatDialog } from '@angular/material/dialog';
import { DocumentDialogComponent } from 'src/app/components/document-dialog/document-dialog.component';
import { Router } from '@angular/router';
import { AppConfiguration } from 'src/app/app-configuration';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-projekt',
  templateUrl: './projekt.component.html',
  styleUrls: ['./projekt.component.scss']
})
export class ProjektComponent implements OnInit {

  @Input() result;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() mapDetail: boolean;
  @Input() isDocumentDialogOpen: boolean;
  @Input() inDocument = false;
  hasDetail: boolean;
  hasRights: boolean;
  bibTex: string;

  constructor(
    private datePipe: DatePipe,
    public state: AppState,
    public service: AppService,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration
  ) { }

  ngOnInit(): void {
    this.hasRights = this.state.hasRights(this.result.pristupnost, this.result.organizace);
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

  toggleDetail() {
    this.detailExpanded = !this.detailExpanded;
    if (!this.hasDetail && !this.inDocument) {
      this.service.getId(this.result.ident_cely).subscribe((res: any) => {
        this.result.akce = res.response.docs[0].akce;
        this.result.samostatny_nalez = res.response.docs[0].samostatny_nalez;
        this.hasDetail = true;
      });
    }
  }

  print() {
    if (this.inDocument) {
      this.service.print();
    } else {
      this.state.printing = true;
      this.router.navigate(['/id', this.result.ident_cely]);
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

  openDocument() {
    this.state.dialogRef = this.dialog.open(DocumentDialogComponent, {
      width: '900px',
      data: this.result,
      panelClass: 'app-document-dialog'
    });
  }
}
