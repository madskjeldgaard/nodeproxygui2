NodeProxyGui2 {

	classvar <>defaultIgnoreParams = #[];

	var <>ignoreParams;
	var <window;

	var ndef;
	var params, paramViews;

	var play, volslider, volvalueBox;
	var header, parameterSection;
	var updateInfoFunc;

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
		paramViews = IdentityDictionary.new();

		window = Window.new(ndef.key);
		window.layout = VLayout.new(
			this.makeInfoSection(),
			this.makeTransportSection(),
			// parameterSection gets added here in makeParameterSection
		);

		window.view.children.do{ | c | c.font = if(c == header, headerFont, font) };

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
					{ this.makeParameterSection() }.defer;
				})
			})
		};

		specChangedFunc = { | obj ...args |
			{ this.makeParameterSection() }.defer;
		};

		ndefChangedFunc = if(limitUpdateRate > 0, {

			limitOrder = OrderedIdentitySet.new(8);
			limitDict = IdentityDictionary.new();
			limitScheduler = SkipJack.new({
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
				limitDict.put(key, args);
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
		var key;

		case
		{ what == \set } {
			key = args[0];

			if(key == \fadeTime, {
				updateInfoFunc.value(ndef)
			}, {
				if(params[key].notNil, {
					this.parameterChanged(key, args[1])
				})
			})
		}
		{ what == \vol } {
			volvalueBox.value_(args[0].max(0.0));
			volslider.value_(args[0]);
		}
		{ what == \play or: { what == \playN } } {
			play.value_(1);
			if(ndef.monitor.notNil and: { volslider.isNil }, {
				this.makeParameterSection()
			});
		}
		{ what == \monitor } {
			ndef.monitor.addDependant(ndefChangedFunc)
		}
		{ what == \source } {
			this.makeParameterSection()
		}
		{ what == \rebuild } {
			updateInfoFunc.value(ndef);
			this.makeParameterSection();
		}
		{ what == \bus } {
			updateInfoFunc.value(ndef)
		}
		{ what == \clear } {
			updateInfoFunc.value(ndef)
		}
		{ what == \stop or: { what == \pause } } {
			play.value_(0)
		}
		{ what == \resume } {
			play.value_(1)
		}
		//{ what == \map } {}
		//{ what == \unset } {}
		//{ what == \free } {}
	}

	parameterChanged { | key, val |
		var spec;

		case
		{ val.isNumber } {
			if(paramViews[key][\type] != \number, { this.makeParameterSection() });
			spec = params[key].value;
			paramViews[key][\numBox].value_(spec.constrain(val));
			paramViews[key][\slider].value_(spec.unmap(val));
		}
		{ val.isArray } {
			if(paramViews[key][\type] != \array, { this.makeParameterSection() });
			spec = params[key].value;
			paramViews[key][\textField].value_(val.collect{ | v, i |
				spec.wrapAt(i).constrain(v);
			});
		}
		{ val.isKindOf(Bus) } {
			if(paramViews[key][\type] != \bus, { this.makeParameterSection() });
			paramViews[key][\numBox].value_(val.index);
			paramViews[key][\text].string_(val);
		}
		{ val.isKindOf(Buffer) } {
			if(paramViews[key][\type] != \buffer, { this.makeParameterSection() });
			paramViews[key][\numBox].value_(val.bufnum);
			paramViews[key][\text].string_(val);
		}
		{ val.isKindOf(NodeProxy) } {
			if(paramViews[key][\type] != \proxy, { this.makeParameterSection() });
			if(val.isKindOf(Ndef), {
				val = "% (%, %)".format(val, val.rate, val.numChannels)
			});
			paramViews[key][\text].string_(val);
		}
		{
			"% parameter '%' not set".format(this.class, key).warn;
		}
	}

	makeInfoSection {
		var fadeTime, fadeTimeLabel, channels, rate;

		fadeTimeLabel = StaticText.new()
		.string_("fadeTime:");

		fadeTime = NumberBox.new()
		.clipLo_(0.0)
		.decimals_(2)
		.scroll_step_(0.1) // mouse
		.step_(0.1)        // keys
		.action_({ | obj |
			ndef.fadeTime = obj.value;
		});

		channels = StaticText.new();

		rate = StaticText.new();

		updateInfoFunc = { | ndef |
			fadeTime.value = ndef.fadeTime;
			channels.string = "channels: %".format(ndef.numChannels);
			rate.string = "rate: %".format(ndef.rate);
		};
		updateInfoFunc.value(ndef);

		header = StaticText.new().string_(ndef.key ? "");

		^VLayout.new(
			header,
			HLayout.new(fadeTimeLabel, [fadeTime, a: \left], channels, rate),
		)
	}

	makeTransportSection {
		var clear, send, scope, free, popup;

		play = Button.new()
		.states_([
			["play"],
			["stop", Color.black, Color.grey(0.5, 0.5)],
		])
		.action_({ | obj |
			if(obj.value == 1, {
				ndef.play
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
			ndef.free
		});

		popup = PopUpMenu.new()
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
				0, { this.defaults() },
				1, { this.randomize() },
				2, { this.vary() },
				3, { ndef.document },
				4, { ndef.asCode.postln },
			)
		})
		.keyDownAction_({ | obj, char |
			if(char == Char.ret, {
				obj.doAction
			})
		})
		.canFocus_(true)
		.fixedWidth_(25);

		^HLayout.new(
			play, clear, free, scope, send, popup
		)
	}

	makeParameterSection {
		params.do{ | spec | spec.removeDependant(specChangedFunc) };
		params.clear;

		ndef.controlKeysValues.pairsDo{ | key, val |
			var spec;

			spec = case
			{ val.isNumber } {
				(Spec.specs.at(key) ?? { ndef.specs.at(key) }).asSpec
			}
			{ val.isArray } {
				(Spec.specs.at(key) ?? { ndef.specs.at(key) }).asSpec.dup(val.size)
			}
			{ val.isKindOf(Bus) } {
				if(val.rate == \control, { \controlbus }, { \audiobus }).asSpec
			}
			{ val.isKindOf(Buffer) } {
				ControlSpec.new(0, Server.default.options.numBuffers - 1, 'lin', 1)
			}
			{ val.isKindOf(NodeProxy) } {
				nil.asSpec
			}
			{
				"% using generic spec for '%'".format(this.class, key).warn;
				nil.asSpec
			};

			// "Spec for paramname %: %".format(key, spec).postln;
			spec.addDependant(specChangedFunc);
			params.put(key, spec);
		};

		if(parameterSection.notNil, { parameterSection.close });
		parameterSection = this.makeParameterViews().resizeToHint;
		if(parameterSection.bounds.height > (Window.availableBounds.height * 0.5), {
			parameterSection = ScrollView.new().canvas_(parameterSection);
		});
		window.layout.add(parameterSection, 1);
		window.resizeToHint;
	}

	makeParameterViews {
		var view, vollabel, volLayout;

		view = View.new().layout_(VLayout.new());

		if(ndef.monitor.notNil and: { ndef.rate == \audio }, {
			volslider = Slider.new()
			.orientation_(\horizontal)
			.value_(ndef.vol)
			.action_({ | obj |
				ndef.vol_(obj.value);
				volvalueBox.value_(obj.value);
			});

			vollabel = StaticText.new
			.string_("vol");

			volvalueBox = NumberBox.new()
			.decimals_(4)
			.action_({ | obj |
				obj.value = obj.value.clip(0.0, 1.0);
				volslider.value_(obj.value);
				ndef.vol_(obj.value);
			})
			.value_(ndef.vol);

			volLayout = HLayout.new(
				[vollabel, s: 1],
				[volvalueBox, s: 1],
				[volslider, s: 4],
			);
			view.layout.add(volLayout);
		});

		paramViews.clear;
		params.sortedKeysValuesDo{ | key, spec |
			var layout, paramVal;
			var slider, valueBox, textField, staticText;

			layout = HLayout.new(
				[StaticText.new().string_(key), s: 1],
			);

			paramVal = ndef.get(key);

			case

			{ paramVal.isNumber } {

				slider = Slider.new()
				.orientation_(\horizontal)
				.value_(spec.unmap(paramVal))
				.action_({ | obj |
					var val = spec.map(obj.value);
					valueBox.value = val;
					ndef.set(key, val);
				});

				valueBox = NumberBox.new()
				.action_({ | obj |
					var val = spec.constrain(obj.value);
					slider.value_(spec.unmap(val));
					ndef.set(key, val);
				})
				.decimals_(4)
				.value_(spec.constrain(paramVal));

				// This is used to be able to fetch the sliders later when they need to be updated
				paramViews.put(key, (type: \number, slider: slider, numBox: valueBox));

				layout.add(valueBox, 1);
				layout.add(slider, 4);
			}

			{ paramVal.isArray } {

				textField = TextField.new()
				.action_({ | obj |
					var val = try{ obj.value.interpret };  // this time as a feature!
					if(val.notNil, {
						val = val.asArray.collect{ | v, i | spec.wrapAt(i).constrain(v) };
						obj.value = val;
						ndef.set(key, val);
					});
				})
				.value_(paramVal);

				paramViews.put(key, (type: \array, textField: textField));

				layout.add(textField, 5);
			}

			{ paramVal.isKindOf(Bus) } {

				valueBox = NumberBox.new()
				.action_({ | obj |
					var bus;
					var val = spec.constrain(obj.value).asInteger;
					obj.value = val;
					bus = Bus.new(paramVal.rate, val, paramVal.numChannels);
					ndef.set(key, bus);
				})
				.value_(paramVal.index);

				staticText = StaticText.new()
				.string_(paramVal);

				paramViews.put(key, (type: \bus, numBox: valueBox, text: staticText));

				layout.add(valueBox, 1);
				layout.add(staticText, 4);
			}

			{ paramVal.isKindOf(Buffer) } {

				valueBox = NumberBox.new()
				.action_({ | obj |
					var buf;
					var val = spec.constrain(obj.value).asInteger;
					obj.value = val;
					buf = Buffer.cachedBufferAt(paramVal.server, val);
					if(buf.notNil, {
						ndef.set(key, buf);
					});
				})
				.value_(paramVal.bufnum);

				staticText = StaticText.new()
				.string_(paramVal);

				paramViews.put(key, (type: \buffer, numBox: valueBox, text: staticText));

				layout.add(valueBox, 1);
				layout.add(staticText, 4);
			}

			{ paramVal.isKindOf(NodeProxy) } {

				if(paramVal.isKindOf(Ndef), {
					paramVal = "% (%, %)".format(paramVal, paramVal.rate, paramVal.numChannels)
				});

				staticText = StaticText.new()
				.string_(paramVal);

				paramViews.put(key, (type: \proxy, text: staticText));

				layout.add(staticText, 5);
			}

			{
				"% parameter '%' ignored".format(this.class, key).warn;
			};

			view.layout.add(layout)
		};

		view.children.do{ | c | c.font = font };

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

	defaults { | except = #[] |
		except = defaultIgnoreParams ++ ignoreParams ++ except;
		ndef.setDefaults(except)
	}

	close {
		^window.close()
	}

}
