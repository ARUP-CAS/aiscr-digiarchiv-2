import { Component, OnInit } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { AppService } from 'src/app/app.service';

@Component({
  selector: 'app-stats',
  templateUrl: './stats.component.html',
  styleUrls: ['./stats.component.scss']
})
export class StatsComponent implements OnInit {

  ident_cely: string;

  constructor(
    private titleService: Title,
    private service: AppService) {}

  ngOnInit(): void {
    this.setTitle();
    this.service.currentLang.subscribe(res => {
      this.setTitle();
    });

  }

  setTitle() {
    this.titleService.setTitle(this.service.getTranslation('navbar.desc.logo_desc') + ' | Stats');
  }

  search() {
    const p: any = {};
    // if ()
    p.ident_cely = this.ident_cely;
    p.page = 0;
    this.service.searchStats(p).subscribe((resp: any) => {});
  }

}
