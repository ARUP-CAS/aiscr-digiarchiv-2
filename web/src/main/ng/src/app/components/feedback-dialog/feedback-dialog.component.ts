import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-feedback-dialog',
  templateUrl: './feedback-dialog.component.html',
  styleUrls: ['./feedback-dialog.component.scss']
})
export class FeedbackDialogComponent implements OnInit {

  email: string;

  constructor() { }

  ngOnInit(): void {
  }

  sendFeedback() {
    // do it
  }

  hasError() {
    // do it
    return false
  }

}
