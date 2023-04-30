+Spec {

	*add { arg name, args;
		var spec = args.asSpec;
		specs.put(name, spec);
		this.changed(\add, [name, spec]);
		^spec
	}

}
