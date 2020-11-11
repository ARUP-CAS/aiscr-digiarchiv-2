import { Component, Inject, OnInit } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
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
