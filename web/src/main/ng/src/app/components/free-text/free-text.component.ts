import { AppService } from 'src/app/app.service';
import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-free-text',
  templateUrl: './free-text.component.html',
  styleUrls: ['./free-text.component.scss']
})
export class FreeTextComponent implements OnInit {

  @Input() id: string;
  text: string;

  constructor(
    private service: AppService
  ) { }

  ngOnInit(): void {
    this.service.currentLang.subscribe(r => {
      this.setText();
    });
  }

  setText() {
    this.service.getText(this.id).subscribe(t => this.text = t);
  }

}
