import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-feedback-dialog',
  templateUrl: './feedback-dialog.component.html',
  styleUrls: ['./feedback-dialog.component.scss']
})
export class FeedbackDialogComponent implements OnInit {

  name: string;
  mail: string;
  text: string;
  ident_cely: string;

  constructor(
    public dialogRef: MatDialogRef<FeedbackDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private service: AppService
    ) { }

  ngOnInit(): void {
    this.ident_cely = this.data;
  }

  sendFeedback() {
    this.service.feedback(this.name, this.mail, this.text, this.ident_cely).subscribe((res: any)=>{
      if(res.hasError) {
        alert(this.service.getTranslation('feedback_failed') + ": " + res.error);
      } else {
        alert(this.service.getTranslation('feedback_success'));
        this.dialogRef.close();
      }
    });
  }

  hasError() {
    // do it
    return false
  }

}
