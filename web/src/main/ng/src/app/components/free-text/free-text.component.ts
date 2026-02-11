
import { Component, OnInit, effect, input, signal } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { AppService } from '../../app.service';

@Component({
  selector: 'app-free-text',
  templateUrl: './free-text.component.html',
  styleUrls: ['./free-text.component.scss']
})
export class FreeTextComponent implements OnInit {

  readonly id = input<string>();
  text = signal<SafeHtml>('');

  constructor(
    private sanitized: DomSanitizer,
    private service: AppService
  ) { 
    effect(() => {
      const id = this.id();
      if (id) {
        this.setText();
      }
    })
  }

  ngOnInit(): void {
    this.service.currentLang.subscribe(r => {
      this.setText();
    });
    
  }

  setText() {
    this.service.getText(this.id()).subscribe(t => {
      this.text.set(this.sanitized.bypassSecurityTrustHtml(t));
    });
  }

}
