import {Component, EventEmitter, Output} from '@angular/core';

@Component({
  selector: 'app-youtube',
  templateUrl: './youtube.component.html',
  styleUrls: ['./youtube.component.scss'],
})
export class YoutubeComponent {
  @Output() selected = new EventEmitter<string>();
  youtubeLink: string | undefined;

  constructor() {
  }

  async onPlayOnDiscordClick() {
    this.selected.emit(this.youtubeLink);
  }
}
