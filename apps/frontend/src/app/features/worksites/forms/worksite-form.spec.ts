/**
 * SPDX-License-Identifier: Apache-2.0
 */
import { FormBuilder } from '@angular/forms';
import { describe, expect, it } from 'vitest';

import {
  createWorksiteFormControls,
  DEFAULT_WORKSITE_TIME_ZONE,
  WORKSITE_SCOPE_OPTIONS,
} from './worksite-form';

describe('createWorksiteFormControls', () => {
  const formBuilder = new FormBuilder();

  it('uses the same initial values for create and edit forms', () => {
    const createControls = createWorksiteFormControls(formBuilder);
    const editControls = createWorksiteFormControls(formBuilder);

    expect(values(createControls)).toEqual({
      name: '',
      timeZone: DEFAULT_WORKSITE_TIME_ZONE,
      scope: 'GLOBAL',
      description: null,
      address: null,
    });
    expect(values(editControls)).toEqual(values(createControls));
    expect(WORKSITE_SCOPE_OPTIONS.map((option) => option.value)).toEqual(['GLOBAL', 'ASSIGNED']);
  });

  it('applies the same validation rules to create and edit fields', () => {
    const createControls = createWorksiteFormControls(formBuilder);
    const editControls = createWorksiteFormControls(formBuilder);

    for (const controls of [createControls, editControls]) {
      controls.name.setValue('');
      expect(controls.name.errors).toEqual(expect.objectContaining({ required: true }));

      controls.name.setValue('invalid/name');
      expect(controls.name.errors).toEqual(expect.objectContaining({ pattern: expect.anything() }));

      controls.name.setValue('a'.repeat(251));
      expect(controls.name.errors).toEqual(
        expect.objectContaining({ maxlength: expect.anything() }),
      );

      controls.timeZone.setValue(null);
      expect(controls.timeZone.errors).toEqual(expect.objectContaining({ required: true }));

      controls.scope.setValue(null as never);
      expect(controls.scope.errors).toEqual(expect.objectContaining({ required: true }));

      controls.description.setValue('a'.repeat(501));
      expect(controls.description.errors).toEqual(
        expect.objectContaining({ maxlength: expect.anything() }),
      );

      controls.address.setValue('a'.repeat(501));
      expect(controls.address.errors).toEqual(
        expect.objectContaining({ maxlength: expect.anything() }),
      );
    }
  });
});

function values(controls: ReturnType<typeof createWorksiteFormControls>) {
  return Object.fromEntries(
    Object.entries(controls).map(([name, control]) => [name, control.value]),
  );
}
