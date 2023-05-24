# NodeProxyGui2

![ndefgui2 in action](ndefgui2.gif)

An alternative to SuperCollider's built-in (and partly broken) Ndef.gui method.

Usage is very simple. Instead of calling `gui` on a nodeproxy object you simply call `gui2`:
```supercollider
(
Ndef(\abc, {|freq=100, amp=0.5, pan=0| 
	Pan2.ar(SinOsc.ar(freq), pos: pan, level: amp)
});
)

g = Ndef(\abc).gui2;
g.close;

// dont randomize/vary ignored parameters
NodeProxyGui2.defaultIgnoreParams_([\amp, "freq*", "*F*"]) // wildcard freq args

(
f = 9;
Ndef('ignore', {
    var a = [\a.kr(0.5), \aa.kr(1/3)];
    var t1 = \a_thing.kr(100), s2 = \a_nother_thing_y.kr(200);
    var freqs = f.collect{|n|n=n+1;("freq"++(n)).asSymbol.kr(100*n)};
    Splay.ar(SinOscFB.ar(freqs, \sinFeedback.kr(1/5), mul:'amp'.kr(0.1)));
});
f.do{|n|
    n=n+1;
    Spec.add(("freq"++(n)).asSymbol.postln, ControlSpec( 100.0, 14000.0, \exp, 0, 50, "Hz") );
};
g = Ndef('ignore').gui2;
g.ignoreParams = ["a", "*thing.*"]; // note .*
)

g.randomize;
g.vary;
```

## Design goals

The reason for this package's existence is to experiment with fixing some issues that are currently in the built in NodeProxy GUI for SuperCollider. These aren't all fixed, but include:

* User scaleable layout – have GUI elements resize with window size.
* Readable text and values – the font size should be readable on modern screens, and labels should not overflow
* Add small extra features like allowing parameters to be randomized
* Use sensible defaults for parameter Specs – if no Spec is defined with the NodeProxy, it should fall back to `[0.0, 1.0, \lin].asSpec` and not some random guess. Also, use a search hierarchy: Look in the NodeProxies specs, then in the global `ControlSpec.specs` and if both those fails, fall back to a default.
* A neutral look and feel. It shouldn't try to be clever with colors, unless necessary.

## Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/nodeproxygui2")`
