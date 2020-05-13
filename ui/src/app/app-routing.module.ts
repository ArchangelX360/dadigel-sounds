import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

const routes: Routes = [
  {
    path: '',
    loadChildren: () =>
      import('./sound-grid/sound-grid.module').then(m => m.SoundGridModule),
  },
  {
    path: 'sounds',
    loadChildren: () =>
      import('./sound-grid/sound-grid.module').then(m => m.SoundGridModule),
  },
  {
    path: 'gifs',
    loadChildren: () => import('./gif/gif.module').then(m => m.GifModule),
  },
  {
    path: '**',
    loadChildren: () =>
      import('./page-not-found/page-not-found.module').then(
        m => m.PageNotFoundModule,
      ),
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {
}
