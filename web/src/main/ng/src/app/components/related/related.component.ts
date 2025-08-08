import { Component, Input, OnInit, forwardRef, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { FlexLayoutModule } from 'ngx-flexible-layout';
import { AppConfiguration } from '../../app-configuration';
import { AppService } from '../../app.service';
import { AppState } from '../../app.state';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { EntityContainer } from "../../entities/entity-container/entity-container";
import { MatButtonModule } from '@angular/material/button';

@Component({
  imports: [
    TranslateModule, RouterModule, FlexLayoutModule,
    MatCardModule, MatIconModule, MatButtonModule, ScrollingModule,
    MatProgressBarModule, MatTooltipModule, MatExpansionModule,
    forwardRef(() => EntityContainer)
],
  selector: 'app-related',
  templateUrl: './related.component.html',
  styleUrls: ['./related.component.scss']
})
export class RelatedComponent implements OnInit {

  readonly mapDetail = input<boolean>();

  public ids: {entity: string, ident_cely: string}[];
  @Input() set related(value: {entity: string, ident_cely: string}[]) {
    this.ids = value;
    this.numChildren = this.ids.length;
    this.vsSize = Math.min(600, Math.min(this.numChildren, 5) * this.itemSize);
    this.ids.sort((c1,c2) => {
      return c1.ident_cely.localeCompare(c2.ident_cely)
    });
    this.toProcess = JSON.parse(JSON.stringify(this.ids));
    if (this.state.printing || this.router.isActive('print', false)) {
      this.state.loading.set(true);;
      this.getRecords(true)
    } else {
      this.getRecords(false);
    }
    
  }

  
  public children: {entity: string, ident_cely: string, result: any}[] = [];
  numChildren: number = 0;
  
  itemSize = 133;
  vsSize = 0;
  math = Math;
  toProcess: {entity: string, ident_cely: string}[];
  loadSize = 20;

  constructor(
    private router: Router,
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
        res.response.docs.forEach((result: any) => {
          this.children.push({entity, ident_cely: result.ident_cely, result})
        });
        this.state.documentProgress = this.children.length / this.numChildren * 100;
        this.state.loading.set((this.children.length) < this.numChildren);
        if ((loadAll && this.state.loading()) || (entitySize < this.loadSize)) {
          this.getRecords(loadAll)
        } else {
          this.state.loading.set(false);;
        }
      });
    } else {
      this.state.loading.set(false);;
    }
  }

  loadAll() {
    this.getRecords(true)
  }

}
