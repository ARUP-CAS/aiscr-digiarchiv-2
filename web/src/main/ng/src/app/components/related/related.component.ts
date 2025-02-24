import { Component, Input, OnInit } from '@angular/core';
import { AppConfiguration } from 'src/app/app-configuration';
import { AppService } from 'src/app/app.service';
import { AppState } from 'src/app/app.state';

@Component({
  selector: 'app-related',
  templateUrl: './related.component.html',
  styleUrls: ['./related.component.scss']
})
export class RelatedComponent implements OnInit {

  @Input() mapDetail: boolean;

  public ids: {entity: string, ident_cely: string}[];
  @Input() set related(value: {entity: string, ident_cely: string}[]) {
    this.ids = value;
    this.numChildren = this.ids.length;
    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
    this.ids.sort((c1,c2) => {
      return c1.ident_cely.localeCompare(c2.ident_cely)
    });
    this.toProcess = JSON.parse(JSON.stringify(this.ids));
    this.getRecords(false);
  }

  
  public children: {entity: string, result: any}[] = [];
  numChildren: number = 0;
  
  itemSize = 133;
  vsSize = 0;
  math = Math;
  toProcess: {entity: string, ident_cely: string}[];
  loadSize = 20;

  constructor(
    public config: AppConfiguration,
    public state: AppState,
    private service: AppService
  ){}

  ngOnInit(): void {
  }

  getRecords(loadAll: boolean) {

    if (this.toProcess.length > 0) {
      const entity = this.toProcess[0].entity;
      let entitySize = 0;
      const max = Math.min(this.loadSize, this.toProcess.length);
      for (let i = 0; i < max; i++ ) {
        if (entity === this.toProcess[i].entity) {
          entitySize++;
        } else {
          break;
        }
      }

      const ids: {entity: string, ident_cely: string}[] = this.toProcess.splice(0, entitySize);

      this.service.getIdAsChild(ids.map(e => e.ident_cely), entity).subscribe((res: any) => {
        res.response.docs.forEach(result => {
          this.children.push({entity, result})
        });
        this.state.documentProgress = this.children.length / this.numChildren * 100;
        this.state.loading = (this.children.length) < this.numChildren;
        if ((loadAll && this.state.loading) || (entitySize < this.loadSize)) {
          this.getRecords(loadAll)
        } else {
          this.state.loading = false;
        }
      });
    }
  }

  loadAll() {
    this.getRecords(true)
  }

}
