
import { Component, Inject, OnInit } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { CitationComponent } from "../citation/citation.component";

@Component({
  imports: [
    TranslateModule,
    MatDialogModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    CitationComponent
],
  selector: 'app-bibtext-dialog',
  templateUrl: './bibtext-dialog.component.html',
  styleUrls: ['./bibtext-dialog.component.scss']
})
export class BibtextDialogComponent implements OnInit {

  constructor(
    public dialogRef: MatDialogRef<BibtextDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) { }

  ngOnInit(): void {
  }

  ok(){
    this.dialogRef.close(true);
  }
  
  cancel(){
    this.dialogRef.close();
  }

}
