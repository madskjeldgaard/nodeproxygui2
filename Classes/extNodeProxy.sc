// The only purpose of this class is to allow the user to set which parameter names to ignore in the randomizer
NodeProxyGuiIgnoreParams{
    classvar <>params;

    *initClass{
        params = params ? [\numChannels, \vol, \numOuts, \buffer, \feedback, \gain];
    }

}

+ NodeProxy {
    prFilteredParams{
        var ignoreParams = NodeProxyGuiIgnoreParams.params;
        var params = this.controlKeys.asArray.reject({ | paramName |
            // Ignore this parameter in the randomization if it is in the ignoreParams list
            var predicate = ignoreParams.includes(paramName.asSymbol) or: {
                // Does the parameter name end with one of the ignored parameters? If so, ignore it as well

                ignoreParams.any({|ignored|
                    paramName.asString.contains(ignored.asString)
                }) or: ignoreParams.any({|ignored|
                    // Check for same name but with first letter capitalized
                    ignored = ignored.asString;
                    ignored = ignored[0].toUpper ++ ignored[1..];
                    paramName.asString.contains(ignored.asString)
                });

            };

            predicate
        });
        ^params
    }

	randomizeAllParamsMapped{ | randmin = 0.0, randmax = 1.0 |
        var params = this.prFilteredParams();

		params.do{ | param |
			var val = rrand(randmin, randmax);
			var spec = Spec.specs.at(param);
			spec = if(spec.isNil, { [0.0,1.0].asSpec }, { spec });
			val = spec.map(val);
			this.set(param, val)
		}
	}

    setDefaults{
        var params = this.prFilteredParams();

        params.do{ | param |
			var spec = Spec.specs.at(param);
			spec = if(spec.isNil, { [0.0,1.0].asSpec }, { spec });
			this.set(param, spec.default)
		}
    }

	gui2{
		NodeProxyGui2.new(this);
	}
}
