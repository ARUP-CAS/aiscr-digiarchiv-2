
import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { FormsModule } from '@angular/forms';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule,
    FormsModule,
    MatIconModule,
    MatListModule,
    MatProgressBarModule,
    MatFormFieldModule,
    MatDialogModule,
    MatButtonModule
],
  selector: 'app-login-dialog',
  templateUrl: './login-dialog.component.html',
  styleUrls: ['./login-dialog.component.scss']
})
export class LoginDialogComponent implements OnInit {

  user: string;
  pwd: string;
  loginError: any;
  loading: boolean;

  constructor(
    public dialogRef: MatDialogRef<LoginDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private router: Router,
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ) { }

  ngOnInit(): void {
  }

  login() {
    this.loading = true;
    this.service.login(this.user, this.pwd).subscribe(res => {
      this.state.setLogged(res);
      this.loading = false;
      if (res.error) {
        this.loginError = res.error;
      } else {
        this.loginError = null;
        this.user = '';
        this.pwd = '';
        this.dialogRef.close();

        if (this.state.ui?.sort?.[this.state.entity]) {
          const uisort = this.state.sorts_by_entity.find(s => (s.field) === this.state.ui.sort[this.state.entity]);
          if (uisort !== this.state.sort) {
            this.router.navigate([], { queryParams: { sort: this.state.ui.sort[this.state.entity] }, queryParamsHandling: 'merge' });
          } else {
            document.location.reload();
          }
        } else {
          document.location.reload();
        }


      }
    });
  }

  logout() {
    this.service.logout().subscribe(res => {
      this.state.setLogged(res);
      this.state.logged = false;
      this.state.user = null;
      this.dialogRef.close();
      document.location.reload();
    });
  }

  showFav() {
    alert('TODO');
  }

  focusp(el: HTMLInputElement) {
    el.focus();
  }

}
