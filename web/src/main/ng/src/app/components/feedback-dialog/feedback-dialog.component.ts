import { Component, OnInit } from '@angular/core';
import { MatDialogRef } from '@angular/material/dialog';
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

  constructor(
    public dialogRef: MatDialogRef<FeedbackDialogComponent>,
    private service: AppService
    ) { }

  ngOnInit(): void {
  }

  sendFeedback() {
    this.service.feedback(this.name, this.mail, this.text).subscribe((res: any)=>{
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
