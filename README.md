# NodeProxyGui2

An alternative to SuperCollider's built-in (and partly broken) Ndef.gui method.

Usage is very simple. Instead of calling `gui` on a nodeproxy object you simply call `gui2`:
```supercollider
Ndef(\yiyi, {|freq=100, amp=0.5, pan=0| 
	Pan2.ar(SinOsc.ar(freq), pan: pan)
});

Ndef(\yiyi).gui2;
```

### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/nodeproxygui2")`
