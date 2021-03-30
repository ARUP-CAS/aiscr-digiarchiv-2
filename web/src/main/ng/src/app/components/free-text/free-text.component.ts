import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-free-text',
  templateUrl: './free-text.component.html',
  styleUrls: ['./free-text.component.scss']
})
export class FreeTextComponent implements OnInit {

  @Input() id: string;
  text: SafeHtml;

  constructor(
    private sanitized: DomSanitizer,
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.service.currentLang.subscribe(r => {
      this.setText();
    });
    this.setText();
  }

  setText() {
    this.service.getText(this.id).subscribe(t => {
      this.text = this.sanitized.bypassSecurityTrustHtml(t);
    });
  }

}
