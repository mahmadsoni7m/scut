# SONI EDITOR ULTRA 1.0.0
Offline-first Android editor (Kotlin, Compose, Media3, Room). Languages: Tajik (default), Russian, English.

## Status (honest)
- This source was written without network access: it has NOT been compiled, linted or tested yet. The first GitHub Actions run may reveal compile errors that need fixing. No APK exists yet.
- Single `:app` module (not multi-module) to reduce build risk. No Hilt (manual wiring).

## Implemented
- 8-tab navigation, dark Material 3 UI, language switch (persisted), Room project CRUD + autosave + reopen, missing-media handling.
- Video: import (SAF), preview, trim, brightness/contrast, H.264/AAC export via Media3 Transformer with progress + cancel, saved to MediaStore.
- Photo: import, brightness/contrast/saturation, rotate, flip, undo/redo, before/after, JPEG save (original untouched).
- Motion: keyframe engine (position, scale, rotation, opacity) with preview and unit tests.
- Audio: import, playback, volume.
- AI Studio: states honestly that no models are bundled.

## Not implemented
Multitrack timeline, split/merge/reorder, transitions, text/image/sticker overlays, speed/reverse, audio trim/waveform/recording, PNG/WebP export, resolution/FPS presets, masks, ONNX AI, UI/instrumented/DB tests.
