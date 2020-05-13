import {Component, EventEmitter, Input, Output} from '@angular/core';
import {Sound} from '../../models/sound';
import {SoundManagerService} from '../../services/sound-manager.service';
import {ColorService} from '../../services/color.service';

@Component({
  selector: 'app-sound-tile',
  templateUrl: './sound-tile.component.html',
  styleUrls: ['./sound-tile.component.scss'],
})
export class SoundTileComponent {
  @Input() sound: Sound;
  @Output() selected = new EventEmitter<Sound>();
  randomBackground: string;
  randomForeground: string;

  constructor(
    private soundManager: SoundManagerService,
    private colorService: ColorService,
  ) {
    const randomColor = this.colorService.getRandomMaterialColor();
    this.randomBackground = randomColor.background;
    this.randomForeground = randomColor.foreground;
  }

  async onSoundSelected() {
    this.selected.emit(this.sound);
  }
}
