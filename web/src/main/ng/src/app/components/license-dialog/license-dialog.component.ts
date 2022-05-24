import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppConfiguration } from 'src/app/app-configuration';

@Component({
  selector: 'app-license-dialog',
  templateUrl: './license-dialog.component.html',
  styleUrls: ['./license-dialog.component.scss']
})
export class LicenseDialogComponent implements OnInit {

  result: any;
  link: string;
  now = new Date();

  constructor(
    private config: AppConfiguration,
    public dialogRef: MatDialogRef<LicenseDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) { }

  ngOnInit(): void {
    this.result = this.data.result;
    if (this.result.autor) {
      this.result.autorFormatted = this.result.autor.join(' - ');
    }
    this.link = this.config.serverUrl + 'id/' + this.result.ident_cely;
  }

  ok(){
    this.dialogRef.close(true);
  }
  
  cancel(){
    this.dialogRef.close();
  }

}
