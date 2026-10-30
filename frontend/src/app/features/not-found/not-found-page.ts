import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { FR } from '../../core/i18n/fr';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink],
  template: `
    <div class="card">
      <h1>{{ text.notFound.title }}</h1>
      <p class="muted">{{ text.notFound.body }}</p>
      <a class="btn" routerLink="/dashboard">{{ text.notFound.home }}</a>
    </div>
  `,
  styles: `
    .card {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }
    h1 {
      margin: 0;
    }
  `,
})
export class NotFoundPage {
  protected readonly text = FR;
}
