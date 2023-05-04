+ NodeProxy {

	prFilteredParams { | except |
		except = except.asArray;

		^this.controlKeys.reject({ | paramName |

			var predicate = except.any{ | ignoreParam |
				ignoreParam.asString.matchRegexp(paramName.asString)
			};

			predicate
		})
	}

	randomizeAllParamsMapped { | randmin = 0.0, randmax = 1.0, except = #[] |
		var params = this.prFilteredParams(except);

		params.do{ | param |
			var val = rrand(randmin, randmax);
			var spec = Spec.specs.at(param).asSpec;
			val = spec.map(val);
			this.set(param, val)
		}
	}

	varyAllParamsMapped { | deviation = 0.1, except = #[] |
		var params = this.prFilteredParams(except);

		params.do{ | param |
			var val = this.get(param);
			var spec = Spec.specs.at(param).asSpec;
			val = (spec.unmap(val) + 0.0.gauss(deviation)).clip(0, 1);
			this.set(param, spec.map(val))
		}
	}

	setDefaults { | except = #[] |
		var params = this.prFilteredParams(except);

		params.do{ | param |
			var spec = Spec.specs.at(param).asSpec;
			this.set(param, spec.default)
		}
	}

	vol_ { | val |
		super.vol_(val);
		this.changed(\vol, [val]);
	}

	gui2 { | limitUpdateRate = 0 |
		^NodeProxyGui2.new(this, limitUpdateRate)
	}
}
