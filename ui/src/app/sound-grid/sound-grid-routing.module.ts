import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {SoundGridComponent} from './sound-grid.component';

const routes: Routes = [{path: '', component: SoundGridComponent}];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class SoundGridRoutingModule {
}
