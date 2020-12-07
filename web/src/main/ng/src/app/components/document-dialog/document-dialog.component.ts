import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-document-dialog',
  templateUrl: './document-dialog.component.html',
  styleUrls: ['./document-dialog.component.scss']
})
export class DocumentDialogComponent implements OnInit {

  result: any;

  constructor(
    public dialogRef: MatDialogRef<DocumentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any,
    private service: AppService) { }

  ngOnInit(): void {
    this.result = this.data;
    this.getFullId();
  }

  getFullId() {
    this.service.getId(this.result.ident_cely).subscribe((res: any) => {
      this.result = res.response.docs[0];
    });
  }

}
