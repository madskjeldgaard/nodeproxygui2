# TODO

- Make parameter list / slider section resize when new parameters are defined or old removed in Nodeproxy
- Write help file
- Rework ignoreParams -> global/local ignores (look up Ndef.all)
- Add local ignoreParam toggle to gui
- Add skin/palette selector
- √ NumberBox: clip values (ex. volume can easily be set to -123). Kind of fixed but can still set .vol= -123 from code
- Ndef(\yiyi).monitor.vol= 0.1 does not change volumeslider+numberbox. only Ndef(\yiyi).vol= 0.1 work for now
- Make A/B buttons to allow A/B testing two different parameter sets
- NodeProxyGui2: seperate state into dict (old todo)
- gui2's _stop_ is "free" and _free_ is "stop" .. oh my `8=()`
- add visual indicator (darker grey) on play button state
- Volume button placement (default + dynamic?)
- Volume button sync

## Add standard gui functionality

* Drag and drop values between `NumberBoxes`
* Drag and drop NodeProxies to NumberBoxes internally and externally between guis,

## Notes

* indentation
* 'clear' and 'free'.
