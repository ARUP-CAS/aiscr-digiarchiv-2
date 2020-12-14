import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateParser, TranslateService } from '@ngx-translate/core';

export class PaginatorI18n extends MatPaginatorIntl {

  private rangeLabelIntl: string;

  getRangeLabel = (page, pageSize, length) => {
      if (length === 0 || pageSize === 0) {
        return this.translate.instant('rangeLabel1', { length });
      }
      length = Math.max(length, 0);
      const startIndex = page * pageSize;
      // If the start index exceeds the list length, do not try and fix the end index to the end.
      const endIndex = startIndex < length ? Math.min(startIndex + pageSize, length) : startIndex + pageSize;
      return this.translate.instant('rangeLabel2', { startIndex: startIndex + 1, endIndex, length });
  };

  constructor(private readonly translate: TranslateService) {
    super();
    this.getTranslations();
    this.translate.onLangChange.subscribe(() => this.getTranslations());
   }

  getTranslations() {
    this.translate.get([
      'itemsPerPageLabel',
      'nextPageLabel',
      'previousPageLabel',
      'firstPageLabel',
      'lastPageLabel'
    ])
      .subscribe(translation => {
        this.itemsPerPageLabel = translation['itemsPerPageLabel'];
        this.nextPageLabel = translation['nextPageLabel'];
        this.previousPageLabel = translation['previousPageLabel'];
        this.firstPageLabel = translation['firstPageLabel'];
        this.lastPageLabel = translation['lastPageLabel'];
        this.getRangeLabel = this.getRangeLabel.bind(this);
        this.changes.next();
      });
  }

  // getPaginatorIntl() {
  //   const paginatorIntl = new MatPaginatorIntl();
  //   paginatorIntl.itemsPerPageLabel = this.translate.instant('itemsPerPageLabel');
  //   paginatorIntl.nextPageLabel = this.translate.instant('nextPageLabel');
  //   paginatorIntl.previousPageLabel = this.translate.instant('previousPageLabel');
  //   paginatorIntl.firstPageLabel = this.translate.instant('firstPageLabel');
  //   paginatorIntl.lastPageLabel = this.translate.instant('lastPageLabel');
  //   paginatorIntl.getRangeLabel = this.getRangeLabel.bind(this);
  //   return paginatorIntl;
  // }

}