+ NodeProxy {
	prFilteredParams{
		var ignoreParams = (NodeProxyGui2.ignoreParams ? []) ++ NodeProxyGui2.defaultIgnoreParams;
		^this.controlKeys.reject({ | paramName |

            var predicate = ignoreParams.any{|ignoreParam|
                ignoreParam.matchRegexp(paramName.asString)
            };

            // if(predicate){
            //     ("Ignoring param: " ++ paramName.asString ++ " because it matches ignoreparams" ).postln
            // };

            predicate

		})
	}

	randomizeAllParamsMapped{ | randmin = 0.0, randmax = 1.0 |
		var params = this.prFilteredParams();

		params.do{ | param |
			var val = rrand(randmin, randmax);
			var spec = Spec.specs.at(param).asSpec;
			val = spec.map(val);
			this.set(param, val)
		}
	}

	varyAllParamsMapped{ | deviation = 0.1 |
		var params = this.prFilteredParams();

		params.do{ | param |
			var val = this.get(param);
			var spec = Spec.specs.at(param).asSpec;
			val = (spec.unmap(val) + 0.0.gauss(deviation)).clip(0, 1);
			this.set(param, spec.map(val))
		}
	}

	setDefaults{
		var params = this.prFilteredParams();

		params.do{ | param |
			var spec = Spec.specs.at(param).asSpec;
			this.set(param, spec.default)
		}
	}

	gui2{
		^NodeProxyGui2.new(this)
	}
}
