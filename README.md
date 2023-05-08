# NodeProxyGui2

![ndefgui2 in action](ndefgui2.gif)

An alternative to SuperCollider's built-in (and partly broken) Ndef.gui method.

Usage is very simple. Instead of calling `gui` on a nodeproxy object you simply call `gui2`:
```supercollider
(
Ndef(\guigui, {|freq=100, amp=0.5, pan=0| 
	Pan2.ar(SinOsc.ar(freq), pos: pan, level: amp)
});
)

g = Ndef(\guigui).gui2;
g.close;

// dont randomize/vary ignored parameters
NodeProxyGui2.defaultIgnoreParams_(["freq*"]) // wildcard freq args

(
Ndef('ignore1', {
    var a = \a.kr(0.5), b = \b.kr(1/3);
    var t1 = \athing.kr(100), s2 = \thingy.kr(200);
    SinOsc.ar(['freq1'.kr(123), 'freq2'.kr(223)], mul:'amp'.kr(0.1));
});
Ndef('ignore2', {
    var a = \a.kr(0.5), b = \b.kr(1/3);
    var s1 = 'some1'.kr(100), s2 = \sometwo.kr(200);
    SinOsc.ar(['freq1'.kr(123), 'freq2'.kr(223)], mul:'amp'.kr(0.1));
});
Spec.add(\freq1, ControlSpec( 100.0, 14000.0, \exp, 0, 70, "Hz") );
Spec.add(\freq2, ControlSpec( 100.0, 14000.0, \exp, 0, 4, "Hz") );
~g1 = Ndef('ignore1').gui2;
~g1.ignoreParams_(["some1"]);
~g2 = Ndef('ignore2').gui2;
~g2.ignoreParams_(["a", "thing*"]);
)
(
~g1.randomize;
~g2.randomize;
)
(
~g1.vary;
~g2.vary;
)



(
Ndef('hej', {
    var a = \yoyo.kr(0.5);
    SinOsc.ar(['freq1'.kr(123), 'freq2'.kr(223)], mul:'amp'.kr(0.1));
});
Spec.add(\freq1, ControlSpec( 100.0, 14000.0, \exp, 0, 70, "Hz") );
Spec.add(\freq2, ControlSpec( 100.0, 14000.0, \exp, 0, 4, "Hz") );
)
Ndef('hej').gui;
Ndef('hej').except;


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
