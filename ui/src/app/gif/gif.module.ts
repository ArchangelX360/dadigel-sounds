import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {GifRoutingModule} from './gif-routing.module';
import {GifComponent} from './gif.component';
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';

@NgModule({
  declarations: [GifComponent],
  imports: [CommonModule, GifRoutingModule, MatCardModule, MatButtonModule],
})
export class GifModule {
}
