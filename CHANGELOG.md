# Changelog

## [1.1.0] - 2024-03-21

### Added
- Google Cast Support
  - Added Chromecast integration
  - Cast button in player controls
  - Seamless playback transition between device and Cast
  - Cast status notifications
  - Support for all streaming formats on Cast devices

### Enhanced
- Improved Media Format Support
  - Added HLS streaming (.m3u8, .m3u, .hls)
  - Added DASH streaming (.mpd)
  - Added Smooth Streaming (.ism)
  - Added Transport Stream formats (.ts, .mts, .m2ts)
  - Added additional playlist formats (.pls, .asx, .xspf)

- Playback Control Improvements
  - Enhanced play/pause state management
  - Better buffering state handling
  - Improved error recovery
  - Smoother state transitions
  - Fixed edge cases in playback control

### Technical
- Added Cast dependencies
  - Google Play Services Cast Framework
  - MediaRouter support library
- Added CastOptionsProvider for Cast configuration
- Enhanced MIME type support for various formats
- Improved error handling and logging

### UI/UX
- Added Cast button with white tint
- Improved playback state visual feedback
- Better error messages and notifications
- Seamless Cast device selection dialog

### Fixed
- Play/pause state inconsistencies
- Buffering state visual feedback
- Media format detection issues
- Playback transition edge cases

## [1.0.0] - Initial Release
- Base video player functionality 