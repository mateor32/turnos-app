import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  imports: [RouterLink],
  selector: 'app-client',
  templateUrl: './client.component.html',
  styleUrl: './client.component.css',
})
export class ClientComponent {
  protected refreshTurn(): void {
    // Mock action until the queue service is connected.
  }
}
