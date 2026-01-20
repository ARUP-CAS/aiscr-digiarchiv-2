
import { Component, OnInit } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  imports: [
    TranslateModule,
    MatDialogModule
],
  selector: 'app-licence-dialog',
  templateUrl: './licence-dialog.component.html',
  styleUrls: ['./licence-dialog.component.scss']
})
export class LicenceDialogComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
