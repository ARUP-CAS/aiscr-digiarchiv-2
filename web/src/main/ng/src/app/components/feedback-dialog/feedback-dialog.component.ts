import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-feedback-dialog',
  templateUrl: './feedback-dialog.component.html',
  styleUrls: ['./feedback-dialog.component.scss']
})
export class FeedbackDialogComponent implements OnInit {

  @ViewChild('captchaRef') captchaRef: any;

  name: string;
  mail: string;
  text: string;
  ident_cely: string;
  reCaptchaValid = true;

  constructor(
    public dialogRef: MatDialogRef<FeedbackDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    public config: AppConfiguration,
    private service: AppService
    ) { }

  ngOnInit(): void {
    this.ident_cely = this.data;
  }

  ngAfterViewInit() {
    this.captchaRef.execute();
  }

  public resolved(captchaResponse: string): void {
    this.reCaptchaValid = true;
    // console.log(captchaResponse);
  }

  errored(captchaResponse: any): void {
    this.reCaptchaValid = false;
    // console.log(captchaResponse);
  }

  sendFeedback() {
      this.service.feedback(this.name, this.mail, this.text, this.ident_cely).subscribe((res: any)=>{
        if(res.hasError) {
          alert(this.service.getTranslation('dialog.alert.feedback_failed') + ": " + res.error);
        } else {
          alert(this.service.getTranslation('dialog.alert.feedback_success'));
          this.dialogRef.close();
        }
      });
  }

  hasError() {
    // do it
    return false
  }

}
