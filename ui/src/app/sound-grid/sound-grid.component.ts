import {Component, OnDestroy, OnInit} from '@angular/core';
import {Sound} from '../models/sound';
import {SoundManagerService} from '../services/sound-manager.service';
import {ActivatedRoute, Router} from '@angular/router';
import {from, Observable, Subscription} from 'rxjs';
import {Connection} from '../models/discord';
import {tap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';
import {DiscordService} from '../services/discord.service';

@Component({
  selector: 'app-sound-grid',
  templateUrl: './sound-grid.component.html',
  styleUrls: ['./sound-grid.component.scss'],
})
export class SoundGridComponent implements OnInit, OnDestroy {
  sounds: Observable<Array<Sound>>;
  readonly soundQueryParamKey = 'play_sound';
  isConnected: Observable<boolean>;
  private subscriptions: Subscription[] = [];
  private readonly initSound: string | undefined;
  private activeConnection: Connection | undefined;

  constructor(
    private soundManager: SoundManagerService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private snackbar: MatSnackBar,
    private discordService: DiscordService,
  ) {
    this.sounds = this.soundManager.getSounds();
    this.initSound = this.activatedRoute.snapshot.queryParamMap.get(
      this.soundQueryParamKey,
    );

    this.isConnected = this.discordService.isConnected('1');
  }

  async ngOnInit() {
    if (this.initSound) {
      this.subscriptions.push(
        this.soundManager
          .playSound(
            this.initSound,
            this.activeConnection?.guild,
            this.activeConnection?.channel,
          )
          .subscribe(
            result => console.log(result),
            err => this.handleError(err),
          ),
      );
    }
  }

  onConnection(connection: Connection | undefined) {
    this.activeConnection = connection;
  }

  async onSoundSelected(s: Sound) {
    this.subscriptions.push(
      this.soundManager
        .playSound(
          s.fileName,
          this.activeConnection?.guild,
          this.activeConnection?.channel,
        )
        .pipe(
          tap(() =>
            from(
              this.router.navigate([], {
                relativeTo: this.activatedRoute,
                queryParams: {
                  [this.soundQueryParamKey]: s.fileName,
                },
              }),
            ),
          ),
        )
        .subscribe(
          result => console.log(result),
          err => this.handleError(err),
        ),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private handleError(err: any) {
    console.error(err);
    const errorMessage = err?.error ?? err.message ?? 'no error message';
    this.snackbar.open(
      `[${err?.statusText ?? 'ERR'}] failed to play sound: ${errorMessage}`,
      'Dismiss',
      {
        duration: 5000,
      },
    );
  }
}
