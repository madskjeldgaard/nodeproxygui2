# TODO

- Make parameter list / slider section resize when new parameters are defined or old removed in Nodeproxy
- Write help file
- Rework ignoreParams -> global/local ignores (look up Ndef.all)
- Add local ignoreParam toggle to gui
- Add skin/palette selector
- NumberBox: clip values (ex. volume can easily be set to -123)
- Make A/B buttons to allow A/B testing two different parameter sets
- NodeProxyGui2: seperate state into dict (old todo)
- ignoreParams: impl a wildcard option e.g. "*feedback" so that all "MadsSuperfeedback", "MadsIsAGain", and alike will be ignored instead of the some what personal implementation as of now i.e. (3 lines down) <<// Does the parameter name end with one of the ignored parameters? If so, ignore it as well>> in `extNodeProxy` -> `prFilteredParams`
- gui2's _stop_ is "free" and _free_ is "stop" .. oh my `8=()`


## Add standard gui functionality

* Drag and drop values between `NumberBoxes`
* Drag and drop NodeProxies to NumberBoxes internally and externally between guis,

## Notes

* indentation
* 'clear' and 'free'.
