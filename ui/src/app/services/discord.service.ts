import {Injectable} from '@angular/core';
import {Channel, Guild} from '../models/discord';
import {Observable, Subject} from 'rxjs';
import {HttpClient} from '@angular/common/http';
import {environment} from '../../environments/environment';
import {finalize} from 'rxjs/operators';

@Injectable()
export class DiscordService {
  constructor(private http: HttpClient) {
  }

  getSounds(): Observable<Array<string>> {
    return this.http.get<Array<string>>(`${environment.botApi}/sounds`);
  }

  getGuilds(): Observable<Array<Guild>> {
    return this.observe<Array<Guild>>(`${environment.botApi}/guilds`);
  }

  getChannels(guildId: string): Observable<Array<Channel>> {
    return this.observe<Array<Channel>>(
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

  // FIXME: use this
  isConnected(guildId: string): Observable<boolean> {
    return this.observe<boolean>(`${environment.botApi}/guilds/${guildId}/isConnected`, (e) => e);
  }

  playSoundInGuild(
    guildId: string,
    soundName: string,
  ): Observable<string> {
    return this.http.get(
      `${environment.botApi}/guilds/${guildId}/play`,
      {
        params: {
          soundIdentifier: soundName,
        },
        responseType: 'text',
        headers: {
          'Content-Type': 'text/plain; charset=utf-8',
        },
      },
    );
  }

  private observe<T>(url: string, converter: (e: any) => T = JSON.parse): Observable<T> {
    const o = new Subject<T>();
    const es = new EventSource(url);
    es.onmessage = (ev) => {
      o.next(converter(ev.data));
    };
    return o
      .pipe(finalize(() => {
        es.close();
      }));
  }
}
