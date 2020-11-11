import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-nalez',
  templateUrl: './nalez.component.html',
  styleUrls: ['./nalez.component.scss']
})
export class NalezComponent implements OnInit {

  @Input() result;

  constructor() { }

  ngOnInit(): void {
  }

}
