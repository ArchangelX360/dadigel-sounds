import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {SoundGridRoutingModule} from './sound-grid-routing.module';
import {SoundGridComponent} from './sound-grid.component';
import {SoundTileComponent} from './sound-tile/sound-tile.component';
import {MatButtonModule} from '@angular/material/button';
import {DiscordIntegrationComponent} from './discord-integration/discord-integration.component';
import {MatCardModule} from '@angular/material/card';
import {DiscordService} from '../services/discord.service';
import {HttpClientModule} from '@angular/common/http';
import {MatSelectModule} from '@angular/material/select';
import {FormsModule} from '@angular/forms';
import {MatSnackBar} from '@angular/material/snack-bar';
import {MatInputModule} from '@angular/material/input';
import {YoutubeComponent} from './youtube/youtube.component';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatListModule} from '@angular/material/list';
import {MatIconModule} from '@angular/material/icon';
import {MatExpansionModule} from '@angular/material/expansion';

@NgModule({
  declarations: [
    DiscordIntegrationComponent,
    SoundGridComponent,
    SoundTileComponent,
    YoutubeComponent,
  ],
  imports: [
    CommonModule,
    SoundGridRoutingModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatInputModule,
    MatCardModule,
    MatListModule,
    MatIconModule,
    MatExpansionModule,
    HttpClientModule,
    MatSelectModule,
    FormsModule,
  ],
  providers: [DiscordService, MatSnackBar],
})
export class SoundGridModule {
}
