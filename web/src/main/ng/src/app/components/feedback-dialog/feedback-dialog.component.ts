import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';
import { environment } from 'src/environments/environment.prod';

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
  reCaptchaValid = false;
  reCaptchaMsg = '';

  constructor(
    public dialogRef: MatDialogRef<FeedbackDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: string,
    private state: AppState,
    public config: AppConfiguration,
    private service: AppService,
    private http: HttpClient
    ) { }

  ngOnInit(): void {
    this.ident_cely = this.data;
    if (this.state.logged) {
      this.name = this.state.user.jmeno + ' ' + this.state.user.prijmeni;
      this.mail = this.state.user.email;
    }
  }

  ngAfterViewInit() {
    this.captchaRef.execute();
  }

  public resolved(captchaResponse: string): void {
    this.reCaptchaMsg = captchaResponse;
    if (!this.reCaptchaMsg) {
      return;
    }
    this.service.verifyRecaptcha(this.reCaptchaMsg).subscribe((res: any)=>{
      //console.log(res);
      if (res.tokenProperties?.valid && res.riskAnalysis?.score > this.config.reCaptchaScore) {
        this.reCaptchaValid = true;
      }
      
    });
    
  }

  errored(captchaResponse: any): void {
    this.reCaptchaValid = false;
  }

  sendFeedback() {
    // this.service.verifyRecaptcha(this.reCaptchaMsg).subscribe((res: any)=>{
    //   console.log(res);
    // });

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
