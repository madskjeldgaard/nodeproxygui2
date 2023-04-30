NodeProxyGui2 {
	classvar <>ignoreParams;
	const <defaultIgnoreParams = #["numChannels", "vol", "numOuts", "buffer"];

	var ndef, ndefrate, window, play, numChannels, fadeTime, volslider, volvalueBox;
	var parameterSection;
	var sliderDict;

	var headerFont;

	var labelFont;
	var infolabelFont;

	var valueFont;
	var buttonFont;

	var params;
	var specUpdateFunc;

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
		window.layout = VLayout(
			[this.makeInfoSection(), s: 1],
			[this.makeTransportSection(), s: 1],
			//parameterSection gets added here in makeParameterSection
		);

		this.setUpDependencies();

		this.makeParameterSection();

		window.front;

	}

	setUpDependencies {

		var updateFunc, specAddFunc;

		updateFunc = { | obj ...args |
			{
				var key, val, spec;
				switch(args[0],
					\set, {
						key = args[1][0];
						if(key == \fadeTime, {
							fadeTime.value = ndef.fadeTime
						}, {
							if(params[key].notNil, {
								val = args[1][1];
								spec = params[key].value;
								sliderDict[key][\numBox].value_(spec.constrain(val));
								sliderDict[key][\slider].value_(spec.unmap(val));
							})
						})
					},
					\play, {play.value_(1)},
					\stop, {play.value_(0)},
					\vol, {
						val = args[1][0];
						volvalueBox.value_(val.max(0.0));
						volslider.value_(val);
					},
					\bus, {numChannels.value = args[1].numChannels},
					\monitor, {ndef.monitor.addDependant(updateFunc)},
					\rebuild, {ndefrate.value = if(ndef.rate == \audio, 0, 1)},
					\source, {this.makeParameterSection()},
				)
			}.defer
		};
		ndef.addDependant(updateFunc);

		specAddFunc = { | obj ...args |
			var key, spec;
			if(args[0] == \add, {
				key = args[1][0];
				spec = args[1][1];
				if(params[key].notNil and:{params[key] != spec}, {
					this.setUpParameters();
				})
			})
		};
		Spec.addDependant(specAddFunc);

		specUpdateFunc = { | obj ...args |
			this.setUpParameters();
		};

		window.onClose = {
			ndef.monitor.removeDependant(updateFunc);
			ndef.removeDependant(updateFunc);
			Spec.removeDependant(specAddFunc);
			params.do{|spec| spec.removeDependant(specUpdateFunc)};
		};
	}

	makeInfoSection {

		var numChannelsLabel, rateLabel, fadeTimeLabel, header;

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

		^VLayout(
			header,
			HLayout([numChannelsLabel, s: 1], [numChannels, s: 1, a: \left]),
			HLayout([rateLabel, s: 1], [ndefrate, s: 1, a: \left]),
			HLayout([fadeTimeLabel, s: 1], [fadeTime, s: 1, a: \left]),
		)
	}

	makeTransportSection {

		var clear, send, scope, free, randomizeParams, defaultsButton;

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
		.value_(ndef.isMonitoring.binaryValue);

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

		^HLayout(
			play, clear, free, scope, send, randomizeParams, defaultsButton
		)
	}

	makeParameterSection {

		var ndefKeys = ndef.controlKeys;
		var paramKeys = params.keys;

		if(paramKeys.size != ndefKeys.size or: {paramKeys.includesAll(ndefKeys).not}, {
			this.setUpParameters();
		});

	}

	setUpParameters {

		params.do{|spec| spec.removeDependant(specUpdateFunc)};
		params.clear;

		ndef.controlKeys.do{ | paramname |
			var spec = (Spec.specs.at(paramname) ?? {ndef.specs.at(paramname)}).asSpec;
			//"Spec for paramname %: %".format(paramname, spec).postln;
			spec.addDependant(specUpdateFunc);
			params.put(paramname, spec)
		};

		if(parameterSection.notNil, {parameterSection.close});
		parameterSection = this.makeSliders();
		window.layout.add(parameterSection);
		window.resizeToHint;

	}

	makeSliders {

		var view, vollabel, volLayout;

		view = View().layout_(VLayout());

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

		volLayout = HLayout([vollabel, s: 1], [volvalueBox, s: 1], [volslider, s: 4]);
		view.layout.add(volLayout);

		sliderDict.clear;
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
				var sliderLayout = HLayout([label, s: 1], [valueBox, s: 1], [slider, s: 4]);

				// This is used to be able to fetch the sliders later when they need to be updated
				sliderDict.put(pName, (slider: slider, numBox: valueBox));

				view.layout.add(sliderLayout)

			}
		};

		^view
	}

	initFonts {

		var fontSize, headerFontSize;

		fontSize = 14;
		headerFontSize = fontSize;
		headerFont = Font.sansSerif(headerFontSize, bold: true, italic: false);
		labelFont = Font.monospace(fontSize, bold: false, italic: false);
		infolabelFont = Font.monospace(fontSize, bold: false, italic: false);
		valueFont = Font.monospace(fontSize, bold: false, italic: false);
		buttonFont = Font.monospace(fontSize, bold: false, italic: false);
	}

	randomize {
		ndef.randomizeAllParamsMapped(0.0, 1.0);
		// sliderDict.keysValuesDo{ | name, dict |
		// 	dict[\slider].valueAction_(rrand(0.0,1.0))
		// }
	}

	vary {| deviation = 0.1 |
		ndef.varyAllParamsMapped(deviation)
	}

}
