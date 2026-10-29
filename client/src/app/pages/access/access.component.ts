import { Component, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'app-access',
  templateUrl: './access.component.html',
  styleUrl: './access.component.css',
})
export class AccessComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly platformId = inject(PLATFORM_ID);
  protected readonly isAdmin = this.route.snapshot.data['role'] === 'admin';

  protected handleActionClick(): void {
    if (!this.isAdmin && isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('turnoId');
    }
  }
}
