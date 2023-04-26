/*

TODO:
- Seperate state in to one dict
- Every time the update routine polls the values of ndef, it should compare them to the state and only if it is new update it

*/
NodeProxyGui2 {
	classvar <>numDigits = 4;

	var ndef, rateLabel, ndefrate, info, window, sliders, transport, play, clear, send, free, numChannels, numChannelsLabel, name, scope, fade, fadeKnob, fadeLabel, header, randomizeParams, volslider, vollabel, volvalueBox;

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
		var updateRoutine, restartUpdateFunc;

		ndef = nodeproxy;
		this.initFonts();
		params = IdentityDictionary.new();
		sliderDict = IdentityDictionary.new();

		window = Window.new(ndef.key);
		// Get parameter names and make sliders
		this.sync();
		this.makeSliders();
		this.makeTransportSection();
		this.makeInfoSection();

		window.layout = VLayout(
			[info, s: 1],
			[transport, s:1],
			*sliders
		);

		restartUpdateFunc = { updateRoutine = this.makeUpdateRoutine(updateRate) };
		restartUpdateFunc.value;

		// Keep alive even though user presses cmd-period
		CmdPeriod.add(restartUpdateFunc);

		window.onClose = {
			updateRoutine.stop;
			CmdPeriod.remove(restartUpdateFunc);
		};
		window.front;

	}

	makeUpdateRoutine { | updateRate |
		^Routine({
			loop{
				updateRate.wait; this.updateAll()
			}
		}).play(AppClock)
	}

	makeInfoSection {

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
		.states_([
			["audio"],
			["control"]
		])
		.action_({ | obj |
			var index = obj.value;
			var newrate = obj.states[index][0].asSymbol;
			ndef.mold(ndef.numChannels, newrate)
		})
		.font_(infolabelFont);

		/*
		* Fade
		*/
		fadeLabel = StaticText.new()
		.string_("fade:")
		.font_(infolabelFont);

		fade = NumberBox.new()
		.step_(0.01)
		.clipLo_(0.0)
		.decimals_(numDigits)
		.scroll_step_(0.01)
		// .clipHi_(100.0)
		.value_(ndef.fadeTime)
		.action_({ | obj |
			var val = obj.value;
			ndef.fadeTime = val;
		})
		.increment(0.01)
		.step_(0.1)
		.font_(valueFont);

		header = StaticText.new().string_(ndef.key).font_(headerFont);
		info = VLayout(
			header,
			HLayout([numChannelsLabel, s:1], [numChannels, s: 1, a: \left]),
			HLayout([rateLabel, s: 1], [ndefrate, s:1, a: \left]),
			HLayout([fadeLabel, s: 1], [fade, s:1, a: \left]),
		);

	}

	makeTransportSection {
		play = Button.new()
		.states_([
			["play"],
			["stop"]
		])
		.action_({ | obj |
			var val = obj.value;

			if(val == 1, {
				ndef.play;
			}, {
				ndef.stop
			})

		})
		.font_(buttonFont);

		// Get current play state
		ndef.isPlaying.if({
			play.value_(1)
		}, {
			play.value_(0)
		});

		clear = Button.new()
		.states_([
			["clear"]
		])
		.action_({ | obj |
			ndef.clear
		})
		.font_(buttonFont);

        send = Button.new()
        .states_([
            ["send"]
        ])
        .action_({|obj|
            ndef.send
        })
        .font_(buttonFont);

		scope = Button.new()
		.states_([
			["scope"]
		])
		.action_({ | obj |
			ndef.scope
		})
		.font_(buttonFont);

		free = Button.new()
		.states_([
			["free"]
		])
		.action_({ | obj |
			var val = obj.value;

			// if(val == 1, {
			ndef.free;
			// })

		})
		.font_(buttonFont);

		randomizeParams = Button.new()
		.states_([
			["randomize"]
		])
		.action_({ | obj |
			this.randomize()
		})
		.font_(buttonFont);

		// Create layout
		transport = HLayout(
			play, clear, free, scope, send, randomizeParams

		)

	}

	randomize {
		sliderDict.keysValuesDo{ | name, dict |
			dict[\slider].valueAction_(rrand(0.0,1.0))
		}
	}

	makeSliders {

		sliders = [];

		params.sortedKeysValuesDo{ | pName, spec |

			// Slider
			// TODO: What about Buffer etc?
			var paramVal = if(ndef.get(pName).isKindOf(SimpleNumber), {ndef.get(pName)}, {0});
			var slider = Slider.new()
			.step_(spec.step)
			.orientation_(\horizontal)
			.value_(paramVal)
			.action_({ | obj |
				var sliderVal = obj.value;
				var mappedVal = spec.map(sliderVal);
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
				var val = obj.value;
				var mappedVal = spec.unmap(val);
				slider.value_(mappedVal);
				ndef.set(pName, val);
			})
			.decimals_(numDigits)
			.value_(paramVal)
			.font_(valueFont);

			// Slider Layout
			var sliderLayout = HLayout([label, s: 1], [valueBox, s:1], [slider, s: 4]);

			// This is used to be able to fetch the sliders later when they need to be updated
			sliderDict.put(pName, (slider: slider, numBox: valueBox));

			sliders = sliders.add(sliderLayout);
		};


		volslider = Slider.new()
		.orientation_(\horizontal)
		.value_(ndef.vol)
		.action_({ | obj |
			var sliderVal = obj.value;
			ndef.vol_(sliderVal);
			volvalueBox.value_(sliderVal);
		});

		// Label
		vollabel = StaticText.new
		.string_("volume")
		.font_(labelFont);

		// Value box
		volvalueBox = NumberBox.new()
		.decimals_(numDigits)
		.action_({ | obj |
			var val = obj.value;
			volslider.value_(val);
			ndef.vol_(val);
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

	// Get latest info from NodeProxy and store in the gui object
	sync {
		paramNames = ndef.controlKeys;

		// Populate param dict
		paramNames.do{ | paramname |
			var spec = Spec.specs.at(paramname) ?? ndef.specs.at(paramname) ?? [0.0,1.0].asSpec;
			// "Spec for paramname %: %".format(paramname, spec).postln;

			// @TODO this should remove no longer used params (and sliders)
			params.put(paramname, spec)
		};

		// this.makeSliders();
		// window.refresh();
		// this.updateSliders();

	}

	// @TODO: Check if values are new before updating them in elements
	updateAll {
		this.sync();
		this.updateSliders();
		this.updateLabels();
		this.updateButtons();
	}

	updateButtons {
		if(ndef.isPlaying, {
			play.value_(1);
		}, {
			play.value_(0);
		})
	}

	updateLabels {
		numChannels.value_(ndef.numChannels);

		if(ndef.rate == \audio, {
			ndefrate.value_(0)
		}, {
			ndefrate.value_(1)
		});

		fade.value_(ndef.fadeTime);
	}

	updateSliders {
		params.keysValuesDo{ | paramname, spec |
			var ndefval = ndef.get(paramname);
			// TODO: What to do about Buffers and such?
			if(ndefval.isKindOf(SimpleNumber), {
				sliderDict[paramname][\slider].value_(spec.unmap(ndefval));
				sliderDict[paramname][\numBox].value_(ndefval);
			});
		}
	}


}

+ NodeProxy {
	randomizeAllParamsMapped{ | randmin = 0.0, randmax = 1.0 |
		var nd = this;
		var params = nd.controlKeys;

		params.do{ | param |
			var val = rrand(randmin, randmax);
			var spec = Spec.specs.at(param);
			spec = if(spec.isNil, { [0.0,1.0].asSpec }, { spec });
			val = spec.map(val);
			nd.set(param, val)
		}
	}

	gui2{
		NodeProxyGui2.new(this);
	}
}
