import {Injectable} from '@angular/core';
import {Sound} from '../models/sound';
import {DiscordService} from './discord.service';
import {from, Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Injectable()
export class SoundManagerService {
  soundRoot = 'assets/sounds/';
  currentSong: HTMLAudioElement;

  constructor(private discordService: DiscordService) {
  }

  playSound(soundIdentifier: string, guildId?: string): Observable<string> {
    if (guildId) {
      return this.discordService.playSoundInGuild(guildId, soundIdentifier);
    }

    if (this.currentSong) {
      this.currentSong.pause();
    }
    // eslint-disable-next-line no-undef
    this.currentSong = new Audio(this.soundRoot + soundIdentifier);
    this.currentSong.load();
    return from(this.currentSong.play()).pipe(
      map(() => {
        return 'successfully played locally';
      }),
    );
  }

  getSounds(): Observable<Sound[]> {
    return this.discordService.getSounds();
  }
}
