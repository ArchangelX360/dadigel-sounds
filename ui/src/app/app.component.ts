import {Component} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {filter, map} from 'rxjs/operators';
import {Observable} from 'rxjs';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  mainSectionId = 'main';
  mainSection: Observable<string>;

  constructor(private router: Router) {
    this.mainSection = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      filter(() => !this.router.url.endsWith(`#${this.mainSectionId}`)),
      map(() => `${this.router.url}#${this.mainSectionId}`),
    );
  }
}
