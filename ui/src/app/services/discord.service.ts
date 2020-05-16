import {Injectable} from '@angular/core';
import {BotStatus, Channel, Guild} from '../models/discord';
import {Observable, ReplaySubject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {finalize, map, tap} from 'rxjs/operators';
import {Sound} from '../models/sound';

@Injectable()
export class DiscordService {
  constructor(private http: HttpClient) {
  }

  getSounds(): Observable<Sound[]> {
    return this.observe<Sound[]>(`${environment.botApi}/sounds`).pipe(
      tap(console.log),
      map(sounds => {
        return sounds.map(s => ({
          filename: s.filename,
          displayName: s.displayName ?? s.filename,
        }));
      }),
    );
  }

  getGuilds(): Observable<Guild[]> {
    return this.observe<Guild[]>(`${environment.botApi}/guilds`);
  }

  getChannels(guildId: string): Observable<Channel[]> {
    return this.observe<Channel[]>(
      `${environment.botApi}/guilds/${guildId}/channels`,
    );
  }

  leaveChannel(guildId: string): Observable<string> {
    return this.http.get(`${environment.botApi}/guilds/${guildId}/leave`, {
      responseType: 'text',
      headers: {
        'Content-Type': 'text/plain; charset=utf-8',
      },
    });
  }

  joinChannel(guildId: string, channelId: string): Observable<string> {
    return this.http.get(
      `${environment.botApi}/guilds/${guildId}/channels/${channelId}/join`,
      {
        responseType: 'text',
        headers: {
          'Content-Type': 'text/plain; charset=utf-8',
        },
      },
    );
  }

  getStatus(guildId: string): Observable<BotStatus> {
    return this.observe<BotStatus>(
      `${environment.botApi}/guilds/${guildId}/status`,
    );
  }

  playSoundInGuild(guildId: string, soundName: string): Observable<string> {
    return this.http.get(`${environment.botApi}/guilds/${guildId}/play`, {
      params: {
        soundIdentifier: soundName,
      },
      responseType: 'text',
      headers: {
        'Content-Type': 'text/plain; charset=utf-8',
      },
    });
  }

  private observe<T>(url: string): Observable<T> {
    const o = new ReplaySubject<T>(1);
    const es = new EventSource(url);
    es.onmessage = ev => {
      o.next(JSON.parse(ev.data));
    };
    return o.pipe(finalize(() => es.close()));
  }
}
