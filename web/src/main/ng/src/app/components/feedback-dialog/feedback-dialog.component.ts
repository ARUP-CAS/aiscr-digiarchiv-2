
import { HttpClient } from '@angular/common/http';
import { Component, Inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { MatFormFieldModule } from '@angular/material/form-field';
import { RECAPTCHA_SETTINGS, RecaptchaFormsModule, RecaptchaModule, RecaptchaSettings, RecaptchaV3Module, ReCaptchaV3Service } from "ng-recaptcha-2";
import { MatInputModule } from '@angular/material/input';
import { environment } from '../../../environments/environment';

@Component({
  imports: [
    TranslateModule,
    MatDialogModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    RecaptchaModule,
    RecaptchaFormsModule,
    RecaptchaV3Module
  ],
  selector: 'app-feedback-dialog',
  templateUrl: './feedback-dialog.component.html',
  styleUrls: ['./feedback-dialog.component.scss'],
  providers: [
    {
      provide: RECAPTCHA_SETTINGS,
      useValue: {
        siteKey: environment.recaptcha.siteKey
      } as RecaptchaSettings
    }
  ]
})
export class FeedbackDialogComponent implements OnInit {

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
    private http: HttpClient,
    private recaptchaV3Service: ReCaptchaV3Service
  ) { }

  ngOnInit(): void {
    this.ident_cely = this.data;
    if (this.state.logged) {
      this.name = this.state.user.jmeno + ' ' + this.state.user.prijmeni;
      this.mail = this.state.user.email;
    }
  }

  public executeRecaptchaV3() {
    this.recaptchaV3Service.execute('myAction').subscribe(
      (token: any) => {
        this.verify(token);
      },
      (error: any) => {
        console.log(`Recaptcha v3 error:`, error);
      }
    );
  }

  verify(token: string) {
    this.service.verifyRecaptcha(token).subscribe((res: any) => {
      // console.log(res);
      if (res.tokenProperties?.valid && res.riskAnalysis?.score > this.config.reCaptchaScore) {
        this.reCaptchaValid = true;
      } else {
        setTimeout(() => {
          this.executeRecaptchaV3(  );
        }, 2000)
      }

    });
  }

  ngAfterViewInit() {
    // this.captchaRef.execute();
    this.executeRecaptchaV3();
  }

  sendFeedback() {
    // this.service.verifyRecaptcha(this.reCaptchaMsg).subscribe((res: any)=>{
    //   console.log(res);
    // });

    this.service.feedback(this.name, this.mail, this.text, this.ident_cely).subscribe((res: any) => {
      if (res.hasError) {
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
