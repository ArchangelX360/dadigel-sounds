import {Component, EventEmitter, OnDestroy, Output} from '@angular/core';
import {Channel, Connection, Guild} from '../../models/discord';
import {Observable, Subscription} from 'rxjs';
import {DiscordService} from '../../services/discord.service';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-discord-integration',
  templateUrl: './discord-integration.component.html',
  styleUrls: ['./discord-integration.component.scss'],
})
export class DiscordIntegrationComponent implements OnDestroy {
  @Output() connection = new EventEmitter<Connection>();
  guilds: Observable<Array<Guild>>;
  channels: Observable<Array<Channel>>;
  selectedGuild: Guild;
  selectedChannel: Channel;
  // FIXME: transform into an Observable to handle connection drops
  activeConnection: Connection | undefined;

  private subscriptions: Subscription[] = [];

  constructor(
    private discordService: DiscordService,
    private snackbar: MatSnackBar,
  ) {
    this.guilds = this.discordService.getGuilds();
  }

  selectGuild(g: Guild) {
    this.selectedGuild = g;
    this.channels = this.discordService.getChannels(g.id);
  }

  selectChannel(c: Channel) {
    this.selectedChannel = c;
  }

  joinChannel(guild: Guild, channel: Channel) {
    this.subscriptions.push(
      this.discordService.joinChannel(guild.id, channel.id).subscribe(
        () => {
          this.activeConnection = {guild, channel};
          this.connection.emit(this.activeConnection);
        },
        httpError =>
          this.handleError('failed to join channel in Discord', httpError),
      ),
    );
  }

  leaveChannel(guild: Guild) {
    this.subscriptions.push(
      this.discordService.leaveChannel(guild.id).subscribe(
        () => {
          this.activeConnection = undefined;
          this.connection.emit(this.activeConnection);
        },
        httpError =>
          this.handleError('failed to leave channel in Discord', httpError),
      ),
    );
  }

  ngOnDestroy(): void {
    if (this.activeConnection && this.selectedGuild) {
      this.leaveChannel(this.selectedGuild);
    }
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
