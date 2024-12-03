import { NgModule } from '@angular/core';

import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatButtonModule} from '@angular/material/button';
import {MatInputModule} from '@angular/material/input';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatRadioModule} from '@angular/material/radio';
import {MatSelectModule} from '@angular/material/select';
import {MatMenuModule} from '@angular/material/menu';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatListModule} from '@angular/material/list';
import {MatCardModule} from '@angular/material/card';
import {MatStepperModule} from '@angular/material/stepper';
import {MatTabsModule} from '@angular/material/tabs';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatDialogModule} from '@angular/material/dialog';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatSnackBarModule} from '@angular/material/snack-bar';
import {MatTableModule} from '@angular/material/table';
import {MatPaginatorModule} from '@angular/material/paginator';
import {MatExpansionModule} from '@angular/material/expansion';
import {MatTreeModule} from '@angular/material/tree';
import {MatDatepickerModule} from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule } from '@angular/common';
import { MatBottomSheetModule } from '@angular/material/bottom-sheet';

@NgModule({
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatMenuModule,
    MatToolbarModule,
    MatIconModule,
    MatDialogModule,
    MatCardModule,
    MatSelectModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatListModule,
    MatPaginatorModule,
    MatTableModule,
    MatSnackBarModule,
    MatRadioModule,
    MatProgressBarModule,
    MatTabsModule,
    MatStepperModule,
    MatExpansionModule,
    MatTreeModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSidenavModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatBottomSheetModule
  ],
  exports: [
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatMenuModule,
    MatToolbarModule,
    MatIconModule,
    MatDialogModule,
    MatCardModule,
    MatSelectModule,
    MatTooltipModule,
    MatCheckboxModule,
    MatListModule,
    MatPaginatorModule,
    MatTableModule,
    MatSnackBarModule,
    MatRadioModule,
    MatProgressBarModule,
    MatTabsModule,
    MatStepperModule,
    MatExpansionModule,
    MatTreeModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSidenavModule,
    MatProgressSpinnerModule,
    MatIconModule
  ]
})
export class AppMaterialModule {}