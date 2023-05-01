+ Spec {

	*add { | name, args |
		var spec = args.asSpec;
		specs.put(name, spec);
		this.changed(\add, [name, spec]);
		^spec
	}

}
