import {Component, OnDestroy, OnInit, Output} from '@angular/core';
import {filter, flatMap, withLatestFrom} from 'rxjs/operators';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {BotStatus, Channel, Connection, Guild} from '../../models/discord';
import {DiscordService} from '../../services/discord.service';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-discord-integration',
  templateUrl: './discord-integration.component.html',
  styleUrls: ['./discord-integration.component.scss'],
})
export class DiscordIntegrationComponent implements OnInit, OnDestroy {
  @Output()
  readonly connection: BehaviorSubject<Connection | null> = new BehaviorSubject(
    null,
  );
  readonly guilds$: Observable<Guild[]>;
  readonly channels$: Observable<Channel[]>;

  selectedGuildId: string | null;
  botStatus: BotStatus | null;
  channelSwitching = false;

  private subscriptions: Subscription[] = [];

  private readonly selectedGuildId$: BehaviorSubject<string | null> = new BehaviorSubject(null);

  constructor(
    private discordService: DiscordService,
    private snackbar: MatSnackBar,
  ) {
    this.guilds$ = this.discordService.getGuilds();
    this.channels$ = this.selectedGuildId$.pipe(
      flatMap(gId => this.discordService.getChannels(gId)),
    );
  }

  ngOnInit(): void {
    // these subscriptions are made without "| async" to avoid
    // multi-subscription in the template
    this.subscriptions.push(
      this.selectedGuildId$
        .pipe(
          filter(gId => gId !== null),
          flatMap(guildId => this.discordService.getStatus(guildId)),
          withLatestFrom(this.selectedGuildId$),
        )
        .subscribe(([status, guildId]) => {
          this.selectedGuildId = guildId;
          this.botStatus = status;
          if (this.selectedGuildId && this.botStatus?.joinedChannel?.id) {
            this.connection.next({
              guildId,
              channelId: this.botStatus.joinedChannel.id,
            });
          } else {
            this.connection.next(null);
          }
        }),
    );
  }

  selectGuild(gId: string) {
    this.selectedGuildId$.next(gId);
  }

  joinChannel(channelId: string) {
    this.channelSwitching = true;
    const guildId = this.selectedGuildId;
    if (guildId && channelId) {
      this.subscriptions.push(
        this.discordService.joinChannel(guildId, channelId).subscribe(
          () => (this.channelSwitching = false),
          httpError =>
            this.handleError('failed to join channel in Discord', httpError),
        ),
      );
    }
  }

  leaveChannel() {
    this.channelSwitching = true;
    const guildId = this.selectedGuildId;
    if (guildId) {
      this.subscriptions.push(
        this.discordService.leaveChannel(guildId).subscribe(
          () => (this.channelSwitching = false),
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
