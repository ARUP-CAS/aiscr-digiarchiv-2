import { Component, OnInit, Input } from '@angular/core';
import { NeidentAkce } from 'src/app/shared/neident-akce';
import { Utils } from 'src/app/shared/utils';

@Component({
  selector: 'app-neident-akce',
  templateUrl: './neident-akce.component.html',
  styleUrls: ['./neident-akce.component.scss']
})
export class NeidentAkceComponent implements OnInit {
  @Input() data: NeidentAkce;

  constructor() { }

  ngOnInit(): void {
  }

  hasValue(field: string): boolean {
    return Utils.hasValue(this.data, field);
  }

}
