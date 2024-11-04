import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { DocumentDialogComponent } from '../document-dialog/document-dialog.component';
import { FeedbackDialogComponent } from '../feedback-dialog/feedback-dialog.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-result-actions',
  templateUrl: './result-actions.component.html',
  styleUrls: ['./result-actions.component.scss']
})
export class ResultActionsComponent {

  @Input() result: any;
  @Input() bibTex: string;
  @Input() isDocumentDialogOpen: boolean;
  @Input() detailExpanded: boolean;
  @Input() mapDetail: boolean;

  @Output() onToggleDetail  = new EventEmitter<string>();

  constructor(
    public service: AppService,
    public state: AppState,
    private dialog: MatDialog,
    private router: Router,
    public config: AppConfiguration
  ) { }

  toggleDetail() {
    this.onToggleDetail.emit('');
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

  openFeedback() {
    this.state.dialogRef = this.dialog.open(FeedbackDialogComponent, {
      width: '900px',
      data: this.result.ident_cely,
      panelClass: 'app-feedback-dialog'
    });
  }

}
