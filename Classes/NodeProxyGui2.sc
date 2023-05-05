NodeProxyGui2 {

	classvar <>defaultIgnoreParams = #[];

	var <>ignoreParams;
	var <window;

	var ndef;
	var params;
	var sliderDict;

	var play, fadeTime, numChannels, ndefRate, volslider, volvalueBox;
	var header, parameterSection;

	var font, headerFont;

	var ndefChangedFunc, specChangedFunc;

	// this is a normal constructor method
	*new { | nodeproxy, limitUpdateRate = 0 |
		^super.new.init(nodeproxy, limitUpdateRate)
	}

	init { | nodeproxy, limitUpdateRate |
		ndef = nodeproxy;

		this.initFonts();

		params = IdentityDictionary.new();
		sliderDict = IdentityDictionary.new();

		window = Window.new(ndef.key);
		window.layout = VLayout(
			this.makeInfoSection(),
			this.makeTransportSection(),
			// parameterSection gets added here in makeParameterSection
		);

		window.view.children.do{ | x | x.font = font };
		header.font = headerFont;

		this.setUpDependencies(limitUpdateRate.max(0));

		this.makeParameterSection();

		window.front;
	}

	setUpDependencies { | limitUpdateRate |
		var limitOrder, limitDict, limitScheduler;
		var specAddedFunc;

		specAddedFunc = { | obj ...args |
			var key, spec;
			if(args[0] == \add, {
				key = args[1][0];
				spec = args[1][1];
				if(params[key].notNil and:{ params[key] != spec }, {
					{ this.setUpParameters() }.defer;
				})
			})
		};

		specChangedFunc = { | obj ...args |
			{ this.setUpParameters() }.defer;
		};

		ndefChangedFunc = if(limitUpdateRate > 0, {

			limitOrder = OrderedIdentitySet.new(8);
			limitDict = IdentityDictionary.new();
			limitScheduler = SkipJack({
				if(limitOrder.size > 0, {
					limitOrder.do{ | key | this.ndefChanged(*limitDict[key]) };
					limitOrder.clear;
					limitDict.clear;
				});
			}, limitUpdateRate, clock: AppClock);
			{ | obj ...args |
				var key = args[0];
				if(key == \set, {
					key = (key ++ args[1][0]).asSymbol;
				});
				limitOrder.add(key);
				limitDict.put(key, args)
			}

		}, {

			{ | obj ...args | { this.ndefChanged(*args) }.defer }
		});

		Spec.addDependant(specAddedFunc);
		ndef.addDependant(ndefChangedFunc);
		if(ndef.monitor.notNil, {
			ndef.monitor.addDependant(ndefChangedFunc)
		});

		window.onClose = {
			limitScheduler.stop;
			ndef.monitor.removeDependant(ndefChangedFunc);
			ndef.removeDependant(ndefChangedFunc);
			Spec.removeDependant(specAddedFunc);
			params.do{ | spec | spec.removeDependant(specChangedFunc) };
		};
	}

	ndefChanged { | what, args |
		var key, val, spec;
		switch(what,
			\set, {
				key = args[0];
				if(key == \fadeTime, {
					fadeTime.value = args[1]
				}, {
					if(params[key].notNil, {
						val = args[1];
						spec = params[key].value;
						sliderDict[key][\numBox].value_(spec.constrain(val));
						sliderDict[key][\slider].value_(spec.unmap(val));
					})
				})
			},
			\play, { play.value_(1) },
			\stop, { play.value_(0) },
			\vol, {
				val = args[0];
				volvalueBox.value_(val.max(0.0));
				volslider.value_(val);
			},
			\bus, { numChannels.string = "channels: %".format(args.numChannels) },
			\rebuild, { ndefRate.string = "rate: %".format(ndef.rate) },
			\monitor, { ndef.monitor.addDependant(ndefChangedFunc) },
			\source, { this.makeParameterSection() },
		)
	}

	makeInfoSection {
		var fadeTimeLabel;

		fadeTimeLabel = StaticText.new()
		.string_("fadeTime:");

		fadeTime = NumberBox.new()
		.clipLo_(0.0)
		.decimals_(2)
		.scroll_step_(0.1) // mouse
		.step_(0.1)        // keys
		.value_(ndef.fadeTime)
		.action_({ | obj |
			ndef.fadeTime = obj.value;
		});

		numChannels = StaticText.new()
		.string_("channels: %".format(ndef.numChannels));

		ndefRate = StaticText.new()
		.string_("rate: %".format(ndef.rate));

		header = StaticText.new().string_(ndef.key);

		^VLayout(
			header,
			HLayout(fadeTimeLabel, [fadeTime, a: \left], numChannels, ndefRate),
		)
	}

	makeTransportSection {
		var clear, send, scope, free, popup;

		play = Button.new()
		.states_([
			["play"],
			["stop", Color.black, Color.grey(0.5, 0.5)]
		])
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
		});

		send = Button.new()
		.states_(#[
			["send"]
		])
		.action_({ | obj |
			ndef.send
		});

		scope = Button.new()
		.states_(#[
			["scope"]
		])
		.action_({ | obj |
			ndef.scope
		});

		free = Button.new()
		.states_(#[
			["free"]
		])
		.action_({ | obj |
			ndef.free;
		});

		popup = PopUpMenu()
		.allowsReselection_(true)
		.items_(#[
			"defaults",
			"randomize parameters",
			"vary parameters",
			"document",
			"post",
		])
		.action_({ | obj |
			switch(obj.value,
				0, { ndef.setDefaults() },
				1, { this.randomize() },
				2, { this.vary() },
				3, { ndef.document },
				4, { ndef.asCode.postln },
			);
		})
		.keyDownAction_({ | obj, char |
			if(char == Char.ret, {
				obj.doAction
			})
		})
		.canFocus_(true)
		.fixedWidth_(25);

		^HLayout(
			play, clear, free, scope, send, popup
		)
	}

	makeParameterSection {
		var ndefKeys = ndef.controlKeys;
		var paramKeys = params.keys;

		if(paramKeys.size != ndefKeys.size or: {
			paramKeys.includesAll(ndefKeys).not
		}, {
			this.setUpParameters();
		});
	}

	setUpParameters {
		params.do{ | spec | spec.removeDependant(specChangedFunc) };
		params.clear;

		ndef.controlKeys.do{ | paramname |
			var spec = (Spec.specs.at(paramname) ?? { ndef.specs.at(paramname) }).asSpec;
			//"Spec for paramname %: %".format(paramname, spec).postln;
			spec.addDependant(specChangedFunc);
			params.put(paramname, spec)
		};

		if(parameterSection.notNil, { parameterSection.close });
		parameterSection = this.makeSliders();
		parameterSection.resizeToHint;
		if(parameterSection.bounds.height > (Window.availableBounds.height * 0.5), {
			parameterSection = ScrollView().canvas_(parameterSection);
		});
		window.layout.add(parameterSection, 1);

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
		.string_("vol");

		// Value box
		volvalueBox = NumberBox.new()
		.decimals_(4)
		.action_({ | obj |
			obj.value = obj.value.clip(0.0, 1.0);
			volslider.value_(obj.value);
			ndef.vol_(obj.value);
		})
		.value_(ndef.vol);

		volLayout = HLayout(
			[vollabel, s: 1],
			[volvalueBox, s: 1],
			[volslider, s: 4],
		);
		view.layout.add(volLayout);

		sliderDict.clear;
		params.sortedKeysValuesDo{ | pName, spec |
			var paramVal, slider, label, valueBox, sliderLayout;

			// Only make slider for this parameter, if it is a number
			ndef.get(pName).isKindOf(SimpleNumber).if{

				// Slider
				paramVal = ndef.get(pName);
				slider = Slider.new()
				// .step_(spec.step)
				.orientation_(\horizontal)
				.value_(spec.unmap(paramVal))
				.action_({ | obj |
					var mappedVal = spec.map(obj.value);
					valueBox.value = mappedVal;
					ndef.set(pName, mappedVal);
				});

				// Label
				label = StaticText.new
				.string_(pName);

				// Value box
				valueBox = NumberBox.new()
				.action_({ | obj |
					var mappedVal = spec.unmap(obj.value);
					slider.value_(mappedVal);
					ndef.set(pName, obj.value);
				})
				.decimals_(4)
				.value_(spec.constrain(paramVal));

				// Slider Layout
				sliderLayout = HLayout(
					[label, s: 1],
					[valueBox, s: 1],
					[slider, s: 4],
				);

				// This is used to be able to fetch the sliders later when they need to be updated
				sliderDict.put(pName, (slider: slider, numBox: valueBox));

				view.layout.add(sliderLayout)
			}
		};

		view.children.do{ | x | x.font = font };

		^view
	}

	initFonts {
		var fontSize, headerFontSize;

		fontSize = 14;
		headerFontSize = fontSize * 2;

		headerFont = Font.sansSerif(headerFontSize, bold: true, italic: false);
		font = Font.monospace(fontSize, bold: false, italic: false);
	}

	randomize { | randmin = 0.0, randmax = 1.0, except = #[] |
		except = defaultIgnoreParams ++ ignoreParams ++ except;
		ndef.randomizeAllParamsMapped(randmin, randmax, except)
	}

	vary { | deviation = 0.1, except = #[] |
		except = defaultIgnoreParams ++ ignoreParams ++ except;
		ndef.varyAllParamsMapped(deviation, except)
	}

	close {
		^window.close()
	}

}
