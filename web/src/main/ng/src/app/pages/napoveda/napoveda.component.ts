import { Component, OnInit } from '@angular/core';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-napoveda',
  templateUrl: './napoveda.component.html',
  styleUrls: ['./napoveda.component.scss']
})
export class NapovedaComponent implements OnInit {

  loading: boolean;

  constructor(public state: AppState) { }

  ngOnInit(): void {
    this.state.hasError = false;
  }

}
