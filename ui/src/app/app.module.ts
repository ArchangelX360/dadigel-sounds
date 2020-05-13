import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppRoutingModule} from './app-routing.module';
import {AppComponent} from './app.component';
import {SoundManagerService} from './services/sound-manager.service';
import {ColorService} from './services/color.service';
import {SoundGridModule} from './sound-grid/sound-grid.module';
import {GifModule} from './gif/gif.module';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatButtonModule} from '@angular/material/button';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    MatToolbarModule,
    MatButtonModule,
    SoundGridModule,
    GifModule,
    BrowserAnimationsModule,
  ],
  providers: [SoundManagerService, ColorService],
  bootstrap: [AppComponent],
})
export class AppModule {
}
