/*
 * Copyright 2026 Abel Ferrer Jiménez
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package es.nivel36.janus.api.v1.timelog;

/** Actions supported when finalizing a clock-out-without-clock-in event. */
public enum ClockOutWithoutClockInEventAction {
	RESOLVE,
	INVALIDATE
}
