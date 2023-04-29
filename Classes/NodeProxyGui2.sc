NodeProxyGui2 {
	classvar <>ignoreParams;
	const <defaultIgnoreParams = #[\numChannels, \vol, \numOuts, \buffer];

	var ndef, rateLabel, ndefrate, info, window, sliders, transport, play, clear, send, free, numChannels, numChannelsLabel, name, scope, fadeTime, fadeTimeLabel, header, randomizeParams, volslider, vollabel, volvalueBox, defaultsButton;

	var sliderDict;

	var fontSize;
	var headerFontSize;
	var headerFont;

	var labelFont;
	var infolabelFont;

	var valueFont;
	var buttonFont;

	var params;
	var paramNames;


	// this is a normal constructor method
	*new { | nodeproxy, updateRate = 0.5 |
		^super.new.init(nodeproxy, updateRate);
	}

	init { | nodeproxy, updateRate |

		ignoreParams = Set.newFrom(defaultIgnoreParams);
		ndef = nodeproxy;
		this.initFonts();
		params = IdentityDictionary.new();
		sliderDict = IdentityDictionary.new();

		window = Window.new(ndef.key);

		// Get parameter names and make sliders
		paramNames = ndef.controlKeys;

		// Populate param dict
		paramNames.do{ | paramname |
			var spec = (Spec.specs.at(paramname) ?? {ndef.specs.at(paramname)}).asSpec;
			//"Spec for paramname %: %".format(paramname, spec).postln;

			// @TODO this should remove no longer used params (and sliders)
			params.put(paramname, spec)
		};

		this.makeSliders();
		this.makeTransportSection();
		this.makeInfoSection();

		window.layout = VLayout(
			[info, s: 1],
			[transport, s:1],
			*sliders
		);

		window.resizeToHint.front;

	}

	makeInfoSection {
		var infoFunc = { | obj ...args |
			switch(args[0],
				\set, {
					switch(args[1][0],
						\fadeTime, {
							{fadeTime.value = ndef.fadeTime}.defer;
						},
					);
				},
				\bus, {
					{numChannels.value = args[1].numChannels}.defer;
				},
				\rebuild, {
					{ndefrate.value = if(ndef.rate == \audio, 0, 1)}.defer;
		})};
		ndef.addDependant(infoFunc);
		window.onClose = {
			ndef.removeDependant(infoFunc);
		};

		numChannelsLabel = StaticText.new()
		.string_("channels:")
		.font_(infolabelFont);

		numChannels = NumberBox.new()
		.value_(ndef.numChannels)
		// .decimals_(0)
		.action_({ | obj |
			ndef.mold(obj.value.asInteger, ndef.rate)
		})
		.font_(valueFont);

		// numChannels = StaticText.new()
		// .string_(ndef.numChannels)
		// .font_(valueFont);

		rateLabel = StaticText.new()
		.string_("rate:")
		.font_(infolabelFont);

		// ndefrate = StaticText.new()
		// .string_(ndef.rate)
		// .font_(valueFont);

		ndefrate = Button.new()
		.states_(#[
			["audio"],
			["control"]
		])
		.action_({ | obj |
			var index = obj.value;
			var newrate = obj.states[index][0].asSymbol;
			ndef.mold(ndef.numChannels, newrate)
		})
		.font_(infolabelFont);

		fadeTimeLabel = StaticText.new()
		.string_("fadeTime:")
		.font_(infolabelFont);

		fadeTime = NumberBox.new()
		.clipLo_(0.0)
		.decimals_(4)
		.scroll_step_(0.1) // mouse
		.step_(0.1)        // keys
		.value_(ndef.fadeTime)
		.action_({ | obj |
			ndef.fadeTime = obj.value;
		})
		.font_(valueFont);

		header = StaticText.new().string_(ndef.key).font_(headerFont);
		info = VLayout(
			header,
			HLayout([numChannelsLabel, s:1], [numChannels, s: 1, a: \left]),
			HLayout([rateLabel, s: 1], [ndefrate, s:1, a: \left]),
			HLayout([fadeTimeLabel, s: 1], [fadeTime, s:1, a: \left]),
		);
	}

	makeTransportSection {
		var playFunc = { | obj ...args |
			switch(args[0],
				\play, {{play.value_(1)}.defer},
				\stop, {{play.value_(0)}.defer}
		)};
		play = Button.new()
		.states_(#[
			["play"],
			["stop"]
		])
		.font_(buttonFont)
		.action_({ | obj |
			if(obj.value == 1, {
				ndef.play;
			}, {
				ndef.stop
			})
		})
		.value_(ndef.isMonitoring.binaryValue)
		.onClose_({ ndef.removeDependant(playFunc) });
		ndef.addDependant(playFunc);

		clear = Button.new()
		.states_(#[
			["clear"]
		])
		.action_({ | obj |
			ndef.clear
		})
		.font_(buttonFont);

		send = Button.new()
		.states_(#[
			["send"]
		])
		.action_({|obj|
			ndef.send
		})
		.font_(buttonFont);

		scope = Button.new()
		.states_(#[
			["scope"]
		])
		.action_({ | obj |
			ndef.scope
		})
		.font_(buttonFont);

		free = Button.new()
		.states_(#[
			["free"]
		])
		.action_({ | obj |

			// if(obj.value == 1, {
			ndef.free;
			// })

		})
		.font_(buttonFont);

		randomizeParams = Button.new()
		.states_(#[
			["randomize"]
		])
		.action_({ | obj |
			this.randomize()
		})
		.font_(buttonFont);

		defaultsButton = Button.new()
		.states_(#[
			["defaults"]
		])
		.action_({ | obj |
			ndef.setDefaults()
		})
		.font_(buttonFont);


		// Create layout
		transport = HLayout(
			play, clear, free, scope, send, randomizeParams, defaultsButton
		)
	}

	randomize {
		ndef.randomizeAllParamsMapped(0.0, 1.0);
		// sliderDict.keysValuesDo{ | name, dict |
		// 	dict[\slider].valueAction_(rrand(0.0,1.0))
		// }
	}

	makeSliders {
		var slidersFunc = { | obj ...args |
			var key, val, spec;
			switch(args[0],
				\set, {
					key = args[1][0];
					if(params[key].notNil, {
						val = args[1][1];
						spec = params[key].value;
						{
							sliderDict[key][\numBox].value_(spec.constrain(val));
							sliderDict[key][\slider].value_(spec.unmap(val));
						}.defer;
					});
			})
		};
		ndef.addDependant(slidersFunc);
		window.onClose = {
			ndef.removeDependant(slidersFunc);
		};

		sliders = [];

		params.sortedKeysValuesDo{ | pName, spec |

			// Only make slider for this parameter, if it is a number
			ndef.get(pName).isKindOf(SimpleNumber).if{

				// Slider
				var paramVal = ndef.get(pName);
				var slider = Slider.new()
				// .step_(spec.step)
				.orientation_(\horizontal)
				.value_(spec.unmap(paramVal))
				.action_({ | obj |
					var mappedVal = spec.map(obj.value);
					valueBox.value = mappedVal;
					ndef.set(pName, mappedVal);
				});

				// Label
				var label = StaticText.new
				.string_(pName)
				.font_(labelFont);

				// Value box
				var valueBox = NumberBox.new()
				.action_({ | obj |
					var mappedVal = spec.unmap(obj.value);
					slider.value_(mappedVal);
					ndef.set(pName, obj.value);
				})
				.decimals_(4)
				.value_(spec.constrain(paramVal))
				.font_(valueFont);

				// Slider Layout
				var sliderLayout = HLayout([label, s: 1], [valueBox, s:1], [slider, s: 4]);

				// This is used to be able to fetch the sliders later when they need to be updated
				sliderDict.put(pName, (slider: slider, numBox: valueBox));

				sliders = sliders.add(sliderLayout);

			}
		};

		volslider = Slider.new()
		.orientation_(\horizontal)
		.value_(ndef.vol)
		.action_({ | obj |
			ndef.vol_(obj.value);
			volvalueBox.value_(obj.value);
		});

		// Label
		vollabel = StaticText.new
		.string_("volume")
		.font_(labelFont);

		// Value box
		volvalueBox = NumberBox.new()
		.decimals_(4)
		.action_({ | obj |
			obj.value = obj.value.clip(0.0, 1.0);
			volslider.value_(obj.value);
			ndef.vol_(obj.value);
		})
		.value_(ndef.vol)
		.font_(valueFont);

		// Put volume slider at the top
		sliders = [HLayout([vollabel, s: 1], [volvalueBox, s:1], [volslider, s: 4]) ]++ sliders;
	}

	initFonts {
		fontSize = 14;
		headerFontSize = fontSize;
		headerFont = Font.sansSerif(headerFontSize, bold: true, italic: false);
		labelFont = Font.monospace(fontSize, bold: false, italic: false);
		infolabelFont = Font.monospace(fontSize, bold: false, italic: false);
		valueFont = Font.monospace(fontSize, bold: false, italic: false);
		buttonFont = Font.monospace(fontSize, bold: false, italic: false);
	}

}
