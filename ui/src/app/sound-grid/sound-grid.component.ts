import {Component, OnDestroy, OnInit} from '@angular/core';
import {Sound} from '../models/sound';
import {SoundManagerService} from '../services/sound-manager.service';
import {ActivatedRoute, Router} from '@angular/router';
import {from, Observable, Subscription} from 'rxjs';
import {Connection} from '../models/discord';
import {tap} from 'rxjs/operators';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-sound-grid',
  templateUrl: './sound-grid.component.html',
  styleUrls: ['./sound-grid.component.scss'],
})
export class SoundGridComponent implements OnInit, OnDestroy {
  sounds: Observable<Array<Sound>>;
  readonly soundQueryParamKey = 'play_sound';
  private subscriptions: Subscription[] = [];
  private readonly initSound: string | undefined;
  private activeConnection: Connection | undefined;

  constructor(
    private soundManager: SoundManagerService,
    private router: Router,
    private activatedRoute: ActivatedRoute,
    private snackbar: MatSnackBar,
  ) {
    this.sounds = this.soundManager.getSounds();
    this.initSound = this.activatedRoute.snapshot.queryParamMap.get(
      this.soundQueryParamKey,
    );
  }

  async ngOnInit() {
    if (this.initSound) {
      this.subscriptions.push(
        this.soundManager
          .playSound(
            this.initSound,
            this.activeConnection?.guild,
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

  async onSoundSelected(soundIdentifier: string) {
    this.subscriptions.push(
      this.soundManager
        .playSound(
          soundIdentifier,
          this.activeConnection?.guild,
        )
        .pipe(
          tap(() =>
            from(
              this.router.navigate([], {
                relativeTo: this.activatedRoute,
                queryParams: {
                  [this.soundQueryParamKey]: soundIdentifier,
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
