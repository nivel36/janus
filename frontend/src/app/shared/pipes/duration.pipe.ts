import { Pipe, PipeTransform } from '@angular/core';
import { Duration } from '../../features/timelogs/models/duration';

@Pipe({
	name: 'duration',
	standalone: true
})
export class DurationPipe implements PipeTransform {

	transform(duration?: Duration | null): string {
		if (!duration) {
			return '';
		}

		const { hours, minutes, seconds } = duration;

		if (seconds > 0) {
			return `${hours}h ${minutes}m ${seconds}s`;
		}

		if (minutes > 0) {
			return `${hours}h ${minutes}m`;
		}

		return `${hours}h`;
	}
}
