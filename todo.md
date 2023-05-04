# TODO

## Improvements
- Slider placement for ".vol" (default + dynamic?)
- Minimize function (or gui3)

## Bugs
- PopUp stays open when tabbing away from sc application (bug in scqt)

## Features 
- Make A/B buttons to allow A/B testing two different parameter sets
- Add skin/palette selector
- Drag and drop values between `NumberBoxes`
- Drag and drop NodeProxies to NumberBoxes internally and externally between guis,

## Needed before v1.0 (release)
- Fix "Type Handling" -> args/namedControls which is not a SimpleNumber.
  Dynamic change of NumberBox - StaticText? remove slider on Buffer usage? And so on. These questions.
  Add Array containing allowed Abstract types?
  Do it the way std .gui does it?
- Decide which toggle ndef state logic variant should be the gui's start/end sound button (`play/stop`, `play/free`, `play/stop(fadeTime)`)
- New gif in README
- v1.0 in quarkfile
