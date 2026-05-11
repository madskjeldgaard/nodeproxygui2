NodeProxyGui2 {

	classvar <>defaultExcludeParams = #[];
	classvar <>defaultIgnoreParams = #[];

	var <nodeProxy;
	var collapseArrays;
	var params, paramViews;

	var prExcludeParams;
	var <>ignoreParams;
	var <window;

	var play, volslider, volvalueBox, transportButtons;
	var header, <parameterSection;
	var updateInfoFunc;
	var <contentView;

	var <font, <headerFont, <headerHeight;
	var <paramSectionMaxHeight, <transportButtonMinWidth;
	var <sliderHeight, <sliderWidth, <numberBoxWidth, <labelWidth;

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
		contentView = View.new();
		contentView.layout = VLayout.new(
			// parameterSection gets added here in makeParameterSection
		);
		window.layout = VLayout(contentView).margins_(0);

		contentView.layout.add(this.makeInfoSection());
		contentView.layout.add(this.makeTransportSection());

		contentView.children.do{ | c | c.font = if(c == header, headerFont, font) };
		headerHeight = contentView.sizeHint.height;
		paramSectionMaxHeight = nil;
		transportButtonMinWidth = 65;

		this.setUpDependencies(limitUpdateRate.max(0));

		// Defer so the caller has a chance to add contentView to a host
		// layout before makeParameterSection checks contentView.parent.
		{
			this.makeParameterSection();
			if(show) { window.front };
		}.defer;
	}

	asView { ^contentView }

	// Override the maximum height of the parameter scroll area.
	// Call this before excludeParams_ when embedding NPG2 inside a
	// height-constrained host layout. The change takes effect on the
	// next makeParameterSection call.
	paramSectionMaxHeight_ { | height |
		paramSectionMaxHeight = height.asInteger;
		{ this.makeParameterSection }.defer;
	}

	// Override the minimum width of all transport buttons (play, clear, free, scope, send).
	// Can be called after creation; takes effect immediately via stored button references.
	transportButtonMinWidth_ { | width |
		transportButtonMinWidth = width.asInteger;
		{ transportButtons.do(_.minWidth_(transportButtonMinWidth)) }.defer;
	}

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
		{ what == \unmap } {
			nodeProxy.getKeysValues(args).do{ | arr |
				this.parameterChanged(*arr)
			}
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
		{ val.isKindOf(Symbol) and: { val.asString[0] == $c or: { val.asString[0] == $a } } } {
			if(paramViews[key][\type] != \map, { this.makeParameterSection() });
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
		var proxyButtons, popup;

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
		.value_(nodeProxy.isMonitoring.binaryValue)
		.minWidth_(transportButtonMinWidth);

		proxyButtons = #[\clear, \free, \scope, \send].collect { |sym|
			Button.new()
			.states_([[sym.asString]])
			.action_({ nodeProxy.perform(sym) })
			.minWidth_(transportButtonMinWidth)
		};

		transportButtons = [play] ++ proxyButtons;

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
				3, { this.document },
				4, { this.asCode.postln },
			)
		})
		.keyDownAction_({ | obj, char |
			if(char == Char.ret, {
				obj.doAction
			})
		})
		.canFocus_(true)
		.fixedWidth_(25);

		^HLayout.new(*([play] ++ proxyButtons ++ [popup]))
	}

	makeParameterSection {
		var excluded = defaultExcludeParams ++ prExcludeParams;
		var numParams = params.flatSize;
		var sectionSize, innerView, innerH, windowTargetH;

		params.do{ | spec | spec.removeDependant(specChangedFunc) };
		params.clear;

		nodeProxy.controlKeysValues.pairsDo{ | key, val |
			var spec;

			if(this.paramPresentInArray(key, excluded).not, {

				spec = case
				{ val.isNumber } {
					(nodeProxy.specs.at(key) ?? { nodeProxy.respondsTo(\getSpec).if { nodeProxy.getSpec(key) } } ?? { Spec.specs.at(key) }).asSpec
				}
				{ val.isArray } {
					(nodeProxy.specs.at(key) ?? { nodeProxy.respondsTo(\getSpec).if { nodeProxy.getSpec(key) } } ?? { Spec.specs.at(key) }).asSpec.dup(val.size)
				}
				{ val.isKindOf(Bus) } {
					if(val.rate == \control, { \controlbus }, { \audiobus }).asSpec
				}
				{ val.isKindOf(Buffer) } {
					ControlSpec.new(0, Server.default.options.numBuffers - 1, 'lin', 1)
				}
				{ val.isKindOf(NodeProxy) or: { val.isKindOf(Symbol) } } {
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
		innerView = this.makeParameterViews();
		sectionSize = innerView.sizeHint;
		innerH = sectionSize.height;

		paramSectionMaxHeight.notNil.if({
			// Pinned path: cap viewport at paramSectionMaxHeight; scroll if needed.
			// Pin the inner view height BEFORE canvas_() so the canvas does not
			// collapse to the zero-sized viewport of the freshly created ScrollView.
			innerView.fixedHeight_(innerH);
			parameterSection = ScrollView.new().autohidesScrollers_(true).canvas_(innerView);
			parameterSection.fixedHeight_(innerH.min(paramSectionMaxHeight));
			contentView.layout.add(parameterSection);

			windowTargetH = headerHeight + innerH.min(paramSectionMaxHeight) + 4;
			// If embedded, the host controls contentView's height via fixedHeight_.
			if(contentView.parent.isNil, {
				contentView.fixedHeight_(windowTargetH);
				contentView.maxHeight_(windowTargetH);
			});

			if(window.view.parent.isNil and: { contentView.parent.isNil }, {
				window.view.bounds = window.view.bounds.resizeTo(
					window.view.bounds.width,
					windowTargetH.min(Window.availableBounds.height)
				);
			});
			// Deferred re-pin to counter Qt layout engine sizeHint contamination
			// across multiple windows created in sequence.
			if(contentView.parent.isNil, {
				{
					innerView.fixedHeight_(innerH);
					contentView.fixedHeight_(windowTargetH);
					contentView.maxHeight_(windowTargetH);
				}.defer(0.07);
			});
		}, {
			// Upstream stretch path: sliders grow to fill available vertical
			// space. No height pinning anywhere; ScrollView added with stretch
			// factor 1 so it claims surplus space inside the layout.
			parameterSection = ScrollView.new().canvas_(innerView);
			contentView.layout.add(parameterSection, 1);
		});
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
			sliderHeight !? { volslider.fixedHeight_(sliderHeight) };
			sliderWidth !? { volslider.fixedWidth_(sliderWidth) };

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
		nodeProxy.getKeysValues.select{ | kv | params.includesKey(kv[0]) }.do{ | keyVal |
			var key = keyVal[0];
			var spec = params[key];
			var layout, paramVal;
			var slider, valueBox, textField, staticText;
			var sliders, valueBoxes;

			paramVal = nodeProxy.get(key);

			layout = HLayout.new(
				if(collapseArrays.not and: { paramVal.isArray and: { paramVal.every(_.isNumber) } }, {
					var labels = paramVal.collect { | v, n |
						var lbl = StaticText.new().string_(key++"["++n++"]");
						labelWidth !? { lbl.fixedWidth_(labelWidth) };
						lbl
					};
					[VLayout(*labels), s: 1]

				}, {
					var lbl = StaticText.new().string_(key);
					labelWidth !? { lbl.fixedWidth_(labelWidth) };
					[lbl, s: 1]
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
				sliderHeight !? { slider.fixedHeight_(sliderHeight) };
				sliderWidth !? { slider.fixedWidth_(sliderWidth) };

				valueBox = NumberBox.new()
				.action_({ | obj |
					var val = spec.constrain(obj.value);
					slider.value_(spec.unmap(val));
					nodeProxy.set(key, val);
				})
				.decimals_(4)
				.value_(spec.constrain(paramVal));
				numberBoxWidth !? { valueBox.fixedWidth_(numberBoxWidth) };

				// This is used to be able to fetch the sliders later when they need to be updated
				paramViews.put(key, (type: \number, slider: slider, numBox: valueBox));

				layout.add(valueBox, 1);
				layout.add(slider, 4);
			}

			{ collapseArrays.not and: { paramVal.isArray and: { paramVal.every(_.isNumber) } } } {

				sliders = paramVal.collect { |val, n|
					var sl = Slider.new()
					.orientation_(\horizontal)
					.value_(spec.wrapAt(n).unmap(val))
					.action_({ | obj |
						var val = spec.wrapAt(n).map(obj.value);
						valueBoxes[n].value = val;
						nodeProxy.seti(key, n, val);
					});
					sliderHeight !? { sl.fixedHeight_(sliderHeight) };
					sliderWidth !? { sl.fixedWidth_(sliderWidth) };
					sl
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
				numberBoxWidth !? { valueBoxes.do(_.fixedWidth_(numberBoxWidth)) };

				paramViews.put(key, (type: \array, sliders: sliders, numBoxes: valueBoxes));
				layout.add(VLayout(*valueBoxes.collect{|v| [nil, v, nil]}.flat), 1);
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

			{ paramVal.isKindOf(Symbol) } {

				staticText = StaticText.new()
				.string_(paramVal);

				paramViews.put(key, (type: \map, text: staticText));

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
		font = font ?? { Font.monospace(14, bold: false, italic: false) };
		headerFont = headerFont ?? { Font.sansSerif(font.size * 2, bold: true, italic: false) };
	}

	font_ { | aFont |
		font = aFont;
		{ contentView.children.do{ | c | if(c != header) { c.font = font } };
		  this.makeParameterSection() }.defer;
	}

	headerFont_ { | aFont |
		headerFont = aFont;
		{ if(header.notNil) { header.font = headerFont } }.defer;
	}

	sliderHeight_ { | height |
		sliderHeight = height;
		{ this.makeParameterSection() }.defer;
	}

	sliderWidth_ { | width |
		sliderWidth = width;
		{ this.makeParameterSection() }.defer;
	}

	numberBoxWidth_ { | width |
		numberBoxWidth = width;
		{ this.makeParameterSection() }.defer;
	}

	labelWidth_ { | width |
		labelWidth = width;
		{ this.makeParameterSection() }.defer;
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
				spec = (nodeProxy.specs.at(key) ?? { nodeProxy.respondsTo(\getSpec).if { nodeProxy.getSpec(key) } } ?? { Spec.specs.at(key) }).asSpec;
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

	asCode { | includeMonitor = false, envir |
		var nameStr, accessStr = "a", isAnon;
		var str, position, map, ignored;

		envir = envir ? currentEnvironment;

		nameStr = envir.use { nodeProxy.asCompileString };
		isAnon = nameStr.beginsWith("a = ");
		if(isAnon.not, { accessStr = nameStr });

		str = nodeProxy.asCode(false, includeMonitor, envir);

		position = str.findBackwards(");", true);
		map = ProxyNodeMap();
		ignored = defaultExcludeParams ++ prExcludeParams;

		nodeProxy.controlKeysValues.pairsDo({ | key, val |
			if(ignored.includes(key).not, {
				map.set(key, val)
			});
		});
		prExcludeParams.do{|x| map.removeAt(x)};

		^str.insert(position, map.asCode(accessStr, true))
	}

	document { | includeMonitor = false |
		var nameStr = nodeProxy.class.asString ++"_" ++ nodeProxy.key;
		^this.asCode(includeMonitor).newTextWindow("document-" ++ nameStr)
	}
}
