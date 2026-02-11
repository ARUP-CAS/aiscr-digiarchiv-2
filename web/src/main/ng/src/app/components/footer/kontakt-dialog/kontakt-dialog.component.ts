
import { Component, OnInit } from '@angular/core';
import { MatDialogModule } from '@angular/material/dialog';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  imports: [
    TranslateModule,
    MatDialogModule
],
  selector: 'app-kontakt-dialog',
  templateUrl: './kontakt-dialog.component.html',
  styleUrls: ['./kontakt-dialog.component.scss']
})
export class KontaktDialogComponent implements OnInit {

  constructor() { }

  ngOnInit(): void {
  }

}
