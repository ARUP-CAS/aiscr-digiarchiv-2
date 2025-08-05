import { DatePipe } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';

@Component({
  imports: [
    TranslateModule, FlexLayoutModule, DatePipe
  ],
  selector: 'app-citation',
  templateUrl: './citation.component.html',
  styleUrls: ['./citation.component.scss']
})
export class CitationComponent implements OnInit {

  @Input() result:any;
  @Input() link: string;
  now = new Date();

  constructor() { }

  ngOnInit(): void {
    if (this.result?.autor) {
      this.result.autorFormatted = this.result.autor.join(' – ');
    }
  }

}
