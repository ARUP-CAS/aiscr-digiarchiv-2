import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-tvar',
  templateUrl: './tvar.component.html',
  styleUrls: ['./tvar.component.scss']
})
export class TvarComponent implements OnInit {
  
  @Input() tvar: string;
  @Input() poznamka: string = null;

  constructor() { }

  ngOnInit(): void {
  }

  hasPoznamka(): boolean{
    return this.poznamka !== null && this.poznamka !== '';
  }

}
