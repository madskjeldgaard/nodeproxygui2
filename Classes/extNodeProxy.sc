+ NodeProxy {

	prFilteredParams { | except |
		var accepted = IdentityDictionary.new;

		except = except.asArray.collect{ | param | param.asSymbol };

		this.controlKeysValues.pairsDo({ | key, val |
			var ignore, spec;

			ignore = except.any{ | ignoreParam | ignoreParam == key };
			if(ignore.not, {
				spec = (this.specs.at(key) ?? { Spec.specs.at(key) }).asSpec;
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

	randomizeAllParamsMapped { | randmin = 0.0, randmax = 1.0, except = #[] |
		var params = this.prFilteredParams(except);

		params.keysValuesDo{ | key, spec |
			var val;

			if(spec.isArray, {
				val = spec.collect(_.map(rrand(randmin, randmax)))
			}, {
				val = spec.map(rrand(randmin, randmax))
			});

			this.set(key, val)
		}
	}

	varyAllParamsMapped { | deviation = 0.05, except = #[] |
		var params = this.prFilteredParams(except);

		params.keysValuesDo{ | key, spec |
			var val = this.get(key);

			if(spec.isArray, {
				val = spec.collect{ | s, i |
					s.map((s.unmap(val[i]) + 0.0.gauss(deviation)).clip(0, 1))
				}
			}, {
				val = spec.unmap(val) + 0.0.gauss(deviation);
				val = spec.map(val.clip(0, 1))
			});

			this.set(key, val)
		}
	}

	setDefaults { | except = #[] |
		var params = this.prFilteredParams(except);

		params.keysValuesDo{ | key, spec |
			var val;

			if(spec.isArray, {
				val = spec.collect(_.default)
			}, {
				val = spec.default
			});

			this.set(key, val)
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
