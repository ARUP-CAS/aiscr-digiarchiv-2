
import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { MatFormFieldModule } from '@angular/material/form-field';
import { RecaptchaModule } from "ng-recaptcha-2";

@Component({
  imports: [
    TranslateModule,
    MatDialogModule,
    FormsModule,
    MatFormFieldModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    RecaptchaModule
],
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
