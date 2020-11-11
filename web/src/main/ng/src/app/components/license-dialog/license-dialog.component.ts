import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  selector: 'app-license-dialog',
  templateUrl: './license-dialog.component.html',
  styleUrls: ['./license-dialog.component.scss']
})
export class LicenseDialogComponent implements OnInit {

  result: any;
  now = new Date();

  constructor(
    public dialogRef: MatDialogRef<LicenseDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any) { }

  ngOnInit(): void {
    this.result = this.data.result;
  }

  ok(){
    this.dialogRef.close(true);
  }
  
  cancel(){
    this.dialogRef.close();
  }

}
