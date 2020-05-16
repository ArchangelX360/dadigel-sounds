import {Component, OnDestroy, Output} from '@angular/core';
import {filter, flatMap, tap} from 'rxjs/operators';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {BotStatus, Channel, Connection, Guild} from '../../models/discord';
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

  selectedGuildId: string | null;
  selectedChannelId: string | null;
  botStatus: BotStatus | null;
  loading = false;

  private subscriptions: Subscription[] = [];

  private readonly selectedGuildId$: BehaviorSubject<string | null> = new BehaviorSubject(null);
  private readonly selectedChannelId$: BehaviorSubject<string | null> = new BehaviorSubject(null);

  constructor(
    private discordService: DiscordService,
    private snackbar: MatSnackBar,
  ) {
    this.guilds$ = this.discordService.getGuilds();
    this.channels$ = this.selectedGuildId$.pipe(
      flatMap(gId => this.discordService.getChannels(gId)),
    );

    // these subscriptions are made without "| async" to avoid
    // multi-subscription in the template
    this.subscriptions.push(
      this.selectedGuildId$
        .pipe(
          filter(gId => gId !== null),
          flatMap(gId => this.discordService.getStatus(gId)),
        )
        .subscribe(b => (this.botStatus = b)),
    );
    this.subscriptions.push(
      this.selectedGuildId$.subscribe(gId => (this.selectedGuildId = gId)),
    );
    this.subscriptions.push(
      this.selectedChannelId$.subscribe(cId => (this.selectedChannelId = cId)),
    );
  }

  selectGuild(gId: string) {
    this.selectedGuildId$.next(gId);
  }

  selectChannel(cId: string) {
    this.selectedChannelId$.next(cId);
  }

  joinChannel() {
    this.loading = true;
    const guildId = this.selectedGuildId;
    const channelId = this.selectedChannelId;
    if (guildId && channelId) {
      this.subscriptions.push(
        this.discordService
          .joinChannel(guildId, channelId)
          .pipe(tap(() => (this.loading = false)))
          .subscribe(
            () => this.connection.next({guildId, channelId}),
            httpError =>
              this.handleError('failed to join channel in Discord', httpError),
          ),
      );
    }
  }

  leaveChannel() {
    this.loading = true;
    const guildId = this.selectedGuildId;
    if (guildId) {
      this.subscriptions.push(
        this.discordService
          .leaveChannel(guildId)
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
