import { Component, OnInit, Input } from '@angular/core';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-adb',
  templateUrl: './adb.component.html',
  styleUrls: ['./adb.component.scss']
})
export class AdbComponent implements OnInit {

  @Input() result: any;
  @Input() detailExpanded: boolean;
  @Input() isChild: boolean;
  @Input() inDocument = false;

  constructor(
    public service: AppService
  ) { }

  ngOnInit(): void {
  }

}
