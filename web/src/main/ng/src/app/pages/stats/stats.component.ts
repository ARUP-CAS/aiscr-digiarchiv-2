import { Component, OnInit } from '@angular/core';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {

  ident_cely: string;

  constructor(private service: AppService) {}

  ngOnInit(): void {

  }

  search() {
    const p: any = {};
    // if ()
    p.q = this.ident_cely;
    p.page = 0;
    this.service.searchStats(p).subscribe((resp: any) => {});
  }

}
