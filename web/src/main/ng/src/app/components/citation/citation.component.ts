import { DatePipe } from '@angular/common';
import { Component, OnInit, input } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';


@Component({
  imports: [
    TranslateModule,  DatePipe
  ],
  selector: 'app-citation',
  templateUrl: './citation.component.html',
  styleUrls: ['./citation.component.scss']
})
export class CitationComponent implements OnInit {

  readonly result = input<any>();
  readonly link = input<string>();
  now = new Date();

  constructor() { }

  ngOnInit(): void {
    const result = this.result();
    if (result?.autor) {
      result.autorFormatted = result.autor.join(' – ');
    }
  }

}
