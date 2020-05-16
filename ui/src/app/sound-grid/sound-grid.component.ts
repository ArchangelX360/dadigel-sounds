import {Component, OnDestroy} from '@angular/core';
import {Sound} from '../models/sound';
import {SoundManagerService} from '../services/sound-manager.service';
import {BehaviorSubject, Observable, Subscription} from 'rxjs';
import {Connection} from '../models/discord';
import {MatSnackBar} from '@angular/material/snack-bar';

@Component({
  selector: 'app-sound-grid',
  templateUrl: './sound-grid.component.html',
  styleUrls: ['./sound-grid.component.scss'],
})
export class SoundGridComponent implements OnDestroy {
  private subscriptions: Subscription[] = [];
  readonly sounds$: Observable<Sound[]>;
  private activeConnection$: BehaviorSubject<Connection | null>;

  constructor(
    private soundManager: SoundManagerService,
    private snackbar: MatSnackBar,
  ) {
    this.sounds$ = this.soundManager.getSounds();
    this.activeConnection$ = new BehaviorSubject(null);
  }

  onConnection(connection: Connection | null) {
    this.activeConnection$.next(connection);
  }

  async onSoundSelected(soundIdentifier: string) {
    this.subscriptions.push(
      this.soundManager
        .playSound(soundIdentifier, this.activeConnection$.value?.guild)
        .subscribe(
          m => console.log(m),
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
