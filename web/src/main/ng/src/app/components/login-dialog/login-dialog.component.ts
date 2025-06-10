import { Component, OnInit, Inject } from '@angular/core';
import { AppState } from 'src/app/app.state';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { Router } from '@angular/router';

@Component({
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

  focusp(e, el) {
    el.focus();
  }

}
