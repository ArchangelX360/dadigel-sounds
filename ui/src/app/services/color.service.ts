import {Injectable} from '@angular/core';

@Injectable()
export class ColorService {
  black = 'rgba(0, 0, 0, 0.87)';
  colors = [
    {background: '#EF5350', foreground: this.black},
    {background: '#EC407A', foreground: this.black},
    {background: '#42A5F5', foreground: this.black},
    {background: '#29B6F6', foreground: this.black},
    {background: '#26C6DA', foreground: this.black},
    {background: '#26A69A', foreground: this.black},
    {background: '#66BB6A', foreground: this.black},
    {background: '#9CCC65', foreground: this.black},
    {background: '#D4E157', foreground: this.black},
    {background: '#FFEE58', foreground: this.black},
    {background: '#FFCA28', foreground: this.black},
    {background: '#FFA726', foreground: this.black},
    {background: '#FF7043', foreground: this.black},
    {background: '#BDBDBD', foreground: this.black},
    {background: '#78909C', foreground: this.black},
  ];

  getRandomMaterialColor() {
    const colorIndex = Math.floor(
      Math.random() * Math.floor(this.colors.length),
    );
    return this.colors[colorIndex];
  }
}
