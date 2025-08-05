import { Component, input } from '@angular/core';
import { AppState } from '../../app.state';
import { DokumentComponent } from "../dokument/dokument.component";

@Component({
  selector: 'app-entity-container',
  imports: [DokumentComponent],
  templateUrl: './entity-container.html',
  styleUrl: './entity-container.scss'
})
export class EntityContainer {
  entity = input<string>();

  result = input<any>();
  inDocument = input<boolean>(false);
  detailExpanded = input<boolean>(false);
  isChild = input<boolean>(false);
  mapDetail = input<boolean>(false);
  isDocumentDialogOpen = input<boolean>(false);

  constructor(){}
}
