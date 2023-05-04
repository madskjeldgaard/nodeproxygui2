# TODO

## Improvements
- Rework ignoreParams -> global/local ignores (look up Ndef.all)
- √ NumberBox: clip values (ex. volume can easily be set to -123). Kind of fixed but can still set .vol= -123 from code
- gui2's _stop_ is "free" and _free_ is "stop" .. oh my `8=()`
- add visual indicator (darker grey) on play button state
- Volume button placement (default + dynamic?)
- minimize function (or gui3)

## Bugs
- reducing the number of parameters should resize the window. (resizeToHint)

## Features 
- Make A/B buttons to allow A/B testing two different parameter sets
- Add local ignoreParam toggle to gui
- Add skin/palette selector
- Drag and drop values between `NumberBoxes`
- Drag and drop NodeProxies to NumberBoxes internally and externally between guis,
