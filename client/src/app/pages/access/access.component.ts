import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'app-access',
  templateUrl: './access.component.html',
  styleUrl: './access.component.css',
})
export class AccessComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly isAdmin = this.route.snapshot.data['role'] === 'admin';
}
