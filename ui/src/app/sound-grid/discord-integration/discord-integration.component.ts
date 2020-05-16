import {Component, OnDestroy, Output} from '@angular/core';
import {filter, flatMap, map, tap} from 'rxjs/operators';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {Channel, Connection, Guild, TrackInfo} from '../../models/discord';
import {DiscordService} from '../../services/discord.service';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-discord-integration',
  templateUrl: './discord-integration.component.html',
  styleUrls: ['./discord-integration.component.scss'],
})
export class DiscordIntegrationComponent implements OnDestroy {
  @Output()
  readonly connection: BehaviorSubject<Connection | null> = new BehaviorSubject(
    null,
  );
  readonly guilds$: Observable<Guild[]>;
  readonly channels$: Observable<Channel[]>;
  readonly playingTrack$: Observable<TrackInfo | null>;

  selectedGuild: Guild | null;
  selectedChannel: Channel | null;
  joinedChannel: Channel | null;
  loading = false;

  private subscriptions: Subscription[] = [];

  private readonly selectedGuild$: BehaviorSubject<Guild | null> = new BehaviorSubject(
    null,
  );
  private readonly selectedChannel$: BehaviorSubject<Channel | null> = new BehaviorSubject(
    null,
  );
  private readonly joinedChannel$: Observable<Channel | null>;

  constructor(
    private discordService: DiscordService,
    private snackbar: MatSnackBar,
  ) {
    this.guilds$ = this.discordService.getGuilds();
    this.channels$ = this.selectedGuild$.pipe(
      flatMap(g => this.discordService.getChannels(g.id)),
    );

    const botStatus$ = this.selectedGuild$.pipe(
      filter(g => g !== null),
      flatMap(g => this.discordService.getStatus(g.id)),
    );
    this.joinedChannel$ = botStatus$.pipe(map(bs => bs.joinedChannel));
    this.playingTrack$ = botStatus$.pipe(map(bs => bs.playingTrack));

    // these subscriptions are made without "| async" to avoid
    // multi-subscription in the template
    this.subscriptions.push(
      this.joinedChannel$.subscribe(jc => (this.joinedChannel = jc)),
    );
    this.subscriptions.push(
      this.selectedGuild$.subscribe(g => (this.selectedGuild = g)),
    );
    this.subscriptions.push(
      this.selectedChannel$.subscribe(c => (this.selectedChannel = c)),
    );
  }

  selectGuild(g: Guild) {
    this.selectedGuild$.next(g);
  }

  selectChannel(c: Channel) {
    this.selectedChannel$.next(c);
  }

  joinChannel() {
    this.loading = true;
    const guild = this.selectedGuild;
    const channel = this.selectedChannel;
    if (guild && channel) {
      this.subscriptions.push(
        this.discordService
          .joinChannel(guild.id, channel.id)
          .pipe(tap(() => (this.loading = false)))
          .subscribe(
            () => this.connection.next({guild, channel}),
            httpError =>
              this.handleError('failed to join channel in Discord', httpError),
          ),
      );
    }
  }

  leaveChannel() {
    this.loading = true;
    const guild = this.selectedGuild;
    if (guild) {
      this.subscriptions.push(
        this.discordService
          .leaveChannel(guild.id)
          .pipe(tap(() => (this.loading = false)))
          .subscribe(
            () => this.connection.next(null),
            httpError =>
              this.handleError('failed to leave channel in Discord', httpError),
          ),
      );
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handleError(message: string, err: any) {
    console.error(err);
    this.snackbar.open(
      `[${err?.statusText ?? 'ERR'}] ${message}: ${
        err?.error ?? err.message ?? 'no error message'
      }`,
      'Dismiss',
      {
        duration: 5000,
      },
    );
  }
}
