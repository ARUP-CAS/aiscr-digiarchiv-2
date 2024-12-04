import { Component, Input, OnInit } from '@angular/core';

@Component({
  selector: 'app-citation',
  templateUrl: './citation.component.html',
  styleUrls: ['./citation.component.scss']
})
export class CitationComponent implements OnInit {

  @Input() result;
  @Input() link;
  now = new Date();

  constructor() { }

  ngOnInit(): void {
    if (this.result?.autor) {
      this.result.autorFormatted = this.result.autor.join(' – ');
    }
  }

}
