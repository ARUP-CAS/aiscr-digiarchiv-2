import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BibtextDialogComponent } from '../bibtext-dialog/bibtext-dialog.component';
import { DocumentDialogComponent } from '../document-dialog/document-dialog.component';
import { FeedbackDialogComponent } from '../feedback-dialog/feedback-dialog.component';

@Component({
  imports: [
    TranslateModule, CommonModule, FormsModule, FlexLayoutModule,
    RouterModule, MatDialogModule, MatButtonModule,
    MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule
  ],
  selector: 'app-result-actions',
  templateUrl: './result-actions.component.html',
  styleUrls: ['./result-actions.component.scss']
})
export class ResultActionsComponent {

  readonly result = input<any>();
  readonly bibTex = input<string>();
  readonly isDocumentDialogOpen = input<boolean>();
  readonly detailExpanded = input<boolean>();
  readonly inDocument = input<boolean>();
  readonly mapDetail = input<boolean>();
  readonly ident_cely_api = input<any>();

  @Output() onToggleDetail  = new EventEmitter<string>();

  useParentEntities = ['']

  constructor(
    public service: AppService,
    public state: AppState,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration
  ) { }

  apiIdentCely(item:{label: string, metadataPrefix: string, url: string, useParent: boolean}) {
    const ident_cely_api = this.ident_cely_api();
    // console.log(item.useParent)
    return (item.useParent && ident_cely_api) ? ident_cely_api : this.result().ident_cely;
  }

  toggleDetail() {
    this.onToggleDetail.emit('');
  }

  toggleFav() {
    const result = this.result();
    if (result.isFav) {
      this.service.removeFav(result.ident_cely).subscribe(res => {
        this.result().isFav = false;
      });
    } else {
      this.service.addFav(result.ident_cely).subscribe(res => {
        this.result().isFav = true;
      });
    }
  }

  openDocument() {
    this.state.dialogRef = this.dialog.open(DocumentDialogComponent, {
      width: '900px',
      data: this.result(),
      panelClass: 'app-card-result-dialog'
    });
  }

  openFeedback() {
    this.state.dialogRef = this.dialog.open(FeedbackDialogComponent, {
      width: '900px',
      data: this.result().ident_cely,
      panelClass: 'app-feedback-dialog'
    });
  }

  showCitation() {
    this.state.dialogRef = this.dialog.open(BibtextDialogComponent, {
      width: '900px',
      data: {result: this.result(), link: this.config.serverUrl + 'id/' + this.result().ident_cely},
      panelClass: 'app-feedback-dialog'
    });
    
  }

}
