NodeProxyGui2 {

	classvar <>defaultExcludeParams = #[];
	classvar <>defaultIgnoreParams = #[];

	var <nodeProxy;
	var collapseArrays;
	var params, paramViews;

	var prExcludeParams;
	var <>ignoreParams;
	var <window;

	var play, volslider, volvalueBox;
	var header, parameterSection;
	var updateInfoFunc;

	var font, headerFont;

	var nodeProxyChangedFunc, specChangedFunc;

	// this is a normal constructor method
	*new { | nodeproxy, limitUpdateRate = 0, show = true, collapseArrays = false |
		^super.newCopyArgs(nodeproxy, collapseArrays).init(limitUpdateRate, show)
	}

	init { | limitUpdateRate, show |

		this.initFonts();

		params = IdentityDictionary.new();
		paramViews = IdentityDictionary.new();

		window = Window.new(nodeProxy.key);
		window.layout = VLayout.new(
			this.makeInfoSection(),
			this.makeTransportSection(),
			// parameterSection gets added here in makeParameterSection
		);

		window.view.children.do{ | c | c.font = if(c == header, headerFont, font) };

		this.setUpDependencies(limitUpdateRate.max(0));

		this.makeParameterSection();

		if(show) {
			window.front;
		}
	}

	asView { ^window.asView }

	setUpDependencies { | limitUpdateRate |
		var limitOrder, limitDict, limitScheduler;
		var specAddedFunc;

		specAddedFunc = { | obj ...args |
			var key, spec;
			if(args[0] == \add, {
				key = args[1][0];
				spec = args[1][1];
				if(params[key].notNil and: { params[key] != spec }, {
					{ this.makeParameterSection() }.defer;
				})
			})
		};

		specChangedFunc = { | obj ...args |
			{ this.makeParameterSection() }.defer;
		};

		nodeProxyChangedFunc = if(limitUpdateRate > 0, {

			limitOrder = OrderedIdentitySet.new(8);
			limitDict = IdentityDictionary.new();
			limitScheduler = SkipJack.new({
				if(limitOrder.size > 0, {
					limitOrder.do{ | key | this.nodeProxyChanged(*limitDict[key]) };
					limitOrder.clear;
					limitDict.clear;
				});
			}, limitUpdateRate, clock: AppClock);
			{ | obj ...args |
				var key = args[0];
				if(key == \set) {
					args[1].pairsDo { | paramKey, v |
						key = (key ++ paramKey).asSymbol;
						limitOrder.add(key);
						limitDict.put(key, [\set, [paramKey, v]]);
					}
				} {
					limitOrder.add(key);
					limitDict.put(key, args);
				}
			}

		}, {

			{ | obj ...args | { this.nodeProxyChanged(*args) }.defer }

		});

		Spec.addDependant(specAddedFunc);
		nodeProxy.addDependant(nodeProxyChangedFunc);
		if(nodeProxy.monitor.notNil, {
			nodeProxy.monitor.addDependant(nodeProxyChangedFunc)
		});

		window.onClose = {
			limitScheduler.stop;
			nodeProxy.monitor.removeDependant(nodeProxyChangedFunc);
			nodeProxy.removeDependant(nodeProxyChangedFunc);
			Spec.removeDependant(specAddedFunc);
			params.do{ | spec | spec.removeDependant(specChangedFunc) };
		};
	}

	nodeProxyChanged { | what, args |
		var key;

		case
		{ what == \set } {
			key = args[0];

			if(key == \fadeTime, {
				updateInfoFunc.value(nodeProxy)
			}, {
				args.pairsDo { | paramKey, val |
					if(params[paramKey].notNil, {
						this.parameterChanged(paramKey, val)
					})
				}
			})
		}
		{ what == \vol } {
			if(volslider.notNil, {
				volvalueBox.value_(args[0].max(0.0));
				volslider.value_(args[0]);
			});
		}
		{ what == \play or: { what == \playN } } {
			play.value_(1);
			if(nodeProxy.monitor.notNil and: { volslider.isNil }, {
				this.makeParameterSection()
			});
		}
		{ what == \monitor } {
			nodeProxy.monitor.addDependant(nodeProxyChangedFunc)
		}
		{ what == \source } {
			this.makeParameterSection()
		}
		{ what == \rebuild } {
			updateInfoFunc.value(nodeProxy);
			this.makeParameterSection();
		}
		{ what == \bus } {
			updateInfoFunc.value(nodeProxy)
		}
		{ what == \clear } {
			updateInfoFunc.value(nodeProxy)
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
			if(collapseArrays.not and: { val.every(_.isNumber) }, {
				val.do { | v, i |
					paramViews[key][\numBoxes].wrapAt(i).value = spec.wrapAt(i).constrain(v);
					paramViews[key][\sliders].wrapAt(i).value = spec.wrapAt(i).unmap(v)
				};
			}, {
				paramViews[key][\textField].value_(val.collect{ | v, i |
					spec.wrapAt(i).constrain(v);
				});
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
			nodeProxy.fadeTime = obj.value;
		});

		channels = StaticText.new();

		rate = StaticText.new();

		updateInfoFunc = { | np |
			fadeTime.value = np.fadeTime;
			channels.string = "channels: %".format(np.numChannels);
			rate.string = "rate: %".format(np.rate);
		};
		updateInfoFunc.value(nodeProxy);

		if(nodeProxy.key.notNil, {
			header = StaticText.new().string_(nodeProxy.key)
		});

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
				nodeProxy.play
			}, {
				nodeProxy.stop
			})
		})
		.value_(nodeProxy.isMonitoring.binaryValue);

		clear = Button.new()
		.states_(#[
			["clear"]
		])
		.action_({ | obj |
			nodeProxy.clear
		});

		send = Button.new()
		.states_(#[
			["send"]
		])
		.action_({ | obj |
			nodeProxy.send
		});

		scope = Button.new()
		.states_(#[
			["scope"]
		])
		.action_({ | obj |
			nodeProxy.scope
		});

		free = Button.new()
		.states_(#[
			["free"]
		])
		.action_({ | obj |
			nodeProxy.free
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
				3, { nodeProxy.document },
				4, { nodeProxy.asCode.postln },
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
		var excluded = defaultExcludeParams ++ prExcludeParams;

		params.do{ | spec | spec.removeDependant(specChangedFunc) };
		params.clear;

		nodeProxy.controlKeysValues.pairsDo{ | key, val |
			var spec;

			if(this.paramPresentInArray(key, excluded).not, {

				spec = case
				{ val.isNumber } {
					(nodeProxy.specs.at(key) ?? { Spec.specs.at(key) }).asSpec
				}
				{ val.isArray } {
					(nodeProxy.specs.at(key) ?? { Spec.specs.at(key) }).asSpec.dup(val.size)
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
			});
		};

		if(parameterSection.notNil, { parameterSection.remove });
		parameterSection = this.makeParameterViews().resizeToHint;
		if(parameterSection.bounds.height > (Window.availableBounds.height * 0.5), {
			parameterSection = ScrollView.new().canvas_(parameterSection);
		});
		window.layout.add(parameterSection, 1);
		{ window.view.resizeToHint }.defer(0.07);
	}

	makeParameterViews {
		var view, vollabel, volLayout;

		view = View.new().layout_(VLayout.new());

		if(nodeProxy.monitor.notNil and: { nodeProxy.rate == \audio }, {
			volslider = Slider.new()
			.orientation_(\horizontal)
			.value_(nodeProxy.vol)
			.action_({ | obj |
				nodeProxy.vol_(obj.value);
				volvalueBox.value_(obj.value);
			});

			vollabel = StaticText.new
			.string_("vol");

			volvalueBox = NumberBox.new()
			.decimals_(4)
			.action_({ | obj |
				obj.value = obj.value.clip(0.0, 1.0);
				volslider.value_(obj.value);
				nodeProxy.vol_(obj.value);
			})
			.value_(nodeProxy.vol);

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

			paramVal = nodeProxy.get(key);

			layout = HLayout.new(
				if(collapseArrays.not and: { paramVal.isArray and: { paramVal.every(_.isNumber) } }, {
					[VLayout(*paramVal.collect { | v, n |
						StaticText.new().string_(key++"["++n++"]")
					}), s: 1]

				}, {
					[StaticText.new().string_(key), s: 1]
				});
			);

			case

			{ paramVal.isNumber } {

				slider = Slider.new()
				.orientation_(\horizontal)
				.value_(spec.unmap(paramVal))
				.action_({ | obj |
					var val = spec.map(obj.value);
					valueBox.value = val;
					nodeProxy.set(key, val);
				});

				valueBox = NumberBox.new()
				.action_({ | obj |
					var val = spec.constrain(obj.value);
					slider.value_(spec.unmap(val));
					nodeProxy.set(key, val);
				})
				.decimals_(4)
				.value_(spec.constrain(paramVal));

				// This is used to be able to fetch the sliders later when they need to be updated
				paramViews.put(key, (type: \number, slider: slider, numBox: valueBox));

				layout.add(valueBox, 1);
				layout.add(slider, 4);
			}

			{ collapseArrays.not and: { paramVal.isArray and: { paramVal.every(_.isNumber) } } } {

				var sliders, valueBoxes;
				sliders = paramVal.collect { |val, n|
					Slider.new()
					.orientation_(\horizontal)
					.value_(spec.wrapAt(n).unmap(val))
					.action_({ | obj |
						var val = spec.wrapAt(n).map(obj.value);
						valueBoxes[n].value = val;
						nodeProxy.seti(key, n, val);
					});
				};

				valueBoxes = paramVal.collect { |pVal, n|
					NumberBox.new()
					.action_({ | obj |
						var val = spec.wrapAt(n).constrain(obj.value);
						sliders[n].value_(spec.wrapAt(n).unmap(val));
						nodeProxy.seti(key, n, val);
					})
					.decimals_(4)
					.value_(spec.wrapAt(n).constrain(pVal));
				};

				paramViews.put(key, (type: \array, sliders: sliders, numBoxes: valueBoxes));
				layout.add(VLayout(*valueBoxes), 1);
				layout.add(VLayout(*sliders), 4);
			}

			{ paramVal.isArray } {

				textField = TextField.new()
				.action_({ | obj |
					var val = try{ obj.value.interpret };  // this time as a feature!
					if(val.notNil, {
						val = val.asArray.collect{ | v, i | spec.wrapAt(i).constrain(v) };
						obj.value = val;
						nodeProxy.set(key, val);
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
					nodeProxy.set(key, bus);
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
						nodeProxy.set(key, buf);
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

	randomize { | randmin = 0.0, randmax = 1.0 |
		this.filteredParamsDo{ | val, spec |
			spec.map(rrand(randmin, randmax))
		}
	}

	vary { | deviation = 0.1 |
		this.filteredParamsDo{ | val, spec |
			spec.map((spec.unmap(val) + 0.0.gauss(deviation)).clip(0, 1))
		}
	}

	excludeParams { ^prExcludeParams }

	excludeParams_ {| value |
		prExcludeParams = value;
		{ this.makeParameterSection() }.defer;
	}

	defaults {
		this.filteredParamsDo{ | val, spec |
			spec.default
		}
	}

	ndef { ^nodeProxy }

	close {
		^window.close()
	}

	filteredParamsDo { | func |
		this.filteredParams.keysValuesDo{ | key, spec |
			var val;

			val = nodeProxy.get(key);
			if(val.isArray, {
				val = val.collect{ | v, n | func.value(v, spec.wrapAt(n)) }
			}, {
				val = func.value(val, spec)
			});

			nodeProxy.set(key, val)
		}
	}

	filteredParams {
		var accepted = IdentityDictionary.new;
		var ignored;

		ignored = defaultIgnoreParams ++ ignoreParams ++ defaultExcludeParams ++ prExcludeParams;

		nodeProxy.controlKeysValues.pairsDo({ | key, val |
			var spec;

			if(this.paramPresentInArray(key, ignored).not, {
				spec = (nodeProxy.specs.at(key) ?? { Spec.specs.at(key) }).asSpec;
				if(val.isNumber, {
					accepted.put(key, spec)
				}, {
					if(val.isArray, {
						accepted.put(key, spec.dup(val.size))
					})
					// Buffer and Bus values are ignored
				})
			})
		});

		^accepted
	}

	paramPresentInArray { | key, array |
		^array.any{ | param |
			if(param.isString and: { param.indexOf($*).notNil }, {
				param.replace("*", ".*").addFirst($^).add($$).matchRegexp(key.asString)
			}, {
				param.asSymbol == key
			})
		}
	}
}
